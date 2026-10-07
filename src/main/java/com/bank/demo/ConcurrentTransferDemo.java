package com.bank.demo;

import com.bank.concurrent.LockManager;
import com.bank.dao.AccountDao;
import com.bank.dao.TransactionDao;
import com.bank.exception.BankingException;
import com.bank.model.Account;
import com.bank.model.AccountFactory;
import com.bank.model.SavingsAccount;
import com.bank.model.Transaction;
import com.bank.model.TxnStatus;
import com.bank.model.TxnType;
import com.bank.service.SettingsProvider;
import com.bank.service.TransferService;
import com.bank.util.DBUtil;
import com.bank.util.Money;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;

/**
 * Concurrency stress simulation demonstrating deadlock-free, high-throughput fund transfers across multiple accounts.
 * <p>
 * Specifications:
 * <ul>
 *   <li>Initial state: 4 bank accounts, each seeded with $10,000.00 (Total system money: $40,000.00).</li>
 *   <li>Workload: 50 concurrent worker threads processing 500 randomized fund transfers via an {@link ExecutorService}.</li>
 *   <li>Invariants:
 *     <ol>
 *       <li>Conservation of money: Total system balance must strictly equal $40,000.00 at all times.</li>
 *       <li>No negative balance on any account.</li>
 *       <li>Tracks and reports exact counts of SUCCESS and FAILED transactions.</li>
 *     </ol>
 *   </li>
 * </ul>
 * </p>
 */
public class ConcurrentTransferDemo {

    public static final int DEFAULT_THREAD_COUNT = 50;
    public static final int DEFAULT_TRANSFER_COUNT = 500;
    public static final BigDecimal INITIAL_ACCOUNT_BALANCE = new BigDecimal("10000.00");

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println("            ONLINE BANKING SYSTEM - CONCURRENT TRANSFER DEMO");
        System.out.println("================================================================================");

        // Self-contained in-memory DAO environment ensuring standalone execution without MySQL dependency
        DemoAccountDao demoAccountDao = new DemoAccountDao();
        DemoTransactionDao demoTransactionDao = new DemoTransactionDao();
        DemoSettingsProvider demoSettings = new DemoSettingsProvider();

        setupMockDataSource(demoAccountDao, demoTransactionDao);

        List<String> accountNos = List.of(
                "100000000001",
                "100000000002",
                "100000000003",
                "100000000004"
        );

        for (String accNo : accountNos) {
            demoAccountDao.save(AccountFactory.createSavingsAccount(
                    accNo, 100L, INITIAL_ACCOUNT_BALANCE, new BigDecimal("0.00")));
        }

        TransferService transferService = new TransferService(
                demoAccountDao, demoTransactionDao, demoSettings, new LockManager(), txn -> {});

        SimulationResult result = runSimulation(transferService, demoAccountDao, accountNos,
                DEFAULT_THREAD_COUNT, DEFAULT_TRANSFER_COUNT);

        printResults(result);

        // Verification assertions
        if (result.getTotalEndingBalance().compareTo(result.getTotalStartingBalance()) != 0) {
            throw new AssertionError("Conservation of money violated! Expected: "
                    + result.getTotalStartingBalance() + ", Actual: " + result.getTotalEndingBalance());
        }
        for (Account acc : result.getFinalAccounts()) {
            if (Money.isNegative(acc.getBalance())) {
                throw new AssertionError("Negative balance detected on account " + acc.getAccountNo());
            }
        }

        System.out.println("\nAll concurrency invariants successfully verified!");
        System.out.println("================================================================================");
    }

    /**
     * Executes the concurrent transfer stress test.
     *
     * @param transferService transfer service instance
     * @param accountDao account DAO to read balances
     * @param accountNos list of participating account numbers
     * @param threadCount thread pool size
     * @param totalTransfers total transfers to dispatch
     * @return structured simulation results
     */
    public static SimulationResult runSimulation(TransferService transferService, AccountDao accountDao,
                                                List<String> accountNos, int threadCount, int totalTransfers)
            throws InterruptedException {

        BigDecimal totalStart = BigDecimal.ZERO;
        for (String no : accountNos) {
            Account acc = accountDao.findById(no).orElseThrow();
            totalStart = Money.add(totalStart, acc.getBalance());
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finishSignal = new CountDownLatch(totalTransfers);
        Random random = new Random(42);

        for (int i = 0; i < totalTransfers; i++) {
            int fromIdx = random.nextInt(accountNos.size());
            int toIdx;
            do {
                toIdx = random.nextInt(accountNos.size());
            } while (toIdx == fromIdx);

            String fromAcc = accountNos.get(fromIdx);
            String toAcc = accountNos.get(toIdx);
            BigDecimal amount = Money.of(BigDecimal.valueOf(10 + random.nextInt(490)));

            executor.submit(() -> {
                try {
                    startSignal.await();
                    transferService.transfer(fromAcc, toAcc, amount, "Random Stress Transfer");
                    successCount.incrementAndGet();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    failCount.incrementAndGet();
                } catch (BankingException | RuntimeException e) {
                    failCount.incrementAndGet();
                } finally {
                    finishSignal.countDown();
                }
            });
        }

        long startNs = System.nanoTime();
        startSignal.countDown();
        finishSignal.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        List<Account> finalAccounts = new ArrayList<>();
        BigDecimal totalEnd = BigDecimal.ZERO;
        for (String no : accountNos) {
            Account acc = accountDao.findById(no).orElseThrow();
            finalAccounts.add(acc);
            totalEnd = Money.add(totalEnd, acc.getBalance());
        }

        return new SimulationResult(totalStart, totalEnd, successCount.get(), failCount.get(),
                finalAccounts, elapsedMs);
    }

    private static void printResults(SimulationResult res) {
        System.out.printf("Processed %d transfers in %d ms%n",
                res.getSuccessCount() + res.getFailCount(), res.getElapsedMs());
        System.out.println("  --------------------------------------------------");
        System.out.printf("  SUCCESSFUL Transfers: %d%n", res.getSuccessCount());
        System.out.printf("  FAILED Transfers:     %d%n", res.getFailCount());
        System.out.println("  --------------------------------------------------");
        System.out.println("  Final Account Balances:");
        for (Account a : res.getFinalAccounts()) {
            System.out.printf("    Account %s: $%s (Status: %s)%n",
                    a.getAccountNo(), Money.format(a.getBalance()), a.getStatus());
        }
        System.out.println("  --------------------------------------------------");
        System.out.printf("  Total Starting Money: $%s%n", Money.format(res.getTotalStartingBalance()));
        System.out.printf("  Total Ending Money:   $%s%n", Money.format(res.getTotalEndingBalance()));
        System.out.printf("  Money Delta:          $%s (Invariant preserved)%n",
                Money.format(res.getTotalEndingBalance().subtract(res.getTotalStartingBalance())));
    }

    /**
     * Provisions a lightweight mock DataSource routing transactional commit/rollback to demo DAOs.
     */
    public static void setupMockDataSource(DemoAccountDao accountDao, DemoTransactionDao txnDao) {
        DataSource mockDs = (DataSource) Proxy.newProxyInstance(
                ConcurrentTransferDemo.class.getClassLoader(),
                new Class<?>[]{DataSource.class},
                (proxy, method, args) -> {
                    if ("getConnection".equals(method.getName())) {
                        AtomicBoolean closed = new AtomicBoolean(false);
                        AtomicBoolean autoCommit = new AtomicBoolean(true);

                        return Proxy.newProxyInstance(
                                ConcurrentTransferDemo.class.getClassLoader(),
                                new Class<?>[]{Connection.class},
                                (cProxy, cMethod, cArgs) -> {
                                    String name = cMethod.getName();
                                    switch (name) {
                                        case "setAutoCommit":
                                            autoCommit.set((Boolean) cArgs[0]);
                                            return null;
                                        case "getAutoCommit":
                                            return autoCommit.get();
                                        case "commit":
                                            accountDao.commit((Connection) cProxy);
                                            txnDao.commit((Connection) cProxy);
                                            return null;
                                        case "rollback":
                                            accountDao.rollback((Connection) cProxy);
                                            txnDao.rollback((Connection) cProxy);
                                            return null;
                                        case "close":
                                            closed.set(true);
                                            return null;
                                        case "isClosed":
                                            return closed.get();
                                        case "hashCode":
                                            return System.identityHashCode(cProxy);
                                        case "equals":
                                            return cProxy == cArgs[0];
                                        case "toString":
                                            return "DemoMockConnection@" + Integer.toHexString(System.identityHashCode(cProxy));
                                        default:
                                            return null;
                                    }
                                }
                        );
                    }
                    return null;
                }
        );

        DBUtil.setDataSource(mockDs);
    }

    /**
     * DTO containing stress simulation output metrics.
     */
    public static class SimulationResult {
        private final BigDecimal totalStartingBalance;
        private final BigDecimal totalEndingBalance;
        private final int successCount;
        private final int failCount;
        private final List<Account> finalAccounts;
        private final long elapsedMs;

        public SimulationResult(BigDecimal totalStartingBalance, BigDecimal totalEndingBalance,
                                int successCount, int failCount, List<Account> finalAccounts, long elapsedMs) {
            this.totalStartingBalance = totalStartingBalance;
            this.totalEndingBalance = totalEndingBalance;
            this.successCount = successCount;
            this.failCount = failCount;
            this.finalAccounts = Collections.unmodifiableList(finalAccounts);
            this.elapsedMs = elapsedMs;
        }

        public BigDecimal getTotalStartingBalance() { return totalStartingBalance; }
        public BigDecimal getTotalEndingBalance() { return totalEndingBalance; }
        public int getSuccessCount() { return successCount; }
        public int getFailCount() { return failCount; }
        public List<Account> getFinalAccounts() { return finalAccounts; }
        public long getElapsedMs() { return elapsedMs; }
    }

    /**
     * Self-contained in-memory account DAO for standalone demo execution.
     */
    public static class DemoAccountDao extends AccountDao {
        private final Map<String, Account> storage = new ConcurrentHashMap<>();
        private final Map<Connection, Map<String, Account>> uncommitted = new ConcurrentHashMap<>();

        @Override
        public Optional<Account> findById(Connection conn, String accountNo) {
            return findById(accountNo);
        }

        @Override
        public Optional<Account> findById(String accountNo) {
            if (accountNo == null) return Optional.empty();
            return Optional.ofNullable(copy(storage.get(accountNo)));
        }

        @Override
        public Optional<Account> findByIdForUpdate(Connection conn, String accountNo) {
            if (accountNo == null) return Optional.empty();
            if (conn != null) {
                Map<String, Account> staged = uncommitted.get(conn);
                if (staged != null && staged.containsKey(accountNo)) {
                    return Optional.of(copy(staged.get(accountNo)));
                }
            }
            return Optional.ofNullable(copy(storage.get(accountNo)));
        }

        @Override
        public Account save(Connection conn, Account account) {
            storage.put(account.getAccountNo(), copy(account));
            return account;
        }

        @Override
        public boolean updateBalance(Connection conn, Account account) {
            if (account == null || account.getAccountNo() == null) return false;
            Account c = copy(account);
            if (conn != null) {
                try {
                    if (!conn.getAutoCommit()) {
                        uncommitted.computeIfAbsent(conn, k -> new ConcurrentHashMap<>()).put(account.getAccountNo(), c);
                        return true;
                    }
                } catch (SQLException ignored) {
                }
            }
            storage.put(account.getAccountNo(), c);
            return true;
        }

        public void commit(Connection conn) {
            if (conn != null) {
                Map<String, Account> staged = uncommitted.remove(conn);
                if (staged != null) {
                    storage.putAll(staged);
                }
            }
        }

        public void rollback(Connection conn) {
            if (conn != null) {
                uncommitted.remove(conn);
            }
        }

        private Account copy(Account a) {
            if (a == null) return null;
            if (a instanceof SavingsAccount sa) {
                return new SavingsAccount(sa.getAccountNo(), sa.getOwnerId(), sa.getBalance(), sa.getStatus(), sa.getMinimumBalance());
            }
            return a;
        }
    }

    /**
     * Self-contained in-memory transaction DAO for standalone demo execution.
     */
    public static class DemoTransactionDao extends TransactionDao {
        private final List<Transaction> storage = Collections.synchronizedList(new ArrayList<>());
        private final Map<Connection, List<Transaction>> uncommitted = new ConcurrentHashMap<>();
        private final AtomicLong seq = new AtomicLong(1);

        @Override
        public Transaction insert(Connection conn, Transaction txn) {
            Transaction saved = Transaction.builder()
                    .txnId(seq.getAndIncrement())
                    .fromAccount(txn.getFromAccount())
                    .toAccount(txn.getToAccount())
                    .txnType(txn.getTxnType())
                    .amount(txn.getAmount())
                    .status(txn.getStatus())
                    .remarks(txn.getRemarks())
                    .createdAt(LocalDateTime.now())
                    .build();

            if (conn != null) {
                try {
                    if (!conn.getAutoCommit()) {
                        uncommitted.computeIfAbsent(conn, k -> new ArrayList<>()).add(saved);
                        return saved;
                    }
                } catch (SQLException ignored) {
                }
            }
            storage.add(saved);
            return saved;
        }

        @Override
        public BigDecimal sumTransfersToday(Connection conn, String accountNo) {
            LocalDate today = LocalDate.now();
            synchronized (storage) {
                return storage.stream()
                        .filter(t -> accountNo.equals(t.getFromAccount()))
                        .filter(t -> t.getTxnType() == TxnType.TRANSFER && t.getStatus() == TxnStatus.SUCCESS)
                        .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().equals(today))
                        .map(Transaction::getAmount)
                        .reduce(Money.ZERO, Money::add);
            }
        }

        public void commit(Connection conn) {
            if (conn != null) {
                List<Transaction> staged = uncommitted.remove(conn);
                if (staged != null) {
                    storage.addAll(staged);
                }
            }
        }

        public void rollback(Connection conn) {
            if (conn != null) {
                uncommitted.remove(conn);
            }
        }
    }

    /**
     * Self-contained demo settings provider.
     */
    public static class DemoSettingsProvider implements SettingsProvider {
        private final Map<String, String> settings = new ConcurrentHashMap<>();

        public DemoSettingsProvider() {
            settings.put("transfer.per_txn_limit", "10000.00");
            settings.put("transfer.daily_limit", "1000000.00");
        }

        @Override
        public String getString(String key, String defaultValue) {
            return settings.getOrDefault(key, defaultValue);
        }

        @Override
        public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
            String val = settings.get(key);
            return val != null ? Money.of(new BigDecimal(val)) : defaultValue;
        }

        @Override
        public int getInt(String key, int defaultValue) {
            String val = settings.get(key);
            return val != null ? Integer.parseInt(val) : defaultValue;
        }

        @Override
        public long getLong(String key, long defaultValue) {
            String val = settings.get(key);
            return val != null ? Long.parseLong(val) : defaultValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            String val = settings.get(key);
            return val != null ? Boolean.parseBoolean(val) : defaultValue;
        }
    }
}

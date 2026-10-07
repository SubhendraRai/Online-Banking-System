package com.bank.service;

import com.bank.concurrent.AccountLockManager;
import com.bank.dao.AccountDao;
import com.bank.dao.TransactionDao;
import com.bank.dao.TxnFilter;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.BankingException;
import com.bank.exception.ValidationException;
import com.bank.model.Account;
import com.bank.model.AccountFactory;
import com.bank.model.AccountType;
import com.bank.model.DashboardSummary;
import com.bank.model.SavingsAccount;
import com.bank.model.Transaction;
import com.bank.model.TxnStatus;
import com.bank.model.TxnType;
import com.bank.util.Money;
import com.bank.util.Page;
import com.bank.util.TxRunner;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service managing retail bank accounts, monetary movements, statements, and portfolio reporting.
 * <p>
 * Implements core banking business rules:
 * <ul>
 *   <li>Atomic deposit and withdrawal workflows coordinating in-memory {@link AccountLockManager}
 *       and database row-level {@code SELECT ... FOR UPDATE} inside {@link TxRunner}.</li>
 *   <li>Double-entry ledger entry recording for all monetary modifications.</li>
 *   <li>Account statement generation sorted newest-first using explicit {@link Comparator}s.</li>
 *   <li>Customer portfolio dashboard generation computing balances and monthly cash flow metrics
 *       via Java Streams and {@link Collectors#groupingBy}.</li>
 * </ul>
 * </p>
 */
public class AccountService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountService.class);

    private static final Comparator<Transaction> NEWEST_FIRST_COMPARATOR = Comparator
            .comparing(Transaction::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Transaction::getTxnId, Comparator.nullsLast(Comparator.naturalOrder()))
            .reversed();

    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final SettingsProvider settingsProvider;

    /**
     * Default constructor wiring production DAOs and default settings provider.
     */
    public AccountService() {
        this(new AccountDao(), new TransactionDao(), new DefaultSettingsProvider());
    }

    /**
     * Parameterized constructor for dependency injection and testing.
     *
     * @param accountDao account repository DAO
     * @param transactionDao transaction repository DAO
     * @param settingsProvider configuration provider
     */
    public AccountService(AccountDao accountDao, TransactionDao transactionDao, SettingsProvider settingsProvider) {
        this.accountDao = Objects.requireNonNull(accountDao, "accountDao cannot be null");
        this.transactionDao = Objects.requireNonNull(transactionDao, "transactionDao cannot be null");
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider cannot be null");
    }

    /**
     * Opens a new bank account for an existing user.
     *
     * @param userId owner user ID
     * @param type account type (SAVINGS or CURRENT)
     * @param initialDeposit initial opening balance
     * @return newly opened {@link Account}
     * @throws ValidationException if parameters are invalid or initial deposit is below savings minimum
     * @throws BankingException on transactional or persistence failure
     */
    public Account openAccount(Long userId, AccountType type, BigDecimal initialDeposit)
            throws ValidationException, BankingException {

        if (userId == null) {
            throw new ValidationException("userId", "User ID is required to open an account.");
        }
        if (type == null) {
            throw new ValidationException("accountType", "Account type is required.");
        }

        BigDecimal depositAmount = (initialDeposit != null) ? Money.of(initialDeposit) : Money.ZERO;
        if (Money.isNegative(depositAmount)) {
            throw new ValidationException("initialDeposit", "Initial deposit cannot be negative.");
        }

        if (type == AccountType.SAVINGS) {
            BigDecimal minBalance = settingsProvider.getDecimal("savings.min_balance", SavingsAccount.DEFAULT_MINIMUM_BALANCE);
            if (depositAmount.compareTo(BigDecimal.ZERO) > 0 && depositAmount.compareTo(minBalance) < 0) {
                throw new ValidationException("initialDeposit",
                        String.format("Initial deposit %s is below mandatory savings minimum balance %s.",
                                Money.format(depositAmount), Money.format(minBalance)));
            }
        }

        return TxRunner.execute(conn -> {
            String accountNo = accountDao.nextAccountNumber(conn);
            Account account;

            if (type == AccountType.SAVINGS) {
                BigDecimal minBalance = settingsProvider.getDecimal("savings.min_balance", SavingsAccount.DEFAULT_MINIMUM_BALANCE);
                account = AccountFactory.createSavingsAccount(accountNo, userId, depositAmount, minBalance);
            } else {
                BigDecimal defaultOverdraft = settingsProvider.getDecimal("current.max_overdraft", Money.ZERO);
                account = AccountFactory.createCurrentAccount(accountNo, userId, depositAmount, defaultOverdraft);
            }

            accountDao.save(conn, account);

            if (depositAmount.compareTo(BigDecimal.ZERO) > 0) {
                Transaction initialTxn = Transaction.builder()
                        .fromAccount(null)
                        .toAccount(accountNo)
                        .txnType(TxnType.DEPOSIT)
                        .amount(depositAmount)
                        .status(TxnStatus.SUCCESS)
                        .remarks("Initial Account Opening Deposit")
                        .createdAt(LocalDateTime.now())
                        .build();
                transactionDao.insert(conn, initialTxn);
            }

            LOGGER.info("Opened new {} account {} for user ID {}", type, accountNo, userId);
            return account;
        });
    }

    /**
     * Retrieves all accounts associated with a customer.
     *
     * @param userId user identifier
     * @return list of bank accounts
     */
    public List<Account> getAccountsForUser(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return accountDao.findByUser(userId);
    }

    /**
     * Retrieves an account by its unique 12-digit account number.
     *
     * @param accountNo account number
     * @return found {@link Account}
     * @throws BankingException if the account does not exist or input is invalid
     */
    public Account getAccount(String accountNo) throws BankingException {
        if (accountNo == null || accountNo.isBlank()) {
            throw new ValidationException("accountNo", "Account number cannot be empty.");
        }
        return accountDao.findById(accountNo.trim())
                .orElseThrow(() -> new AccountNotFoundException(accountNo.trim()));
    }

    /**
     * Retrieves an account verifying that it is owned by the specified customer.
     *
     * @param accountNo account number
     * @param userId user identifier
     * @return found and ownership-verified {@link Account}
     * @throws BankingException if account is missing or does not belong to the user
     */
    public Account getUserAccount(String accountNo, Long userId) throws BankingException {
        Account account = getAccount(accountNo);
        if (userId == null || !userId.equals(account.getOwnerId())) {
            throw new ValidationException("accountNo", "Account does not belong to the authenticated user.");
        }
        return account;
    }

    /**
     * Executes a cash or credit deposit into a bank account.
     * <p>
     * Acquires an in-memory lock, begins a database transaction with {@code SELECT ... FOR UPDATE},
     * credits the account balance, and writes a ledger transaction entry.
     * </p>
     *
     * @param accountNo destination account number
     * @param amount monetary amount to deposit
     * @param remarks audit remarks or description
     * @return persisted {@link Transaction} receipt
     * @throws BankingException if account is missing, frozen, closed, or amount is invalid
     */
    public Transaction deposit(String accountNo, BigDecimal amount, String remarks) throws BankingException {
        Money.validatePositive(amount, "Deposit amount");

        AccountLockManager.acquireLock(accountNo);
        try {
            return TxRunner.execute(conn -> {
                Account account = accountDao.findByIdForUpdate(conn, accountNo)
                        .orElseThrow(() -> new AccountNotFoundException(accountNo));

                account.deposit(amount);
                accountDao.updateBalance(conn, account);

                Transaction txn = Transaction.builder()
                        .fromAccount(null)
                        .toAccount(accountNo)
                        .txnType(TxnType.DEPOSIT)
                        .amount(Money.of(amount))
                        .status(TxnStatus.SUCCESS)
                        .remarks(remarks != null && !remarks.isBlank() ? remarks.trim() : "Cash Deposit")
                        .createdAt(LocalDateTime.now())
                        .build();

                Transaction persistedTxn = transactionDao.insert(conn, txn);
                LOGGER.info("Successfully deposited {} to account {}. New balance: {}",
                        Money.format(amount), accountNo, Money.format(account.getBalance()));
                return persistedTxn;
            });
        } finally {
            AccountLockManager.releaseLock(accountNo);
        }
    }

    /**
     * Executes a cash withdrawal or debit from a bank account.
     * <p>
     * Acquires an in-memory lock, begins a database transaction with {@code SELECT ... FOR UPDATE},
     * debits the balance (verifying minimum balance/overdraft invariants), and writes a ledger entry.
     * </p>
     *
     * @param accountNo source account number
     * @param amount monetary amount to withdraw
     * @param remarks audit remarks or description
     * @return persisted {@link Transaction} receipt
     * @throws BankingException if account is missing, frozen, closed, or funds are insufficient
     */
    public Transaction withdraw(String accountNo, BigDecimal amount, String remarks) throws BankingException {
        Money.validatePositive(amount, "Withdrawal amount");

        AccountLockManager.acquireLock(accountNo);
        try {
            return TxRunner.execute(conn -> {
                Account account = accountDao.findByIdForUpdate(conn, accountNo)
                        .orElseThrow(() -> new AccountNotFoundException(accountNo));

                account.withdraw(amount);
                accountDao.updateBalance(conn, account);

                Transaction txn = Transaction.builder()
                        .fromAccount(accountNo)
                        .toAccount(null)
                        .txnType(TxnType.WITHDRAWAL)
                        .amount(Money.of(amount))
                        .status(TxnStatus.SUCCESS)
                        .remarks(remarks != null && !remarks.isBlank() ? remarks.trim() : "Cash Withdrawal")
                        .createdAt(LocalDateTime.now())
                        .build();

                Transaction persistedTxn = transactionDao.insert(conn, txn);
                LOGGER.info("Successfully withdrew {} from account {}. Remaining balance: {}",
                        Money.format(amount), accountNo, Money.format(account.getBalance()));
                return persistedTxn;
            });
        } finally {
            AccountLockManager.releaseLock(accountNo);
        }
    }

    /**
     * Retrieves an account's transaction history sorted newest first using a {@link Comparator}.
     *
     * @param accountNo bank account number
     * @return sorted list of transactions
     * @throws BankingException if the account does not exist
     */
    public List<Transaction> getStatement(String accountNo) throws BankingException {
        accountDao.findById(accountNo)
                .orElseThrow(() -> new AccountNotFoundException(accountNo));

        Page<Transaction> paged = transactionDao.findByAccount(accountNo, new TxnFilter(), 1, 1000);
        return paged.getItems().stream()
                .sorted(NEWEST_FIRST_COMPARATOR)
                .toList();
    }

    /**
     * Retrieves a paginated and filtered transaction statement for an account.
     *
     * @param accountNo bank account number
     * @param filter transaction filter criteria
     * @param page page number (1-indexed)
     * @param size page size
     * @return paginated {@link Page} of transactions
     * @throws BankingException if the account does not exist
     */
    public Page<Transaction> getStatement(String accountNo, TxnFilter filter, int page, int size)
            throws BankingException {
        accountDao.findById(accountNo)
                .orElseThrow(() -> new AccountNotFoundException(accountNo));

        return transactionDao.findByAccount(accountNo, filter, page, size);
    }

    /**
     * Compiles a comprehensive dashboard summary for a customer using Streams and Collectors.
     * <p>
     * Aggregates total balance, retrieves the last 5 transactions sorted newest-first, and
     * partitions current-month cash flow into "Money In" and "Money Out" via {@link Collectors#groupingBy}.
     * </p>
     *
     * @param userId user identifier
     * @return aggregated {@link DashboardSummary}
     */
    public DashboardSummary getDashboardSummary(Long userId) {
        if (userId == null) {
            return new DashboardSummary(Money.ZERO, Collections.emptyList(),
                    Collections.emptyList(), Money.ZERO, Money.ZERO);
        }

        List<Account> accounts = accountDao.findByUser(userId);
        if (accounts.isEmpty()) {
            return new DashboardSummary(Money.ZERO, Collections.emptyList(),
                    Collections.emptyList(), Money.ZERO, Money.ZERO);
        }

        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(Money.ZERO, Money::add);

        Set<String> userAccountNos = accounts.stream()
                .map(Account::getAccountNo)
                .collect(Collectors.toSet());

        List<Transaction> allTxns = new ArrayList<>();
        for (Account account : accounts) {
            Page<Transaction> page = transactionDao.findByAccount(account.getAccountNo(), new TxnFilter(), 1, 100);
            allTxns.addAll(page.getItems());
        }

        Map<Long, Transaction> distinctTxns = allTxns.stream()
                .filter(t -> t.getTxnId() != null)
                .collect(Collectors.toMap(
                        Transaction::getTxnId,
                        t -> t,
                        (existing, replacement) -> existing
                ));

        List<Transaction> recentTransactions = distinctTxns.values().stream()
                .sorted(NEWEST_FIRST_COMPARATOR)
                .limit(5)
                .toList();

        YearMonth currentMonth = YearMonth.now();
        List<Transaction> monthTxns = distinctTxns.values().stream()
                .filter(t -> t.getStatus() == TxnStatus.SUCCESS)
                .filter(t -> t.getCreatedAt() != null && YearMonth.from(t.getCreatedAt()).equals(currentMonth))
                .toList();

        Map<String, BigDecimal> flowMap = monthTxns.stream()
                .collect(Collectors.groupingBy(
                        txn -> classifyFlow(txn, userAccountNos),
                        Collectors.reducing(Money.ZERO, Transaction::getAmount, Money::add)
                ));

        BigDecimal moneyIn = flowMap.getOrDefault("IN", Money.ZERO);
        BigDecimal moneyOut = flowMap.getOrDefault("OUT", Money.ZERO);

        return new DashboardSummary(totalBalance, accounts, recentTransactions, moneyIn, moneyOut);
    }

    private String classifyFlow(Transaction txn, Set<String> userAccounts) {
        boolean fromUser = txn.getFromAccount() != null && userAccounts.contains(txn.getFromAccount());
        boolean toUser = txn.getToAccount() != null && userAccounts.contains(txn.getToAccount());

        if (fromUser && toUser) {
            return "INTERNAL";
        } else if (toUser) {
            return "IN";
        } else if (fromUser) {
            return "OUT";
        }
        return "OTHER";
    }
}

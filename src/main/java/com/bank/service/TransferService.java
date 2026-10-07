package com.bank.service;

import com.bank.concurrent.LockManager;
import com.bank.dao.AccountDao;
import com.bank.dao.TransactionDao;
import com.bank.dao.UserDao;
import com.bank.exception.AccountFrozenException;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.BankingException;
import com.bank.exception.DataAccessException;
import com.bank.exception.ErrorCode;
import com.bank.exception.InvalidAmountException;
import com.bank.exception.LimitExceededException;
import com.bank.exception.ValidationException;
import com.bank.model.Account;
import com.bank.model.AccountStatus;
import com.bank.model.Transaction;
import com.bank.model.TxnStatus;
import com.bank.model.TxnType;
import com.bank.model.User;
import com.bank.util.DBUtil;
import com.bank.util.Money;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service orchestrating peer-to-peer fund transfers with transactional integrity and strict concurrency controls.
 * <p>
 * Enforces Architectural Rules 1-5:
 * <ul>
 *   <li><b>Two-Phase Deadlock Avoidance:</b> Acquires in-memory {@link LockManager} locks and database
 *       {@code SELECT ... FOR UPDATE} rows in strictly ascending account-number order.</li>
 *   <li><b>ACID Boundaries:</b> Explicit service-level transaction lifecycle ({@code setAutoCommit(false)},
 *       {@code commit()}, and {@code rollback()} on checked or runtime exceptions).</li>
 *   <li><b>Polymorphic Validation:</b> Evaluates withdrawal constraints dynamically via {@link Account#withdraw(BigDecimal)}.</li>
 *   <li><b>Velocity Limits:</b> Validates per-transaction and daily cumulative debit thresholds configured in {@link SettingsProvider}.</li>
 *   <li><b>Post-Commit Audit Seam:</b> Dispatches transactions to {@link FraudChecker} post-commit in an isolated block.</li>
 * </ul>
 * </p>
 */
public class TransferService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferService.class);

    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final UserDao userDao;
    private final SettingsProvider settingsProvider;
    private final LockManager lockManager;
    private final FraudChecker fraudChecker;

    /**
     * Test seam hook allowing tests to inject faults immediately after the source account debit.
     */
    private volatile Runnable postDebitHook;

    /**
     * Default constructor wiring production DAOs, configuration, and default lock manager.
     */
    public TransferService() {
        this(new AccountDao(), new TransactionDao(), new UserDao(), new DefaultSettingsProvider(),
                LockManager.getInstance(), new NoOpFraudChecker());
    }

    /**
     * Parameterized constructor for dependency injection and testing.
     *
     * @param accountDao account repository DAO
     * @param transactionDao transaction repository DAO
     * @param settingsProvider configuration provider
     */
    public TransferService(AccountDao accountDao, TransactionDao transactionDao, SettingsProvider settingsProvider) {
        this(accountDao, transactionDao, new UserDao(), settingsProvider, LockManager.getInstance(), new NoOpFraudChecker());
    }

    /**
     * Constructor supporting custom lock managers and fraud checkers.
     *
     * @param accountDao account repository DAO
     * @param transactionDao transaction repository DAO
     * @param settingsProvider configuration provider
     * @param lockManager account lock coordinator
     * @param fraudChecker post-commit fraud inspector
     */
    public TransferService(AccountDao accountDao, TransactionDao transactionDao, SettingsProvider settingsProvider,
                           LockManager lockManager, FraudChecker fraudChecker) {
        this(accountDao, transactionDao, new UserDao(), settingsProvider, lockManager, fraudChecker);
    }

    /**
     * Full constructor supporting custom user DAO, lock managers, and fraud checkers.
     *
     * @param accountDao account repository DAO
     * @param transactionDao transaction repository DAO
     * @param userDao user repository DAO
     * @param settingsProvider configuration provider
     * @param lockManager account lock coordinator
     * @param fraudChecker post-commit fraud inspector
     */
    public TransferService(AccountDao accountDao, TransactionDao transactionDao, UserDao userDao,
                           SettingsProvider settingsProvider, LockManager lockManager, FraudChecker fraudChecker) {
        this.accountDao = Objects.requireNonNull(accountDao, "accountDao cannot be null");
        this.transactionDao = Objects.requireNonNull(transactionDao, "transactionDao cannot be null");
        this.userDao = Objects.requireNonNull(userDao, "userDao cannot be null");
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider cannot be null");
        this.lockManager = Objects.requireNonNull(lockManager, "lockManager cannot be null");
        this.fraudChecker = (fraudChecker != null) ? fraudChecker : new NoOpFraudChecker();
    }

    /**
     * Resolves the verified account holder's full name for a recipient account.
     *
     * @param toNo destination 12-digit account number
     * @return recipient account owner's full name
     * @throws BankingException if the account does not exist or is inactive/frozen
     */
    public String getRecipientName(String toNo) throws BankingException {
        if (toNo == null || toNo.trim().isEmpty()) {
            throw new ValidationException("toAccount", "Destination account number is required.");
        }
        String cleanAccNo = toNo.trim();
        Account account = accountDao.findById(cleanAccNo)
                .orElseThrow(() -> new AccountNotFoundException(cleanAccNo));
        validateAccountOperational(account, "Destination");

        return userDao.findById(account.getOwnerId())
                .map(User::getFullName)
                .orElse("Verified Customer");
    }

    /**
     * Injects a test hook executed immediately after debiting the source account.
     *
     * @param postDebitHook runnable test seam
     */
    public void setPostDebitHook(Runnable postDebitHook) {
        this.postDebitHook = postDebitHook;
    }

    /**
     * Executes an atomic fund transfer between two bank accounts with default remarks.
     *
     * @param fromNo source 12-digit account number
     * @param toNo destination 12-digit account number
     * @param amount monetary amount to transfer
     * @return persisted {@link Transaction} receipt record
     * @throws BankingException on business rule violations, limit breaches, or lock timeouts
     */
    public Transaction transfer(String fromNo, String toNo, BigDecimal amount) throws BankingException {
        return transfer(fromNo, toNo, amount, "Fund Transfer");
    }

    /**
     * Executes an atomic fund transfer between two bank accounts following the 11-step protocol.
     *
     * @param fromNo source 12-digit account number
     * @param toNo destination 12-digit account number
     * @param amount monetary amount to transfer
     * @param remarks audit remarks or user memo
     * @return persisted {@link Transaction} receipt record
     * @throws BankingException on business rule violations, limit breaches, or lock timeouts
     */
    public Transaction transfer(String fromNo, String toNo, BigDecimal amount, String remarks) throws BankingException {
        // Step 1: Validate (amount > 0, scale <= 2, accounts differ)
        validateTransferParameters(fromNo, toNo, amount);

        // Step 2: Read limits through SettingsProvider
        BigDecimal perTxnLimit = settingsProvider.getDecimal("transfer.per_txn_limit", new BigDecimal("50000.00"));
        BigDecimal dailyLimit = settingsProvider.getDecimal("transfer.daily_limit", new BigDecimal("100000.00"));

        // Step 3: lockAll in ascending account-number order
        List<String> sortedAccountNos = determineAscendingOrder(fromNo.trim(), toNo.trim());
        lockManager.lockAll(sortedAccountNos);

        Transaction persistedTxn;
        try {
            persistedTxn = executeTransferTransaction(fromNo.trim(), toNo.trim(), amount, remarks,
                    sortedAccountNos, perTxnLimit, dailyLimit);
        } finally {
            // Step 10: unlockAll
            lockManager.unlockAll(sortedAccountNos);
        }

        // Step 11: Call FraudChecker inside its own try/catch so it can never undo a committed transfer
        executeFraudCheckSafely(persistedTxn);

        return persistedTxn;
    }

    /**
     * Executes Steps 4-10 inside an isolated JDBC connection transaction.
     */
    private Transaction executeTransferTransaction(String fromNo, String toNo, BigDecimal amount, String remarks,
                                                   List<String> sortedAccountNos, BigDecimal perTxnLimit,
                                                   BigDecimal dailyLimit) throws BankingException {
        Connection conn = null;
        boolean originalAutoCommit = true;

        try {
            // Step 4: Open a connection, autocommit false
            conn = DBUtil.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            // Step 5: SELECT ... FOR UPDATE both rows in ascending order
            Account first = accountDao.findByIdForUpdate(conn, sortedAccountNos.get(0))
                    .orElseThrow(() -> new AccountNotFoundException(sortedAccountNos.get(0)));
            Account second = accountDao.findByIdForUpdate(conn, sortedAccountNos.get(1))
                    .orElseThrow(() -> new AccountNotFoundException(sortedAccountNos.get(1)));

            Account fromAccount = first.getAccountNo().equals(fromNo) ? first : second;
            Account toAccount = first.getAccountNo().equals(toNo) ? first : second;

            // Step 6: Re-check status ACTIVE, limits, and balance rules polymorphically
            validateAccountOperational(fromAccount, "Source");
            validateAccountOperational(toAccount, "Destination");
            validateTransferLimits(conn, fromAccount.getAccountNo(), amount, perTxnLimit, dailyLimit);

            fromAccount.withdraw(amount);
            toAccount.deposit(amount);

            // Step 7: Update both balances and insert a TRANSFER transaction
            accountDao.updateBalance(conn, fromAccount);
            onAfterDebit(conn, fromNo, amount);
            accountDao.updateBalance(conn, toAccount);

            Transaction txn = buildTransactionRecord(fromNo, toNo, amount, remarks);
            Transaction persisted = transactionDao.insert(conn, txn);

            // Step 8: Commit
            conn.commit();
            LOGGER.info("Successfully transferred {} from {} to {}. TxnId: {}",
                    Money.format(amount), fromNo, toNo, persisted.getTxnId());
            return persisted;

        } catch (BankingException | RuntimeException e) {
            // Step 9: On BankingException or RuntimeException rollback and rethrow
            rollbackSilently(conn);
            throw e;
        } catch (SQLException e) {
            rollbackSilently(conn);
            throw new DataAccessException("Database error executing fund transfer: " + e.getMessage(), e);
        } finally {
            // Step 10: Restore autocommit, close
            closeAndRestore(conn, originalAutoCommit);
        }
    }

    /**
     * Step 1: Parameter validation.
     */
    private void validateTransferParameters(String fromNo, String toNo, BigDecimal amount)
            throws ValidationException, InvalidAmountException {
        if (fromNo == null || fromNo.trim().isEmpty()) {
            throw new ValidationException("fromAccount", "Source account number is required.");
        }
        if (toNo == null || toNo.trim().isEmpty()) {
            throw new ValidationException("toAccount", "Destination account number is required.");
        }
        if (fromNo.trim().equals(toNo.trim())) {
            throw new ValidationException("toAccount", "Transfer source and destination accounts must be different.");
        }
        if (amount == null) {
            throw new InvalidAmountException(null, "Transfer amount cannot be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(amount, "Transfer amount must be strictly greater than 0.00.");
        }
        if (amount.scale() > 2) {
            throw new InvalidAmountException(amount, "Transfer amount scale cannot exceed 2 decimal places.");
        }
    }

    /**
     * Determines ascending alphabetical/numerical order for two account numbers.
     */
    private List<String> determineAscendingOrder(String acc1, String acc2) {
        return (acc1.compareTo(acc2) < 0) ? List.of(acc1, acc2) : List.of(acc2, acc1);
    }

    /**
     * Re-checks that an account is ACTIVE before proceeding.
     */
    private void validateAccountOperational(Account account, String label) throws BankingException {
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new AccountFrozenException(account.getAccountNo());
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BankingException(ErrorCode.ACCOUNT_CLOSED,
                    String.format("%s account %s is %s. Fund transfers are prohibited.",
                            label, account.getAccountNo(), account.getStatus()));
        }
    }

    /**
     * Evaluates per-transaction and daily cumulative debit velocity limits.
     */
    private void validateTransferLimits(Connection conn, String fromNo, BigDecimal amount,
                                        BigDecimal perTxnLimit, BigDecimal dailyLimit) throws LimitExceededException {
        if (perTxnLimit != null && amount.compareTo(perTxnLimit) > 0) {
            throw new LimitExceededException(perTxnLimit, amount,
                    String.format("Transfer amount %s exceeds maximum per-transfer limit of %s.",
                            Money.format(amount), Money.format(perTxnLimit)));
        }

        BigDecimal transfersToday = transactionDao.sumTransfersToday(conn, fromNo);
        BigDecimal projectedDaily = Money.add(transfersToday, amount);
        if (dailyLimit != null && projectedDaily.compareTo(dailyLimit) > 0) {
            throw new LimitExceededException(dailyLimit, projectedDaily,
                    String.format("Daily transfer limit of %s exceeded. Cumulative transfers today would reach %s.",
                            Money.format(dailyLimit), Money.format(projectedDaily)));
        }
    }

    /**
     * Constructs the unpersisted {@link Transaction} record.
     */
    private Transaction buildTransactionRecord(String fromNo, String toNo, BigDecimal amount, String remarks) {
        String cleanRemarks = (remarks != null && !remarks.isBlank()) ? remarks.trim() : "Fund Transfer";
        return Transaction.builder()
                .fromAccount(fromNo)
                .toAccount(toNo)
                .txnType(TxnType.TRANSFER)
                .amount(Money.of(amount))
                .status(TxnStatus.SUCCESS)
                .remarks(cleanRemarks)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Executes test seam fault injection after debit.
     */
    protected void onAfterDebit(Connection conn, String fromNo, BigDecimal amount) {
        if (postDebitHook != null) {
            postDebitHook.run();
        }
    }

    /**
     * Step 11: Safely calls the FraudChecker interface post-commit.
     */
    private void executeFraudCheckSafely(Transaction txn) {
        if (fraudChecker != null && txn != null) {
            try {
                fraudChecker.checkFraud(txn);
            } catch (Throwable t) {
                LOGGER.warn("Post-commit fraud check failed for txn ID {}: {}", txn.getTxnId(), t.getMessage(), t);
            }
        }
    }

    private void rollbackSilently(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                LOGGER.error("Failed to rollback connection during transfer exception", ex);
            }
        }
    }

    private void closeAndRestore(Connection conn, boolean originalAutoCommit) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.setAutoCommit(originalAutoCommit);
                    conn.close();
                }
            } catch (SQLException ex) {
                LOGGER.warn("Failed to restore autocommit or close connection", ex);
            }
        }
    }
}

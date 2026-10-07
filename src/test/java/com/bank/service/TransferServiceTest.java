package com.bank.service;

import com.bank.concurrent.LockManager;
import com.bank.demo.ConcurrentTransferDemo;
import com.bank.exception.AccountFrozenException;
import com.bank.exception.BankingException;
import com.bank.exception.ErrorCode;
import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.exception.LimitExceededException;
import com.bank.exception.ValidationException;
import com.bank.model.Account;
import com.bank.model.AccountFactory;
import com.bank.model.AccountStatus;
import com.bank.model.CurrentAccount;
import com.bank.model.SavingsAccount;
import com.bank.model.Transaction;
import com.bank.model.TxnStatus;
import com.bank.model.TxnType;
import com.bank.service.fakes.FakeAccountDao;
import com.bank.service.fakes.FakeSettingsProvider;
import com.bank.service.fakes.FakeTransactionDao;
import com.bank.util.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferServiceTest extends BaseServiceTest {

    private FakeAccountDao fakeAccountDao;
    private FakeTransactionDao fakeTransactionDao;
    private FakeSettingsProvider fakeSettingsProvider;
    private LockManager lockManager;
    private TransferService transferService;

    private static final String ACC_A = "100000000001";
    private static final String ACC_B = "100000000002";
    private static final Long USER_ID_1 = 101L;
    private static final Long USER_ID_2 = 102L;

    private final AtomicBoolean fraudCheckerCalled = new AtomicBoolean(false);

    @BeforeEach
    void setUp() {
        fakeAccountDao = new FakeAccountDao();
        fakeTransactionDao = new FakeTransactionDao();
        fakeSettingsProvider = new FakeSettingsProvider();
        lockManager = new LockManager();
        fraudCheckerCalled.set(false);

        fakeSettingsProvider.put("transfer.per_txn_limit", "50000.00");
        fakeSettingsProvider.put("transfer.daily_limit", "100000.00");

        FraudChecker testFraudChecker = txn -> fraudCheckerCalled.set(true);

        transferService = new TransferService(
                fakeAccountDao,
                fakeTransactionDao,
                fakeSettingsProvider,
                lockManager,
                testFraudChecker
        );

        // Prepopulate baseline accounts:
        // ACC_A: Savings account with balance 2000.00, min balance 500.00
        SavingsAccount accA = AccountFactory.createSavingsAccount(
                ACC_A, USER_ID_1, new BigDecimal("2000.00"), new BigDecimal("500.00")
        );
        fakeAccountDao.save(accA);

        // ACC_B: Savings account with balance 1000.00, min balance 500.00
        SavingsAccount accB = AccountFactory.createSavingsAccount(
                ACC_B, USER_ID_2, new BigDecimal("1000.00"), new BigDecimal("500.00")
        );
        fakeAccountDao.save(accB);
    }

    @Test
    @DisplayName("1. Successful transfer updates both balances, writes ledger transaction, and invokes FraudChecker")
    void testSuccessfulTransfer() throws Exception {
        BigDecimal amount = new BigDecimal("500.00");
        Transaction receipt = transferService.transfer(ACC_A, ACC_B, amount, "Test Transfer Memo");

        assertNotNull(receipt);
        assertEquals(TxnType.TRANSFER, receipt.getTxnType());
        assertEquals(TxnStatus.SUCCESS, receipt.getStatus());
        assertEquals(ACC_A, receipt.getFromAccount());
        assertEquals(ACC_B, receipt.getToAccount());
        assertEquals(new BigDecimal("500.00"), receipt.getAmount());
        assertEquals("Test Transfer Memo", receipt.getRemarks());

        Account updatedA = fakeAccountDao.findById(ACC_A).orElseThrow();
        Account updatedB = fakeAccountDao.findById(ACC_B).orElseThrow();

        assertEquals(new BigDecimal("1500.00"), updatedA.getBalance());
        assertEquals(new BigDecimal("1500.00"), updatedB.getBalance());

        // Invariant: Total funds conserved
        assertEquals(new BigDecimal("3000.00"), Money.add(updatedA.getBalance(), updatedB.getBalance()));

        // Post-commit fraud checker hook called
        assertTrue(fraudCheckerCalled.get(), "FraudChecker should have been invoked post-commit");
    }

    @Test
    @DisplayName("2. Insufficient funds throws InsufficientFundsException and leaves both balances unchanged")
    void testInsufficientFundsLeavesBothBalancesUnchanged() {
        // ACC_A balance = 2000.00, min balance = 500.00. Max transferable = 1500.00.
        // Attempting to transfer 1600.00 breaches minimum balance.
        BigDecimal excessiveAmount = new BigDecimal("1600.00");

        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, () ->
                transferService.transfer(ACC_A, ACC_B, excessiveAmount)
        );

        assertEquals(ErrorCode.INSUFFICIENT_FUNDS, ex.getErrorCode());

        // Verify neither account balance changed
        Account unchangedA = fakeAccountDao.findById(ACC_A).orElseThrow();
        Account unchangedB = fakeAccountDao.findById(ACC_B).orElseThrow();

        assertEquals(new BigDecimal("2000.00"), unchangedA.getBalance());
        assertEquals(new BigDecimal("1000.00"), unchangedB.getBalance());

        // FraudChecker not called on rollback
        assertFalse(fraudCheckerCalled.get());
    }

    @Test
    @DisplayName("3. Forced failure after debit (via test seam) rolls back both rows")
    void testForcedFailureAfterDebitRollsBackBothRows() {
        BigDecimal amount = new BigDecimal("500.00");

        // Inject simulated failure right after source account is debited
        transferService.setPostDebitHook(() -> {
            throw new RuntimeException("Simulated hardware crash immediately after debiting source row");
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                transferService.transfer(ACC_A, ACC_B, amount)
        );
        assertTrue(ex.getMessage().contains("Simulated hardware crash"));

        // Both balances must remain completely untouched due to transactional rollback
        Account restoredA = fakeAccountDao.findById(ACC_A).orElseThrow();
        Account restoredB = fakeAccountDao.findById(ACC_B).orElseThrow();

        assertEquals(new BigDecimal("2000.00"), restoredA.getBalance(),
                "Source account debit must be rolled back");
        assertEquals(new BigDecimal("1000.00"), restoredB.getBalance(),
                "Destination account must remain uncredited");

        assertFalse(fraudCheckerCalled.get(), "FraudChecker should never run on rolled back transactions");
    }

    @Test
    @DisplayName("4. Daily limit exceeded throws LimitExceededException and leaves balances unchanged")
    void testDailyLimitExceeded() throws Exception {
        fakeSettingsProvider.put("transfer.daily_limit", "3000.00");
        fakeSettingsProvider.put("transfer.per_txn_limit", "50000.00");

        // First transfer: 1000.00 (Current daily sum becomes 1000.00)
        transferService.transfer(ACC_A, ACC_B, new BigDecimal("1000.00"));

        Account afterFirstA = fakeAccountDao.findById(ACC_A).orElseThrow();
        assertEquals(new BigDecimal("1000.00"), afterFirstA.getBalance());

        // Deposit fresh funds into ACC_A so balance is not the limiting factor
        afterFirstA.deposit(new BigDecimal("5000.00"));
        fakeAccountDao.save(afterFirstA);

        // Second transfer: 2500.00 (1000.00 + 2500.00 = 3500.00 > 3000.00 daily limit)
        LimitExceededException ex = assertThrows(LimitExceededException.class, () ->
                transferService.transfer(ACC_A, ACC_B, new BigDecimal("2500.00"))
        );

        assertEquals(ErrorCode.LIMIT_EXCEEDED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Daily transfer limit of 3000.00 exceeded"));

        // Balance remains at state before second transfer (6000.00)
        Account finalA = fakeAccountDao.findById(ACC_A).orElseThrow();
        assertEquals(new BigDecimal("6000.00"), finalA.getBalance());
    }

    @Test
    @DisplayName("5. Transfer to same account is rejected with ValidationException")
    void testSameAccountRejected() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                transferService.transfer(ACC_A, ACC_A, new BigDecimal("100.00"))
        );

        assertTrue(ex.getMessage().contains("must be different"));
        assertTrue(ex.getFieldErrors().containsKey("toAccount"));

        // No database calls performed
        assertEquals(new BigDecimal("2000.00"), fakeAccountDao.findById(ACC_A).orElseThrow().getBalance());
    }

    @Test
    @DisplayName("6. Opposite concurrent transfers A->B and B->A never deadlock")
    void testOppositeConcurrentTransfersNeverDeadlock() throws Exception {
        // Fund both accounts with ample liquidity
        Account a = fakeAccountDao.findById(ACC_A).orElseThrow();
        Account b = fakeAccountDao.findById(ACC_B).orElseThrow();
        a.deposit(new BigDecimal("10000.00"));
        b.deposit(new BigDecimal("10000.00"));
        fakeAccountDao.save(a);
        fakeAccountDao.save(b);

        BigDecimal initialTotal = Money.add(a.getBalance(), b.getBalance());

        int threadCount = 20;
        int transfersPerThread = 25;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successfulTransfers = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final boolean forward = (i % 2 == 0);
            pool.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < transfersPerThread; j++) {
                        String from = forward ? ACC_A : ACC_B;
                        String to = forward ? ACC_B : ACC_A;
                        transferService.transfer(from, to, new BigDecimal("10.00"), "Bidirectional ping-pong");
                        successfulTransfers.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(15, TimeUnit.SECONDS);
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(completed, "Bidirectional concurrent transfers must complete within timeout (deadlock free)");
        assertEquals(threadCount * transfersPerThread, successfulTransfers.get());

        // Invariant: Total combined balance between A and B must remain strictly identical
        BigDecimal endingA = fakeAccountDao.findById(ACC_A).orElseThrow().getBalance();
        BigDecimal endingB = fakeAccountDao.findById(ACC_B).orElseThrow().getBalance();
        assertEquals(initialTotal, Money.add(endingA, endingB), "Total combined funds must be invariant");
    }

    @Test
    @DisplayName("7. Automated version of ConcurrentTransferDemo (4 accounts of 10000, 50 threads, 500 transfers)")
    void testAutomatedConcurrentTransferDemo() throws Exception {
        List<String> demoAccounts = List.of(
                "100000000001",
                "100000000002",
                "100000000003",
                "100000000004"
        );

        BigDecimal initialEach = new BigDecimal("10000.00");
        for (String no : demoAccounts) {
            SavingsAccount acc = AccountFactory.createSavingsAccount(no, 100L, initialEach, new BigDecimal("0.00"));
            fakeAccountDao.save(acc);
        }

        ConcurrentTransferDemo.SimulationResult result = ConcurrentTransferDemo.runSimulation(
                transferService, fakeAccountDao, demoAccounts, 50, 500
        );

        // Verification: conservation of money
        assertEquals(new BigDecimal("40000.00"), result.getTotalStartingBalance());
        assertEquals(new BigDecimal("40000.00"), result.getTotalEndingBalance());
        assertEquals(0, result.getTotalEndingBalance().compareTo(result.getTotalStartingBalance()));

        // Verification: no negative balance
        for (Account acc : result.getFinalAccounts()) {
            assertFalse(Money.isNegative(acc.getBalance()),
                    "Account " + acc.getAccountNo() + " ended with negative balance: " + acc.getBalance());
        }

        // Verification: total transactions accounted for
        assertEquals(500, result.getSuccessCount() + result.getFailCount());
        assertTrue(result.getSuccessCount() > 0, "At least some transfers should succeed");
    }

    @Test
    @DisplayName("Per-transfer limit exceeded throws LimitExceededException")
    void testPerTransferLimitExceeded() {
        fakeSettingsProvider.put("transfer.per_txn_limit", "1000.00");

        LimitExceededException ex = assertThrows(LimitExceededException.class, () ->
                transferService.transfer(ACC_A, ACC_B, new BigDecimal("1000.01"))
        );

        assertEquals(ErrorCode.LIMIT_EXCEEDED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("exceeds maximum per-transfer limit of 1000.00"));
    }

    @Test
    @DisplayName("Transfer rejects non-positive amounts or scale greater than 2")
    void testInvalidAmountsRejected() {
        assertThrows(InvalidAmountException.class, () ->
                transferService.transfer(ACC_A, ACC_B, new BigDecimal("-50.00"))
        );
        assertThrows(InvalidAmountException.class, () ->
                transferService.transfer(ACC_A, ACC_B, BigDecimal.ZERO)
        );
        assertThrows(InvalidAmountException.class, () ->
                transferService.transfer(ACC_A, ACC_B, new BigDecimal("10.555"))
        );
    }

    @Test
    @DisplayName("Transfer on FROZEN or CLOSED account is rejected")
    void testFrozenAndClosedAccountsRejected() {
        Account a = fakeAccountDao.findById(ACC_A).orElseThrow();
        a.setStatus(AccountStatus.FROZEN);
        fakeAccountDao.save(a);

        assertThrows(AccountFrozenException.class, () ->
                transferService.transfer(ACC_A, ACC_B, new BigDecimal("100.00"))
        );

        a.setStatus(AccountStatus.CLOSED);
        fakeAccountDao.save(a);

        assertThrows(BankingException.class, () ->
                transferService.transfer(ACC_A, ACC_B, new BigDecimal("100.00"))
        );
    }

    @Test
    @DisplayName("FraudChecker runtime exception never compromises or rolls back a committed transfer")
    void testFraudCheckerExceptionDoesNotUndoCommittedTransfer() throws Exception {
        FraudChecker failingChecker = txn -> {
            throw new RuntimeException("External fraud detection service timeout");
        };

        TransferService serviceWithFailingChecker = new TransferService(
                fakeAccountDao, fakeTransactionDao, fakeSettingsProvider, lockManager, failingChecker
        );

        Transaction receipt = serviceWithFailingChecker.transfer(ACC_A, ACC_B, new BigDecimal("300.00"));
        assertNotNull(receipt);
        assertEquals(TxnStatus.SUCCESS, receipt.getStatus());

        // Balances updated and committed successfully despite fraud checker throwing
        assertEquals(new BigDecimal("1700.00"), fakeAccountDao.findById(ACC_A).orElseThrow().getBalance());
        assertEquals(new BigDecimal("1300.00"), fakeAccountDao.findById(ACC_B).orElseThrow().getBalance());
    }
}

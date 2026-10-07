package com.bank.service;

import com.bank.exception.AccountFrozenException;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.ErrorCode;
import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.exception.ValidationException;
import com.bank.model.Account;
import com.bank.model.AccountFactory;
import com.bank.model.AccountStatus;
import com.bank.model.AccountType;
import com.bank.model.DashboardSummary;
import com.bank.model.SavingsAccount;
import com.bank.model.Transaction;
import com.bank.model.TxnStatus;
import com.bank.model.TxnType;
import com.bank.service.fakes.FakeAccountDao;
import com.bank.service.fakes.FakeSettingsProvider;
import com.bank.service.fakes.FakeTransactionDao;
import com.bank.util.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountServiceTest extends BaseServiceTest {

    private FakeAccountDao fakeAccountDao;
    private FakeTransactionDao fakeTransactionDao;
    private FakeSettingsProvider fakeSettingsProvider;
    private AccountService accountService;

    private static final String ACC_SAVINGS = "100000000001";
    private static final String ACC_CURRENT = "100000000002";
    private static final Long USER_ID = 42L;

    @BeforeEach
    void setUp() {
        fakeAccountDao = new FakeAccountDao();
        fakeTransactionDao = new FakeTransactionDao();
        fakeSettingsProvider = new FakeSettingsProvider();
        fakeSettingsProvider.put("savings.min_balance", "500.00");
        fakeSettingsProvider.put("current.max_overdraft", "10000.00");

        accountService = new AccountService(fakeAccountDao, fakeTransactionDao, fakeSettingsProvider);

        // Preload standard test accounts
        SavingsAccount savings = AccountFactory.createSavingsAccount(
                ACC_SAVINGS, USER_ID, new BigDecimal("2000.00"), new BigDecimal("500.00")
        );
        fakeAccountDao.save(savings);
    }

    @Test
    @DisplayName("Deposit updates balance and records a ledger transaction receipt")
    void testDepositSuccess() throws Exception {
        Transaction receipt = accountService.deposit(ACC_SAVINGS, new BigDecimal("1500.00"), "Salary bonus");

        assertNotNull(receipt);
        assertEquals(TxnType.DEPOSIT, receipt.getTxnType());
        assertEquals(TxnStatus.SUCCESS, receipt.getStatus());
        assertEquals(new BigDecimal("1500.00"), receipt.getAmount());
        assertEquals(ACC_SAVINGS, receipt.getToAccount());

        Account updated = fakeAccountDao.findById(ACC_SAVINGS).orElseThrow();
        assertEquals(new BigDecimal("3500.00"), updated.getBalance());

        // Verify ledger entry stored in transaction DAO
        List<Transaction> history = fakeTransactionDao.findAll();
        assertEquals(1, history.size());
        assertEquals(new BigDecimal("1500.00"), history.get(0).getAmount());
    }

    @Test
    @DisplayName("Deposit rejects negative or zero monetary amount")
    void testDepositInvalidAmount() {
        assertThrows(InvalidAmountException.class, () ->
                accountService.deposit(ACC_SAVINGS, new BigDecimal("-50.00"), "Negative")
        );
        assertThrows(InvalidAmountException.class, () ->
                accountService.deposit(ACC_SAVINGS, BigDecimal.ZERO, "Zero")
        );
    }

    @Test
    @DisplayName("Withdraw debits balance and records a withdrawal transaction")
    void testWithdrawSuccess() throws Exception {
        Transaction receipt = accountService.withdraw(ACC_SAVINGS, new BigDecimal("500.00"), "ATM Cash");

        assertNotNull(receipt);
        assertEquals(TxnType.WITHDRAWAL, receipt.getTxnType());
        assertEquals(TxnStatus.SUCCESS, receipt.getStatus());
        assertEquals(new BigDecimal("500.00"), receipt.getAmount());
        assertEquals(ACC_SAVINGS, receipt.getFromAccount());

        Account updated = fakeAccountDao.findById(ACC_SAVINGS).orElseThrow();
        assertEquals(new BigDecimal("1500.00"), updated.getBalance());
    }

    @Test
    @DisplayName("Withdrawal exceeding total balance throws InsufficientFundsException")
    void testWithdrawInsufficientFunds() {
        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, () ->
                accountService.withdraw(ACC_SAVINGS, new BigDecimal("5000.00"), "Too high")
        );

        assertEquals(ErrorCode.INSUFFICIENT_FUNDS, ex.getErrorCode());
        assertEquals(new BigDecimal("5000.00"), ex.getRequestedAmount());
        assertEquals(new BigDecimal("1500.00"), ex.getAvailableAmount());
    }

    @Test
    @DisplayName("Withdrawal violating savings minimum balance throws InsufficientFundsException")
    void testWithdrawBelowSavingsMinimum() {
        // Balance is 2000.00, minimum balance is 500.00. Available for withdrawal is 1500.00.
        // Attempting to withdraw 1600.00 would leave balance at 400.00 (below 500.00).
        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, () ->
                accountService.withdraw(ACC_SAVINGS, new BigDecimal("1600.00"), "Violates min balance")
        );

        assertEquals(ErrorCode.INSUFFICIENT_FUNDS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("violates minimum balance"));
    }

    @Test
    @DisplayName("Opening savings account below configured minimum balance throws ValidationException")
    void testOpenSavingsAccountBelowMinimum() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                accountService.openAccount(USER_ID, AccountType.SAVINGS, new BigDecimal("200.00"))
        );

        assertTrue(ex.getFieldErrors().containsKey("initialDeposit"));
        assertTrue(ex.getMessage().contains("below mandatory savings minimum"));
    }

    @Test
    @DisplayName("Operations on FROZEN account throw AccountFrozenException")
    void testFrozenAccountOperations() {
        Account account = fakeAccountDao.findById(ACC_SAVINGS).orElseThrow();
        account.setStatus(AccountStatus.FROZEN);
        fakeAccountDao.update(account);

        AccountFrozenException depEx = assertThrows(AccountFrozenException.class, () ->
                accountService.deposit(ACC_SAVINGS, new BigDecimal("100.00"), "Deposit on frozen")
        );
        assertEquals(ACC_SAVINGS, depEx.getAccountNo());
        assertEquals(ErrorCode.ACCOUNT_FROZEN, depEx.getErrorCode());

        AccountFrozenException withEx = assertThrows(AccountFrozenException.class, () ->
                accountService.withdraw(ACC_SAVINGS, new BigDecimal("100.00"), "Withdraw on frozen")
        );
        assertEquals(ACC_SAVINGS, withEx.getAccountNo());
        assertEquals(ErrorCode.ACCOUNT_FROZEN, withEx.getErrorCode());
    }

    @Test
    @DisplayName("Transactions on non-existent account throw AccountNotFoundException")
    void testAccountNotFound() {
        assertThrows(AccountNotFoundException.class, () ->
                accountService.deposit("999999999999", new BigDecimal("100.00"), "Ghost")
        );
        assertThrows(AccountNotFoundException.class, () ->
                accountService.withdraw("999999999999", new BigDecimal("100.00"), "Ghost")
        );
    }

    @Test
    @DisplayName("getStatement returns transactions sorted newest first via Comparator")
    void testGetStatementOrdering() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        Transaction txnOld = Transaction.builder()
                .txnId(1L)
                .toAccount(ACC_SAVINGS)
                .txnType(TxnType.DEPOSIT)
                .amount(new BigDecimal("100.00"))
                .status(TxnStatus.SUCCESS)
                .createdAt(now.minusHours(5))
                .build();

        Transaction txnMid = Transaction.builder()
                .txnId(2L)
                .toAccount(ACC_SAVINGS)
                .txnType(TxnType.DEPOSIT)
                .amount(new BigDecimal("200.00"))
                .status(TxnStatus.SUCCESS)
                .createdAt(now.minusHours(2))
                .build();

        Transaction txnNewest = Transaction.builder()
                .txnId(3L)
                .fromAccount(ACC_SAVINGS)
                .txnType(TxnType.WITHDRAWAL)
                .amount(new BigDecimal("50.00"))
                .status(TxnStatus.SUCCESS)
                .createdAt(now.minusMinutes(10))
                .build();

        fakeTransactionDao.insert(txnOld);
        fakeTransactionDao.insert(txnNewest);
        fakeTransactionDao.insert(txnMid);

        List<Transaction> statement = accountService.getStatement(ACC_SAVINGS);

        assertEquals(3, statement.size());
        assertEquals(3L, statement.get(0).getTxnId(), "Newest transaction must appear first");
        assertEquals(2L, statement.get(1).getTxnId(), "Middle transaction must appear second");
        assertEquals(1L, statement.get(2).getTxnId(), "Oldest transaction must appear last");
    }

    @Test
    @DisplayName("getDashboardSummary aggregates balance, recent 5, and monthly cash flow with groupingBy")
    void testDashboardSummary() throws Exception {
        // Create second account for USER_ID (Current account with ₹5,000)
        Account current = AccountFactory.createCurrentAccount(
                ACC_CURRENT, USER_ID, new BigDecimal("5000.00"), new BigDecimal("10000.00")
        );
        fakeAccountDao.save(current);

        LocalDateTime now = LocalDateTime.now();

        // 1. Credit to Savings this month (Money IN: 1000.00)
        fakeTransactionDao.insert(Transaction.builder()
                .txnId(101L)
                .toAccount(ACC_SAVINGS)
                .txnType(TxnType.DEPOSIT)
                .amount(new BigDecimal("1000.00"))
                .status(TxnStatus.SUCCESS)
                .createdAt(now.minusDays(2))
                .build());

        // 2. Transfer from External to Current this month (Money IN: 2500.00)
        fakeTransactionDao.insert(Transaction.builder()
                .txnId(102L)
                .fromAccount("888888888888")
                .toAccount(ACC_CURRENT)
                .txnType(TxnType.TRANSFER)
                .amount(new BigDecimal("2500.00"))
                .status(TxnStatus.SUCCESS)
                .createdAt(now.minusDays(3))
                .build());

        // 3. Debit from Savings this month (Money OUT: 400.00)
        fakeTransactionDao.insert(Transaction.builder()
                .txnId(103L)
                .fromAccount(ACC_SAVINGS)
                .txnType(TxnType.WITHDRAWAL)
                .amount(new BigDecimal("400.00"))
                .status(TxnStatus.SUCCESS)
                .createdAt(now.minusDays(1))
                .build());

        // 4. Internal transfer between user's own Savings and Current (INTERNAL: 300.00 - neither in nor out)
        fakeTransactionDao.insert(Transaction.builder()
                .txnId(104L)
                .fromAccount(ACC_SAVINGS)
                .toAccount(ACC_CURRENT)
                .txnType(TxnType.TRANSFER)
                .amount(new BigDecimal("300.00"))
                .status(TxnStatus.SUCCESS)
                .createdAt(now.minusHours(4))
                .build());

        // 5. Transaction from previous month (should not affect this month's flow)
        fakeTransactionDao.insert(Transaction.builder()
                .txnId(105L)
                .toAccount(ACC_SAVINGS)
                .txnType(TxnType.DEPOSIT)
                .amount(new BigDecimal("9999.00"))
                .status(TxnStatus.SUCCESS)
                .createdAt(now.minusMonths(1))
                .build());

        DashboardSummary summary = accountService.getDashboardSummary(USER_ID);

        assertNotNull(summary);
        // Total balance: Savings (2000.00) + Current (5000.00) = 7000.00
        assertEquals(new BigDecimal("7000.00"), summary.getTotalBalance());
        assertEquals(2, summary.getAccounts().size());

        // Recent transactions capped at 5
        assertTrue(summary.getRecentTransactions().size() <= 5);

        // Money In: 1000.00 + 2500.00 = 3500.00
        assertEquals(new BigDecimal("3500.00"), summary.getMoneyInThisMonth());

        // Money Out: 400.00
        assertEquals(new BigDecimal("400.00"), summary.getMoneyOutThisMonth());
    }
}

package com.bank.model;

import com.bank.exception.AccountFrozenException;
import com.bank.exception.BankingException;
import com.bank.exception.ErrorCode;
import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.util.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for domain account polymorphism, template method mechanics, and business rules.
 */
class AccountTest {

    @Test
    @DisplayName("SavingsAccount template deposit updates balance correctly")
    void testSavingsAccountDepositSuccess() throws BankingException {
        SavingsAccount savings = new SavingsAccount("SAV-1001", 1L, new BigDecimal("1000.00"));
        savings.deposit(new BigDecimal("250.50"));

        assertEquals(new BigDecimal("1250.50"), savings.getBalance());
        assertEquals(new BigDecimal("750.50"), savings.getAvailableBalance());
    }

    @Test
    @DisplayName("SavingsAccount withdrawal within available balance updates balance")
    void testSavingsAccountWithdrawalSuccess() throws BankingException {
        SavingsAccount savings = new SavingsAccount("SAV-1002", 1L, new BigDecimal("1000.00"));
        savings.withdraw(new BigDecimal("300.00"));

        assertEquals(new BigDecimal("700.00"), savings.getBalance());
        assertEquals(new BigDecimal("200.00"), savings.getAvailableBalance());
    }

    @Test
    @DisplayName("SavingsAccount withdrawal violating minimum balance throws InsufficientFundsException")
    void testSavingsAccountWithdrawalBreachingMinimumBalanceThrows() {
        SavingsAccount savings = new SavingsAccount("SAV-1003", 1L, new BigDecimal("1000.00"));
        // Minimum balance is 500.00, available is 500.00. Attempting to withdraw 600.00.
        InsufficientFundsException ex = assertThrows(
                InsufficientFundsException.class,
                () -> savings.withdraw(new BigDecimal("600.00"))
        );

        assertEquals(ErrorCode.INSUFFICIENT_FUNDS, ex.getErrorCode());
        assertEquals(new BigDecimal("600.00"), ex.getRequestedAmount());
        assertEquals(new BigDecimal("500.00"), ex.getAvailableAmount());
        assertEquals(new BigDecimal("1000.00"), savings.getBalance());
    }

    @Test
    @DisplayName("SavingsAccount implements InterestBearing and accrues monthly interest")
    void testSavingsAccountInterestAccrualCalculation() {
        SavingsAccount savings = new SavingsAccount("SAV-1004", 1L, new BigDecimal("12000.00"));
        // Annual rate 4.00% on 12,000 -> 480.00/yr -> 40.00/mo
        BigDecimal monthlyInterest = savings.calculateMonthlyInterest(new BigDecimal("4.00"));

        assertEquals(new BigDecimal("40.00"), monthlyInterest);
    }

    @Test
    @DisplayName("CurrentAccount allows withdrawal using overdraft limit down to negative balance")
    void testCurrentAccountOverdraftWithdrawalSuccess() throws BankingException {
        CurrentAccount current = new CurrentAccount("CUR-2001", 2L, new BigDecimal("500.00"),
                AccountStatus.ACTIVE, new BigDecimal("1000.00"));

        current.withdraw(new BigDecimal("800.00"));

        // Balance drops to -300.00, remaining available overdraft is 700.00
        assertEquals(new BigDecimal("-300.00"), current.getBalance());
        assertEquals(new BigDecimal("700.00"), current.getAvailableBalance());
    }

    @Test
    @DisplayName("CurrentAccount withdrawal exceeding overdraft limit throws InsufficientFundsException")
    void testCurrentAccountOverdraftLimitExceededThrows() {
        CurrentAccount current = new CurrentAccount("CUR-2002", 2L, new BigDecimal("500.00"),
                AccountStatus.ACTIVE, new BigDecimal("1000.00"));

        // Total available is 500 + 1000 = 1500.00. Attempting to withdraw 1600.00.
        InsufficientFundsException ex = assertThrows(
                InsufficientFundsException.class,
                () -> current.withdraw(new BigDecimal("1600.00"))
        );

        assertEquals(ErrorCode.INSUFFICIENT_FUNDS, ex.getErrorCode());
        assertEquals(new BigDecimal("1600.00"), ex.getRequestedAmount());
        assertEquals(new BigDecimal("1500.00"), ex.getAvailableAmount());
        assertEquals(new BigDecimal("500.00"), current.getBalance());
    }

    @Test
    @DisplayName("Frozen account rejects both deposits and withdrawals with AccountFrozenException")
    void testAccountFrozenRejectsOperations() {
        SavingsAccount savings = new SavingsAccount("SAV-FROZEN", 3L, new BigDecimal("1000.00"));
        savings.setStatus(AccountStatus.FROZEN);

        AccountFrozenException depEx = assertThrows(
                AccountFrozenException.class,
                () -> savings.deposit(new BigDecimal("100.00"))
        );
        assertEquals("SAV-FROZEN", depEx.getAccountNo());
        assertEquals(ErrorCode.ACCOUNT_FROZEN, depEx.getErrorCode());

        AccountFrozenException wthEx = assertThrows(
                AccountFrozenException.class,
                () -> savings.withdraw(new BigDecimal("100.00"))
        );
        assertEquals("SAV-FROZEN", wthEx.getAccountNo());
    }

    @Test
    @DisplayName("Closed account rejects transactions with BankingException")
    void testAccountClosedRejectsOperations() {
        CurrentAccount current = new CurrentAccount("CUR-CLOSED", 4L, new BigDecimal("200.00"));
        current.setStatus(AccountStatus.CLOSED);

        BankingException ex = assertThrows(
                BankingException.class,
                () -> current.deposit(new BigDecimal("50.00"))
        );
        assertEquals(ErrorCode.ACCOUNT_CLOSED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Template validation rejects non-positive amounts with InvalidAmountException")
    void testInvalidAmountRejectsNonPositiveDepositOrWithdrawal() {
        SavingsAccount savings = new SavingsAccount("SAV-1005", 1L, new BigDecimal("1000.00"));

        assertThrows(InvalidAmountException.class, () -> savings.deposit(BigDecimal.ZERO));
        assertThrows(InvalidAmountException.class, () -> savings.deposit(new BigDecimal("-50.00")));
        assertThrows(InvalidAmountException.class, () -> savings.withdraw(BigDecimal.ZERO));
        assertThrows(InvalidAmountException.class, () -> savings.withdraw(new BigDecimal("-10.00")));
        assertThrows(InvalidAmountException.class, () -> savings.deposit(null));
    }

    @Test
    @DisplayName("AccountFactory creates correct polymorphic subtypes")
    void testAccountFactoryCreatesCorrectSubclasses() {
        Account sav = AccountFactory.createAccount(AccountType.SAVINGS, "SAV-FACT", 10L, new BigDecimal("500.00"));
        Account cur = AccountFactory.createAccount(AccountType.CURRENT, "CUR-FACT", 20L, new BigDecimal("1000.00"), new BigDecimal("2000.00"));

        assertInstanceOf(SavingsAccount.class, sav);
        assertInstanceOf(CurrentAccount.class, cur);
        assertInstanceOf(InterestBearing.class, sav);
        assertFalse(cur instanceof InterestBearing);

        CurrentAccount currentAcc = (CurrentAccount) cur;
        assertEquals(new BigDecimal("2000.00"), currentAcc.getOverdraftLimit());
    }

    @Test
    @DisplayName("Polymorphic iteration over List<Account> executes deposit and withdrawal correctly")
    void testPolymorphicAccountListProcessing() throws BankingException {
        List<Account> portfolio = new ArrayList<>();
        portfolio.add(AccountFactory.createSavingsAccount("SAV-POLY", 1L, new BigDecimal("1000.00"), new BigDecimal("200.00")));
        portfolio.add(AccountFactory.createCurrentAccount("CUR-POLY", 1L, new BigDecimal("500.00"), new BigDecimal("500.00")));

        // Deposit 100 into all accounts polymorphically
        for (Account acc : portfolio) {
            acc.deposit(new BigDecimal("100.00"));
        }

        assertEquals(new BigDecimal("1100.00"), portfolio.get(0).getBalance());
        assertEquals(new BigDecimal("600.00"), portfolio.get(1).getBalance());

        // Withdraw 700 from each account
        portfolio.get(0).withdraw(new BigDecimal("700.00")); // 1100 - 700 = 400 (>= 200 min balance)
        portfolio.get(1).withdraw(new BigDecimal("700.00")); // 600 - 700 = -100 (overdraft allowed down to -500)

        assertEquals(new BigDecimal("400.00"), portfolio.get(0).getBalance());
        assertEquals(new BigDecimal("-100.00"), portfolio.get(1).getBalance());
    }

    @Test
    @DisplayName("Immutable Transaction builder and static factories instantiate valid models")
    void testImmutableTransactionBuilderAndStaticFactories() {
        Transaction xfer = Transaction.transfer("ACC-1", "ACC-2", new BigDecimal("150.00"), "Rent");
        assertEquals("ACC-1", xfer.getFromAccount());
        assertEquals("ACC-2", xfer.getToAccount());
        assertEquals(TxnType.TRANSFER, xfer.getTxnType());
        assertEquals(new BigDecimal("150.00"), xfer.getAmount());
        assertEquals(TxnStatus.SUCCESS, xfer.getStatus());
        assertNotNull(xfer.getCreatedAt());

        Transaction dep = Transaction.deposit("ACC-2", new BigDecimal("50.00"), "Cash deposit");
        assertEquals(TxnType.DEPOSIT, dep.getTxnType());
        assertEquals("ACC-2", dep.getToAccount());

        Transaction wth = Transaction.withdrawal("ACC-1", new BigDecimal("25.00"), "ATM");
        assertEquals(TxnType.WITHDRAWAL, wth.getTxnType());
        assertEquals("ACC-1", wth.getFromAccount());

        assertThrows(IllegalArgumentException.class, () -> Transaction.transfer("A", "B", new BigDecimal("-1.00"), "Invalid"));
    }
}

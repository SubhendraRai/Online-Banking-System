package com.bank.demo;

import com.bank.exception.AccountFrozenException;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.AuthenticationException;
import com.bank.exception.BankingException;
import com.bank.exception.DuplicateEmailException;
import com.bank.exception.ErrorCode;
import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.exception.LimitExceededException;
import com.bank.exception.ServiceBusyException;
import com.bank.exception.ValidationException;
import com.bank.model.Account;
import com.bank.model.AccountFactory;
import com.bank.model.AccountStatus;
import com.bank.model.AccountType;
import com.bank.model.CurrentAccount;
import com.bank.model.InterestBearing;
import com.bank.model.SavingsAccount;
import com.bank.util.Money;
import com.bank.util.Validator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive command-line demonstration illustrating Object-Oriented Design principles
 * and structured banking exception handling.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Polymorphism and the Template Method pattern over a {@code List<Account>}</li>
 *   <li>Interface segregation using {@link InterestBearing}</li>
 *   <li>Every checked exception in {@code com.bank.exception} caught and handled gracefully</li>
 * </ul>
 * </p>
 */
public class OopDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("          ONLINE BANKING SYSTEM - OOP & DOMAIN MODEL DEMO");
        System.out.println("================================================================================\n");

        demonstrateAccountPolymorphism();
        System.out.println();
        demonstrateExceptionHandling();

        System.out.println("\n================================================================================");
        System.out.println("                       OOP DEMONSTRATION COMPLETE");
        System.out.println("================================================================================");
    }

    /**
     * Demonstrates polymorphic behaviour across heterogeneous bank account subtypes.
     */
    private static void demonstrateAccountPolymorphism() {
        System.out.println("--- 1. POLYMORPHIC PROCESSING OVER List<Account> ---");

        List<Account> accounts = new ArrayList<>();
        accounts.add(AccountFactory.createSavingsAccount("SAV-10001", 101L, new BigDecimal("2500.00"), new BigDecimal("500.00")));
        accounts.add(AccountFactory.createCurrentAccount("CUR-20002", 101L, new BigDecimal("1000.00"), new BigDecimal("1500.00")));

        for (Account account : accounts) {
            System.out.println("\nProcessing Account: " + account.getAccountNo());
            System.out.println("  Type:              " + account.getAccountType());
            System.out.println("  Initial Balance:   $" + Money.format(account.getBalance()));
            System.out.println("  Available Funds:   $" + Money.format(account.getAvailableBalance()));

            try {
                // Polymorphic template deposit
                account.deposit(new BigDecimal("500.00"));
                System.out.println("  [+] Deposited $500.00 -> Balance: $" + Money.format(account.getBalance()));

                // Polymorphic template withdrawal
                account.withdraw(new BigDecimal("1200.00"));
                System.out.println("  [-] Withdrew  $1200.00 -> Balance: $" + Money.format(account.getBalance()));

                // Interface hook check
                if (account instanceof InterestBearing ib) {
                    BigDecimal monthlyInterest = ib.calculateMonthlyInterest(new BigDecimal("4.50"));
                    System.out.println("  [*] Monthly Interest @ 4.5% APR: $" + Money.format(monthlyInterest));
                } else {
                    System.out.println("  [*] Non-interest bearing commercial account (InterestBearing not implemented).");
                }

                System.out.println("  Final Available:   $" + Money.format(account.getAvailableBalance()));
            } catch (BankingException ex) {
                System.err.println("  [!] Unexpected error: " + ex.getMessage());
            }
        }
    }

    /**
     * Demonstrates throwing and handling each checked business exception.
     */
    private static void demonstrateExceptionHandling() {
        System.out.println("--- 2. STRUCTURED DOMAIN EXCEPTION HANDLING ---");

        // 1. InsufficientFundsException
        try {
            System.out.print("\n[Scenario 1] Withdrawing $10,000.00 from SavingsAccount with $500.00 balance: ");
            SavingsAccount savings = new SavingsAccount("SAV-9999", 1L, new BigDecimal("500.00"));
            savings.withdraw(new BigDecimal("10000.00"));
        } catch (InsufficientFundsException ex) {
            System.out.println("CAUGHT " + ex.getClass().getSimpleName());
            System.out.println("  Friendly Message:  " + ex.getMessage());
            System.out.println("  Code:              " + ex.getErrorCode().getCode());
            System.out.println("  Requested:         $" + Money.format(ex.getRequestedAmount()));
            System.out.println("  Available:         $" + Money.format(ex.getAvailableAmount()));
        } catch (BankingException ex) {
            System.err.println("  Caught fallback: " + ex.getMessage());
        }

        // 2. AccountNotFoundException
        try {
            System.out.print("\n[Scenario 2] Looking up unmapped account 'ACC-8888': ");
            throw new AccountNotFoundException("ACC-8888");
        } catch (AccountNotFoundException ex) {
            System.out.println("CAUGHT " + ex.getClass().getSimpleName());
            System.out.println("  Friendly Message:  " + ex.getMessage());
            System.out.println("  Account Number:    " + ex.getAccountNo());
        }

        // 3. AccountFrozenException
        try {
            System.out.print("\n[Scenario 3] Withdrawing from frozen account 'CUR-FROZEN': ");
            CurrentAccount current = new CurrentAccount("CUR-FROZEN", 2L, new BigDecimal("1000.00"));
            current.setStatus(AccountStatus.FROZEN);
            current.withdraw(new BigDecimal("100.00"));
        } catch (AccountFrozenException ex) {
            System.out.println("CAUGHT " + ex.getClass().getSimpleName());
            System.out.println("  Friendly Message:  " + ex.getMessage());
            System.out.println("  Frozen Account:    " + ex.getAccountNo());
        } catch (BankingException ex) {
            System.err.println("  Caught fallback: " + ex.getMessage());
        }

        // 4. InvalidAmountException
        try {
            System.out.print("\n[Scenario 4] Depositing negative amount -$200.00: ");
            SavingsAccount savings = new SavingsAccount("SAV-TEST", 1L, new BigDecimal("500.00"));
            savings.deposit(new BigDecimal("-200.00"));
        } catch (InvalidAmountException ex) {
            System.out.println("CAUGHT " + ex.getClass().getSimpleName());
            System.out.println("  Friendly Message:  " + ex.getMessage());
            System.out.println("  Rejected Amount:   " + ex.getAmount());
        } catch (BankingException ex) {
            System.err.println("  Caught fallback: " + ex.getMessage());
        }

        // 5. LimitExceededException
        try {
            System.out.print("\n[Scenario 5] Initiating transfer exceeding daily cap: ");
            throw new LimitExceededException(new BigDecimal("5000.00"), new BigDecimal("12000.00"));
        } catch (LimitExceededException ex) {
            System.out.println("CAUGHT " + ex.getClass().getSimpleName());
            System.out.println("  Friendly Message:  " + ex.getMessage());
            System.out.println("  Daily Limit:       $" + Money.format(ex.getLimit()));
            System.out.println("  Attempted Amount:  $" + Money.format(ex.getRequestedAmount()));
        }

        // 6. DuplicateEmailException
        try {
            System.out.print("\n[Scenario 6] Registering user with existing email 'john@bank.com': ");
            throw new DuplicateEmailException("john@bank.com");
        } catch (DuplicateEmailException ex) {
            System.out.println("CAUGHT " + ex.getClass().getSimpleName());
            System.out.println("  Friendly Message:  " + ex.getMessage());
            System.out.println("  Duplicate Email:   " + ex.getEmail());
        }

        // 7. AuthenticationException
        try {
            System.out.print("\n[Scenario 7] Authenticating with bad credentials: ");
            throw new AuthenticationException("Invalid email or password provided.");
        } catch (AuthenticationException ex) {
            System.out.println("CAUGHT " + ex.getClass().getSimpleName());
            System.out.println("  Friendly Message:  " + ex.getMessage());
            System.out.println("  Code:              " + ex.getErrorCode().getCode());
        }

        // 8. ValidationException
        try {
            System.out.print("\n[Scenario 8] Validating weak password 'secret': ");
            Validator.validatePassword("secret");
        } catch (ValidationException ex) {
            System.out.println("CAUGHT " + ex.getClass().getSimpleName());
            System.out.println("  Friendly Message:  " + ex.getMessage());
            System.out.println("  Field Violations:  " + ex.getFieldErrors());
        }

        // 9. ServiceBusyException
        try {
            System.out.print("\n[Scenario 9] Lock acquisition timeout on concurrent transfer: ");
            throw new ServiceBusyException("Account lock timeout exceeded after 5000ms. Service is busy.");
        } catch (ServiceBusyException ex) {
            System.out.println("CAUGHT " + ex.getClass().getSimpleName());
            System.out.println("  Friendly Message:  " + ex.getMessage());
            System.out.println("  Code:              " + ex.getErrorCode().getCode());
        }

        // 10. General BankingException (Closed Account)
        try {
            System.out.print("\n[Scenario 10] Transacting on permanently closed account: ");
            CurrentAccount closedAcc = new CurrentAccount("CUR-CLOSED", 1L, new BigDecimal("0.00"));
            closedAcc.setStatus(AccountStatus.CLOSED);
            closedAcc.deposit(new BigDecimal("100.00"));
        } catch (BankingException ex) {
            System.out.println("CAUGHT " + ex.getClass().getSimpleName());
            System.out.println("  Friendly Message:  " + ex.getMessage());
            System.out.println("  Code:              " + ex.getErrorCode().getCode());
        }
    }
}

package com.bank.model;

import com.bank.util.Money;
import java.math.BigDecimal;

/**
 * Factory pattern implementation for creating bank account domain instances.
 * <p>
 * Decouples client code from concrete implementations ({@link SavingsAccount}, {@link CurrentAccount})
 * and ensures initial states, balances, and operational limits are correctly initialized.
 * </p>
 */
public final class AccountFactory {

    private AccountFactory() {
        // Prevent instantiation of factory class
    }

    /**
     * Creates a {@link SavingsAccount} with default minimum balance ($500.00).
     *
     * @param accountNo unique account identifier
     * @param ownerId customer user identifier
     * @param initialBalance starting balance
     * @return newly instantiated {@link SavingsAccount}
     */
    public static SavingsAccount createSavingsAccount(String accountNo, Long ownerId, BigDecimal initialBalance) {
        return new SavingsAccount(accountNo, ownerId, Money.of(initialBalance));
    }

    /**
     * Creates a {@link SavingsAccount} with a specified minimum balance.
     *
     * @param accountNo unique account identifier
     * @param ownerId customer user identifier
     * @param initialBalance starting balance
     * @param minimumBalance required minimum balance threshold
     * @return newly instantiated {@link SavingsAccount}
     */
    public static SavingsAccount createSavingsAccount(String accountNo, Long ownerId,
                                                      BigDecimal initialBalance, BigDecimal minimumBalance) {
        return new SavingsAccount(accountNo, ownerId, Money.of(initialBalance),
                AccountStatus.ACTIVE, Money.of(minimumBalance));
    }

    /**
     * Creates a {@link CurrentAccount} with zero initial overdraft limit.
     *
     * @param accountNo unique account identifier
     * @param ownerId customer user identifier
     * @param initialBalance starting balance
     * @return newly instantiated {@link CurrentAccount}
     */
    public static CurrentAccount createCurrentAccount(String accountNo, Long ownerId, BigDecimal initialBalance) {
        return new CurrentAccount(accountNo, ownerId, Money.of(initialBalance));
    }

    /**
     * Creates a {@link CurrentAccount} with an approved overdraft credit limit.
     *
     * @param accountNo unique account identifier
     * @param ownerId customer user identifier
     * @param initialBalance starting balance
     * @param overdraftLimit approved overdraft limit
     * @return newly instantiated {@link CurrentAccount}
     */
    public static CurrentAccount createCurrentAccount(String accountNo, Long ownerId,
                                                      BigDecimal initialBalance, BigDecimal overdraftLimit) {
        return new CurrentAccount(accountNo, ownerId, Money.of(initialBalance),
                AccountStatus.ACTIVE, Money.of(overdraftLimit));
    }

    /**
     * Polymorphic factory method creating an account based on {@link AccountType}.
     *
     * @param type account type (SAVINGS or CURRENT)
     * @param accountNo unique account identifier
     * @param ownerId customer user identifier
     * @param initialBalance starting balance
     * @return the newly instantiated {@link Account} subtype
     */
    public static Account createAccount(AccountType type, String accountNo, Long ownerId, BigDecimal initialBalance) {
        if (type == null) {
            throw new IllegalArgumentException("AccountType cannot be null.");
        }
        return switch (type) {
            case SAVINGS -> createSavingsAccount(accountNo, ownerId, initialBalance);
            case CURRENT -> createCurrentAccount(accountNo, ownerId, initialBalance);
        };
    }

    /**
     * Polymorphic factory method creating an account based on {@link AccountType} with a custom threshold.
     *
     * @param type account type (SAVINGS or CURRENT)
     * @param accountNo unique account identifier
     * @param ownerId customer user identifier
     * @param initialBalance starting balance
     * @param limitOrMin either the minimum balance for SAVINGS or the overdraft limit for CURRENT
     * @return the newly instantiated {@link Account} subtype
     */
    public static Account createAccount(AccountType type, String accountNo, Long ownerId,
                                        BigDecimal initialBalance, BigDecimal limitOrMin) {
        if (type == null) {
            throw new IllegalArgumentException("AccountType cannot be null.");
        }
        return switch (type) {
            case SAVINGS -> createSavingsAccount(accountNo, ownerId, initialBalance, limitOrMin);
            case CURRENT -> createCurrentAccount(accountNo, ownerId, initialBalance, limitOrMin);
        };
    }
}

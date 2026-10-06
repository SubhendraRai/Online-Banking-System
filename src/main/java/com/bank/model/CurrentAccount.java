package com.bank.model;

import com.bank.exception.BankingException;
import com.bank.exception.InsufficientFundsException;
import com.bank.util.Money;
import java.math.BigDecimal;

/**
 * Domain model representing a commercial or retail Current Account.
 * <p>
 * Supports credit facilities via an approved overdraft limit. Balance may become negative
 * down to {@code -overdraftLimit}.
 * </p>
 */
public class CurrentAccount extends Account {

    private BigDecimal overdraftLimit;

    /**
     * Default constructor initializing with zero overdraft.
     */
    public CurrentAccount() {
        super();
        this.overdraftLimit = Money.ZERO;
    }

    /**
     * Constructor initializing current account with zero overdraft.
     *
     * @param accountNo account number
     * @param ownerId customer user ID
     * @param balance opening balance
     */
    public CurrentAccount(String accountNo, Long ownerId, BigDecimal balance) {
        this(accountNo, ownerId, balance, AccountStatus.ACTIVE, Money.ZERO);
    }

    /**
     * Full constructor initializing current account with approved overdraft limit.
     *
     * @param accountNo account number
     * @param ownerId customer user ID
     * @param balance opening balance
     * @param status operational status
     * @param overdraftLimit approved credit line overdraft limit
     */
    public CurrentAccount(String accountNo, Long ownerId, BigDecimal balance,
                          AccountStatus status, BigDecimal overdraftLimit) {
        super(accountNo, ownerId, balance, status);
        this.overdraftLimit = overdraftLimit != null ? Money.of(overdraftLimit) : Money.ZERO;
    }

    @Override
    protected void doDeposit(BigDecimal amount) throws BankingException {
        this.balance = Money.add(this.balance, amount);
    }

    @Override
    protected void doWithdraw(BigDecimal amount) throws BankingException {
        BigDecimal available = getAvailableBalance();
        if (Money.isGreaterThan(amount, available)) {
            throw new InsufficientFundsException(amount, available,
                    String.format("Current account withdrawal of %s exceeds total available limit %s (Balance: %s, Overdraft: %s).",
                            Money.format(amount), Money.format(available),
                            Money.format(balance), Money.format(overdraftLimit)));
        }
        this.balance = Money.subtract(this.balance, amount);
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.CURRENT;
    }

    @Override
    public BigDecimal getAvailableBalance() {
        return Money.add(this.balance, this.overdraftLimit);
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit != null ? Money.of(overdraftLimit) : Money.ZERO;
    }
}

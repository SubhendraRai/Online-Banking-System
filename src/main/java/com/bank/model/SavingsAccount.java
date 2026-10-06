package com.bank.model;

import com.bank.exception.BankingException;
import com.bank.exception.InsufficientFundsException;
import com.bank.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Domain model representing a retail Savings Account.
 * <p>
 * Enforces a mandatory minimum balance threshold. Implements {@link InterestBearing}
 * to support periodic interest accrual on customer balances.
 * </p>
 */
public class SavingsAccount extends Account implements InterestBearing {

    public static final BigDecimal DEFAULT_MINIMUM_BALANCE = new BigDecimal("500.00");

    private BigDecimal minimumBalance;

    /**
     * Default constructor initializing with default minimum balance.
     */
    public SavingsAccount() {
        super();
        this.minimumBalance = DEFAULT_MINIMUM_BALANCE;
    }

    /**
     * Constructor initializing savings account with default minimum balance.
     *
     * @param accountNo account number
     * @param ownerId customer user ID
     * @param balance opening balance
     */
    public SavingsAccount(String accountNo, Long ownerId, BigDecimal balance) {
        this(accountNo, ownerId, balance, AccountStatus.ACTIVE, DEFAULT_MINIMUM_BALANCE);
    }

    /**
     * Full constructor initializing savings account with specific minimum balance.
     *
     * @param accountNo account number
     * @param ownerId customer user ID
     * @param balance opening balance
     * @param status operational status
     * @param minimumBalance required minimum balance threshold
     */
    public SavingsAccount(String accountNo, Long ownerId, BigDecimal balance,
                          AccountStatus status, BigDecimal minimumBalance) {
        super(accountNo, ownerId, balance, status);
        this.minimumBalance = minimumBalance != null ? Money.of(minimumBalance) : DEFAULT_MINIMUM_BALANCE;
    }

    @Override
    protected void doDeposit(BigDecimal amount) throws BankingException {
        this.balance = Money.add(this.balance, amount);
    }

    @Override
    protected void doWithdraw(BigDecimal amount) throws BankingException {
        BigDecimal projectedBalance = Money.subtract(this.balance, amount);
        if (projectedBalance.compareTo(this.minimumBalance) < 0) {
            BigDecimal available = getAvailableBalance();
            throw new InsufficientFundsException(amount, available,
                    String.format("Savings withdrawal of %s violates minimum balance %s (Current: %s, Available: %s).",
                            Money.format(amount), Money.format(minimumBalance),
                            Money.format(balance), Money.format(available)));
        }
        this.balance = projectedBalance;
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.SAVINGS;
    }

    @Override
    public BigDecimal getAvailableBalance() {
        BigDecimal available = Money.subtract(this.balance, this.minimumBalance);
        return Money.isNegative(available) ? Money.ZERO : available;
    }

    @Override
    public BigDecimal calculateMonthlyInterest(BigDecimal annualRatePercent) {
        if (annualRatePercent == null || Money.isNegative(annualRatePercent) || Money.isNegative(this.balance)) {
            return Money.ZERO;
        }
        BigDecimal rateFraction = annualRatePercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal annualInterest = this.balance.multiply(rateFraction);
        return annualInterest.divide(BigDecimal.valueOf(12), Money.SCALE, Money.ROUNDING_MODE);
    }

    public BigDecimal getMinimumBalance() {
        return minimumBalance;
    }

    public void setMinimumBalance(BigDecimal minimumBalance) {
        this.minimumBalance = minimumBalance != null ? Money.of(minimumBalance) : DEFAULT_MINIMUM_BALANCE;
    }
}

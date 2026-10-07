package com.bank.model;

import com.bank.exception.AccountFrozenException;
import com.bank.exception.BankingException;
import com.bank.exception.ErrorCode;
import com.bank.util.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract domain model representing a financial bank account.
 * <p>
 * Implements the <b>Template Method Pattern</b> for {@link #deposit(BigDecimal)} and
 * {@link #withdraw(BigDecimal)}. The template methods enforce common invariants:
 * <ul>
 *   <li>The account must not be in {@link AccountStatus#FROZEN} or {@link AccountStatus#CLOSED} state.</li>
 *   <li>The transaction amount must be strictly positive and normalized via {@link Money}.</li>
 * </ul>
 * Subclasses specialize balance update rules and credit checks by implementing the protected
 * hooks {@link #doDeposit(BigDecimal)} and {@link #doWithdraw(BigDecimal)}.
 * </p>
 */
public abstract class Account {

    private String accountNo;
    private Long ownerId;
    protected BigDecimal balance;
    private AccountStatus status;
    private LocalDateTime openedAt;

    /**
     * Default constructor.
     */
    protected Account() {
        this.balance = Money.ZERO;
        this.status = AccountStatus.ACTIVE;
        this.openedAt = LocalDateTime.now();
    }

    /**
     * Parameterized constructor initializing account attributes.
     *
     * @param accountNo unique 12-character account number
     * @param ownerId unique identifier of the owning user
     * @param balance opening balance
     * @param status operational status
     */
    protected Account(String accountNo, Long ownerId, BigDecimal balance, AccountStatus status) {
        this.accountNo = accountNo;
        this.ownerId = ownerId;
        this.balance = Money.of(balance);
        this.status = status != null ? status : AccountStatus.ACTIVE;
        this.openedAt = LocalDateTime.now();
    }

    /**
     * Template method executing a deposit.
     * <p>
     * Verifies account state and validates amount positivity before invoking the
     * polymorphic hook {@link #doDeposit(BigDecimal)}.
     * </p>
     *
     * @param amount monetary amount to deposit
     * @throws BankingException if the account is frozen, closed, or amount is non-positive
     */
    public final void deposit(BigDecimal amount) throws BankingException {
        validateAccountOperational();
        Money.validatePositive(amount, "Deposit amount");
        doDeposit(Money.of(amount));
    }

    /**
     * Template method executing a withdrawal.
     * <p>
     * Verifies account state and validates amount positivity before invoking the
     * polymorphic hook {@link #doWithdraw(BigDecimal)}.
     * </p>
     *
     * @param amount monetary amount to withdraw
     * @throws BankingException if the account is frozen, closed, amount is invalid, or funds are insufficient
     */
    public final void withdraw(BigDecimal amount) throws BankingException {
        validateAccountOperational();
        Money.validatePositive(amount, "Withdrawal amount");
        doWithdraw(Money.of(amount));
    }

    /**
     * Common template validation enforcing operational status invariants.
     *
     * @throws BankingException if account is frozen or closed
     */
    private void validateAccountOperational() throws BankingException {
        if (this.status == AccountStatus.FROZEN) {
            throw new AccountFrozenException(this.accountNo);
        }
        if (this.status == AccountStatus.CLOSED) {
            throw new BankingException(ErrorCode.ACCOUNT_CLOSED,
                    "Account " + this.accountNo + " is closed. No transactions are permitted.");
        }
    }

    /**
     * Protected polymorphic hook executing account-specific deposit mechanics.
     *
     * @param amount strictly positive normalized monetary amount
     * @throws BankingException if specialized deposit validation fails
     */
    protected abstract void doDeposit(BigDecimal amount) throws BankingException;

    /**
     * Protected polymorphic hook executing account-specific withdrawal mechanics and balance rules.
     *
     * @param amount strictly positive normalized monetary amount
     * @throws BankingException if account-specific limits (minimum balance or overdraft) are breached
     */
    protected abstract void doWithdraw(BigDecimal amount) throws BankingException;

    /**
     * Returns the type of bank account.
     *
     * @return {@link AccountType}
     */
    public abstract AccountType getAccountType();

    /**
     * Computes the maximum funds available for withdrawal (taking into account minimum balance or overdraft).
     *
     * @return available monetary amount
     */
    public abstract BigDecimal getAvailableBalance();

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getFormattedBalance() {
        return Money.formatInr(this.balance);
    }

    public String getFormattedAvailableBalance() {
        return Money.formatInr(getAvailableBalance());
    }

    public void setBalance(BigDecimal balance) {
        this.balance = Money.of(balance);
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    public boolean isFrozen() {
        return this.status == AccountStatus.FROZEN;
    }

    public boolean isClosed() {
        return this.status == AccountStatus.CLOSED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(accountNo, account.accountNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNo);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "accountNo='" + accountNo + '\'' +
                ", ownerId=" + ownerId +
                ", balance=" + Money.format(balance) +
                ", status=" + status +
                '}';
    }
}

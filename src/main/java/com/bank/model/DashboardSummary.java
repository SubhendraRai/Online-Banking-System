package com.bank.model;

import com.bank.util.Money;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable Data Transfer Object summarizing a customer's retail portfolio.
 * <p>
 * Aggregates cumulative balances, linked accounts, the 5 most recent transactions,
 * and monthly cash flow metrics (money in vs. money out).
 * </p>
 */
public class DashboardSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    private final BigDecimal totalBalance;
    private final List<Account> accounts;
    private final List<Transaction> recentTransactions;
    private final BigDecimal moneyInThisMonth;
    private final BigDecimal moneyOutThisMonth;

    /**
     * Constructs a {@code DashboardSummary} instance.
     *
     * @param totalBalance total net balance across all linked accounts
     * @param accounts list of user's active accounts
     * @param recentTransactions up to 5 newest ledger transactions
     * @param moneyInThisMonth aggregate credits deposited/transferred into user accounts this calendar month
     * @param moneyOutThisMonth aggregate debits withdrawn/transferred from user accounts this calendar month
     */
    public DashboardSummary(BigDecimal totalBalance,
                            List<Account> accounts,
                            List<Transaction> recentTransactions,
                            BigDecimal moneyInThisMonth,
                            BigDecimal moneyOutThisMonth) {
        this.totalBalance = totalBalance != null ? Money.of(totalBalance) : Money.ZERO;
        this.accounts = accounts != null ? List.copyOf(accounts) : Collections.emptyList();
        this.recentTransactions = recentTransactions != null ? List.copyOf(recentTransactions) : Collections.emptyList();
        this.moneyInThisMonth = moneyInThisMonth != null ? Money.of(moneyInThisMonth) : Money.ZERO;
        this.moneyOutThisMonth = moneyOutThisMonth != null ? Money.of(moneyOutThisMonth) : Money.ZERO;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public String getFormattedTotalBalance() {
        return Money.formatInr(this.totalBalance);
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public List<Transaction> getRecentTransactions() {
        return recentTransactions;
    }

    public BigDecimal getMoneyInThisMonth() {
        return moneyInThisMonth;
    }

    public String getFormattedMoneyInThisMonth() {
        return Money.formatInr(this.moneyInThisMonth);
    }

    public BigDecimal getMoneyOutThisMonth() {
        return moneyOutThisMonth;
    }

    public String getFormattedMoneyOutThisMonth() {
        return Money.formatInr(this.moneyOutThisMonth);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashboardSummary that = (DashboardSummary) o;
        return Objects.equals(totalBalance, that.totalBalance)
                && Objects.equals(accounts, that.accounts)
                && Objects.equals(recentTransactions, that.recentTransactions)
                && Objects.equals(moneyInThisMonth, that.moneyInThisMonth)
                && Objects.equals(moneyOutThisMonth, that.moneyOutThisMonth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalBalance, accounts, recentTransactions, moneyInThisMonth, moneyOutThisMonth);
    }

    @Override
    public String toString() {
        return "DashboardSummary{" +
                "totalBalance=" + Money.format(totalBalance) +
                ", accountsCount=" + accounts.size() +
                ", recentTransactionsCount=" + recentTransactions.size() +
                ", moneyInThisMonth=" + Money.format(moneyInThisMonth) +
                ", moneyOutThisMonth=" + Money.format(moneyOutThisMonth) +
                '}';
    }
}

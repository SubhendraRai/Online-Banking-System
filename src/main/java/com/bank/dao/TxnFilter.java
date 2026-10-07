package com.bank.dao;

import com.bank.model.TxnType;
import com.bank.util.Money;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Filter criteria DTO for querying transaction history.
 * <p>
 * Supports dynamic filtering by date window, transaction type, monetary amount bounds,
 * and text keyword matching against either transaction remarks or counterparty account number.
 * </p>
 */
public class TxnFilter {

    private LocalDate fromDate;
    private LocalDate toDate;
    private TxnType type;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String keyword;

    /**
     * Default empty constructor.
     */
    public TxnFilter() {
    }

    /**
     * Parameterized constructor initializing filter criteria.
     *
     * @param fromDate lower bound inclusive date
     * @param toDate upper bound inclusive date
     * @param type transaction type
     * @param minAmount lower bound inclusive monetary amount
     * @param maxAmount upper bound inclusive monetary amount
     * @param keyword search keyword matching remarks or counterparty account number
     */
    public TxnFilter(LocalDate fromDate, LocalDate toDate, TxnType type,
                     BigDecimal minAmount, BigDecimal maxAmount, String keyword) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.type = type;
        this.minAmount = minAmount != null ? Money.of(minAmount) : null;
        this.maxAmount = maxAmount != null ? Money.of(maxAmount) : null;
        this.keyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
    }

    /**
     * Returns a new builder instance for constructing {@link TxnFilter}.
     *
     * @return filter builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public TxnType getType() {
        return type;
    }

    public void setType(TxnType type) {
        this.type = type;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount != null ? Money.of(minAmount) : null;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount != null ? Money.of(maxAmount) : null;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
    }

    public boolean hasFilters() {
        return fromDate != null || toDate != null || type != null ||
                minAmount != null || maxAmount != null || keyword != null;
    }

    @Override
    public String toString() {
        return "TxnFilter{" +
                "fromDate=" + fromDate +
                ", toDate=" + toDate +
                ", type=" + type +
                ", minAmount=" + minAmount +
                ", maxAmount=" + maxAmount +
                ", keyword='" + keyword + '\'' +
                '}';
    }

    /**
     * Builder for fluent instantiation of {@link TxnFilter}.
     */
    public static final class Builder {
        private LocalDate fromDate;
        private LocalDate toDate;
        private TxnType type;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private String keyword;

        public Builder fromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public Builder toDate(LocalDate toDate) {
            this.toDate = toDate;
            return this;
        }

        public Builder type(TxnType type) {
            this.type = type;
            return this;
        }

        public Builder minAmount(BigDecimal minAmount) {
            this.minAmount = minAmount != null ? Money.of(minAmount) : null;
            return this;
        }

        public Builder maxAmount(BigDecimal maxAmount) {
            this.maxAmount = maxAmount != null ? Money.of(maxAmount) : null;
            return this;
        }

        public Builder keyword(String keyword) {
            this.keyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
            return this;
        }

        public TxnFilter build() {
            return new TxnFilter(fromDate, toDate, type, minAmount, maxAmount, keyword);
        }
    }
}

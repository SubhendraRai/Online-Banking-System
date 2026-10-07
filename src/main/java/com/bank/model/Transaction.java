package com.bank.model;

import com.bank.util.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable domain model representing a double-entry financial ledger transaction.
 * <p>
 * Constructed exclusively via {@link Builder} or static factory methods
 * ({@link #transfer(String, String, BigDecimal, String)}, {@link #deposit(String, BigDecimal, String)},
 * {@link #withdrawal(String, BigDecimal, String)}). All fields are final and unmodifiable.
 * </p>
 */
public final class Transaction {

    private final Long txnId;
    private final String fromAccount;
    private final String toAccount;
    private final TxnType txnType;
    private final BigDecimal amount;
    private final TxnStatus status;
    private final String remarks;
    private final LocalDateTime createdAt;

    private Transaction(Builder builder) {
        this.txnId = builder.txnId;
        this.fromAccount = builder.fromAccount;
        this.toAccount = builder.toAccount;
        this.txnType = Objects.requireNonNull(builder.txnType, "TxnType cannot be null.");
        this.amount = Money.of(Objects.requireNonNull(builder.amount, "Amount cannot be null."));
        if (!Money.isPositive(this.amount)) {
            throw new IllegalArgumentException("Transaction amount must be strictly greater than 0.00.");
        }
        this.status = builder.status != null ? builder.status : TxnStatus.SUCCESS;
        this.remarks = builder.remarks;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    /**
     * Static factory creating an immutable fund transfer transaction.
     *
     * @param fromAccount source account number
     * @param toAccount destination account number
     * @param amount monetary transfer amount
     * @param remarks transfer remarks / description
     * @return immutable {@link Transaction}
     */
    public static Transaction transfer(String fromAccount, String toAccount, BigDecimal amount, String remarks) {
        return builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .txnType(TxnType.TRANSFER)
                .amount(amount)
                .remarks(remarks)
                .status(TxnStatus.SUCCESS)
                .build();
    }

    /**
     * Static factory creating an immutable deposit transaction.
     *
     * @param toAccount credited destination account number
     * @param amount deposit amount
     * @param remarks deposit description
     * @return immutable {@link Transaction}
     */
    public static Transaction deposit(String toAccount, BigDecimal amount, String remarks) {
        return builder()
                .toAccount(toAccount)
                .txnType(TxnType.DEPOSIT)
                .amount(amount)
                .remarks(remarks)
                .status(TxnStatus.SUCCESS)
                .build();
    }

    /**
     * Static factory creating an immutable withdrawal transaction.
     *
     * @param fromAccount debited source account number
     * @param amount withdrawal amount
     * @param remarks withdrawal description
     * @return immutable {@link Transaction}
     */
    public static Transaction withdrawal(String fromAccount, BigDecimal amount, String remarks) {
        return builder()
                .fromAccount(fromAccount)
                .txnType(TxnType.WITHDRAWAL)
                .amount(amount)
                .remarks(remarks)
                .status(TxnStatus.SUCCESS)
                .build();
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return transaction builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public Long getTxnId() {
        return txnId;
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public TxnType getTxnType() {
        return txnType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getFormattedAmount() {
        return Money.formatInr(this.amount);
    }

    public TxnStatus getStatus() {
        return status;
    }

    public String getRemarks() {
        return remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(txnId, that.txnId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(txnId);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "txnId=" + txnId +
                ", from='" + fromAccount + '\'' +
                ", to='" + toAccount + '\'' +
                ", type=" + txnType +
                ", amount=" + Money.format(amount) +
                ", status=" + status +
                '}';
    }

    /**
     * Builder for constructing immutable {@link Transaction} instances.
     */
    public static final class Builder {
        private Long txnId;
        private String fromAccount;
        private String toAccount;
        private TxnType txnType;
        private BigDecimal amount;
        private TxnStatus status = TxnStatus.SUCCESS;
        private String remarks;
        private LocalDateTime createdAt;

        public Builder txnId(Long txnId) {
            this.txnId = txnId;
            return this;
        }

        public Builder fromAccount(String fromAccount) {
            this.fromAccount = fromAccount;
            return this;
        }

        public Builder toAccount(String toAccount) {
            this.toAccount = toAccount;
            return this;
        }

        public Builder txnType(TxnType txnType) {
            this.txnType = txnType;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder status(TxnStatus status) {
            this.status = status;
            return this;
        }

        public Builder remarks(String remarks) {
            this.remarks = remarks;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Transaction build() {
            return new Transaction(this);
        }
    }
}

package com.bank.model;

import com.bank.util.Money;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Result Data Transfer Object capturing successful fund transfer details
 * for rendering the transaction receipt page.
 */
public class TransferReceipt implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SESSION_KEY = "TRANSFER_EXECUTION_RECEIPT";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a");

    private final Long txnId;
    private final String fromAccount;
    private final String toAccount;
    private final String recipientName;
    private final BigDecimal amount;
    private final String remarks;
    private final LocalDateTime timestamp;

    public TransferReceipt(Long txnId, String fromAccount, String toAccount,
                           String recipientName, BigDecimal amount, String remarks,
                           LocalDateTime timestamp) {
        this.txnId = Objects.requireNonNull(txnId, "txnId cannot be null");
        this.fromAccount = Objects.requireNonNull(fromAccount, "fromAccount cannot be null");
        this.toAccount = Objects.requireNonNull(toAccount, "toAccount cannot be null");
        this.recipientName = (recipientName != null && !recipientName.isBlank()) ? recipientName.trim() : "Beneficiary";
        this.amount = Money.of(Objects.requireNonNull(amount, "amount cannot be null"));
        this.remarks = (remarks != null && !remarks.isBlank()) ? remarks.trim() : "Fund Transfer";
        this.timestamp = (timestamp != null) ? timestamp : LocalDateTime.now();
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

    public String getRecipientName() {
        return recipientName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getFormattedAmount() {
        return Money.formatInr(amount);
    }

    public String getRemarks() {
        return remarks;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }

    @Override
    public String toString() {
        return "TransferReceipt{" +
                "txnId=" + txnId +
                ", from='" + fromAccount + '\'' +
                ", to='" + toAccount + '\'' +
                ", recipient='" + recipientName + '\'' +
                ", amount=" + Money.formatInr(amount) +
                '}';
    }
}

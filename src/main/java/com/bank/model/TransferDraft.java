package com.bank.model;

import com.bank.util.Money;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Session-scoped Data Transfer Object storing staged fund transfer details
 * during the 3-step confirmation workflow.
 * <p>
 * Carries a unique one-time submission token to protect against duplicate form submissions
 * and replay attacks.
 * </p>
 */
public class TransferDraft implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SESSION_KEY = "PENDING_TRANSFER_DRAFT";

    private final String token;
    private final String fromAccount;
    private final String toAccount;
    private final String recipientName;
    private final BigDecimal amount;
    private final String remarks;
    private final LocalDateTime createdAt;

    public TransferDraft(String token, String fromAccount, String toAccount,
                         String recipientName, BigDecimal amount, String remarks) {
        this.token = Objects.requireNonNull(token, "token cannot be null");
        this.fromAccount = Objects.requireNonNull(fromAccount, "fromAccount cannot be null");
        this.toAccount = Objects.requireNonNull(toAccount, "toAccount cannot be null");
        this.recipientName = (recipientName != null && !recipientName.isBlank()) ? recipientName.trim() : "Beneficiary";
        this.amount = Money.of(Objects.requireNonNull(amount, "amount cannot be null"));
        this.remarks = (remarks != null && !remarks.isBlank()) ? remarks.trim() : "Fund Transfer";
        this.createdAt = LocalDateTime.now();
    }

    public String getToken() {
        return token;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "TransferDraft{" +
                "token='" + token + '\'' +
                ", from='" + fromAccount + '\'' +
                ", to='" + toAccount + '\'' +
                ", recipient='" + recipientName + '\'' +
                ", amount=" + Money.formatInr(amount) +
                '}';
    }
}

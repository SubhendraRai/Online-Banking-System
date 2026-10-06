package com.bank.model;

import java.time.LocalDateTime;

/**
 * Domain model representing a fraud detection rule alert flagged on a financial transaction.
 */
public class FraudAlert {

    private Long alertId;
    private Long txnId;
    private String ruleCode;
    private String reason;
    private AlertStatus status;
    private LocalDateTime createdAt;
    private Long reviewedBy;

    public FraudAlert() {
        this.status = AlertStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public FraudAlert(Long alertId, Long txnId, String ruleCode, String reason,
                      AlertStatus status, LocalDateTime createdAt, Long reviewedBy) {
        this.alertId = alertId;
        this.txnId = txnId;
        this.ruleCode = ruleCode;
        this.reason = reason;
        this.status = status != null ? status : AlertStatus.OPEN;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.reviewedBy = reviewedBy;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public Long getTxnId() {
        return txnId;
    }

    public void setTxnId(Long txnId) {
        this.txnId = txnId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    @Override
    public String toString() {
        return "FraudAlert{" +
                "alertId=" + alertId +
                ", txnId=" + txnId +
                ", ruleCode='" + ruleCode + '\'' +
                ", reason='" + reason + '\'' +
                ", status=" + status +
                '}';
    }
}

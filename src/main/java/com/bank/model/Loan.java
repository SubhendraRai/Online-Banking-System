package com.bank.model;

import com.bank.util.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model representing a customer retail loan application and repayment facility.
 */
public class Loan {

    private Long loanId;
    private Long userId;
    private String accountNo;
    private BigDecimal principal;
    private BigDecimal annualRate;
    private Integer tenureMonths;
    private BigDecimal emi;
    private LoanStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime decidedAt;
    private Long decidedBy;

    public Loan() {
        this.status = LoanStatus.PENDING;
        this.appliedAt = LocalDateTime.now();
    }

    public Loan(Long loanId, Long userId, String accountNo, BigDecimal principal,
                BigDecimal annualRate, Integer tenureMonths, BigDecimal emi,
                LoanStatus status, LocalDateTime appliedAt, LocalDateTime decidedAt, Long decidedBy) {
        this.loanId = loanId;
        this.userId = userId;
        this.accountNo = accountNo;
        this.principal = Money.of(principal);
        this.annualRate = Money.of(annualRate);
        this.tenureMonths = tenureMonths;
        this.emi = Money.of(emi);
        this.status = status != null ? status : LoanStatus.PENDING;
        this.appliedAt = appliedAt != null ? appliedAt : LocalDateTime.now();
        this.decidedAt = decidedAt;
        this.decidedBy = decidedBy;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public void setPrincipal(BigDecimal principal) {
        this.principal = Money.of(principal);
    }

    public BigDecimal getAnnualRate() {
        return annualRate;
    }

    public void setAnnualRate(BigDecimal annualRate) {
        this.annualRate = Money.of(annualRate);
    }

    public Integer getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(Integer tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public BigDecimal getEmi() {
        return emi;
    }

    public void setEmi(BigDecimal emi) {
        this.emi = Money.of(emi);
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Long getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(Long decidedBy) {
        this.decidedBy = decidedBy;
    }

    @Override
    public String toString() {
        return "Loan{" +
                "loanId=" + loanId +
                ", userId=" + userId +
                ", accountNo='" + accountNo + '\'' +
                ", principal=" + Money.format(principal) +
                ", emi=" + Money.format(emi) +
                ", status=" + status +
                '}';
    }
}

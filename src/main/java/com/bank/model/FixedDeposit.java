package com.bank.model;

import com.bank.util.Money;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain model representing a customer Fixed Deposit investment.
 */
public class FixedDeposit {

    private Long fdId;
    private String accountNo;
    private BigDecimal principal;
    private BigDecimal annualRate;
    private Integer tenureMonths;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private BigDecimal maturityAmount;
    private FdStatus status;

    public FixedDeposit() {
        this.status = FdStatus.ACTIVE;
        this.startDate = LocalDate.now();
    }

    public FixedDeposit(Long fdId, String accountNo, BigDecimal principal,
                        BigDecimal annualRate, Integer tenureMonths, LocalDate startDate,
                        LocalDate maturityDate, BigDecimal maturityAmount, FdStatus status) {
        this.fdId = fdId;
        this.accountNo = accountNo;
        this.principal = Money.of(principal);
        this.annualRate = Money.of(annualRate);
        this.tenureMonths = tenureMonths;
        this.startDate = startDate != null ? startDate : LocalDate.now();
        this.maturityDate = maturityDate;
        this.maturityAmount = Money.of(maturityAmount);
        this.status = status != null ? status : FdStatus.ACTIVE;
    }

    public Long getFdId() {
        return fdId;
    }

    public void setFdId(Long fdId) {
        this.fdId = fdId;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }

    public BigDecimal getMaturityAmount() {
        return maturityAmount;
    }

    public void setMaturityAmount(BigDecimal maturityAmount) {
        this.maturityAmount = Money.of(maturityAmount);
    }

    public FdStatus getStatus() {
        return status;
    }

    public void setStatus(FdStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "FixedDeposit{" +
                "fdId=" + fdId +
                ", accountNo='" + accountNo + '\'' +
                ", principal=" + Money.format(principal) +
                ", maturityAmount=" + Money.format(maturityAmount) +
                ", status=" + status +
                '}';
    }
}

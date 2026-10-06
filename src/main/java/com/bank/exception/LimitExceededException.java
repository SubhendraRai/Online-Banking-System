package com.bank.exception;

import java.math.BigDecimal;

/**
 * Checked exception thrown when an operation exceeds an allowable limit, such as
 * a daily transfer limit, single transaction maximum, or overdraft limit threshold.
 */
public class LimitExceededException extends BankingException {

    private static final long serialVersionUID = 1L;

    private final BigDecimal limit;
    private final BigDecimal requestedAmount;

    /**
     * Constructs a {@code LimitExceededException} specifying the exceeded threshold and requested amount.
     *
     * @param limit the configured upper threshold
     * @param requestedAmount the requested transaction amount
     */
    public LimitExceededException(BigDecimal limit, BigDecimal requestedAmount) {
        super(ErrorCode.LIMIT_EXCEEDED,
                String.format("Requested amount %s exceeds permissible limit of %s.",
                        requestedAmount != null ? requestedAmount.toPlainString() : "0.00",
                        limit != null ? limit.toPlainString() : "0.00"));
        this.limit = limit;
        this.requestedAmount = requestedAmount;
    }

    /**
     * Constructs a {@code LimitExceededException} with a custom message.
     *
     * @param limit the configured upper threshold
     * @param requestedAmount the requested transaction amount
     * @param message detailed description of the violation
     */
    public LimitExceededException(BigDecimal limit, BigDecimal requestedAmount, String message) {
        super(ErrorCode.LIMIT_EXCEEDED, message);
        this.limit = limit;
        this.requestedAmount = requestedAmount;
    }

    /**
     * Returns the configured limit that was exceeded.
     *
     * @return the limit threshold
     */
    public BigDecimal getLimit() {
        return limit;
    }

    /**
     * Returns the requested transaction amount.
     *
     * @return the requested amount
     */
    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}

package com.bank.exception;

import java.math.BigDecimal;

/**
 * Checked exception thrown when an account has insufficient funds to fulfill a withdrawal or transfer.
 * <p>
 * Carries both the requested amount and the maximum available funds (including any permitted overdraft).
 * </p>
 */
public class InsufficientFundsException extends BankingException {

    private static final long serialVersionUID = 1L;

    private final BigDecimal requestedAmount;
    private final BigDecimal availableAmount;

    /**
     * Constructs an {@code InsufficientFundsException} capturing requested and available amounts.
     *
     * @param requestedAmount the amount requested for withdrawal/transfer
     * @param availableAmount the actual available funds at the time of evaluation
     */
    public InsufficientFundsException(BigDecimal requestedAmount, BigDecimal availableAmount) {
        super(ErrorCode.INSUFFICIENT_FUNDS,
                String.format("Requested amount %s exceeds available balance %s.",
                        requestedAmount != null ? requestedAmount.toPlainString() : "0.00",
                        availableAmount != null ? availableAmount.toPlainString() : "0.00"));
        this.requestedAmount = requestedAmount != null ? requestedAmount : BigDecimal.ZERO;
        this.availableAmount = availableAmount != null ? availableAmount : BigDecimal.ZERO;
    }

    /**
     * Constructs an {@code InsufficientFundsException} with a custom message.
     *
     * @param requestedAmount the amount requested for withdrawal/transfer
     * @param availableAmount the actual available funds at the time of evaluation
     * @param message detailed description of the shortfall condition
     */
    public InsufficientFundsException(BigDecimal requestedAmount, BigDecimal availableAmount, String message) {
        super(ErrorCode.INSUFFICIENT_FUNDS, message);
        this.requestedAmount = requestedAmount != null ? requestedAmount : BigDecimal.ZERO;
        this.availableAmount = availableAmount != null ? availableAmount : BigDecimal.ZERO;
    }

    /**
     * Returns the requested transaction amount that could not be honored.
     *
     * @return the requested amount
     */
    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    /**
     * Returns the funds currently available in the account.
     *
     * @return the available amount
     */
    public BigDecimal getAvailableAmount() {
        return availableAmount;
    }
}

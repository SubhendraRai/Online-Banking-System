package com.bank.exception;

import java.math.BigDecimal;

/**
 * Checked exception thrown when an invalid monetary amount is encountered.
 * <p>
 * Enforces Architectural Rule 2: financial amounts must be strictly positive and non-null.
 * </p>
 */
public class InvalidAmountException extends BankingException {

    private static final long serialVersionUID = 1L;

    private final BigDecimal amount;

    /**
     * Constructs an {@code InvalidAmountException} with the invalid amount.
     *
     * @param amount the non-positive or malformed amount
     */
    public InvalidAmountException(BigDecimal amount) {
        super(ErrorCode.INVALID_AMOUNT,
                String.format("Invalid monetary amount: %s. Amount must be strictly greater than 0.00.",
                        amount != null ? amount.toPlainString() : "null"));
        this.amount = amount;
    }

    /**
     * Constructs an {@code InvalidAmountException} with a custom message.
     *
     * @param amount the non-positive or malformed amount
     * @param message detailed explanation of the violation
     */
    public InvalidAmountException(BigDecimal amount, String message) {
        super(ErrorCode.INVALID_AMOUNT, message);
        this.amount = amount;
    }

    /**
     * Returns the rejected monetary amount.
     *
     * @return the amount
     */
    public BigDecimal getAmount() {
        return amount;
    }
}

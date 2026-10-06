package com.bank.exception;

/**
 * Base checked exception for all business domain errors within the banking application.
 * <p>
 * Implements Architectural Rule 5: checked exceptions represent recoverable business conditions
 * and carry a structured {@link ErrorCode}. This mandates explicit exception handling across
 * the service and servlet layers while preventing raw error leaks to end users.
 * </p>
 */
public class BankingException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    /**
     * Constructs a {@code BankingException} using the default message of the specified error code.
     *
     * @param errorCode the categorized banking error code
     */
    public BankingException(ErrorCode errorCode) {
        super(errorCode != null ? errorCode.getDefaultMessage() : "Banking error occurred");
        this.errorCode = errorCode != null ? errorCode : ErrorCode.INTERNAL_ERROR;
    }

    /**
     * Constructs a {@code BankingException} with an explicit descriptive message.
     *
     * @param errorCode the categorized banking error code
     * @param message detailed description of the business violation
     */
    public BankingException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : ErrorCode.INTERNAL_ERROR;
    }

    /**
     * Constructs a {@code BankingException} with an explicit message and underlying cause.
     *
     * @param errorCode the categorized banking error code
     * @param message detailed description of the business violation
     * @param cause the underlying root cause
     */
    public BankingException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : ErrorCode.INTERNAL_ERROR;
    }

    /**
     * Returns the standardized error code associated with this business failure.
     *
     * @return the {@link ErrorCode}
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

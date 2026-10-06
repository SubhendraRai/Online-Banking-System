package com.bank.exception;

/**
 * Standardized machine-readable error codes for banking domain exceptions.
 * <p>
 * Carried by {@link BankingException} to classify business failures uniformly across
 * services, controllers, and API error responses.
 * </p>
 */
public enum ErrorCode {

    INSUFFICIENT_FUNDS("BANK-1001", "Insufficient available funds to complete the transaction."),
    ACCOUNT_NOT_FOUND("BANK-1002", "The requested bank account was not found."),
    ACCOUNT_FROZEN("BANK-1003", "The account is frozen; transactions are suspended."),
    ACCOUNT_CLOSED("BANK-1004", "The account is permanently closed."),
    INVALID_AMOUNT("BANK-1005", "The transaction amount is invalid. Must be strictly greater than zero."),
    LIMIT_EXCEEDED("BANK-1006", "The transaction exceeds permitted daily or overdraft thresholds."),
    DUPLICATE_EMAIL("BANK-1007", "An account with this email address already exists."),
    AUTHENTICATION_FAILED("BANK-1008", "Invalid email address or password provided."),
    ACCOUNT_LOCKED("BANK-1009", "Account is locked due to repeated failed login attempts."),
    VALIDATION_FAILED("BANK-1010", "Input validation failed. Please check submitted values."),
    SERVICE_BUSY("BANK-1011", "The banking service is temporarily busy. Please retry shortly."),
    INTERNAL_ERROR("BANK-9999", "An internal system error occurred.");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}

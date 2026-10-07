package com.bank.util;

import com.bank.exception.BankingException;
import com.bank.exception.ErrorCode;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Central registry mapping domain {@link ErrorCode}s to clear, customer-friendly messages.
 * <p>
 * Ensures that low-level technical specifics or database invariants are translated into
 * courteous, actionable explanations for UI rendering across web servlets and flash messages.
 * </p>
 */
public final class ErrorMessageResolver {

    private static final Map<ErrorCode, String> FRIENDLY_MESSAGES;

    static {
        Map<ErrorCode, String> map = new EnumMap<>(ErrorCode.class);
        map.put(ErrorCode.INSUFFICIENT_FUNDS,
                "Insufficient available balance in your account to complete this transaction.");
        map.put(ErrorCode.ACCOUNT_NOT_FOUND,
                "The requested bank account could not be found. Please verify the account number.");
        map.put(ErrorCode.ACCOUNT_FROZEN,
                "This account is currently frozen. Transactions are temporarily suspended. Please contact support.");
        map.put(ErrorCode.ACCOUNT_CLOSED,
                "This account is closed. Transactions and balance modifications are prohibited.");
        map.put(ErrorCode.INVALID_AMOUNT,
                "Please enter a valid monetary amount greater than zero with at most 2 decimal places.");
        map.put(ErrorCode.LIMIT_EXCEEDED,
                "Transaction limit exceeded. The transfer amount exceeds per-transaction or daily velocity limits.");
        map.put(ErrorCode.DUPLICATE_EMAIL,
                "An account with this email address already exists. Please sign in or use another email.");
        map.put(ErrorCode.AUTHENTICATION_FAILED,
                "Invalid email or password. Please verify your credentials and try again.");
        map.put(ErrorCode.ACCOUNT_LOCKED,
                "Your account is locked due to multiple consecutive failed login attempts. Please contact support.");
        map.put(ErrorCode.VALIDATION_FAILED,
                "Please check the values entered in the form and correct any errors.");
        map.put(ErrorCode.SERVICE_BUSY,
                "The banking service is experiencing high traffic. Please retry your request in a few moments.");
        map.put(ErrorCode.INTERNAL_ERROR,
                "A temporary banking system error occurred. Your money is safe. Please try again shortly.");

        FRIENDLY_MESSAGES = Collections.unmodifiableMap(map);
    }

    private ErrorMessageResolver() {
        // Utility class
    }

    /**
     * Resolves a friendly message for the specified error code.
     *
     * @param code error code
     * @return friendly message
     */
    public static String resolve(ErrorCode code) {
        if (code == null) {
            return FRIENDLY_MESSAGES.get(ErrorCode.INTERNAL_ERROR);
        }
        return FRIENDLY_MESSAGES.getOrDefault(code, code.getDefaultMessage());
    }

    /**
     * Resolves a user-friendly message for a {@link BankingException}.
     * If the exception contains a specific human-readable message, it is used;
     * otherwise the registered friendly message for its {@link ErrorCode} is returned.
     *
     * @param ex banking exception
     * @return user-friendly message
     */
    public static String resolve(BankingException ex) {
        if (ex == null) {
            return resolve(ErrorCode.INTERNAL_ERROR);
        }
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            return ex.getMessage();
        }
        return resolve(ex.getErrorCode());
    }

    /**
     * Resolves a user-friendly message for any generic {@link Throwable}.
     *
     * @param t throwable or exception
     * @return sanitized user-facing message
     */
    public static String resolve(Throwable t) {
        if (t instanceof BankingException be) {
            return resolve(be);
        }
        if (t instanceof IllegalArgumentException iae && iae.getMessage() != null && !iae.getMessage().isBlank()) {
            return iae.getMessage();
        }
        return resolve(ErrorCode.INTERNAL_ERROR);
    }

    /**
     * Resolves a friendly message with a custom fallback if null.
     *
     * @param code error code
     * @param fallback default fallback text
     * @return resolved message or fallback
     */
    public static String resolveOrDefault(ErrorCode code, String fallback) {
        if (code == null) {
            return fallback;
        }
        return FRIENDLY_MESSAGES.getOrDefault(code, fallback);
    }
}

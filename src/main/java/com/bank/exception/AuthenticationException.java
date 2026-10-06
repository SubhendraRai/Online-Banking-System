package com.bank.exception;

/**
 * Checked exception thrown when an authentication attempt fails due to invalid credentials,
 * a locked user account, or unauthorized access attempts.
 */
public class AuthenticationException extends BankingException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an {@code AuthenticationException} with a failure message.
     *
     * @param message description of the authentication failure
     */
    public AuthenticationException(String message) {
        super(ErrorCode.AUTHENTICATION_FAILED, message);
    }

    /**
     * Constructs an {@code AuthenticationException} with a specific error code and message.
     *
     * @param errorCode the categorized error code (e.g. AUTHENTICATION_FAILED or ACCOUNT_LOCKED)
     * @param message description of the authentication failure
     */
    public AuthenticationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructs an {@code AuthenticationException} with a message and cause.
     *
     * @param message description of the authentication failure
     * @param cause root cause
     */
    public AuthenticationException(String message, Throwable cause) {
        super(ErrorCode.AUTHENTICATION_FAILED, message, cause);
    }
}

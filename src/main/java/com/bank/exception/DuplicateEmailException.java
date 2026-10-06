package com.bank.exception;

/**
 * Checked exception thrown when attempting to register or update a user with an email address
 * that already belongs to another registered user.
 */
public class DuplicateEmailException extends BankingException {

    private static final long serialVersionUID = 1L;

    private final String email;

    /**
     * Constructs a {@code DuplicateEmailException} for the duplicate email.
     *
     * @param email the colliding email address
     */
    public DuplicateEmailException(String email) {
        super(ErrorCode.DUPLICATE_EMAIL, "An account is already registered with email address: " + email);
        this.email = email;
    }

    /**
     * Constructs a {@code DuplicateEmailException} with a custom message.
     *
     * @param email the colliding email address
     * @param message detailed description of the error
     */
    public DuplicateEmailException(String email, String message) {
        super(ErrorCode.DUPLICATE_EMAIL, message);
        this.email = email;
    }

    /**
     * Returns the duplicate email address.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }
}

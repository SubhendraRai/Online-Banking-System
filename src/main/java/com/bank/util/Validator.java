package com.bank.util;

import com.bank.exception.ValidationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validation utility for sanitizing and verifying user inputs and form fields.
 * <p>
 * Enforces email syntax, international/domestic phone number structure, and
 * password security policy (minimum 8 characters, containing at least one letter and one digit).
 * </p>
 */
public final class Validator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9]{10,15}$"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d).{8,}$"
    );

    private Validator() {
        // Prevent instantiation
    }

    /**
     * Tests whether an email address adheres to standard mailbox format.
     *
     * @param email candidate email string
     * @return true if valid and non-blank, false otherwise
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validates an email address and throws a {@link ValidationException} if malformed.
     *
     * @param email candidate email
     * @throws ValidationException if email is null, blank, or invalid
     */
    public static void validateEmail(String email) throws ValidationException {
        if (!isValidEmail(email)) {
            throw new ValidationException("email", "Invalid email format. Must be a valid address (e.g. user@example.com).");
        }
    }

    /**
     * Tests whether a phone number contains 10-15 digits with an optional leading '+'.
     *
     * @param phone candidate phone string
     * @return true if valid, false otherwise
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null) {
            return false;
        }
        String sanitized = phone.replaceAll("[\\s-()]", "");
        return PHONE_PATTERN.matcher(sanitized).matches();
    }

    /**
     * Validates a phone number and throws a {@link ValidationException} if invalid.
     *
     * @param phone candidate phone number
     * @throws ValidationException if phone is invalid
     */
    public static void validatePhone(String phone) throws ValidationException {
        if (!isValidPhone(phone)) {
            throw new ValidationException("phone", "Invalid phone number. Must contain 10 to 15 digits.");
        }
    }

    /**
     * Tests whether a password meets the security policy:
     * at least 8 characters long, containing at least one alphabetical letter and at least one digit.
     *
     * @param password candidate password string
     * @return true if compliant, false otherwise
     */
    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Validates a password against security policy and throws {@link ValidationException} if weak.
     *
     * @param password candidate password
     * @throws ValidationException if password violates security criteria
     */
    public static void validatePassword(String password) throws ValidationException {
        if (!isValidPassword(password)) {
            throw new ValidationException("password",
                    "Password must be at least 8 characters long and contain at least one letter and one digit.");
        }
    }

    /**
     * Checks if a string is non-null and not blank.
     *
     * @param text string to evaluate
     * @return true if containing at least one non-whitespace character
     */
    public static boolean isNotBlank(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Validates that a required text field is non-null and not blank.
     *
     * @param value field value
     * @param fieldName descriptive field name for error reporting
     * @throws ValidationException if blank or null
     */
    public static void validateRequired(String value, String fieldName) throws ValidationException {
        if (!isNotBlank(value)) {
            String label = (fieldName != null && !fieldName.trim().isEmpty()) ? fieldName : "Field";
            throw new ValidationException(fieldName, label + " is required and cannot be blank.");
        }
    }

    /**
     * Validates user registration fields in a single step, aggregating all errors.
     *
     * @param fullName full name
     * @param email email address
     * @param phone phone number
     * @param address physical address
     * @param password raw password
     * @throws ValidationException if any fields are invalid
     */
    public static void validateRegistration(String fullName, String email, String phone,
                                            String address, String password) throws ValidationException {
        Map<String, String> errors = new LinkedHashMap<>();

        if (!isNotBlank(fullName)) {
            errors.put("fullName", "Full name is required.");
        }
        if (!isValidEmail(email)) {
            errors.put("email", "Invalid email address format.");
        }
        if (!isValidPhone(phone)) {
            errors.put("phone", "Invalid phone number. Must contain 10-15 digits.");
        }
        if (!isNotBlank(address)) {
            errors.put("address", "Address is required.");
        }
        if (!isValidPassword(password)) {
            errors.put("password", "Password must be at least 8 characters and contain both letters and digits.");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}

package com.bank.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.bank.exception.ValidationException;

/**
 * Production password hashing and verification utility.
 * <p>
 * Enforces the application password complexity policy from {@link Validator#validatePassword(String)}
 * prior to generating salted BCrypt hashes with a secure cost factor (default 10).
 * </p>
 */
public final class PasswordUtil {

    /** Default BCrypt work factor (2^10 rounds). */
    public static final int DEFAULT_COST = 10;

    private PasswordUtil() {
        // Prevent instantiation of utility class
    }

    /**
     * Validates candidate plaintext password against {@link Validator#validatePassword(String)}
     * and returns a formatted BCrypt hash string.
     *
     * @param plainTextPassword candidate plaintext password
     * @return salted BCrypt hash string
     * @throws ValidationException if password violates security policy
     */
    public static String hash(String plainTextPassword) throws ValidationException {
        return hash(plainTextPassword, DEFAULT_COST);
    }

    /**
     * Validates candidate plaintext password against security policy and hashes
     * it with a custom work cost factor.
     *
     * @param plainTextPassword candidate plaintext password
     * @param cost computational cost factor (between 4 and 31)
     * @return salted BCrypt hash string
     * @throws ValidationException if password violates security policy
     * @throws IllegalArgumentException if cost is out of range
     */
    public static String hash(String plainTextPassword, int cost) throws ValidationException {
        Validator.validatePassword(plainTextPassword);
        if (cost < BCrypt.MIN_COST || cost > BCrypt.MAX_COST) {
            throw new IllegalArgumentException("Cost factor must be between "
                    + BCrypt.MIN_COST + " and " + BCrypt.MAX_COST);
        }
        return BCrypt.withDefaults().hashToString(cost, plainTextPassword.toCharArray());
    }

    /**
     * Verifies a candidate plaintext password against a stored BCrypt hash.
     *
     * @param plainTextPassword candidate plaintext password
     * @param bcryptHash stored BCrypt hash string
     * @return {@code true} if password matches hash, {@code false} otherwise
     */
    public static boolean verify(String plainTextPassword, String bcryptHash) {
        if (plainTextPassword == null || bcryptHash == null || bcryptHash.isBlank()) {
            return false;
        }
        return BCrypt.verifyer().verify(plainTextPassword.toCharArray(), bcryptHash.toCharArray()).verified;
    }
}

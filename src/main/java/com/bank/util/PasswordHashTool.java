package com.bank.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * CLI and utility class to generate and verify secure BCrypt password hashes.
 * <p>
 * Uses {@code at.favre.lib:bcrypt} with a default cost factor of 10 adhering to
 * security specification NFR-SEC-01. Can be executed as a standalone CLI tool
 * to produce hashes for seed data, manual user setup, or test fixtures.
 * </p>
 */
public final class PasswordHashTool {

    /** Default BCrypt computational cost factor. */
    public static final int DEFAULT_COST = 10;

    private PasswordHashTool() {
        // Prevent instantiation of utility class
    }

    /**
     * Hashes a plaintext password using BCrypt with the default cost factor (10).
     *
     * @param plainTextPassword the plaintext password to hash
     * @return the formatted BCrypt hash string (e.g. $2a$10$...)
     * @throws IllegalArgumentException if password is null or empty
     */
    public static String hash(String plainTextPassword) {
        return hash(plainTextPassword, DEFAULT_COST);
    }

    /**
     * Hashes a plaintext password using BCrypt with a custom cost factor.
     *
     * @param plainTextPassword the plaintext password to hash
     * @param cost the computational cost factor (between 4 and 31)
     * @return the formatted BCrypt hash string
     * @throws IllegalArgumentException if password is null/empty or cost is invalid
     */
    public static String hash(String plainTextPassword, int cost) {
        if (plainTextPassword == null || plainTextPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        if (cost < BCrypt.MIN_COST || cost > BCrypt.MAX_COST) {
            throw new IllegalArgumentException("Cost factor must be between " 
                    + BCrypt.MIN_COST + " and " + BCrypt.MAX_COST);
        }
        return BCrypt.withDefaults().hashToString(cost, plainTextPassword.toCharArray());
    }

    /**
     * Verifies a candidate plaintext password against a stored BCrypt hash string.
     *
     * @param plainTextPassword the candidate plaintext password
     * @param bcryptHash the stored BCrypt hash string
     * @return {@code true} if password matches, {@code false} otherwise
     */
    public static boolean verify(String plainTextPassword, String bcryptHash) {
        if (plainTextPassword == null || bcryptHash == null) {
            return false;
        }
        return BCrypt.verifyer().verify(plainTextPassword.toCharArray(), bcryptHash.toCharArray()).verified;
    }

    /**
     * Standalone CLI runner to hash passwords.
     * <p>
     * If passwords are provided as arguments, hashes each one.
     * If no arguments are provided, generates and prints hashes for the standard seed accounts.
     * </p>
     *
     * @param args passwords to hash (optional)
     */
    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   ONLINE BANKING SYSTEM - BCRYPT PASSWORD HASH GENERATOR");
        System.out.println("   Engine: at.favre.lib:bcrypt | Default Cost Factor: " + DEFAULT_COST);
        System.out.println("==================================================================");

        if (args.length > 0) {
            for (String arg : args) {
                String hashed = hash(arg);
                boolean verified = verify(arg, hashed);
                System.out.println("Password : " + arg);
                System.out.println("Hash     : " + hashed);
                System.out.println("Verified : " + verified);
                System.out.println("------------------------------------------------------------------");
            }
        } else {
            System.out.println("Generating hashes for default seed users (Cost = " + DEFAULT_COST + "):\n");

            String adminPass = "AdminPass123!";
            String adminHash = hash(adminPass);
            System.out.println("[ADMIN] admin@bank.com");
            System.out.println("Plaintext: " + adminPass);
            System.out.println("Hash     : " + adminHash);
            System.out.println("Self-Check: " + verify(adminPass, adminHash));
            System.out.println();

            String customerPass = "CustomerPass123!";
            String customerHash = hash(customerPass);
            System.out.println("[CUSTOMERS] rahul.sharma@example.com, priya.patel@example.com, amit.verma@example.com");
            System.out.println("Plaintext: " + customerPass);
            System.out.println("Hash     : " + customerHash);
            System.out.println("Self-Check: " + verify(customerPass, customerHash));
            System.out.println();
            System.out.println("Copy these hashes into sql/seed.sql as needed.");
            System.out.println("==================================================================");
        }
    }
}

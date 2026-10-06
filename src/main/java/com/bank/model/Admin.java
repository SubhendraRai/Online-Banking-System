package com.bank.model;

/**
 * Domain model representing a bank system administrator.
 * <p>
 * Routes administrative users to the administrative management dashboard via {@link #getHomePath()}.
 * </p>
 */
public class Admin extends User {

    /**
     * Default constructor setting role to {@link Role#ADMIN}.
     */
    public Admin() {
        super();
        setRole(Role.ADMIN);
    }

    /**
     * Parameterized constructor for administrator instantiation.
     *
     * @param userId unique user identifier
     * @param fullName administrator full legal name
     * @param email administrator email address
     * @param phone telephone number
     * @param address physical address
     * @param passwordHash BCrypt hashed password
     * @param status account status
     */
    public Admin(Long userId, String fullName, String email, String phone,
                 String address, String passwordHash, UserStatus status) {
        super(userId, fullName, email, phone, address, passwordHash, Role.ADMIN, status);
    }

    /**
     * Polymorphic implementation directing administrators to the admin dashboard.
     *
     * @return {@code "/admin/dashboard"}
     */
    @Override
    public String getHomePath() {
        return "/admin/dashboard";
    }
}

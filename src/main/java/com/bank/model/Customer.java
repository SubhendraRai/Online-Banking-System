package com.bank.model;

/**
 * Domain model representing a retail banking customer.
 * <p>
 * Routes customers to their personalized account dashboard via {@link #getHomePath()}.
 * </p>
 */
public class Customer extends User {

    /**
     * Default constructor setting role to {@link Role#CUSTOMER}.
     */
    public Customer() {
        super();
        setRole(Role.CUSTOMER);
    }

    /**
     * Parameterized constructor for customer instantiation.
     *
     * @param userId unique user identifier
     * @param fullName customer full legal name
     * @param email customer email address
     * @param phone telephone number
     * @param address residential address
     * @param passwordHash BCrypt hashed password
     * @param status account status
     */
    public Customer(Long userId, String fullName, String email, String phone,
                    String address, String passwordHash, UserStatus status) {
        super(userId, fullName, email, phone, address, passwordHash, Role.CUSTOMER, status);
    }

    /**
     * Polymorphic implementation directing customers to the customer dashboard.
     *
     * @return {@code "/customer/dashboard"}
     */
    @Override
    public String getHomePath() {
        return "/customer/dashboard";
    }
}

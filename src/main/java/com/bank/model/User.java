package com.bank.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract domain model representing a registered user in the banking system.
 * <p>
 * Forms the root of the user inheritance hierarchy, extended by {@link Customer}
 * and {@link Admin}. Enforces polymorphic routing through {@link #getHomePath()}.
 * </p>
 */
public abstract class User {

    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String passwordHash;
    private Role role;
    private UserStatus status;
    private int failedAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;

    /**
     * Default constructor.
     */
    protected User() {
        this.status = UserStatus.ACTIVE;
        this.failedAttempts = 0;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Parameterized constructor initializing user core identity.
     *
     * @param userId unique user identifier
     * @param fullName full legal name
     * @param email unique email address
     * @param phone contact telephone number
     * @param address residential/business address
     * @param passwordHash BCrypt hashed password
     * @param role security role
     * @param status account status
     */
    protected User(Long userId, String fullName, String email, String phone,
                    String address, String passwordHash, Role role, UserStatus status) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.failedAttempts = 0;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Polymorphic method returning the role-specific landing or dashboard URL path.
     *
     * @return contextual home path (e.g. "/customer/dashboard" or "/admin/dashboard")
     */
    public abstract String getHomePath();

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isLocked() {
        return this.status == UserStatus.LOCKED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId) || Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", status=" + status +
                '}';
    }
}

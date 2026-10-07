package com.bank.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Lightweight, serializable session principal representing an authenticated user.
 * <p>
 * Stored in the HTTP session scope upon successful login. Keeps sensitive credentials
 * (such as password hashes) out of memory while carrying identity and authorization roles.
 * </p>
 */
public class SessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String name;
    private final String email;
    private final Role role;

    /**
     * Constructs a {@code SessionUser} with basic identifier, display name, and role.
     *
     * @param id user identifier
     * @param name user full display name
     * @param role security role
     */
    public SessionUser(Long id, String name, Role role) {
        this(id, name, null, role);
    }

    /**
     * Constructs a {@code SessionUser} with full principal details.
     *
     * @param id user identifier
     * @param name user full display name
     * @param email primary email address
     * @param role security role
     */
    public SessionUser(Long id, String name, String email, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isCustomer() {
        return role == Role.CUSTOMER;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionUser that = (SessionUser) o;
        return Objects.equals(id, that.id) && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, role);
    }

    @Override
    public String toString() {
        return "SessionUser{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                '}';
    }
}

package com.bank.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying User inheritance and polymorphic home path dispatch.
 */
class UserPolymorphismTest {

    @Test
    @DisplayName("Polymorphic getHomePath routes Customer and Admin to correct dashboards")
    void testCustomerAndAdminPolymorphicHomePath() {
        User customer = new Customer(1L, "Alice Johnson", "alice@example.com",
                "+19876543210", "101 Elm St", "$2a$12$hash", UserStatus.ACTIVE);
        User admin = new Admin(2L, "Bob Administrator", "admin@bank.com",
                "+19876543211", "HQ Operations", "$2a$12$hash", UserStatus.ACTIVE);

        assertEquals("/customer/dashboard", customer.getHomePath());
        assertEquals("/admin/dashboard", admin.getHomePath());
        assertEquals(Role.CUSTOMER, customer.getRole());
        assertEquals(Role.ADMIN, admin.getRole());

        List<User> users = new ArrayList<>();
        users.add(customer);
        users.add(admin);

        List<String> routes = users.stream().map(User::getHomePath).toList();
        assertEquals(List.of("/customer/dashboard", "/admin/dashboard"), routes);
        assertTrue(customer.isActive());
        assertTrue(admin.isActive());
    }
}

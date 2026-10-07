package com.bank.dao;

import com.bank.model.Admin;
import com.bank.model.Customer;
import com.bank.model.Role;
import com.bank.model.User;
import com.bank.model.UserStatus;
import com.bank.util.Page;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDaoTest extends BaseDaoIntegrationTest {

    private UserDao userDao;

    @BeforeEach
    void setUp() {
        userDao = new UserDao();
    }

    @Test
    @DisplayName("save persists a customer and generates a unique user_id")
    void testSaveCustomer() {
        Customer customer = new Customer(null, "Test User", "test.user@bank.com",
                "9998887776", "Test City", "$2a$10$xyz", UserStatus.ACTIVE);
        User saved = userDao.save(customer);

        assertNotNull(saved.getUserId());
        Optional<User> fetched = userDao.findById(saved.getUserId());
        assertTrue(fetched.isPresent());
        assertEquals("Test User", fetched.get().getFullName());
        assertEquals(Role.CUSTOMER, fetched.get().getRole());
    }

    @Test
    @DisplayName("findById and findByEmail retrieve seeded users accurately")
    void testFindByIdAndEmail() {
        Optional<User> optAdmin = userDao.findById(1L);
        assertTrue(optAdmin.isPresent());
        assertTrue(optAdmin.get() instanceof Admin);
        assertEquals("admin@bank.com", optAdmin.get().getEmail());

        Optional<User> optCustomer = userDao.findByEmail("rahul.sharma@example.com");
        assertTrue(optCustomer.isPresent());
        assertTrue(optCustomer.get() instanceof Customer);
        assertEquals(2L, optCustomer.get().getUserId());
    }

    @Test
    @DisplayName("existsByEmail correctly detects registered vs unregistered emails")
    void testExistsByEmail() {
        assertTrue(userDao.existsByEmail("admin@bank.com"));
        assertTrue(userDao.existsByEmail("rahul.sharma@example.com"));
        assertFalse(userDao.existsByEmail("nonexistent@example.com"));
    }

    @Test
    @DisplayName("countByRole correctly counts administrators and customers")
    void testCountByRole() {
        assertEquals(1L, userDao.countByRole(Role.ADMIN));
        assertEquals(2L, userDao.countByRole(Role.CUSTOMER));
        assertEquals(3L, userDao.countByRole(null));
    }

    @Test
    @DisplayName("update alters user details and state successfully")
    void testUpdateUser() {
        User user = userDao.findById(2L).orElseThrow();
        user.setFullName("Rahul S. Sharma");
        user.setPhone("9999988888");
        user.setStatus(UserStatus.LOCKED);
        user.setFailedAttempts(3);

        boolean updated = userDao.update(user);
        assertTrue(updated);

        User refreshed = userDao.findById(2L).orElseThrow();
        assertEquals("Rahul S. Sharma", refreshed.getFullName());
        assertEquals("9999988888", refreshed.getPhone());
        assertEquals(UserStatus.LOCKED, refreshed.getStatus());
        assertEquals(3, refreshed.getFailedAttempts());
    }

    @Test
    @DisplayName("delete removes an existing user without account dependencies")
    void testDeleteUser() {
        Customer tempUser = new Customer(null, "Delete Me", "delete.me@bank.com",
                "1112223334", "Temp St", "$2a$10$xyz", UserStatus.ACTIVE);
        User saved = userDao.save(tempUser);
        Long id = saved.getUserId();

        assertTrue(userDao.delete(id));
        assertFalse(userDao.findById(id).isPresent());
    }

    @Test
    @DisplayName("findAll returns all seeded users")
    void testFindAll() {
        List<User> all = userDao.findAll();
        assertEquals(3, all.size());
    }

    @Test
    @DisplayName("search dynamically filters users by keyword, role, status, and paginates")
    void testSearchWithPagination() {
        Page<User> page = userDao.search("rahul", Role.CUSTOMER, UserStatus.ACTIVE, 1, 10);
        assertEquals(1, page.getTotalItems());
        assertEquals(1, page.getItems().size());
        assertEquals("rahul.sharma@example.com", page.getItems().get(0).getEmail());

        Page<User> pageAllCust = userDao.search(null, Role.CUSTOMER, null, 1, 1);
        assertEquals(2, pageAllCust.getTotalItems());
        assertEquals(1, pageAllCust.getItems().size());
        assertEquals(2, pageAllCust.getTotalPages());
    }
}

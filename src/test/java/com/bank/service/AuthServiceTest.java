package com.bank.service;

import com.bank.exception.AuthenticationException;
import com.bank.exception.DuplicateEmailException;
import com.bank.exception.ErrorCode;
import com.bank.exception.ValidationException;
import com.bank.model.Account;
import com.bank.model.AccountType;
import com.bank.model.Customer;
import com.bank.model.Role;
import com.bank.model.SavingsAccount;
import com.bank.model.SessionUser;
import com.bank.model.User;
import com.bank.model.UserStatus;
import com.bank.service.fakes.FakeAccountDao;
import com.bank.service.fakes.FakeSettingsProvider;
import com.bank.service.fakes.FakeUserDao;
import com.bank.util.PasswordUtil;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest extends BaseServiceTest {

    private FakeUserDao fakeUserDao;
    private FakeAccountDao fakeAccountDao;
    private FakeSettingsProvider fakeSettingsProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        fakeUserDao = new FakeUserDao();
        fakeAccountDao = new FakeAccountDao();
        fakeSettingsProvider = new FakeSettingsProvider();
        fakeSettingsProvider.put("savings.min_balance", "500.00");

        authService = new AuthService(fakeUserDao, fakeAccountDao, fakeSettingsProvider);
    }

    @Test
    @DisplayName("Successful registration creates CUSTOMER and provisions a default 12-digit SAVINGS account")
    void testSuccessfulRegister() throws Exception {
        Customer customer = authService.register(
                "Jane Doe",
                "jane.doe@example.com",
                "9876543210",
                "123 Market St, Financial District",
                "SecurePass123!"
        );

        assertNotNull(customer);
        assertNotNull(customer.getUserId());
        assertEquals("Jane Doe", customer.getFullName());
        assertEquals("jane.doe@example.com", customer.getEmail());
        assertEquals(Role.CUSTOMER, customer.getRole());
        assertEquals(UserStatus.ACTIVE, customer.getStatus());
        assertTrue(PasswordUtil.verify("SecurePass123!", customer.getPasswordHash()));

        // Verify default 12-digit savings account provisioned
        List<Account> userAccounts = fakeAccountDao.findByUser(customer.getUserId());
        assertEquals(1, userAccounts.size(), "Should provision exactly one default account");
        Account defaultAccount = userAccounts.get(0);
        assertEquals(12, defaultAccount.getAccountNo().length(), "Account number must be 12 digits");
        assertEquals(AccountType.SAVINGS, defaultAccount.getAccountType());
        assertTrue(defaultAccount instanceof SavingsAccount);
        assertEquals(new BigDecimal("500.00"), ((SavingsAccount) defaultAccount).getMinimumBalance());
    }

    @Test
    @DisplayName("Registration with duplicate email throws DuplicateEmailException")
    void testRegisterDuplicateEmail() throws Exception {
        authService.register(
                "Existing User",
                "duplicate@example.com",
                "9876543210",
                "123 Main St",
                "SecurePass123!"
        );

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class, () ->
                authService.register(
                        "Second User",
                        "DUPLICATE@example.com", // Case-insensitive collision
                        "9876543211",
                        "456 High St",
                        "SecurePass123!"
                )
        );

        assertEquals("duplicate@example.com", ex.getEmail());
        assertEquals(ErrorCode.DUPLICATE_EMAIL, ex.getErrorCode());
    }

    @Test
    @DisplayName("Registration with invalid fields throws ValidationException")
    void testRegisterValidationFailure() {
        assertThrows(ValidationException.class, () ->
                authService.register(
                        "", // Blank name
                        "invalid-email",
                        "123", // Short phone
                        "",
                        "weak" // Weak password
                )
        );
    }

    @Test
    @DisplayName("Login with valid credentials returns SessionUser principal")
    void testSuccessfulLogin() throws Exception {
        authService.register(
                "Alice Smith",
                "alice@example.com",
                "9811223344",
                "789 Tech Park",
                "Password123!"
        );

        SessionUser sessionUser = authService.login("alice@example.com", "Password123!");

        assertNotNull(sessionUser);
        assertNotNull(sessionUser.getId());
        assertEquals("Alice Smith", sessionUser.getName());
        assertEquals("alice@example.com", sessionUser.getEmail());
        assertEquals(Role.CUSTOMER, sessionUser.getRole());
        assertTrue(sessionUser.isCustomer());
        assertFalse(sessionUser.isAdmin());
    }

    @Test
    @DisplayName("Login with wrong password throws AuthenticationException and tracks failed attempts")
    void testLoginWrongPassword() throws Exception {
        authService.register(
                "Bob Johnson",
                "bob@example.com",
                "9811223344",
                "789 Oak Ave",
                "Password123!"
        );

        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.login("bob@example.com", "WrongPassword999!")
        );

        assertEquals(ErrorCode.AUTHENTICATION_FAILED, ex.getErrorCode());

        User user = fakeUserDao.findByEmail("bob@example.com").orElseThrow();
        assertEquals(1, user.getFailedAttempts(), "Failed attempts counter must increment");
    }

    @Test
    @DisplayName("Account is automatically locked after 5 consecutive failed login attempts")
    void testBruteForceAccountLockout() throws Exception {
        authService.register(
                "Target User",
                "target@example.com",
                "9811223344",
                "500 Pine St",
                "Password123!"
        );

        // 4 failed attempts
        for (int i = 1; i <= 4; i++) {
            final String wrongPass = "WrongPass" + i;
            assertThrows(AuthenticationException.class, () ->
                    authService.login("target@example.com", wrongPass)
            );
        }

        User user = fakeUserDao.findByEmail("target@example.com").orElseThrow();
        assertEquals(4, user.getFailedAttempts());
        assertEquals(UserStatus.ACTIVE, user.getStatus());

        // 5th failed attempt triggers LOCK
        AuthenticationException lockEx = assertThrows(AuthenticationException.class, () ->
                authService.login("target@example.com", "WrongPass5")
        );
        assertEquals(ErrorCode.ACCOUNT_LOCKED, lockEx.getErrorCode());

        User lockedUser = fakeUserDao.findByEmail("target@example.com").orElseThrow();
        assertEquals(5, lockedUser.getFailedAttempts());
        assertEquals(UserStatus.LOCKED, lockedUser.getStatus());
    }

    @Test
    @DisplayName("Login rejects previously LOCKED user")
    void testLoginRejectsLockedUser() throws Exception {
        Customer customer = authService.register(
                "Locked Customer",
                "locked@example.com",
                "9811223344",
                "500 Pine St",
                "Password123!"
        );

        customer.setStatus(UserStatus.LOCKED);
        fakeUserDao.update(customer);

        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.login("locked@example.com", "Password123!")
        );

        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Account is locked"));
    }

    @Test
    @DisplayName("Login rejects DELETED user")
    void testLoginRejectsDeletedUser() throws Exception {
        Customer customer = authService.register(
                "Deleted Customer",
                "deleted@example.com",
                "9811223344",
                "500 Pine St",
                "Password123!"
        );

        customer.setStatus(UserStatus.DELETED);
        fakeUserDao.update(customer);

        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.login("deleted@example.com", "Password123!")
        );

        assertEquals(ErrorCode.AUTHENTICATION_FAILED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("deactivated"));
    }
}

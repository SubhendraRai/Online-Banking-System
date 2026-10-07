package com.bank.util;

import com.bank.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    @DisplayName("Valid password compliant with Validator policy hashes and verifies successfully")
    void testValidPasswordHashAndVerify() throws ValidationException {
        String password = "SecurePass123!";
        String hash = PasswordUtil.hash(password);

        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$10$") || hash.startsWith("$2b$10$") || hash.startsWith("$2y$10$"));
        assertTrue(PasswordUtil.verify(password, hash), "Password must verify against generated BCrypt hash");
        assertFalse(PasswordUtil.verify("WrongPass999!", hash), "Incorrect password must fail verification");
    }

    @Test
    @DisplayName("Weak password violating policy is rejected with ValidationException")
    void testWeakPasswordRejection() {
        assertThrows(ValidationException.class, () -> PasswordUtil.hash("short1"),
                "Password shorter than 8 chars must be rejected");
        assertThrows(ValidationException.class, () -> PasswordUtil.hash("allletters"),
                "Password without numbers must be rejected");
        assertThrows(ValidationException.class, () -> PasswordUtil.hash("12345678"),
                "Password without letters must be rejected");
        assertThrows(ValidationException.class, () -> PasswordUtil.hash(null),
                "Null password must be rejected");
        assertThrows(ValidationException.class, () -> PasswordUtil.hash("   "),
                "Blank password must be rejected");
    }

    @Test
    @DisplayName("Verify returns false gracefully on null or blank inputs")
    void testVerifyGracefulNullHandling() throws ValidationException {
        String hash = PasswordUtil.hash("ValidPass123!");
        assertFalse(PasswordUtil.verify(null, hash));
        assertFalse(PasswordUtil.verify("ValidPass123!", null));
        assertFalse(PasswordUtil.verify("ValidPass123!", "   "));
    }

    @Test
    @DisplayName("Custom cost factor hashes and validates cost boundaries")
    void testCustomCost() throws ValidationException {
        String hash = PasswordUtil.hash("ValidPass123!", 8);
        assertTrue(hash.startsWith("$2a$08$") || hash.startsWith("$2b$08$") || hash.startsWith("$2y$08$"));
        assertTrue(PasswordUtil.verify("ValidPass123!", hash));

        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash("ValidPass123!", 3));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash("ValidPass123!", 32));
    }
}

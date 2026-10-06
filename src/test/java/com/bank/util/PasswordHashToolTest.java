package com.bank.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashToolTest {

    @Test
    @DisplayName("Hash generation produces valid BCrypt hash format")
    void testHashFormat() {
        String password = "SecretPassword123!";
        String hash = PasswordHashTool.hash(password);

        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$10$"), "Hash should start with $2a$10$ prefix");
        assertTrue(PasswordHashTool.verify(password, hash), "Password should verify against generated hash");
    }

    @Test
    @DisplayName("Verify returns false for mismatched password")
    void testVerifyMismatch() {
        String password = "CorrectPassword123!";
        String hash = PasswordHashTool.hash(password);

        assertFalse(PasswordHashTool.verify("WrongPassword456!", hash), "Mismatched password should fail verification");
        assertFalse(PasswordHashTool.verify(null, hash), "Null password should return false");
        assertFalse(PasswordHashTool.verify(password, null), "Null hash should return false");
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 10, 11})
    @DisplayName("Hash generation succeeds with valid custom cost factors")
    void testCustomCostFactors(int cost) {
        String password = "TestPassword99!";
        String hash = PasswordHashTool.hash(password, cost);

        assertNotNull(hash);
        assertTrue(PasswordHashTool.verify(password, hash));
    }

    @Test
    @DisplayName("Invalid inputs throw IllegalArgumentException")
    void testInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHashTool.hash(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordHashTool.hash(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordHashTool.hash("valid", 2));
        assertThrows(IllegalArgumentException.class, () -> PasswordHashTool.hash("valid", 35));
    }
}

package com.bank.util;

import com.bank.exception.ErrorCode;
import com.bank.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying input validation rules for emails, phone numbers, and password security policies.
 */
class ValidatorTest {

    @Test
    @DisplayName("Email validator accepts compliant emails and rejects malformed addresses")
    void testEmailValidationValidAndInvalidFormats() {
        assertTrue(Validator.isValidEmail("john.doe@example.com"));
        assertTrue(Validator.isValidEmail("customer123@bank.org"));
        assertTrue(Validator.isValidEmail("user+tag@sub.domain.co"));

        assertFalse(Validator.isValidEmail("plainaddress"));
        assertFalse(Validator.isValidEmail("@missingusername.com"));
        assertFalse(Validator.isValidEmail("missingdomain@"));
        assertFalse(Validator.isValidEmail("user@domain"));
        assertFalse(Validator.isValidEmail(""));
        assertFalse(Validator.isValidEmail(null));

        assertDoesNotThrow(() -> Validator.validateEmail("valid@bank.com"));
        ValidationException ex = assertThrows(ValidationException.class, () -> Validator.validateEmail("bad-email"));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        assertTrue(ex.getFieldErrors().containsKey("email"));
    }

    @Test
    @DisplayName("Phone validator accepts 10-15 digits and rejects non-digit strings")
    void testPhoneValidationValidAndInvalidFormats() {
        assertTrue(Validator.isValidPhone("+1234567890"));
        assertTrue(Validator.isValidPhone("9876543210"));
        assertTrue(Validator.isValidPhone("+91 98765 43210"));
        assertTrue(Validator.isValidPhone("012345678901234"));

        assertFalse(Validator.isValidPhone("12345")); // too short
        assertFalse(Validator.isValidPhone("abcdefghij")); // alphabetical
        assertFalse(Validator.isValidPhone(""));
        assertFalse(Validator.isValidPhone(null));

        assertDoesNotThrow(() -> Validator.validatePhone("+19876543210"));
        assertThrows(ValidationException.class, () -> Validator.validatePhone("123"));
    }

    @Test
    @DisplayName("Password policy enforces minimum 8 characters with at least one letter and one digit")
    void testPasswordPolicyValidAndInvalidFormats() {
        // Valid passwords
        assertTrue(Validator.isValidPassword("Secret123"));
        assertTrue(Validator.isValidPassword("P@ssw0rd2026"));
        assertTrue(Validator.isValidPassword("abcd5678"));

        // Invalid: less than 8 characters
        assertFalse(Validator.isValidPassword("Sec1"));
        assertFalse(Validator.isValidPassword("Abc1234"));

        // Invalid: only letters (no digit)
        assertFalse(Validator.isValidPassword("SecretPassword"));

        // Invalid: only digits (no letter)
        assertFalse(Validator.isValidPassword("1234567890"));

        // Invalid: null / empty
        assertFalse(Validator.isValidPassword(""));
        assertFalse(Validator.isValidPassword(null));

        assertDoesNotThrow(() -> Validator.validatePassword("ValidPass1"));
        ValidationException ex = assertThrows(ValidationException.class, () -> Validator.validatePassword("weak"));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        assertTrue(ex.getFieldErrors().containsKey("password"));
    }

    @Test
    @DisplayName("validateRequired rejects null or whitespace-only values")
    void testRequiredFieldValidation() {
        assertDoesNotThrow(() -> Validator.validateRequired("Valid Input", "Username"));
        assertThrows(ValidationException.class, () -> Validator.validateRequired("", "Username"));
        assertThrows(ValidationException.class, () -> Validator.validateRequired("   ", "Username"));
        assertThrows(ValidationException.class, () -> Validator.validateRequired(null, "Username"));
    }

    @Test
    @DisplayName("validateRegistration aggregates multiple field errors into a single ValidationException")
    void testValidateRegistrationAggregatesErrors() {
        // All fields invalid
        ValidationException ex = assertThrows(ValidationException.class, () ->
                Validator.validateRegistration("", "invalid-email", "123", "", "weak")
        );

        assertEquals(5, ex.getFieldErrors().size());
        assertTrue(ex.getFieldErrors().containsKey("fullName"));
        assertTrue(ex.getFieldErrors().containsKey("email"));
        assertTrue(ex.getFieldErrors().containsKey("phone"));
        assertTrue(ex.getFieldErrors().containsKey("address"));
        assertTrue(ex.getFieldErrors().containsKey("password"));

        // Fully valid registration
        assertDoesNotThrow(() ->
                Validator.validateRegistration("Jane Doe", "jane@example.com", "+12345678901",
                        "123 Main St", "SecurePass1")
        );
    }
}

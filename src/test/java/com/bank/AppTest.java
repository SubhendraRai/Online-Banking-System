package com.bank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic JUnit 5 test ensuring test execution pipeline and Surefire runner are operational.
 */
class AppTest {

    @Test
    @DisplayName("Verify testing framework initialization")
    void testFrameworkInitialization() {
        assertTrue(true, "JUnit 5 test framework is properly configured and operational.");
    }
}

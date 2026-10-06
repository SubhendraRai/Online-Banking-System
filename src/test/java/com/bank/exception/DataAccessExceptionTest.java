package com.bank.exception;

import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataAccessExceptionTest {

    @Test
    @DisplayName("DataAccessException correctly inherits from RuntimeException (unchecked)")
    void testInheritance() {
        DataAccessException ex = new DataAccessException("Error message");
        assertTrue(ex instanceof RuntimeException, "DataAccessException must be an unchecked RuntimeException");
        assertEquals("Error message", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    @DisplayName("DataAccessException preserves message and root cause SQLException")
    void testWrappingSQLException() {
        SQLException rootCause = new SQLException("Connection timed out", "08001", 1001);
        DataAccessException ex = new DataAccessException("Failed query", rootCause);

        assertEquals("Failed query", ex.getMessage());
        assertSame(rootCause, ex.getCause());
    }

    @Test
    @DisplayName("DataAccessException with cause only retains message from cause")
    void testCauseConstructor() {
        SQLException rootCause = new SQLException("Table not found");
        DataAccessException ex = new DataAccessException(rootCause);

        assertSame(rootCause, ex.getCause());
        assertTrue(ex.getMessage().contains("Table not found"));
    }
}

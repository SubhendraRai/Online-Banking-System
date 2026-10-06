package com.bank.exception;

import java.sql.SQLException;

/**
 * Unchecked runtime exception representing underlying JDBC and database failures.
 * <p>
 * Wraps low-level {@link SQLException} or connection management errors with contextual
 * messages. Adheres to Architectural Rule 5, isolating higher architectural layers
 * from raw database infrastructure exceptions while preserving root causes.
 * </p>
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code DataAccessException} with an informative message.
     *
     * @param message detailed message describing the failed database operation
     */
    public DataAccessException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code DataAccessException} wrapping an underlying cause.
     *
     * @param message detailed message describing the operation context
     * @param cause the underlying root cause, typically a {@link SQLException}
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code DataAccessException} wrapping a root cause directly.
     *
     * @param cause the underlying root cause, typically a {@link SQLException}
     */
    public DataAccessException(Throwable cause) {
        super(cause);
    }
}

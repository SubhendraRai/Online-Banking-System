package com.bank.util;

import com.bank.exception.BankingException;
import com.bank.exception.DataAccessException;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transaction coordinator executing functional units of work within ACID transaction boundaries.
 * <p>
 * Implements Architectural Rule 3:
 * <ul>
 *   <li>Acquires a database connection via {@link DBUtil#getConnection()}</li>
 *   <li>Saves current auto-commit status and disables auto-commit ({@code setAutoCommit(false)})</li>
 *   <li>Executes the transactional unit of work ({@link TxWork}, {@link TxAction}, or {@link TxBankingWork})</li>
 *   <li>Commits the transaction upon successful execution ({@code commit()})</li>
 *   <li>Rolls back the transaction upon any thrown exception ({@code rollback()})</li>
 *   <li>Always restores the original auto-commit state and closes the connection in {@code finally}</li>
 * </ul>
 * </p>
 */
public final class TxRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TxRunner.class);

    private TxRunner() {
        // Prevent instantiation of utility class
    }

    /**
     * Executes functional transactional work that may throw a checked {@link BankingException},
     * rethrowing business exceptions directly while wrapping SQLExceptions in {@link DataAccessException}.
     *
     * @param <T> result type
     * @param work transactional work
     * @return result of work
     * @throws BankingException if a business domain error occurs during execution
     * @throws DataAccessException if a database error occurs
     */
    public static <T> T execute(TxBankingWork<T> work) throws BankingException {
        if (work == null) {
            throw new IllegalArgumentException("TxBankingWork cannot be null");
        }

        Connection connection = null;
        boolean originalAutoCommit = true;

        try {
            connection = DBUtil.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            T result = work.execute(connection);

            connection.commit();
            return result;
        } catch (Throwable t) {
            rollbackQuietly(connection, t);
            if (t instanceof BankingException bankingEx) {
                throw bankingEx;
            } else if (t instanceof RuntimeException runtimeEx) {
                throw runtimeEx;
            } else if (t instanceof SQLException sqlEx) {
                throw new DataAccessException("Transaction failed due to SQL error: " + sqlEx.getMessage(), sqlEx);
            } else {
                throw new DataAccessException("Transaction failed: " + t.getMessage(), t);
            }
        } finally {
            closeAndRestore(connection, originalAutoCommit);
        }
    }

    /**
     * Executes the given transactional work inside a managed transaction and returns its result.
     *
     * @param <T> the result type produced by the work
     * @param work the functional unit of work to execute
     * @return the result produced by {@code work}
     * @throws DataAccessException if a database error occurs or if a checked exception is wrapped
     * @throws RuntimeException if an unchecked runtime exception is thrown during execution
     */
    public static <T> T run(TxWork<T> work) {
        if (work == null) {
            throw new IllegalArgumentException("TxWork cannot be null");
        }

        Connection connection = null;
        boolean originalAutoCommit = true;

        try {
            connection = DBUtil.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            T result = work.execute(connection);

            connection.commit();
            return result;
        } catch (Throwable t) {
            rollbackQuietly(connection, t);
            if (t instanceof RuntimeException runtimeEx) {
                throw runtimeEx;
            } else if (t instanceof SQLException sqlEx) {
                throw new DataAccessException("Transaction failed due to SQL error: " + sqlEx.getMessage(), sqlEx);
            } else {
                throw new DataAccessException("Transaction failed: " + t.getMessage(), t);
            }
        } finally {
            closeAndRestore(connection, originalAutoCommit);
        }
    }

    /**
     * Executes the given transactional action inside a managed transaction without returning a result.
     *
     * @param action the functional unit of action to execute
     * @throws DataAccessException if a database error occurs or if a checked exception is wrapped
     * @throws RuntimeException if an unchecked runtime exception is thrown during execution
     */
    public static void runAction(TxAction action) {
        if (action == null) {
            throw new IllegalArgumentException("TxAction cannot be null");
        }
        run(connection -> {
            action.execute(connection);
            return null;
        });
    }

    /**
     * Performs a rollback safely without hiding the primary exception.
     *
     * @param connection the database connection to rollback
     * @param originalError the primary error that triggered the rollback
     */
    private static void rollbackQuietly(Connection connection, Throwable originalError) {
        if (connection != null) {
            try {
                connection.rollback();
                LOGGER.debug("Transaction rolled back successfully after error: {}", originalError.getMessage());
            } catch (SQLException rollbackEx) {
                LOGGER.error("Failed to rollback transaction", rollbackEx);
                originalError.addSuppressed(rollbackEx);
            }
        }
    }

    /**
     * Restores the connection's original auto-commit state and closes it.
     *
     * @param connection the database connection to restore and close
     * @param originalAutoCommit the previous auto-commit status
     */
    private static void closeAndRestore(Connection connection, boolean originalAutoCommit) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.setAutoCommit(originalAutoCommit);
                    connection.close();
                }
            } catch (SQLException closeEx) {
                LOGGER.error("Failed to restore autocommit or close connection", closeEx);
            }
        }
    }
}

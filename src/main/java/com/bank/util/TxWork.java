package com.bank.util;

import java.sql.Connection;

/**
 * Functional interface representing a unit of transactional work that produces a result of type {@code T}.
 * <p>
 * Passed to {@link TxRunner#run(TxWork)} to execute within a managed ACID transaction boundary.
 * </p>
 *
 * @param <T> the type of result produced by the transactional computation
 */
@FunctionalInterface
public interface TxWork<T> {

    /**
     * Executes the unit of work using the provided transactional connection.
     *
     * @param connection the active JDBC {@link Connection} with auto-commit disabled
     * @return the computation result
     * @throws Exception if an error occurs during execution
     */
    T execute(Connection connection) throws Exception;
}

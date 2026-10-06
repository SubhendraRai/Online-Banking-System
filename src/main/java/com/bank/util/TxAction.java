package com.bank.util;

import java.sql.Connection;

/**
 * Functional interface representing a unit of transactional work that does not return a result.
 * <p>
 * Passed to {@link TxRunner#run(TxAction)} to execute within a managed ACID transaction boundary.
 * </p>
 */
@FunctionalInterface
public interface TxAction {

    /**
     * Executes the transactional action using the provided connection.
     *
     * @param connection the active JDBC {@link Connection} with auto-commit disabled
     * @throws Exception if an error occurs during execution
     */
    void execute(Connection connection) throws Exception;
}

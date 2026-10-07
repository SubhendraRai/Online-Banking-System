package com.bank.util;

import com.bank.exception.BankingException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Functional interface representing a unit of transactional work that can throw
 * checked {@link BankingException} business failures or {@link SQLException}.
 * <p>
 * Executed via {@link TxRunner#execute(TxBankingWork)}.
 * </p>
 *
 * @param <T> the type of result produced by the transactional computation
 */
@FunctionalInterface
public interface TxBankingWork<T> {

    /**
     * Executes the unit of work using the provided transactional connection.
     *
     * @param connection the active JDBC {@link Connection} with auto-commit disabled
     * @return the computation result
     * @throws BankingException if a business domain rule is violated
     * @throws SQLException if a database access error occurs
     */
    T execute(Connection connection) throws BankingException, SQLException;
}

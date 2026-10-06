/**
 * Data Access Object (DAO) interfaces and JDBC implementations.
 * <p>
 * Responsible strictly for executing PreparedStatement queries and mapping ResultSets
 * to domain entities. DAOs do not hold business rules. Methods that join
 * an existing database transaction accept an active {@link java.sql.Connection} parameter.
 * </p>
 */
package com.bank.dao;

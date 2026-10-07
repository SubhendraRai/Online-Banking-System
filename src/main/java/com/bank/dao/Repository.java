package com.bank.dao;

import com.bank.exception.DataAccessException;
import com.bank.util.DBUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generic root contract for all Data Access Objects (DAOs) in the system.
 * <p>
 * Provides standardized CRUD operations. Every method capable of participating in a
 * database transaction provides an overload accepting an active {@link Connection}.
 * Default methods handle acquiring a standalone connection via {@link DBUtil#getConnection()}
 * and managing resource lifecycle within try-with-resources.
 * </p>
 *
 * @param <T> entity domain type
 * @param <ID> unique identifier type
 */
public interface Repository<T, ID> {

    /**
     * Retrieves an entity by its identifier using an acquired connection.
     *
     * @param id non-null identifier
     * @return {@link Optional} containing entity if found, empty otherwise
     */
    default Optional<T> findById(ID id) {
        try (Connection conn = DBUtil.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for findById(id=" + id + ")", e);
        }
    }

    /**
     * Retrieves an entity by its identifier within the context of an existing transaction connection.
     *
     * @param conn active database connection
     * @param id non-null identifier
     * @return {@link Optional} containing entity if found, empty otherwise
     */
    Optional<T> findById(Connection conn, ID id);

    /**
     * Retrieves all entities using an acquired connection.
     *
     * @return unmodifiable or populated list of all entities
     */
    default List<T> findAll() {
        try (Connection conn = DBUtil.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for findAll()", e);
        }
    }

    /**
     * Retrieves all entities within the context of an existing transaction connection.
     *
     * @param conn active database connection
     * @return list of all entities
     */
    List<T> findAll(Connection conn);

    /**
     * Persists a new entity using an acquired connection.
     *
     * @param entity entity to persist
     * @return the persisted entity with generated keys populated
     */
    default T save(T entity) {
        try (Connection conn = DBUtil.getConnection()) {
            return save(conn, entity);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for save()", e);
        }
    }

    /**
     * Persists a new entity within an existing transaction connection.
     *
     * @param conn active database connection
     * @param entity entity to persist
     * @return the persisted entity with generated keys populated
     */
    T save(Connection conn, T entity);

    /**
     * Updates an existing entity using an acquired connection.
     *
     * @param entity entity containing updated state
     * @return {@code true} if a record was updated, {@code false} otherwise
     */
    default boolean update(T entity) {
        try (Connection conn = DBUtil.getConnection()) {
            return update(conn, entity);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for update()", e);
        }
    }

    /**
     * Updates an existing entity within an existing transaction connection.
     *
     * @param conn active database connection
     * @param entity entity containing updated state
     * @return {@code true} if a record was updated, {@code false} otherwise
     */
    boolean update(Connection conn, T entity);

    /**
     * Deletes an entity by its identifier using an acquired connection.
     *
     * @param id identifier of entity to delete
     * @return {@code true} if a record was deleted, {@code false} otherwise
     */
    default boolean delete(ID id) {
        try (Connection conn = DBUtil.getConnection()) {
            return delete(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for delete(id=" + id + ")", e);
        }
    }

    /**
     * Deletes an entity by its identifier within an existing transaction connection.
     *
     * @param conn active database connection
     * @param id identifier of entity to delete
     * @return {@code true} if a record was deleted, {@code false} otherwise
     */
    boolean delete(Connection conn, ID id);
}

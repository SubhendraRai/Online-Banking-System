package com.bank.dao;

import com.bank.exception.DataAccessException;
import com.bank.model.Admin;
import com.bank.model.Customer;
import com.bank.model.Role;
import com.bank.model.User;
import com.bank.model.UserStatus;
import com.bank.util.DBUtil;
import com.bank.util.Page;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link User} entities and polymorphically mapped subtypes
 * ({@link Customer} and {@link Admin}).
 * <p>
 * Complies strictly with parameterized {@link PreparedStatement} standards, manages
 * transaction propagation via connection overloads, and wraps underlying SQL exceptions
 * into {@link DataAccessException}.
 * </p>
 */
public class UserDao implements Repository<User, Long> {

    private static final String SELECT_BASE =
            "SELECT user_id, full_name, email, phone, address, password_hash, role, status, "
            + "failed_attempts, locked_until, created_at FROM users ";

    @Override
    public Optional<User> findById(Connection conn, Long id) {
        String sql = SELECT_BASE + "WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find user by ID: " + id, e);
        }
    }

    @Override
    public List<User> findAll(Connection conn) {
        String sql = SELECT_BASE + "ORDER BY user_id ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (rs.next()) {
                users.add(mapRow(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch all users", e);
        }
    }

    @Override
    public User save(Connection conn, User user) {
        String sql = "INSERT INTO users (full_name, email, phone, address, password_hash, role, "
                + "status, failed_attempts, locked_until) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindUserParameters(stmt, user);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setUserId(keys.getLong(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert user with email: " + user.getEmail(), e);
        }
    }

    @Override
    public boolean update(Connection conn, User user) {
        String sql = "UPDATE users SET full_name = ?, phone = ?, address = ?, password_hash = ?, "
                + "role = ?, status = ?, failed_attempts = ?, locked_until = ? WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getPhone());
            stmt.setString(3, user.getAddress());
            stmt.setString(4, user.getPasswordHash());
            stmt.setString(5, user.getRole().name());
            stmt.setString(6, user.getStatus().name());
            stmt.setInt(7, user.getFailedAttempts());
            stmt.setTimestamp(8, user.getLockedUntil() != null ? Timestamp.valueOf(user.getLockedUntil()) : null);
            stmt.setLong(9, user.getUserId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update user ID: " + user.getUserId(), e);
        }
    }

    @Override
    public boolean delete(Connection conn, Long id) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete user ID: " + id, e);
        }
    }

    /**
     * Finds a user by their unique email address using an acquired connection.
     *
     * @param email unique user email
     * @return {@link Optional} containing user if present
     */
    public Optional<User> findByEmail(String email) {
        try (Connection conn = DBUtil.getConnection()) {
            return findByEmail(conn, email);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for findByEmail", e);
        }
    }

    /**
     * Finds a user by their unique email address within a transaction connection.
     *
     * @param conn active database connection
     * @param email unique user email
     * @return {@link Optional} containing user if present
     */
    public Optional<User> findByEmail(Connection conn, String email) {
        String sql = SELECT_BASE + "WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find user by email: " + email, e);
        }
    }

    /**
     * Checks whether an account exists with the specified email using an acquired connection.
     *
     * @param email email to verify
     * @return {@code true} if present, {@code false} otherwise
     */
    public boolean existsByEmail(String email) {
        try (Connection conn = DBUtil.getConnection()) {
            return existsByEmail(conn, email);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for existsByEmail", e);
        }
    }

    /**
     * Checks whether an account exists with the specified email within a transaction connection.
     *
     * @param conn active database connection
     * @param email email to verify
     * @return {@code true} if present, {@code false} otherwise
     */
    public boolean existsByEmail(Connection conn, String email) {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to verify existence of email: " + email, e);
        }
    }

    /**
     * Counts registered users matching the given role using an acquired connection.
     *
     * @param role user role filter
     * @return total user count
     */
    public long countByRole(Role role) {
        try (Connection conn = DBUtil.getConnection()) {
            return countByRole(conn, role);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for countByRole", e);
        }
    }

    /**
     * Counts registered users matching the given role within a transaction connection.
     *
     * @param conn active database connection
     * @param role user role filter
     * @return total user count
     */
    public long countByRole(Connection conn, Role role) {
        String sql = role != null ? "SELECT COUNT(*) FROM users WHERE role = ?" : "SELECT COUNT(*) FROM users";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (role != null) {
                stmt.setString(1, role.name());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count users by role: " + role, e);
        }
    }

    /**
     * Dynamically searches users with pagination using an acquired connection.
     *
     * @param keyword search keyword (matches name, email, phone)
     * @param role role filter
     * @param status status filter
     * @param page 1-indexed page number
     * @param size page size
     * @return paginated {@link Page} of users
     */
    public Page<User> search(String keyword, Role role, UserStatus status, int page, int size) {
        try (Connection conn = DBUtil.getConnection()) {
            return search(conn, keyword, role, status, page, size);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for search", e);
        }
    }

    /**
     * Dynamically searches users with pagination within a transaction connection.
     *
     * @param conn active database connection
     * @param keyword search keyword (matches name, email, phone)
     * @param role role filter
     * @param status status filter
     * @param page 1-indexed page number
     * @param size page size
     * @return paginated {@link Page} of users
     */
    public Page<User> search(Connection conn, String keyword, Role role, UserStatus status, int page, int size) {
        int currentPage = Math.max(1, page);
        int pageSize = Math.max(1, size);
        int offset = (currentPage - 1) * pageSize;

        StringBuilder whereSql = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        buildSearchPredicates(whereSql, params, keyword, role, status);

        long totalItems = executeCount(conn, "SELECT COUNT(*) FROM users" + whereSql, params);
        if (totalItems == 0) {
            return new Page<>(Collections.emptyList(), currentPage, pageSize, 0L);
        }

        String dataSql = SELECT_BASE + whereSql + " ORDER BY user_id ASC LIMIT ? OFFSET ?";
        List<User> items = executeSearchQuery(conn, dataSql, params, pageSize, offset);
        return new Page<>(items, currentPage, pageSize, totalItems);
    }

    private void buildSearchPredicates(StringBuilder whereSql, List<Object> params,
                                       String keyword, Role role, UserStatus status) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            whereSql.append(" AND (LOWER(full_name) LIKE ? OR LOWER(email) LIKE ? OR phone LIKE ?)");
            params.add(pattern);
            params.add(pattern);
            params.add("%" + keyword.trim() + "%");
        }
        if (role != null) {
            whereSql.append(" AND role = ?");
            params.add(role.name());
        }
        if (status != null) {
            whereSql.append(" AND status = ?");
            params.add(status.name());
        }
    }

    private long executeCount(Connection conn, String sql, List<Object> params) {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed executing user search count query", e);
        }
    }

    private List<User> executeSearchQuery(Connection conn, String sql, List<Object> params, int limit, int offset) {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Object param : params) {
                stmt.setObject(idx++, param);
            }
            stmt.setInt(idx++, limit);
            stmt.setInt(idx, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
                return users;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed executing user search data query", e);
        }
    }

    private void bindUserParameters(PreparedStatement stmt, User user) throws SQLException {
        stmt.setString(1, user.getFullName());
        stmt.setString(2, user.getEmail());
        stmt.setString(3, user.getPhone());
        stmt.setString(4, user.getAddress());
        stmt.setString(5, user.getPasswordHash());
        stmt.setString(6, user.getRole().name());
        stmt.setString(7, user.getStatus().name());
        stmt.setInt(8, user.getFailedAttempts());
        stmt.setTimestamp(9, user.getLockedUntil() != null ? Timestamp.valueOf(user.getLockedUntil()) : null);
    }

    private User mapRow(ResultSet rs) throws SQLException {
        Role role = Role.valueOf(rs.getString("role"));
        User user = (role == Role.ADMIN) ? new Admin() : new Customer();

        user.setUserId(rs.getLong("user_id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setAddress(rs.getString("address"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(role);
        user.setStatus(UserStatus.valueOf(rs.getString("status")));
        user.setFailedAttempts(rs.getInt("failed_attempts"));

        Timestamp lockedTs = rs.getTimestamp("locked_until");
        if (lockedTs != null) {
            user.setLockedUntil(lockedTs.toLocalDateTime());
        }
        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            user.setCreatedAt(createdTs.toLocalDateTime());
        }
        return user;
    }
}

package com.bank.dao;

import com.bank.exception.DataAccessException;
import com.bank.model.SystemSetting;
import com.bank.util.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object for {@link SystemSetting} configuration records.
 * <p>
 * Handles retrieval of global parameter dictionaries and updates to system
 * operational settings and financial limits.
 * </p>
 */
public class SettingsDao implements Repository<SystemSetting, String> {

    private static final String SELECT_BASE =
            "SELECT setting_key, setting_value, description, updated_by, updated_at FROM system_settings ";

    @Override
    public Optional<SystemSetting> findById(Connection conn, String key) {
        String sql = SELECT_BASE + "WHERE setting_key = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find setting by key: " + key, e);
        }
    }

    @Override
    public List<SystemSetting> findAll(Connection conn) {
        String sql = SELECT_BASE + "ORDER BY setting_key ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<SystemSetting> settings = new ArrayList<>();
            while (rs.next()) {
                settings.add(mapRow(rs));
            }
            return settings;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch all settings", e);
        }
    }

    @Override
    public SystemSetting save(Connection conn, SystemSetting setting) {
        String sql = "INSERT INTO system_settings (setting_key, setting_value, description, updated_by) "
                + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                + "setting_value = VALUES(setting_value), description = VALUES(description), updated_by = VALUES(updated_by)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, setting.getKey());
            stmt.setString(2, setting.getValue());
            stmt.setString(3, setting.getDescription());
            if (setting.getUpdatedBy() != null) {
                stmt.setLong(4, setting.getUpdatedBy());
            } else {
                stmt.setNull(4, java.sql.Types.BIGINT);
            }
            stmt.executeUpdate();
            return setting;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save setting: " + setting.getKey(), e);
        }
    }

    @Override
    public boolean update(Connection conn, SystemSetting setting) {
        return update(conn, setting.getKey(), setting.getValue(), setting.getUpdatedBy());
    }

    @Override
    public boolean delete(Connection conn, String key) {
        String sql = "DELETE FROM system_settings WHERE setting_key = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete setting: " + key, e);
        }
    }

    /**
     * Retrieves all settings as a key-value mapping using an acquired connection.
     *
     * @return unmodifiable map of configuration key-value pairs
     */
    public Map<String, String> getAll() {
        try (Connection conn = DBUtil.getConnection()) {
            return getAll(conn);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for getAll settings", e);
        }
    }

    /**
     * Retrieves all settings as a key-value mapping within a transaction connection.
     *
     * @param conn active database connection
     * @return unmodifiable map of configuration key-value pairs
     */
    public Map<String, String> getAll(Connection conn) {
        String sql = "SELECT setting_key, setting_value FROM system_settings ORDER BY setting_key ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            Map<String, String> map = new LinkedHashMap<>();
            while (rs.next()) {
                map.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
            return Collections.unmodifiableMap(map);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve all settings map", e);
        }
    }

    /**
     * Updates a configuration setting value using an acquired connection.
     *
     * @param key setting key
     * @param value updated value string
     * @return {@code true} if updated, {@code false} otherwise
     */
    public boolean update(String key, String value) {
        return update(key, value, null);
    }

    /**
     * Updates a configuration setting value within a transaction connection.
     *
     * @param conn active database connection
     * @param key setting key
     * @param value updated value string
     * @return {@code true} if updated, {@code false} otherwise
     */
    public boolean update(Connection conn, String key, String value) {
        return update(conn, key, value, null);
    }

    /**
     * Updates a configuration setting value and auditing administrator ID using an acquired connection.
     *
     * @param key setting key
     * @param value updated value string
     * @param updatedBy administrator user ID
     * @return {@code true} if updated, {@code false} otherwise
     */
    public boolean update(String key, String value, Long updatedBy) {
        try (Connection conn = DBUtil.getConnection()) {
            return update(conn, key, value, updatedBy);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for update setting", e);
        }
    }

    /**
     * Updates a configuration setting value and auditing administrator ID within a transaction connection.
     *
     * @param conn active database connection
     * @param key setting key
     * @param value updated value string
     * @param updatedBy administrator user ID
     * @return {@code true} if updated, {@code false} otherwise
     */
    public boolean update(Connection conn, String key, String value, Long updatedBy) {
        String sql = "UPDATE system_settings SET setting_value = ?, updated_by = ? WHERE setting_key = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            if (updatedBy != null) {
                stmt.setLong(2, updatedBy);
            } else {
                stmt.setNull(2, java.sql.Types.BIGINT);
            }
            stmt.setString(3, key);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update setting key: " + key, e);
        }
    }

    private SystemSetting mapRow(ResultSet rs) throws SQLException {
        String key = rs.getString("setting_key");
        String value = rs.getString("setting_value");
        String desc = rs.getString("description");
        Long updatedBy = rs.getObject("updated_by", Long.class);
        Timestamp ts = rs.getTimestamp("updated_at");
        return new SystemSetting(key, value, desc, updatedBy, ts != null ? ts.toLocalDateTime() : null);
    }
}

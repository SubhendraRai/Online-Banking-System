package com.bank.dao;

import com.bank.util.DBUtil;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base fixture for DAO integration tests executing against {@code bankdb_test}.
 * <p>
 * Configures DBUtil from {@code db-test.properties}, detects database availability,
 * ensures schema tables exist, and resets data fixtures before every test run.
 * </p>
 */
public abstract class BaseDaoIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDaoIntegrationTest.class);
    private static final String TEST_CONFIG_FILE = "db-test.properties";
    private static boolean databaseAvailable = false;

    @BeforeAll
    static void initDatabaseConnection() {
        try {
            DBUtil.reloadConfiguration(TEST_CONFIG_FILE);
            try (Connection conn = DBUtil.getConnection()) {
                ensureSchemaCreated(conn);
                databaseAvailable = true;
                LOGGER.info("MySQL test database 'bankdb_test' is reachable. Running integration tests.");
            }
        } catch (Exception e) {
            databaseAvailable = false;
            LOGGER.warn("MySQL database 'bankdb_test' is NOT accessible: {}. Tests will be skipped.", e.getMessage());
        }
    }

    @AfterAll
    static void tearDownDatabaseConnection() {
        try {
            DBUtil.reloadConfiguration();
        } catch (Exception ignored) {
        }
    }

    @BeforeEach
    void verifyDatabaseAndReset() {
        Assumptions.assumeTrue(databaseAvailable,
                "Integration test requires active MySQL server on localhost:3306 with bankdb_test database.");
        try (Connection conn = DBUtil.getConnection()) {
            resetTestData(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed resetting test database fixture", e);
        }
    }

    private static void ensureSchemaCreated(Connection conn) throws SQLException {
        boolean usersTableExists = false;
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'users'");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                usersTableExists = true;
            }
        }

        if (!usersTableExists) {
            LOGGER.info("Schema not found in test database. Executing schema.sql initialization DDL...");
            ClassLoader cl = BaseDaoIntegrationTest.class.getClassLoader();
            try (InputStream in = cl.getResourceAsStream("schema.sql")) {
                if (in != null) {
                    executeSqlScript(conn, in);
                }
            } catch (Exception e) {
                LOGGER.warn("Could not auto-apply schema.sql: {}", e.getMessage());
            }
        }
    }

    private static void executeSqlScript(Connection conn, InputStream in) throws Exception {
        String script = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        String[] statements = script.split(";");
        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    private static void resetTestData(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            stmt.execute("TRUNCATE TABLE audit_log");
            stmt.execute("TRUNCATE TABLE fraud_alerts");
            stmt.execute("TRUNCATE TABLE fixed_deposits");
            stmt.execute("TRUNCATE TABLE loans");
            stmt.execute("TRUNCATE TABLE transactions");
            stmt.execute("TRUNCATE TABLE accounts");
            stmt.execute("TRUNCATE TABLE system_settings");
            stmt.execute("TRUNCATE TABLE users");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            // Seed system settings
            stmt.execute("INSERT INTO system_settings (setting_key, setting_value, description) VALUES "
                    + "('transfer.per_txn_limit', '50000.00', 'Max per txn limit'), "
                    + "('transfer.daily_limit', '100000.00', 'Daily transfer limit'), "
                    + "('savings.min_balance', '500.00', 'Savings min balance')");

            // Seed users (1 Admin, 2 Customers)
            stmt.execute("INSERT INTO users (user_id, full_name, email, phone, address, password_hash, role, status) VALUES "
                    + "(1, 'System Administrator', 'admin@bank.com', '9876543210', 'HQ Mumbai', '$2a$10$wuPl6h9I4h9DNNMzaMriC.P5h9BKz6LeEXnnqvOKt0dJ4.4PvJN/C', 'ADMIN', 'ACTIVE'), "
                    + "(2, 'Rahul Sharma', 'rahul.sharma@example.com', '9811122233', 'Connaught Delhi', '$2a$10$QDHFuxfmTLx8mAlofdakfuKqD.MoCwDQ9bY0XJIASvWDq7zIzbY66', 'CUSTOMER', 'ACTIVE'), "
                    + "(3, 'Priya Patel', 'priya.patel@example.com', '9822233344', 'Navrangpura Ahmedabad', '$2a$10$e8nwObhojGMAbrCXrtdI7Ots9uAt80jiOerhIBauV5lFaZZpAUb4a', 'CUSTOMER', 'ACTIVE')");

            // Seed accounts (2 for Rahul, 1 for Priya)
            stmt.execute("INSERT INTO accounts (account_no, user_id, account_type, balance, overdraft_limit, status) VALUES "
                    + "('100000000001', 2, 'SAVINGS', 25000.00, 0.00, 'ACTIVE'), "
                    + "('100000000002', 2, 'CURRENT', 50000.00, 10000.00, 'ACTIVE'), "
                    + "('100000000003', 3, 'SAVINGS', 75000.00, 0.00, 'ACTIVE')");
        }
    }
}

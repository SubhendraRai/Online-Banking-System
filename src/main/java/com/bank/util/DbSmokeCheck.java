package com.bank.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Diagnostic utility with a standalone {@code main} method to verify database connectivity.
 * <p>
 * Reads database credentials from {@code db.properties} on the classpath, establishes a JDBC
 * connection via {@link DriverManager}, and executes a ping query ({@code SELECT 1}) using
 * a {@link PreparedStatement} within try-with-resources blocks.
 * </p>
 */
public final class DbSmokeCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbSmokeCheck.class);
    private static final String CONFIG_FILE = "db.properties";
    private static final String PING_QUERY = "SELECT 1";

    private DbSmokeCheck() {
        // Utility class prevention of instantiation
    }

    /**
     * Executes the smoke check.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        LOGGER.info("Starting Database Smoke Check diagnostic...");
        try {
            Properties properties = loadDatabaseConfiguration();
            verifyDatabaseConnectivity(properties);
            LOGGER.info(">>> SUCCESS: Database connection and query execution verified successfully!");
        } catch (IOException e) {
            LOGGER.error("Configuration error: Failed to read '{}' from classpath. "
                    + "Please ensure you have copied 'db.properties.example' to 'db.properties'.", CONFIG_FILE, e);
            System.exit(1);
        } catch (SQLException e) {
            LOGGER.error("Database connection failure: Unable to connect or execute query on MySQL server. "
                    + "Verify that MySQL is running and credentials in 'db.properties' are correct.", e);
            System.exit(2);
        }
    }

    /**
     * Loads database credentials from the classpath properties file.
     *
     * @return populated {@link Properties} object
     * @throws IOException if configuration file is missing or unreadable
     */
    private static Properties loadDatabaseConfiguration() throws IOException {
        ClassLoader classLoader = DbSmokeCheck.class.getClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IOException("Resource file '" + CONFIG_FILE + "' not found on classpath.");
            }
            Properties props = new Properties();
            props.load(input);
            return props;
        }
    }

    /**
     * Connects to the database and executes a {@code SELECT 1} query.
     *
     * @param props connection configuration properties
     * @throws SQLException if a database access error occurs
     */
    private static void verifyDatabaseConnectivity(Properties props) throws SQLException {
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        if (url == null || user == null) {
            throw new IllegalArgumentException("db.url or db.user is not configured in " + CONFIG_FILE);
        }

        LOGGER.info("Attempting connection to JDBC URL: {}", url);
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData metaData = connection.getMetaData();
            LOGGER.info("Connected to {} version {}", metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion());

            try (PreparedStatement stmt = connection.prepareStatement(PING_QUERY);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int resultValue = rs.getInt(1);
                    LOGGER.info("Executed '{}' -> Result: {}", PING_QUERY, resultValue);
                } else {
                    throw new SQLException("No rows returned from ping query '" + PING_QUERY + "'");
                }
            }
        }
    }
}

package com.bank.util;

import com.bank.exception.DataAccessException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe database utility responsible for connection provisioning.
 * <p>
 * Reads connection credentials from {@code db.properties} on the classpath.
 * Designed with a pluggable connection provider architecture: defaults to
 * {@link DriverManager} and allows swapping in a production connection pool
 * (such as HikariCP or Apache Commons DBCP) via {@link #setDataSource(DataSource)}
 * without requiring changes to service or DAO callers.
 * </p>
 */
public final class DBUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBUtil.class);
    private static final String DEFAULT_CONFIG_FILE = "db.properties";
    private static String activeConfigFile = DEFAULT_CONFIG_FILE;

    private static final Properties PROPERTIES = new Properties();
    private static volatile DataSource pooledDataSource = null;

    static {
        loadConfiguration();
    }

    private DBUtil() {
        // Prevent instantiation of utility class
    }

    /**
     * Loads database credentials from the active configuration file and initializes driver.
     */
    private static void loadConfiguration() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = DBUtil.class.getClassLoader();
        }

        try (InputStream input = classLoader.getResourceAsStream(activeConfigFile)) {
            if (input == null) {
                LOGGER.warn("Configuration file '{}' not found on classpath. "
                        + "Default credentials or programmatic DataSource must be configured.", activeConfigFile);
                return;
            }
            PROPERTIES.clear();
            PROPERTIES.load(input);
            String driverClass = PROPERTIES.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
            Class.forName(driverClass);
            LOGGER.info("DBUtil initialized successfully using config '{}' and driver: {}", activeConfigFile, driverClass);
        } catch (IOException e) {
            LOGGER.error("Failed to load database configuration from '{}'", activeConfigFile, e);
            throw new DataAccessException("Failed to load " + activeConfigFile + " from classpath", e);
        } catch (ClassNotFoundException e) {
            LOGGER.error("MySQL JDBC driver class not found on classpath", e);
            throw new DataAccessException("MySQL JDBC driver not found on classpath", e);
        }
    }

    /**
     * Obtains an active JDBC {@link Connection}.
     * <p>
     * If a connection pool {@link DataSource} has been configured via {@link #setDataSource(DataSource)},
     * returns a connection from the pool. Otherwise, creates a direct connection via {@link DriverManager}.
     * </p>
     *
     * @return an active {@link Connection} instance
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        if (pooledDataSource != null) {
            return pooledDataSource.getConnection();
        }

        String url = PROPERTIES.getProperty("db.url");
        String user = PROPERTIES.getProperty("db.user");
        String password = PROPERTIES.getProperty("db.password");

        if (url == null || user == null) {
            throw new SQLException("Database connection properties ('db.url', 'db.user') are not configured in " + activeConfigFile);
        }

        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Pluggable hook to swap in a connection pool (e.g. HikariCP or Tomcat DataSource).
     *
     * @param dataSource the connection pool DataSource to use for all future connections
     */
    public static void setDataSource(DataSource dataSource) {
        LOGGER.info("Configuring custom DataSource for DBUtil: {}", 
                dataSource != null ? dataSource.getClass().getName() : "null");
        DBUtil.pooledDataSource = dataSource;
    }

    /**
     * Returns the currently configured DataSource, or {@code null} if using DriverManager.
     *
     * @return current {@link DataSource} or null
     */
    public static DataSource getDataSource() {
        return pooledDataSource;
    }

    /**
     * Reloads configuration properties from the default classpath properties file.
     */
    public static synchronized void reloadConfiguration() {
        reloadConfiguration(DEFAULT_CONFIG_FILE);
    }

    /**
     * Reloads configuration properties from a specified classpath configuration file.
     *
     * @param configFilename configuration resource file name (e.g. {@code "db-test.properties"})
     */
    public static synchronized void reloadConfiguration(String configFilename) {
        activeConfigFile = (configFilename != null && !configFilename.isBlank()) ? configFilename : DEFAULT_CONFIG_FILE;
        loadConfiguration();
    }

    /**
     * Safely resets any configured DataSource, closing it if AutoCloseable, and restores defaults.
     */
    public static synchronized void reset() {
        if (pooledDataSource instanceof AutoCloseable closeable) {
            try {
                closeable.close();
                LOGGER.info("Successfully closed custom DataSource during DBUtil reset.");
            } catch (Exception e) {
                LOGGER.warn("Failed closing custom DataSource during DBUtil reset", e);
            }
        }
        pooledDataSource = null;
        activeConfigFile = DEFAULT_CONFIG_FILE;
        loadConfiguration();
    }
}

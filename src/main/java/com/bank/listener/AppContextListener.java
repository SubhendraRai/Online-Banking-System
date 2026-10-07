package com.bank.listener;

import com.bank.dao.SettingsDao;
import com.bank.service.DefaultSettingsProvider;
import com.bank.util.DBUtil;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet context lifecycle listener managing application bootstrapping and teardown.
 * <p>
 * At startup, verifies database connectivity and preloads system settings into application scope.
 * At shutdown, deregisters JDBC drivers, stops background threads, and resets DB resources
 * to eliminate memory and thread leaks on container reload.
 * </p>
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        LOGGER.info("=================================================");
        LOGGER.info("Starting Online Banking System (Tomcat 10.1 / Jakarta EE 10)...");
        LOGGER.info("=================================================");

        bootstrapDatabaseAndSettings(context);
        LOGGER.info("Online Banking System web application initialized successfully.");
    }

    private void bootstrapDatabaseAndSettings(ServletContext context) {
        SettingsDao settingsDao = new SettingsDao();
        try (Connection conn = DBUtil.getConnection()) {
            LOGGER.info("Database connectivity established: {}", conn.getMetaData().getDatabaseProductName());
            Map<String, String> settings = settingsDao.getAll(conn);
            context.setAttribute("systemSettings", settings);
            context.setAttribute("settingsProvider", new DefaultSettingsProvider(settingsDao));
            LOGGER.info("Successfully loaded {} global settings into application scope.", settings.size());
        } catch (Exception e) {
            LOGGER.warn("Database or settings bootstrap warning (running in fallback defaults mode): {}", e.getMessage());
            context.setAttribute("settingsProvider", new DefaultSettingsProvider(settingsDao));
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Shutting down Online Banking System web application context...");
        cleanUpJdbcDrivers();
        cleanUpMySqlThreads();
        DBUtil.reset();
        LOGGER.info("Application context destruction complete.");
    }

    private void cleanUpJdbcDrivers() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == cl) {
                try {
                    DriverManager.deregisterDriver(driver);
                    LOGGER.info("Deregistered JDBC driver on context destruction: {}", driver);
                } catch (SQLException ex) {
                    LOGGER.warn("Failed deregistering JDBC driver {}: {}", driver, ex.getMessage());
                }
            }
        }
    }

    private void cleanUpMySqlThreads() {
        try {
            Class<?> cleanupThreadClass = Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");
            cleanupThreadClass.getMethod("checkedShutdown").invoke(null);
            LOGGER.info("MySQL AbandonedConnectionCleanupThread successfully stopped.");
        } catch (ClassNotFoundException ignored) {
            // Not running with MySQL Connector/J on classpath
        } catch (Exception ex) {
            LOGGER.warn("Failed to stop MySQL AbandonedConnectionCleanupThread: {}", ex.getMessage());
        }
    }
}

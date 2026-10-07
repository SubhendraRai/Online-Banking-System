package com.bank.dao;

import com.bank.model.Admin;
import com.bank.model.SavingsAccount;
import com.bank.model.SystemSetting;
import com.bank.model.Transaction;
import com.bank.model.User;
import com.bank.model.UserStatus;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DaoConnectionLifecycleTest {

    private Connection mockConnection;
    private List<String> invocationLog;

    @BeforeEach
    void setUp() {
        invocationLog = new ArrayList<>();

        ResultSet mockResultSet = (ResultSet) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("next".equals(name)) return false;
                    if ("close".equals(name)) {
                        invocationLog.add("resultSet.close()");
                        return null;
                    }
                    return null;
                }
        );

        PreparedStatement mockPreparedStatement = (PreparedStatement) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("executeQuery".equals(name)) {
                        invocationLog.add("statement.executeQuery()");
                        return mockResultSet;
                    }
                    if ("executeUpdate".equals(name)) {
                        invocationLog.add("statement.executeUpdate()");
                        return 1;
                    }
                    if ("getGeneratedKeys".equals(name)) {
                        return mockResultSet;
                    }
                    if ("close".equals(name)) {
                        invocationLog.add("statement.close()");
                        return null;
                    }
                    return null;
                }
        );

        mockConnection = (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("prepareStatement".equals(name)) {
                        invocationLog.add("connection.prepareStatement()");
                        return mockPreparedStatement;
                    }
                    if ("close".equals(name)) {
                        invocationLog.add("connection.close()");
                        return null;
                    }
                    return null;
                }
        );
    }

    @Test
    @DisplayName("DAO methods accepting Connection must NOT close the caller-managed connection")
    void testDaoDoesNotCloseTransactionConnection() {
        UserDao userDao = new UserDao();
        AccountDao accountDao = new AccountDao();
        TransactionDao transactionDao = new TransactionDao();
        SettingsDao settingsDao = new SettingsDao();

        userDao.findById(mockConnection, 1L);
        accountDao.findById(mockConnection, "100000000001");
        accountDao.findByIdForUpdate(mockConnection, "100000000001");
        transactionDao.findById(mockConnection, 99L);
        settingsDao.findById(mockConnection, "transfer.daily_limit");

        assertTrue(invocationLog.contains("connection.prepareStatement()"));
        assertTrue(invocationLog.contains("statement.close()"));
        assertFalse(invocationLog.contains("connection.close()"),
                "DAO closed the transaction Connection! Connection must remain open for caller transaction control.");
    }
}

package com.bank.util;

import com.bank.exception.DataAccessException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TxRunnerTest {

    private List<String> callLog;
    private boolean autoCommitState;
    private boolean isClosed;

    @BeforeEach
    void setUp() {
        callLog = new ArrayList<>();
        autoCommitState = true;
        isClosed = false;

        // Configure a mock Connection & DataSource via Java dynamic proxy
        Connection proxyConnection = (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "setAutoCommit":
                            autoCommitState = (Boolean) args[0];
                            callLog.add("setAutoCommit(" + autoCommitState + ")");
                            return null;
                        case "getAutoCommit":
                            callLog.add("getAutoCommit()");
                            return autoCommitState;
                        case "commit":
                            callLog.add("commit()");
                            return null;
                        case "rollback":
                            callLog.add("rollback()");
                            return null;
                        case "close":
                            isClosed = true;
                            callLog.add("close()");
                            return null;
                        case "isClosed":
                            return isClosed;
                        default:
                            return null;
                    }
                }
        );

        DataSource proxyDataSource = (DataSource) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{DataSource.class},
                (proxy, method, args) -> {
                    if ("getConnection".equals(method.getName())) {
                        callLog.add("getConnection()");
                        return proxyConnection;
                    }
                    return null;
                }
        );

        DBUtil.setDataSource(proxyDataSource);
    }

    @AfterEach
    void tearDown() {
        DBUtil.setDataSource(null);
    }

    @Test
    @DisplayName("Successful transaction executes work, commits, restores autocommit, and closes")
    void testSuccessfulTransaction() {
        String result = TxRunner.run(conn -> {
            callLog.add("workExecuted()");
            return "SUCCESS_VALUE";
        });

        assertEquals("SUCCESS_VALUE", result);
        assertTrue(callLog.contains("getConnection()"));
        assertTrue(callLog.contains("setAutoCommit(false)"));
        assertTrue(callLog.contains("workExecuted()"));
        assertTrue(callLog.contains("commit()"));
        assertTrue(callLog.contains("setAutoCommit(true)"));
        assertTrue(callLog.contains("close()"));
    }

    @Test
    @DisplayName("Failing transaction executes work, rolls back, restores autocommit, closes, and wraps SQLException")
    void testFailingTransactionRollsBack() {
        DataAccessException ex = assertThrows(DataAccessException.class, () ->
                TxRunner.run(conn -> {
                    callLog.add("workFailed()");
                    throw new SQLException("Simulated deadlock failure");
                })
        );

        assertTrue(ex.getMessage().contains("Simulated deadlock failure"));
        assertTrue(callLog.contains("getConnection()"));
        assertTrue(callLog.contains("setAutoCommit(false)"));
        assertTrue(callLog.contains("workFailed()"));
        assertTrue(callLog.contains("rollback()"));
        assertTrue(callLog.contains("setAutoCommit(true)"));
        assertTrue(callLog.contains("close()"));
    }

    @Test
    @DisplayName("TxAction executes void operation within transaction boundary")
    void testTxActionExecution() {
        final boolean[] flag = {false};
        TxRunner.runAction(conn -> flag[0] = true);

        assertTrue(flag[0]);
        assertTrue(callLog.contains("commit()"));
        assertTrue(callLog.contains("close()"));
    }
}

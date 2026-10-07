package com.bank.service;

import com.bank.util.DBUtil;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base test harness for service tier testing.
 * <p>
 * Provisions a dynamic proxy {@link DataSource} and {@link Connection} to satisfy
 * {@link DBUtil#getConnection()} and {@link com.bank.util.TxRunner} transactions
 * without requiring an active MySQL database instance.
 * </p>
 */
public abstract class BaseServiceTest {

    protected List<String> connectionCallLog;
    protected boolean autoCommitState;
    protected boolean connectionClosed;

    @BeforeEach
    void setUpBase() {
        connectionCallLog = new ArrayList<>();
        autoCommitState = true;
        connectionClosed = false;

        Connection proxyConnection = (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "setAutoCommit":
                            autoCommitState = (Boolean) args[0];
                            connectionCallLog.add("setAutoCommit(" + autoCommitState + ")");
                            return null;
                        case "getAutoCommit":
                            connectionCallLog.add("getAutoCommit()");
                            return autoCommitState;
                        case "commit":
                            connectionCallLog.add("commit()");
                            return null;
                        case "rollback":
                            connectionCallLog.add("rollback()");
                            return null;
                        case "close":
                            connectionClosed = true;
                            connectionCallLog.add("close()");
                            return null;
                        case "isClosed":
                            return connectionClosed;
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
                        connectionCallLog.add("getConnection()");
                        return proxyConnection;
                    }
                    return null;
                }
        );

        DBUtil.setDataSource(proxyDataSource);
    }

    @AfterEach
    void tearDownBase() {
        DBUtil.setDataSource(null);
    }
}

package com.bank.service;

import com.bank.service.fakes.FakeAccountDao;
import com.bank.service.fakes.FakeTransactionDao;
import com.bank.util.DBUtil;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base test harness for service tier testing.
 * <p>
 * Provisions a dynamic proxy {@link DataSource} and thread-safe {@link Connection} instances
 * to satisfy {@link DBUtil#getConnection()} and transactional services without requiring an active MySQL database instance.
 * </p>
 */
public abstract class BaseServiceTest {

    protected List<String> connectionCallLog;
    protected volatile boolean autoCommitState;
    protected volatile boolean connectionClosed;

    protected Consumer<Connection> commitHook;
    protected Consumer<Connection> rollbackHook;

    @BeforeEach
    void setUpBase() {
        connectionCallLog = Collections.synchronizedList(new ArrayList<>());
        autoCommitState = true;
        connectionClosed = false;
        commitHook = null;
        rollbackHook = null;

        DataSource proxyDataSource = (DataSource) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{DataSource.class},
                (proxy, method, args) -> {
                    if ("getConnection".equals(method.getName())) {
                        connectionCallLog.add("getConnection()");
                        return createConnectionProxy();
                    }
                    return null;
                }
        );

        DBUtil.setDataSource(proxyDataSource);
    }

    protected Connection createConnectionProxy() {
        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicBoolean autoCommit = new AtomicBoolean(true);

        return (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "setAutoCommit":
                            boolean ac = (Boolean) args[0];
                            autoCommit.set(ac);
                            autoCommitState = ac;
                            connectionCallLog.add("setAutoCommit(" + ac + ")");
                            return null;
                        case "getAutoCommit":
                            connectionCallLog.add("getAutoCommit()");
                            return autoCommit.get();
                        case "commit":
                            connectionCallLog.add("commit()");
                            FakeAccountDao aDao = FakeAccountDao.ACTIVE_DAOS.remove((Connection) proxy);
                            if (aDao != null) aDao.commit((Connection) proxy);
                            FakeTransactionDao tDao = FakeTransactionDao.ACTIVE_DAOS.remove((Connection) proxy);
                            if (tDao != null) tDao.commit((Connection) proxy);
                            if (commitHook != null) {
                                commitHook.accept((Connection) proxy);
                            }
                            return null;
                        case "rollback":
                            connectionCallLog.add("rollback()");
                            FakeAccountDao raDao = FakeAccountDao.ACTIVE_DAOS.remove((Connection) proxy);
                            if (raDao != null) raDao.rollback((Connection) proxy);
                            FakeTransactionDao rtDao = FakeTransactionDao.ACTIVE_DAOS.remove((Connection) proxy);
                            if (rtDao != null) rtDao.rollback((Connection) proxy);
                            if (rollbackHook != null) {
                                rollbackHook.accept((Connection) proxy);
                            }
                            return null;
                        case "close":
                            closed.set(true);
                            connectionClosed = true;
                            connectionCallLog.add("close()");
                            return null;
                        case "isClosed":
                            return closed.get();
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        case "toString":
                            return "MockConnection@" + Integer.toHexString(System.identityHashCode(proxy));
                        default:
                            return null;
                    }
                }
        );
    }

    @AfterEach
    void tearDownBase() {
        DBUtil.setDataSource(null);
    }
}

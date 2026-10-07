package com.bank.dao;

import com.bank.model.Account;
import com.bank.model.AccountStatus;
import com.bank.model.CurrentAccount;
import com.bank.model.SavingsAccount;
import com.bank.util.DBUtil;
import com.bank.util.Money;
import com.bank.util.TxRunner;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountDaoTest extends BaseDaoIntegrationTest {

    private AccountDao accountDao;

    @BeforeEach
    void setUp() {
        accountDao = new AccountDao();
    }

    @Test
    @DisplayName("save persists SavingsAccount and CurrentAccount with generated account number")
    void testSaveAccounts() {
        SavingsAccount savings = new SavingsAccount(null, 3L, new BigDecimal("15000.00"));
        Account savedSavings = accountDao.save(savings);
        assertNotNull(savedSavings.getAccountNo());
        assertEquals(12, savedSavings.getAccountNo().length());

        Optional<Account> fetched = accountDao.findById(savedSavings.getAccountNo());
        assertTrue(fetched.isPresent());
        assertTrue(fetched.get() instanceof SavingsAccount);
        assertEquals(0, new BigDecimal("15000.00").compareTo(fetched.get().getBalance()));

        CurrentAccount current = new CurrentAccount(null, 3L, new BigDecimal("30000.00"),
                AccountStatus.ACTIVE, new BigDecimal("5000.00"));
        Account savedCurrent = accountDao.save(current);
        assertTrue(savedCurrent instanceof CurrentAccount);
        assertEquals(0, new BigDecimal("5000.00").compareTo(((CurrentAccount) savedCurrent).getOverdraftLimit()));
    }

    @Test
    @DisplayName("findById retrieves existing accounts and distinguishes account types")
    void testFindById() {
        Optional<Account> acc1 = accountDao.findById("100000000001");
        assertTrue(acc1.isPresent());
        assertTrue(acc1.get() instanceof SavingsAccount);

        Optional<Account> acc2 = accountDao.findById("100000000002");
        assertTrue(acc2.isPresent());
        assertTrue(acc2.get() instanceof CurrentAccount);
        assertEquals(0, new BigDecimal("10000.00").compareTo(((CurrentAccount) acc2.get()).getOverdraftLimit()));
    }

    @Test
    @DisplayName("findByUser returns all accounts owned by a customer")
    void testFindByUser() {
        List<Account> rahulAccounts = accountDao.findByUser(2L);
        assertEquals(2, rahulAccounts.size());

        List<Account> priyaAccounts = accountDao.findByUser(3L);
        assertEquals(1, priyaAccounts.size());
    }

    @Test
    @DisplayName("nextAccountNumber generates unique sequential 12-digit identifier")
    void testNextAccountNumber() {
        String nextNo = accountDao.nextAccountNumber();
        assertNotNull(nextNo);
        assertEquals(12, nextNo.length());
        assertTrue(nextNo.compareTo("100000000003") > 0);
    }

    @Test
    @DisplayName("updateBalance updates monetary balance accurately")
    void testUpdateBalance() {
        Account account = accountDao.findById("100000000001").orElseThrow();
        account.setBalance(new BigDecimal("32000.50"));

        boolean updated = accountDao.updateBalance(account);
        assertTrue(updated);

        Account refreshed = accountDao.findById("100000000001").orElseThrow();
        assertEquals(0, new BigDecimal("32000.50").compareTo(refreshed.getBalance()));
    }

    @Test
    @DisplayName("delete removes account when no foreign key ledger transactions exist")
    void testDeleteAccount() {
        SavingsAccount temp = new SavingsAccount(null, 3L, new BigDecimal("1000.00"));
        Account saved = accountDao.save(temp);
        String accNo = saved.getAccountNo();

        assertTrue(accountDao.delete(accNo));
        assertFalse(accountDao.findById(accNo).isPresent());
    }

    @Test
    @DisplayName("findByIdForUpdate acquires row lock inside transaction and updates balance atomically")
    void testFindByIdForUpdateInsideTransaction() throws Exception {
        String targetAccNo = "100000000001";
        BigDecimal depositAmount = new BigDecimal("1500.00");
        CountDownLatch lockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch finishTransactionLatch = new CountDownLatch(1);
        AtomicBoolean secondThreadBlocked = new AtomicBoolean(false);

        // Thread 1: Starts transaction, acquires SELECT ... FOR UPDATE, holds lock
        Thread txThread = new Thread(() -> {
            TxRunner.runAction(conn -> {
                Optional<Account> locked = accountDao.findByIdForUpdate(conn, targetAccNo);
                assertTrue(locked.isPresent());
                Account acc = locked.get();
                acc.deposit(depositAmount);
                accountDao.updateBalance(conn, acc);

                lockAcquiredLatch.countDown();
                try {
                    // Hold lock until second thread tests lock contention
                    finishTransactionLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });
        txThread.start();

        // Wait until Thread 1 holds the lock
        assertTrue(lockAcquiredLatch.await(3, TimeUnit.SECONDS));

        // Thread 2: Attempts to acquire lock with short wait timeout
        Thread contenderThread = new Thread(() -> {
            try (Connection conn = DBUtil.getConnection()) {
                conn.setAutoCommit(false);
                // Set lock wait timeout to 1 second for this session to verify blocking
                try (var stmt = conn.createStatement()) {
                    stmt.execute("SET innodb_lock_wait_timeout = 1");
                }
                accountDao.findByIdForUpdate(conn, targetAccNo);
            } catch (Exception e) {
                // Expected lock wait timeout or blocking while Thread 1 holds lock
                secondThreadBlocked.set(true);
            }
        });
        contenderThread.start();
        contenderThread.join(2500);

        // Release Thread 1 transaction
        finishTransactionLatch.countDown();
        txThread.join(3000);

        // Verify balance committed by Thread 1
        Account committedAcc = accountDao.findById(targetAccNo).orElseThrow();
        assertEquals(0, new BigDecimal("26500.00").compareTo(committedAcc.getBalance()));
        assertTrue(secondThreadBlocked.get(), "Second transaction was blocked by pessimistic FOR UPDATE lock");
    }
}

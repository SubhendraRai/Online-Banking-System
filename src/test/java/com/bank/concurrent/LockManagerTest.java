package com.bank.concurrent;

import com.bank.exception.ServiceBusyException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LockManagerTest {

    private ConcurrentHashMap<String, ReentrantLock> lockMap;
    private LockManager lockManager;

    @BeforeEach
    void setUp() {
        lockMap = new ConcurrentHashMap<>();
        lockManager = new LockManager(lockMap, 1L); // 1-second timeout for rapid test execution
    }

    @Test
    @DisplayName("Single account lock acquisition and release")
    void testSingleAccountLockAndUnlock() throws Exception {
        String accNo = "100000000001";
        lockManager.lock(accNo);

        ReentrantLock lock = lockManager.getLock(accNo);
        assertNotNull(lock);
        assertTrue(lock.isHeldByCurrentThread());
        assertEquals(1, lock.getHoldCount());

        lockManager.unlock(accNo);
        assertFalse(lock.isHeldByCurrentThread());
    }

    @Test
    @DisplayName("Multiple accounts locked in sorted order")
    void testMultipleAccountsLockAll() throws Exception {
        List<String> accounts = List.of("100000000001", "100000000002", "100000000003");
        lockManager.lockAll(accounts);

        for (String acc : accounts) {
            assertTrue(lockManager.getLock(acc).isHeldByCurrentThread());
        }

        lockManager.unlockAll(accounts);

        for (String acc : accounts) {
            assertFalse(lockManager.getLock(acc).isHeldByCurrentThread());
        }
    }

    @Test
    @DisplayName("Lock failure on second account releases previously acquired locks and throws ServiceBusyException")
    void testLockAllRollbackOnTimeout() throws Exception {
        String acc1 = "100000000001";
        String acc2 = "100000000002";

        ReentrantLock lock2 = lockManager.getLock(acc2);

        // Background thread acquires lock2 and holds it
        CountDownLatch threadAcquired = new CountDownLatch(1);
        CountDownLatch testFinished = new CountDownLatch(1);

        Thread holdingThread = new Thread(() -> {
            lock2.lock();
            try {
                threadAcquired.countDown();
                testFinished.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            } finally {
                lock2.unlock();
            }
        });
        holdingThread.start();

        assertTrue(threadAcquired.await(2, TimeUnit.SECONDS));

        // Current thread tries to lock [acc1, acc2]. acc1 succeeds, acc2 times out!
        ServiceBusyException ex = assertThrows(ServiceBusyException.class, () ->
                lockManager.lockAll(List.of(acc1, acc2))
        );

        assertTrue(ex.getMessage().contains("temporarily busy"));

        // Crucial check: acc1 MUST have been released despite being acquired before acc2 failed
        ReentrantLock lock1 = lockManager.getLock(acc1);
        assertFalse(lock1.isHeldByCurrentThread(), "First lock must be released when second lock times out");

        testFinished.countDown();
        holdingThread.join(2000);
    }

    @Test
    @DisplayName("Interrupted thread aborts lock acquisition, releases acquired locks, and throws ServiceBusyException")
    void testLockAllRollbackOnInterrupt() throws Exception {
        String acc1 = "100000000001";
        String acc2 = "100000000002";

        ReentrantLock lock2 = lockManager.getLock(acc2);
        CountDownLatch lock2Held = new CountDownLatch(1);
        CountDownLatch readyToInterrupt = new CountDownLatch(1);
        AtomicBoolean threwExpected = new AtomicBoolean(false);

        Thread lockThread = new Thread(() -> {
            try {
                lock2.lock();
                lock2Held.countDown();
                readyToInterrupt.await();
            } catch (Exception ignored) {
            } finally {
                lock2.unlock();
            }
        });
        lockThread.start();
        assertTrue(lock2Held.await(2, TimeUnit.SECONDS));

        Thread victimThread = new Thread(() -> {
            try {
                lockManager.lockAll(List.of(acc1, acc2));
            } catch (ServiceBusyException e) {
                threwExpected.set(true);
            }
        });

        victimThread.start();
        Thread.sleep(100); // Allow victim to acquire acc1 and wait on acc2
        victimThread.interrupt();
        victimThread.join(2000);

        readyToInterrupt.countDown();
        lockThread.join(2000);

        assertTrue(threwExpected.get(), "Expected ServiceBusyException on thread interruption");
        assertFalse(lockManager.getLock(acc1).isLocked(), "Lock 1 must be unlocked after interruption");
    }
}

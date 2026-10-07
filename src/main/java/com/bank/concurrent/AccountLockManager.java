package com.bank.concurrent;

import com.bank.exception.ServiceBusyException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe account-level lock manager preventing concurrent race conditions and deadlocks.
 * <p>
 * Implements Architectural Rule 4:
 * <ul>
 *   <li>Maintains exactly one fair {@link ReentrantLock} per bank account in a {@link ConcurrentHashMap}.</li>
 *   <li>Acquires multi-account locks strictly in ascending account-number order to eliminate circular waits.</li>
 *   <li>Employs {@link ReentrantLock#tryLock(long, TimeUnit)} with timeout to avoid unbounded thread starvation.</li>
 * </ul>
 * </p>
 */
public final class AccountLockManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountLockManager.class);
    private static final ConcurrentHashMap<String, ReentrantLock> LOCK_MAP = new ConcurrentHashMap<>();
    private static final long DEFAULT_TIMEOUT_MS = 5000L;

    private AccountLockManager() {
        // Prevent instantiation of utility class
    }

    /**
     * Retrieves or creates the fair {@link ReentrantLock} assigned to an account number.
     *
     * @param accountNo unique 12-digit account number
     * @return dedicated lock instance
     */
    public static ReentrantLock getLock(String accountNo) {
        if (accountNo == null) {
            throw new IllegalArgumentException("Account number cannot be null for lock acquisition");
        }
        return LOCK_MAP.computeIfAbsent(accountNo, k -> new ReentrantLock(true));
    }

    /**
     * Acquires an account lock with the default 5-second timeout.
     *
     * @param accountNo account number to lock
     * @throws ServiceBusyException if lock acquisition times out or is interrupted
     */
    public static void acquireLock(String accountNo) throws ServiceBusyException {
        acquireLock(accountNo, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Acquires an account lock with an explicit timeout.
     *
     * @param accountNo account number to lock
     * @param timeoutMs timeout in milliseconds
     * @throws ServiceBusyException if lock acquisition times out or is interrupted
     */
    public static void acquireLock(String accountNo, long timeoutMs) throws ServiceBusyException {
        if (accountNo == null) return;
        ReentrantLock lock = getLock(accountNo);
        try {
            boolean acquired = lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                LOGGER.warn("Timed out acquiring lock on account {} after {}ms", accountNo, timeoutMs);
                throw new ServiceBusyException("Account " + accountNo + " is temporarily busy. Please retry.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceBusyException("Interrupted while acquiring lock for account " + accountNo, e);
        }
    }

    /**
     * Releases the account lock if held by the current calling thread.
     *
     * @param accountNo account number to unlock
     */
    public static void releaseLock(String accountNo) {
        if (accountNo == null) return;
        ReentrantLock lock = LOCK_MAP.get(accountNo);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * Acquires locks for two accounts in strictly ascending alphabetical/numerical order,
     * guaranteeing deadlock freedom.
     *
     * @param acc1 first account number
     * @param acc2 second account number
     * @param timeoutMs lock acquisition timeout in milliseconds
     * @throws ServiceBusyException if either lock cannot be acquired within the timeout
     */
    public static void acquireLocks(String acc1, String acc2, long timeoutMs) throws ServiceBusyException {
        if (acc1 == null || acc2 == null) {
            throw new IllegalArgumentException("Account numbers cannot be null for dual-lock acquisition");
        }
        if (acc1.equals(acc2)) {
            acquireLock(acc1, timeoutMs);
            return;
        }

        String first = acc1.compareTo(acc2) < 0 ? acc1 : acc2;
        String second = acc1.compareTo(acc2) < 0 ? acc2 : acc1;

        acquireLock(first, timeoutMs);
        try {
            acquireLock(second, timeoutMs);
        } catch (ServiceBusyException e) {
            releaseLock(first);
            throw e;
        }
    }

    /**
     * Releases locks for two accounts if held by the current thread.
     *
     * @param acc1 first account number
     * @param acc2 second account number
     */
    public static void releaseLocks(String acc1, String acc2) {
        releaseLock(acc1);
        releaseLock(acc2);
    }
}

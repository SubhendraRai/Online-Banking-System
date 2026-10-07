package com.bank.concurrent;

import com.bank.exception.ServiceBusyException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Backward-compatible facade delegating account locking operations to {@link LockManager}.
 * <p>
 * Implements Architectural Rule 4 ensuring uniform lock allocation across the application.
 * </p>
 */
public final class AccountLockManager {

    private static final LockManager DELEGATE = LockManager.getInstance();

    private AccountLockManager() {
        // Prevent instantiation
    }

    /**
     * Retrieves or creates the fair {@link ReentrantLock} assigned to an account number.
     *
     * @param accountNo unique account number
     * @return dedicated lock instance
     */
    public static ReentrantLock getLock(String accountNo) {
        return DELEGATE.getLock(accountNo);
    }

    /**
     * Acquires an account lock with the standard 5-second timeout.
     *
     * @param accountNo account number to lock
     * @throws ServiceBusyException if lock acquisition times out
     */
    public static void acquireLock(String accountNo) throws ServiceBusyException {
        DELEGATE.lock(accountNo);
    }

    /**
     * Releases the account lock if held by the calling thread.
     *
     * @param accountNo account number to unlock
     */
    public static void releaseLock(String accountNo) {
        DELEGATE.unlock(accountNo);
    }

    /**
     * Acquires locks for two accounts in strictly ascending order.
     *
     * @param acc1 first account number
     * @param acc2 second account number
     * @param timeoutMs unused legacy timeout parameter; delegated to standard LockManager
     * @throws ServiceBusyException if lock acquisition times out
     */
    public static void acquireLocks(String acc1, String acc2, long timeoutMs) throws ServiceBusyException {
        if (acc1 == null || acc2 == null) {
            throw new IllegalArgumentException("Account numbers cannot be null for dual-lock acquisition.");
        }
        if (acc1.equals(acc2)) {
            acquireLock(acc1);
            return;
        }
        List<String> sorted = (acc1.compareTo(acc2) < 0) ? List.of(acc1, acc2) : List.of(acc2, acc1);
        DELEGATE.lockAll(sorted);
    }

    /**
     * Releases locks for two accounts in reverse order.
     *
     * @param acc1 first account number
     * @param acc2 second account number
     */
    public static void releaseLocks(String acc1, String acc2) {
        if (acc1 == null || acc2 == null) return;
        List<String> sorted = (acc1.compareTo(acc2) < 0) ? List.of(acc1, acc2) : List.of(acc2, acc1);
        DELEGATE.unlockAll(sorted);
    }
}

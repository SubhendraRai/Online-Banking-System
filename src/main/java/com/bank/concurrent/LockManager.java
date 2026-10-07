package com.bank.concurrent;

import com.bank.exception.ServiceBusyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Account-level concurrency controller managing fair {@link ReentrantLock} instances in a {@link ConcurrentHashMap}.
 * <p>
 * Implements Architectural Rule 4:
 * <ul>
 *   <li>Guarantees deadlock freedom by requiring callers to acquire locks in strictly ascending account-number order.</li>
 *   <li>Enforces a bounded 5-second acquisition timeout using {@link ReentrantLock#tryLock(long, TimeUnit)}.</li>
 *   <li>Deduplicates and normalizes account numbers to prevent accidental multi-locking of identical accounts.</li>
 *   <li>Provides atomic all-or-nothing acquisition: if any subsequent lock cannot be acquired within the timeout,
 *       all locks already acquired during the invocation are immediately released in reverse order before
 *       throwing {@link ServiceBusyException}.</li>
 * </ul>
 * </p>
 */
public class LockManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockManager.class);
    private static final long DEFAULT_TIMEOUT_SECONDS = 5L;

    private static final ConcurrentHashMap<String, ReentrantLock> SHARED_LOCKS = new ConcurrentHashMap<>();
    private static final LockManager INSTANCE = new LockManager(SHARED_LOCKS, DEFAULT_TIMEOUT_SECONDS);

    private final ConcurrentHashMap<String, ReentrantLock> locks;
    private final long timeoutSeconds;

    /**
     * Default constructor backed by the shared JVM-wide lock map and standard 5-second timeout.
     */
    public LockManager() {
        this(SHARED_LOCKS, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Constructor allowing isolated lock map and custom timeout configuration.
     *
     * @param locks concurrent map storing account locks
     * @param timeoutSeconds timeout duration in seconds
     */
    public LockManager(ConcurrentHashMap<String, ReentrantLock> locks, long timeoutSeconds) {
        this.locks = Objects.requireNonNull(locks, "locks map cannot be null");
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
    }

    /**
     * Static accessor for the JVM-wide default instance.
     *
     * @return shared singleton LockManager
     */
    public static LockManager getInstance() {
        return INSTANCE;
    }

    /**
     * Retrieves or creates a fair {@link ReentrantLock} for the given account number.
     *
     * @param accountNo unique 12-digit account number
     * @return dedicated fair ReentrantLock instance
     */
    public ReentrantLock getLock(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be null or blank for lock retrieval.");
        }
        return locks.computeIfAbsent(accountNo.trim(), k -> new ReentrantLock(true));
    }

    /**
     * Acquires locks for all provided account numbers in their given sorted order.
     * <p>
     * Applies a 5-second timeout on each lock. If any lock fails to be acquired,
     * any previously acquired locks in this invocation are released in reverse order
     * and a {@link ServiceBusyException} is thrown.
     * </p>
     *
     * @param sortedAccountNos pre-sorted list of account numbers
     * @throws ServiceBusyException if any lock acquisition times out or is interrupted
     */
    public void lockAll(List<String> sortedAccountNos) throws ServiceBusyException {
        if (sortedAccountNos == null || sortedAccountNos.isEmpty()) {
            return;
        }

        List<String> normalized = sortedAccountNos.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        if (normalized.isEmpty()) {
            return;
        }

        List<ReentrantLock> acquired = new ArrayList<>(normalized.size());
        try {
            for (String accNo : normalized) {
                ReentrantLock lock = getLock(accNo);
                boolean success = lock.tryLock(timeoutSeconds, TimeUnit.SECONDS);
                if (!success) {
                    LOGGER.warn("Timed out acquiring lock on account {} after {}s", accNo, timeoutSeconds);
                    releaseAcquired(acquired);
                    throw new ServiceBusyException("Account " + accNo + " is temporarily busy. Please retry.");
                }
                acquired.add(lock);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            releaseAcquired(acquired);
            LOGGER.warn("Interrupted while acquiring locks: {}", e.getMessage());
            throw new ServiceBusyException("Operation interrupted while acquiring account locks.", e);
        }
    }

    /**
     * Releases locks for all provided account numbers in reverse order if held by the current thread.
     *
     * @param sortedAccountNos list of account numbers to unlock
     */
    public void unlockAll(List<String> sortedAccountNos) {
        if (sortedAccountNos == null || sortedAccountNos.isEmpty()) {
            return;
        }
        List<String> normalized = sortedAccountNos.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        for (int i = normalized.size() - 1; i >= 0; i--) {
            String accNo = normalized.get(i);
            ReentrantLock lock = locks.get(accNo);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * Convenience method locking a single account.
     *
     * @param accountNo account number to lock
     * @throws ServiceBusyException if lock acquisition times out
     */
    public void lock(String accountNo) throws ServiceBusyException {
        lockAll(List.of(accountNo));
    }

    /**
     * Convenience method unlocking a single account.
     *
     * @param accountNo account number to unlock
     */
    public void unlock(String accountNo) {
        unlockAll(List.of(accountNo));
    }

    /**
     * Returns the number of currently tracked accounts with locks.
     *
     * @return active lock count
     */
    public int size() {
        return locks.size();
    }

    private void releaseAcquired(List<ReentrantLock> acquired) {
        for (int i = acquired.size() - 1; i >= 0; i--) {
            ReentrantLock lock = acquired.get(i);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}

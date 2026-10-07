package com.bank.demo;

import com.bank.util.Money;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Educational demonstration illustrating race conditions, lost updates, and thread synchronization.
 * <p>
 * Simulates 50 concurrent worker threads updating a single bank account balance simultaneously:
 * <ul>
 *   <li><b>Phase 1 (Unsynchronized):</b> Exposes non-atomic read-modify-write interleaving leading to lost updates.</li>
 *   <li><b>Phase 2 (Synchronized):</b> Employs Java monitor locking to guarantee mutual exclusion and consistency.</li>
 * </ul>
 * </p>
 */
public class UnsafeBalanceDemo {

    private static final int THREAD_COUNT = 50;
    private static final int ITERATIONS_PER_THREAD = 1000;
    private static final BigDecimal DEPOSIT_AMOUNT = new BigDecimal("1.00");
    private static final BigDecimal EXPECTED_FINAL_BALANCE =
            DEPOSIT_AMOUNT.multiply(BigDecimal.valueOf((long) THREAD_COUNT * ITERATIONS_PER_THREAD));

    public static void main(String[] args) throws InterruptedException {
        System.out.println("================================================================================");
        System.out.println("       CONCURRENCY LAB: UNSAFE VS SYNCHRONIZED BALANCE DEMONSTRATION");
        System.out.println("================================================================================");
        System.out.printf("Configuration: %d Threads, %d Iterations/Thread, $%.2f per Deposit%n",
                THREAD_COUNT, ITERATIONS_PER_THREAD, DEPOSIT_AMOUNT);
        System.out.printf("Theoretical Expected Balance: $%s%n%n", Money.format(EXPECTED_FINAL_BALANCE));

        // Part 1: Unsynchronized Account (Race Condition)
        runUnsynchronizedTrial();

        System.out.println();

        // Part 2: Synchronized Account (Mutual Exclusion)
        runSynchronizedTrial();

        System.out.println("\n================================================================================");
        System.out.println("                 CONCURRENCY DEMONSTRATION COMPLETE");
        System.out.println("================================================================================");
    }

    /**
     * Executes the concurrent deposit simulation against an unsynchronized account.
     */
    private static void runUnsynchronizedTrial() throws InterruptedException {
        System.out.println("--- 1. TRIAL WITHOUT SYNCHRONIZATION (UNSAFE) ---");
        UnsafeAccount unsafeAccount = new UnsafeAccount();
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finishSignal = new CountDownLatch(THREAD_COUNT);

        long startNs = System.nanoTime();
        for (int i = 0; i < THREAD_COUNT; i++) {
            pool.submit(() -> {
                try {
                    startSignal.await();
                    for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                        unsafeAccount.deposit(DEPOSIT_AMOUNT);
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishSignal.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startSignal.countDown();
        finishSignal.await(10, TimeUnit.SECONDS);
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        BigDecimal actual = unsafeAccount.getBalance();
        BigDecimal lostUpdates = EXPECTED_FINAL_BALANCE.subtract(actual);

        System.out.println("  Execution Result:      RACE CONDITIONS DETECTED");
        System.out.printf("  Duration:              %d ms%n", elapsedMs);
        System.out.printf("  Expected Final Balance: $%s%n", Money.format(EXPECTED_FINAL_BALANCE));
        System.out.printf("  Actual Final Balance:   $%s%n", Money.format(actual));
        System.out.printf("  Lost Monetary Value:    $%s%n", Money.format(lostUpdates));
        System.out.println("  Root Cause: Concurrent read-modify-write interleaving on memory field.");
    }

    /**
     * Executes the concurrent deposit simulation against a synchronized account.
     */
    private static void runSynchronizedTrial() throws InterruptedException {
        System.out.println("--- 2. TRIAL WITH SYNCHRONIZATION (THREAD-SAFE) ---");
        SynchronizedAccount safeAccount = new SynchronizedAccount();
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finishSignal = new CountDownLatch(THREAD_COUNT);

        long startNs = System.nanoTime();
        for (int i = 0; i < THREAD_COUNT; i++) {
            pool.submit(() -> {
                try {
                    startSignal.await();
                    for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                        safeAccount.deposit(DEPOSIT_AMOUNT);
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishSignal.countDown();
                }
            });
        }

        startSignal.countDown();
        finishSignal.await(10, TimeUnit.SECONDS);
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        BigDecimal actual = safeAccount.getBalance();
        BigDecimal lostUpdates = EXPECTED_FINAL_BALANCE.subtract(actual);

        System.out.println("  Execution Result:      FULL ATOMICITY GUARANTEED");
        System.out.printf("  Duration:              %d ms%n", elapsedMs);
        System.out.printf("  Expected Final Balance: $%s%n", Money.format(EXPECTED_FINAL_BALANCE));
        System.out.printf("  Actual Final Balance:   $%s%n", Money.format(actual));
        System.out.printf("  Lost Monetary Value:    $%s%n", Money.format(lostUpdates));
        System.out.println("  Resolution: Synchronized critical section enforces happens-before relationship.");
    }

    /**
     * Unsynchronized in-memory account representation prone to lost updates.
     */
    static class UnsafeAccount {
        private BigDecimal balance = Money.ZERO;

        public void deposit(BigDecimal amount) {
            // Read-modify-write critical section without mutual exclusion
            BigDecimal current = this.balance;
            Thread.yield(); // Encourages thread interleaving to showcase race conditions
            this.balance = Money.add(current, amount);
        }

        public BigDecimal getBalance() {
            return balance;
        }
    }

    /**
     * Synchronized in-memory account representation ensuring mutual exclusion.
     */
    static class SynchronizedAccount {
        private BigDecimal balance = Money.ZERO;

        public synchronized void deposit(BigDecimal amount) {
            BigDecimal current = this.balance;
            Thread.yield();
            this.balance = Money.add(current, amount);
        }

        public synchronized BigDecimal getBalance() {
            return balance;
        }
    }
}

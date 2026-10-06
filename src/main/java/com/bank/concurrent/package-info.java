/**
 * Thread-safety and concurrency control utilities.
 * <p>
 * Manages striped or account-level {@link java.util.concurrent.locks.ReentrantLock} instances
 * via {@link java.util.concurrent.ConcurrentHashMap}, ensuring deadlock-free multi-account operations
 * by ordering lock acquisition (e.g., ascending account ID order) with timeouts.
 * </p>
 */
package com.bank.concurrent;

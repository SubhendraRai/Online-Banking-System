package com.bank.service;

import com.bank.model.Transaction;

/**
 * Strategy interface for post-commit fraud inspection and anomaly evaluation.
 * <p>
 * Evaluates completed transactions against suspicious activity rules.
 * Because calls to {@link #checkFraud(Transaction)} occur post-commit inside an isolated
 * protective try-catch block, unexpected checker errors never compromise or roll back
 * successfully committed transfers.
 * </p>
 */
@FunctionalInterface
public interface FraudChecker {

    /**
     * Default no-op implementation.
     */
    FraudChecker NO_OP = txn -> {
        // No-op for baseline Phase 7; ready for Phase 8 fraud detection rules
    };

    /**
     * Evaluates a committed transaction for fraudulent patterns.
     *
     * @param txn successfully committed transaction record
     */
    void checkFraud(Transaction txn);
}

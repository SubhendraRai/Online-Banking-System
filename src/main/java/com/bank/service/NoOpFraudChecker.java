package com.bank.service;

import com.bank.model.Transaction;

/**
 * Baseline no-op implementation of {@link FraudChecker}.
 * <p>
 * Satisfies the dependency contract without altering system state, ready for Phase 8 rule engines.
 * </p>
 */
public class NoOpFraudChecker implements FraudChecker {

    @Override
    public void checkFraud(Transaction txn) {
        // No-op implementation for Phase 7
    }
}

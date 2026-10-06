package com.bank.model;

/**
 * Types of financial ledger transactions.
 */
public enum TxnType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,
    INTEREST,
    LOAN_DISBURSAL,
    FD_OPEN,
    FD_MATURITY
}

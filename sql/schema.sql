-- ============================================================================
-- Online Banking System - Relational Database Schema (DDL)
-- Complies with 3NF, InnoDB Storage Engine, and Strict Referential Integrity
-- Target RDBMS: MySQL 8.x
-- ============================================================================

-- Enforce foreign key constraints and standard SQL mode
SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- 1. Table: users
-- Stores customer profiles, administrative accounts, credentials, and state.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(100) NOT NULL UNIQUE,
    phone           VARCHAR(20)  NOT NULL,
    address         VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    role            ENUM('CUSTOMER', 'ADMIN') NOT NULL,
    status          ENUM('ACTIVE', 'LOCKED', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until    DATETIME DEFAULT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. Table: accounts
-- Stores bank accounts belonging to users. Supports savings and current accounts.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
    account_no      VARCHAR(12) PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    account_type    ENUM('SAVINGS', 'CURRENT') NOT NULL,
    balance         DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    overdraft_limit DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    status          ENUM('ACTIVE', 'FROZEN', 'CLOSED') NOT NULL DEFAULT 'ACTIVE',
    opened_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_user 
        FOREIGN KEY (user_id) REFERENCES users (user_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_accounts_balance 
        CHECK (balance >= -overdraft_limit),
    CONSTRAINT chk_accounts_overdraft 
        CHECK (overdraft_limit >= 0.00),
    INDEX idx_accounts_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. Table: transactions
-- Immutable double-entry financial transaction ledger.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    txn_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_account    VARCHAR(12) DEFAULT NULL,
    to_account      VARCHAR(12) DEFAULT NULL,
    txn_type        ENUM('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'INTEREST', 'LOAN_DISBURSAL', 'FD_OPEN', 'FD_MATURITY') NOT NULL,
    amount          DECIMAL(15, 2) NOT NULL,
    status          ENUM('SUCCESS', 'FAILED') NOT NULL,
    remarks         VARCHAR(255) DEFAULT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_txn_from 
        FOREIGN KEY (from_account) REFERENCES accounts (account_no) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_txn_to 
        FOREIGN KEY (to_account) REFERENCES accounts (account_no) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_transactions_amount 
        CHECK (amount > 0.00),
    INDEX idx_txn_from_created (from_account, created_at),
    INDEX idx_txn_to_created (to_account, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. Table: loans
-- Retail loan applications, calculated EMI, and adjudication records.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS loans (
    loan_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    account_no      VARCHAR(12) NOT NULL,
    principal       DECIMAL(15, 2) NOT NULL,
    annual_rate     DECIMAL(5, 2) NOT NULL,
    tenure_months   INT NOT NULL,
    emi             DECIMAL(15, 2) NOT NULL,
    status          ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    applied_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at      TIMESTAMP NULL DEFAULT NULL,
    decided_by      BIGINT DEFAULT NULL,
    CONSTRAINT fk_loans_user 
        FOREIGN KEY (user_id) REFERENCES users (user_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_loans_account 
        FOREIGN KEY (account_no) REFERENCES accounts (account_no) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_loans_decided_by 
        FOREIGN KEY (decided_by) REFERENCES users (user_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_loans_principal 
        CHECK (principal > 0.00),
    CONSTRAINT chk_loans_rate 
        CHECK (annual_rate >= 0.00),
    CONSTRAINT chk_loans_tenure 
        CHECK (tenure_months > 0),
    CONSTRAINT chk_loans_emi 
        CHECK (emi > 0.00),
    INDEX idx_loans_user (user_id),
    INDEX idx_loans_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. Table: fixed_deposits
-- Fixed deposit investment accounts and maturity payout schedules.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fixed_deposits (
    fd_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_no      VARCHAR(12) NOT NULL,
    principal       DECIMAL(15, 2) NOT NULL,
    annual_rate     DECIMAL(5, 2) NOT NULL,
    tenure_months   INT NOT NULL,
    start_date      DATE NOT NULL,
    maturity_date   DATE NOT NULL,
    maturity_amount DECIMAL(15, 2) NOT NULL,
    status          ENUM('ACTIVE', 'MATURED', 'PRECLOSED') NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_fd_account 
        FOREIGN KEY (account_no) REFERENCES accounts (account_no) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_fd_principal 
        CHECK (principal > 0.00),
    CONSTRAINT chk_fd_rate 
        CHECK (annual_rate >= 0.00),
    CONSTRAINT chk_fd_tenure 
        CHECK (tenure_months > 0),
    CONSTRAINT chk_fd_dates 
        CHECK (maturity_date >= start_date),
    CONSTRAINT chk_fd_maturity_amount 
        CHECK (maturity_amount >= principal),
    INDEX idx_fd_account (account_no),
    INDEX idx_fd_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 6. Table: fraud_alerts
-- Post-commit asynchronous fraud rule evaluation alerts and review statuses.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fraud_alerts (
    alert_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    txn_id          BIGINT NOT NULL,
    rule_code       VARCHAR(50) NOT NULL,
    reason          VARCHAR(255) NOT NULL,
    status          ENUM('OPEN', 'REVIEWED', 'DISMISSED') NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by     BIGINT DEFAULT NULL,
    CONSTRAINT fk_fraud_txn 
        FOREIGN KEY (txn_id) REFERENCES transactions (txn_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_fraud_reviewed_by 
        FOREIGN KEY (reviewed_by) REFERENCES users (user_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_fraud_status (status),
    INDEX idx_fraud_txn (txn_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 7. Table: system_settings
-- System-wide key-value configuration parameters dynamically cached in memory.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key     VARCHAR(100) PRIMARY KEY,
    setting_value   VARCHAR(255) NOT NULL,
    description     VARCHAR(255) NOT NULL,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_settings_updated_by 
        FOREIGN KEY (updated_by) REFERENCES users (user_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 8. Table: audit_log
-- Audit log recording administrative modifications and security critical actions.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
    log_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT DEFAULT NULL,
    action          VARCHAR(100) NOT NULL,
    details         TEXT DEFAULT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user 
        FOREIGN KEY (user_id) REFERENCES users (user_id) 
        ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_audit_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

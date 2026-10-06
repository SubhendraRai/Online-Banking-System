-- ============================================================================
-- Online Banking System - Full Database Reset Script
-- Drops all tables, recreates schema DDL, and repopulates seed fixtures.
-- Single-Command Reset Execution:
--   mysql -u bank_app -p bankdb < sql/reset.sql
-- For Test Database Reset:
--   mysql -u bank_app -p bankdb_test < sql/reset.sql
-- ============================================================================

-- ----------------------------------------------------------------------------
-- STEP 1: Drop All Existing Tables (Foreign Key Checks Disabled)
-- ----------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS fraud_alerts;
DROP TABLE IF EXISTS fixed_deposits;
DROP TABLE IF EXISTS loans;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS system_settings;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- STEP 2: Recreate Schema Tables (DDL)
-- ----------------------------------------------------------------------------

-- 1. Table: users
CREATE TABLE users (
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

-- 2. Table: accounts
CREATE TABLE accounts (
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

-- 3. Table: transactions
CREATE TABLE transactions (
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

-- 4. Table: loans
CREATE TABLE loans (
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

-- 5. Table: fixed_deposits
CREATE TABLE fixed_deposits (
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

-- 6. Table: fraud_alerts
CREATE TABLE fraud_alerts (
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

-- 7. Table: system_settings
CREATE TABLE system_settings (
    setting_key     VARCHAR(100) PRIMARY KEY,
    setting_value   VARCHAR(255) NOT NULL,
    description     VARCHAR(255) NOT NULL,
    updated_by      BIGINT DEFAULT NULL,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_settings_updated_by 
        FOREIGN KEY (updated_by) REFERENCES users (user_id) 
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Table: audit_log
CREATE TABLE audit_log (
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

-- ----------------------------------------------------------------------------
-- STEP 3: Populate Seed Data (DML)
-- ----------------------------------------------------------------------------

-- System Settings (12 rows)
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('transfer.per_txn_limit',      '50000.00',  'Maximum allowed value for a single fund transfer in INR'),
('transfer.daily_limit',        '100000.00', 'Maximum cumulative debit transfer amount per account per day in INR'),
('savings.min_balance',         '500.00',    'Mandatory minimum balance for Savings Accounts in INR'),
('savings.interest_rate',       '3.50',      'Annual interest percentage credited to Savings Accounts (% p.a.)'),
('current.max_overdraft',       '100000.00', 'Maximum ceiling for configurable Current Account overdraft limit in INR'),
('loan.interest_rate',          '8.50',      'Standard annual interest rate applied to retail personal loans (% p.a.)'),
('fd.interest_rate',            '6.50',      'Annualized return rate applied to Fixed Deposits (% p.a.)'),
('fraud.high_amount_threshold', '100000.00', 'Transfer threshold triggering high-value fraud review in INR'),
('fraud.velocity_count',        '5',         'Maximum transfers allowed in window before triggering velocity alert'),
('fraud.velocity_minutes',      '10',        'Time window (minutes) evaluated for transfer velocity checks'),
('session.timeout_minutes',     '15',        'Inactive HTTP session timeout period in minutes'),
('maintenance.mode',            'false',     'When true, customer login is suspended with a maintenance notice');

-- Admin User (Password: AdminPass123!)
INSERT INTO users (user_id, full_name, email, phone, address, password_hash, role, status, failed_attempts) VALUES
(1, 'System Administrator', 'admin@bank.com', '9876543210', 'Head Office, 100 Financial District, Mumbai, MH',
 '$2a$10$wuPl6h9I4h9DNNMzaMriC.P5h9BKz6LeEXnnqvOKt0dJ4.4PvJN/C',
 'ADMIN', 'ACTIVE', 0);

-- Customer Users (Password: CustomerPass123!)
INSERT INTO users (user_id, full_name, email, phone, address, password_hash, role, status, failed_attempts) VALUES
(2, 'Rahul Sharma', 'rahul.sharma@example.com', '9811122233', '42 Connaught Place, New Delhi, DL',
 '$2a$10$QDHFuxfmTLx8mAlofdakfuKqD.MoCwDQ9bY0XJIASvWDq7zIzbY66',
 'CUSTOMER', 'ACTIVE', 0),

(3, 'Priya Patel', 'priya.patel@example.com', '9822233344', '15 Navrangpura, Ahmedabad, GJ',
 '$2a$10$e8nwObhojGMAbrCXrtdI7Ots9uAt80jiOerhIBauV5lFaZZpAUb4a',
 'CUSTOMER', 'ACTIVE', 0),

(4, 'Amit Verma', 'amit.verma@example.com', '9833344455', '88 Indiranagar 100 Feet Rd, Bengaluru, KA',
 '$2a$10$vBQFSAr9J6dIjsGCJse2he2PQdm7H8SAC34LK1Z8UGV7dJwsgQiVO',
 'CUSTOMER', 'ACTIVE', 0);

-- Accounts with Starting Balances
INSERT INTO accounts (account_no, user_id, account_type, balance, overdraft_limit, status) VALUES
('100000000001', 2, 'SAVINGS', 25000.00,     0.00, 'ACTIVE'),
('100000000002', 2, 'CURRENT', 50000.00, 10000.00, 'ACTIVE'),
('100000000003', 3, 'SAVINGS', 75000.00,     0.00, 'ACTIVE'),
('100000000004', 4, 'SAVINGS', 10000.00,     0.00, 'ACTIVE');

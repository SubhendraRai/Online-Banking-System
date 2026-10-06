-- ============================================================================
-- Online Banking System - Database Initialization and User Provisioning
-- Target RDBMS: MySQL 8.x
-- ============================================================================

-- 1. Create Application Database
CREATE DATABASE IF NOT EXISTS bankdb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Create Test Database (for integration testing)
CREATE DATABASE IF NOT EXISTS bankdb_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 3. Create Restricted Application Service User
-- Note: Replace 'BankAppSecret123!' with your secure production password
CREATE USER IF NOT EXISTS 'bank_app'@'localhost' IDENTIFIED BY 'BankAppSecret123!';
CREATE USER IF NOT EXISTS 'bank_app'@'127.0.0.1' IDENTIFIED BY 'BankAppSecret123!';

-- 4. Grant Least-Privilege Permissions strictly on bankdb and bankdb_test
-- The user has DDL/DML privileges on these two databases only, and NO administrative rights across MySQL.
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES, EXECUTE 
    ON bankdb.* TO 'bank_app'@'localhost';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES, EXECUTE 
    ON bankdb_test.* TO 'bank_app'@'localhost';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES, EXECUTE 
    ON bankdb.* TO 'bank_app'@'127.0.0.1';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES, EXECUTE 
    ON bankdb_test.* TO 'bank_app'@'127.0.0.1';

-- 5. Apply Privilege Changes
FLUSH PRIVILEGES;

-- ============================================================================
-- Verification Queries (Run as root or admin to verify user permissions)
-- SHOW GRANTS FOR 'bank_app'@'localhost';
-- ============================================================================

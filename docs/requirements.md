# Software Requirements Specification (SRS)
## Online Banking System

* **Document Version:** 1.0.0
* **Target Audience:** Engineering Faculty, Technical Evaluators, Pair Programmers
* **Standard:** IEEE 830-compliant format tailored for Enterprise Java Web Architectures

---

## 1. Introduction & Purpose

### 1.1 Purpose
This document specifies the software requirements, business domain rules, system parameters, and architectural constraints for the **Online Banking System**. The application is engineered as a robust, two-tier web application using Java 17 LTS, Jakarta Servlets 6.0, JavaServer Pages (JSP), Java Database Connectivity (JDBC), and MySQL 8.x, hosted on Apache Tomcat 10.1.

The primary objective is to demonstrate enterprise-grade core Java paradigms—object-oriented domain modeling, strict monetary precision, multi-threaded concurrency safety with explicit deadlock prevention, ACID transaction orchestration, defense-in-depth web security, and clean separation of architectural concerns.

### 1.2 Scope
The system delivers an online banking platform with two distinct administrative and user-facing roles:
1. **Customer Portal:** Secure account management, portfolio overview, real-time fund transfers, transaction history search/filtering, banking services (loan applications and fixed deposit placements), and profile/credential management.
2. **Admin Console:** User account lifecycle management (CRUD with validation and safety constraints), live transaction surveillance and reporting, dynamic key-value system settings modification, operational metrics analysis, and post-transaction fraud alert review.

### 1.3 Definitions, Acronyms, and Abbreviations
* **ACID:** Atomicity, Consistency, Isolation, Durability
* **BCrypt:** Adaptive cryptographic hash function based on the Blowfish cipher
* **DAO:** Data Access Object
* **EMI:** Equated Monthly Installment
* **FD:** Fixed Deposit
* **PRG:** Post/Redirect/Get design pattern
* **SRS:** Software Requirements Specification
* **JSTL:** Jakarta Standard Tag Library (v3.0)

---

## 2. User Roles & Personas

| Role Name | Identifier | Description | Access Permissions |
| :--- | :--- | :--- | :--- |
| **Customer** | `CUSTOMER` | Authenticated retail bank customer owning one or more accounts. | View balances, execute transfers, apply for loans, open fixed deposits, view transaction history, update personal profile. |
| **Administrator** | `ADMIN` | Bank operations officer responsible for system oversight and configuration. | Manage user lifecycles, monitor transactions, update system thresholds, adjudicate loan applications, review fraud alerts, view operational metrics. |

---

## 3. Functional Requirements

### 3.1 Authentication & Security Requirements (Common)

* **FR-AUTH-01 (Authentication Mechanism):** The system shall authenticate users via email and password. Passwords shall be verified against salted BCrypt hashes (`at.favre.lib:bcrypt`).
* **FR-AUTH-02 (Session Regeneration):** Upon successful authentication, the system shall invalidate the previous session and issue a new session identifier (`request.changeSessionId()`) to prevent session fixation attacks.
* **FR-AUTH-03 (Brute-Force Defense):** The system shall record consecutive failed login attempts. Upon 5 consecutive failed attempts, the account status shall be set to `LOCKED` with `locked_until` timestamp set to 15 minutes in the future. Subsequent login attempts within this window shall be rejected.
* **FR-AUTH-04 (Role-Based Access Control):** Centralized HTTP filters (`jakarta.servlet.Filter`) shall restrict access to `/customer/*` exclusively to `CUSTOMER` roles and `/admin/*` exclusively to `ADMIN` roles. Unauthenticated requests shall redirect to `/auth/login`.
* **FR-AUTH-05 (Session Expiration):** Inactive sessions shall expire automatically after 15 minutes of inactivity (`session.timeout_minutes` setting).
* **FR-AUTH-06 (Post/Redirect/Get):** All form submissions modifying server state (POST requests) shall implement the Post/Redirect/Get pattern and communicate status via temporary flash session attributes.

---

### 3.2 Customer Portal Requirements

* **FR-CUST-01 (Portfolio Overview):** The customer dashboard shall display the customer's active accounts (Savings and Current), showing current balances, available overdraft limits, account numbers (masked where applicable), and the 5 most recent transactions.
* **FR-CUST-02 (Fund Transfer Initiation):** A customer shall be able to initiate a fund transfer by specifying:
  1. Source account (must belong to the customer).
  2. Destination account (12-digit number).
  3. Transfer amount (positive number, up to 2 decimal places).
  4. Remarks / transaction description (optional, max 255 chars).
* **FR-CUST-03 (Transfer Confirmation Screen):** The system shall present a pre-execution confirmation screen displaying the source account, beneficiary account, transfer amount, applicable limits, and description prior to final commitment.
* **FR-CUST-04 (Transfer Concurrency & Atomicity):** Transfers shall execute under dual-account locks acquired in ascending numerical order of account numbers, followed by database pessimistic locking (`SELECT ... FOR UPDATE`). Debit and credit operations shall be executed inside a single JDBC transaction boundary (`setAutoCommit(false)`).
* **FR-CUST-05 (Transfer Validation):** The system shall enforce that:
  * Source and destination account numbers are distinct.
  * Both accounts hold `ACTIVE` status (`FROZEN` or `CLOSED` accounts are rejected).
  * Transfer amount does not exceed `transfer.per_txn_limit`.
  * Total transfers from the account within the current calendar day do not exceed `transfer.daily_limit`.
  * Source account balance plus any available overdraft limit is sufficient to cover the transfer amount.
* **FR-CUST-06 (Transaction History & Filtering):** The customer shall be able to query historical transactions for their accounts with multi-criteria filtering:
  * Date range (start date to end date).
  * Transaction type (`TRANSFER`, `DEPOSIT`, `WITHDRAWAL`, `INTEREST`, `LOAN_DISBURSAL`, `FD_OPEN`, `FD_MATURITY`).
  * Status (`SUCCESS`, `FAILED`).
  * Free-text search on remarks or counterparty account number.
  * Server-side pagination (default 10 records per page).
* **FR-CUST-07 (Loan Application):** A customer shall be able to submit a loan application specifying desired principal and tenure in months. The system shall dynamically compute and display the projected Equated Monthly Installment (EMI) using the system-configured interest rate (`loan.interest_rate`). Initial status shall be `PENDING`.
* **FR-CUST-08 (Fixed Deposit Creation):** A customer shall be able to open a Fixed Deposit by selecting a source account, principal amount, and tenure in months. Upon submission, the principal shall be debited immediately, maturity amount calculated from `fd.interest_rate`, and status set to `ACTIVE`.
* **FR-CUST-09 (Profile Management):** A customer shall be able to update their full name, phone number, and mailing address.
* **FR-CUST-10 (Password Change):** A customer shall be able to update their password by submitting their current password along with a new password meeting complexity rules (minimum 8 characters, at least 1 letter, at least 1 digit).

---

### 3.3 Admin Console Requirements

* **FR-ADMIN-01 (User Lifecycle Management - Listing):** The admin console shall provide a searchable, paginated table of all registered users displaying user ID, full name, email, role, account status, and creation date, with quick actions for Edit and Delete.
* **FR-ADMIN-02 (User Creation):** The administrator shall be able to register a new user by specifying full name, email, temporary password, role (`CUSTOMER` or `ADMIN`), phone number, and address. Upon creation, an informative flash message shall confirm the operation.
* **FR-ADMIN-03 (User Update):** The administrator shall be able to modify user attributes (full name, phone, address, role, status) with a mandatory confirmation prompt.
* **FR-ADMIN-04 (Safe User Soft-Deletion):** The administrator shall be able to soft-delete a user (setting status to `DELETED`). 
  * **Zero-Balance Invariant:** Deletion shall be strictly blocked if any linked account has a non-zero balance (`balance != 0.00`).
  * **Confirmation Requirement:** The interface shall prompt the admin with a modal dialog requiring explicit confirmation before executing the soft-delete.
* **FR-ADMIN-05 (Transaction Surveillance):** The administrator shall have real-time visibility into all transactions across the bank, filterable by date, account number, status (`SUCCESS` vs `FAILED`), amount threshold, and transaction type.
* **FR-ADMIN-06 (Operational Metrics & Visualizations):** The admin console shall display aggregate operational metrics:
  * Total system deposits and active account counts.
  * 24-hour transaction volume (count) and total turnover value (₹).
  * System health, failed transaction ratios, and active user tallies.
* **FR-ADMIN-07 (System Settings Configuration):** The administrator shall be able to inspect and modify all operational parameters stored in `system_settings`. Modifications shall require a confirmation prompt, and changes shall immediately update the in-memory `SettingsService` cache without requiring a server restart.
* **FR-ADMIN-08 (Loan Adjudication):** The administrator shall be able to view all `PENDING` loan applications and either `APPROVE` or `REJECT` them. Approving a loan shall atomically disburse the principal into the customer's linked account and generate a `LOAN_DISBURSAL` transaction record.
* **FR-ADMIN-09 (Fraud Alert Oversight):** The administrator shall be able to view the queue of transactions flagged by the asynchronous fraud rules (`OPEN`), inspect the trigger reason, and transition alert statuses to `REVIEWED` or `DISMISSED`.
* **FR-ADMIN-10 (Audit Trail Log):** The system shall log all administrative actions and critical security events in the `audit_log` table for regulatory and evaluation compliance.

---

## 4. Business Domain Rules (Plan Section 4.3)

* **BR-01 (Monetary Precision):** All currency amounts shall be represented as `java.math.BigDecimal` in Java with a scale of 2 and `RoundingMode.HALF_UP`. In the database, monetary attributes shall be defined as `DECIMAL(15, 2)`. Primitive `float` and `double` are strictly prohibited. The base currency is Indian Rupee (INR / ₹).
* **BR-02 (Account Number Invariant):** Account numbers must be exactly 12 numeric digits, uniquely generated by the system, and immutable.
* **BR-03 (Savings Account Minimum Balance & Interest):** 
  * Savings accounts must maintain a minimum balance configured by `savings.min_balance` (default ₹500.00).
  * Savings accounts accrue annual interest configured by `savings.interest_rate` (default 3.50% p.a.), calculated and credited periodically.
* **BR-04 (Current Account Overdraft):**
  * Current accounts do not earn interest.
  * Current accounts may support an overdraft limit (default ₹0.00, admin-adjustable up to `current.max_overdraft`).
  * The account balance combined with overdraft must satisfy the invariant: `balance >= -overdraft_limit`.
* **BR-05 (Transaction Limits):**
  * Maximum amount per single transfer: governed by `transfer.per_txn_limit` (default ₹50,000.00).
  * Cumulative daily transfer limit per account: governed by `transfer.daily_limit` (default ₹1,00,000.00).
* **BR-06 (Amount Validation):** Any transfer, deposit, or withdrawal amount must be strictly greater than zero (`amount > 0.00`) and contain at most 2 decimal places.
* **BR-07 (Transfer Integrity & Atomicity):**
  * Source account and destination account must not be identical (`from_account != to_account`).
  * Both source and destination accounts must hold `ACTIVE` status. Transactions involving `FROZEN` or `CLOSED` accounts must be rejected.
  * A transfer must be strictly atomic: debiting the source, crediting the destination, and recording the transaction log occur in one transactional unit.
  * Unsuccessful transfers must be persisted in `transactions` with status `FAILED` and an explanatory `remarks` entry.
* **BR-08 (Password Complexity):** Passwords must be at least 8 characters long, containing at least one alphabetic letter and at least one numeric digit. Stored as salted BCrypt hashes.
* **BR-09 (Email Uniqueness):** User email addresses must be globally unique and treated as case-insensitive.
* **BR-10 (Account Lockout Policy):** Upon 5 consecutive failed login attempts, the user's status is set to `LOCKED` with `locked_until = NOW() + 15 minutes`.
* **BR-11 (User Soft Deletion & Balance Clearance):** User deletion is a soft delete (`status = 'DELETED'`). Deletion is rejected if any linked account has a non-zero balance (`balance != 0.00`). Historical transactions, loans, and audit logs remain intact. The admin must receive an explicit confirmation dialog before execution.
* **BR-12 (Loan EMI Calculation):**
  * Customer submits principal ($P$) and tenure in months ($n$).
  * Annual interest rate ($R$) is read from `loan.interest_rate` setting.
  * Monthly rate $r = \frac{R}{12 \times 100}$.
  * The monthly EMI is calculated as:
    $$\text{EMI} = P \times r \times \frac{(1+r)^n}{(1+r)^n - 1}$$
  * Status starts as `PENDING`. Admin approval atomically credits $P$ to the designated account.
* **BR-13 (Fixed Deposit Maturity):**
  * Customer selects principal ($P$) and tenure in months ($n$).
  * Rate ($R$) is obtained from `fd.interest_rate`.
  * Principal is immediately debited from the funding account.
  * Maturity amount $M = P \times \left(1 + \frac{R \times n}{12 \times 100}\right)$ (simple annualized tenure accrual) is calculated, and maturity date is fixed.
* **BR-14 (Fraud Detection Rules - Flag Only, Never Block):**
  * Post-commit fraud evaluation evaluates every successful transfer asynchronously without blocking customer execution:
    1. **Rule 1 (High Amount):** Any transfer where `amount >= fraud.high_amount_threshold` (default ₹1,00,000.00).
    2. **Rule 2 (Velocity):** More than `fraud.velocity_count` (default 5) transfers originating from the same account within `fraud.velocity_minutes` (default 10 minutes).
    3. **Rule 3 (First-Time Payee Large Transfer):** Any transfer to a counterparty account with no prior history where `amount >= 25000.00`.
  * Triggered rules generate a record in `fraud_alerts` with `OPEN` status.

---

## 5. System Settings Specification (Plan Section 4.4)

Settings are maintained in the database table `system_settings` and cached in memory using a thread-safe `ConcurrentHashMap` via `SettingsService`. Any administrative update persists to the database and invalidates/refreshes the local cache atomically.

| Setting Key | Data Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `transfer.per_txn_limit` | Decimal | `50000.00` | Maximum allowed value for a single fund transfer (₹). |
| `transfer.daily_limit` | Decimal | `100000.00` | Maximum cumulative debit transfer amount per account per day (₹). |
| `savings.min_balance` | Decimal | `500.00` | Mandatory minimum balance for Savings Accounts (₹). |
| `savings.interest_rate` | Decimal | `3.50` | Annual interest percentage credited to Savings Accounts (% p.a.). |
| `current.max_overdraft` | Decimal | `100000.00` | Maximum ceiling for configurable Current Account overdraft limit (₹). |
| `loan.interest_rate` | Decimal | `8.50` | Standard annual interest rate applied to retail personal loans (% p.a.). |
| `fd.interest_rate` | Decimal | `6.50` | Annualized return rate applied to Fixed Deposits (% p.a.). |
| `fraud.high_amount_threshold` | Decimal | `100000.00` | Transfer threshold triggering high-value fraud review (₹). |
| `fraud.velocity_count` | Integer | `5` | Maximum transfers allowed in window before triggering velocity alert. |
| `fraud.velocity_minutes` | Integer | `10` | Time window (minutes) evaluated for transfer velocity checks. |
| `session.timeout_minutes` | Integer | `15` | Inactive HTTP session timeout period. |
| `maintenance.mode` | Boolean | `false` | When true, customer login is suspended with a maintenance notice. |

---

## 6. Database Relational Model (Plan Section 6)

The schema complies with Third Normal Form (3NF), utilizes the InnoDB storage engine for foreign key referential integrity (`ON DELETE RESTRICT`), and enforces database-level constraints.

### 6.1 Entities Summary

1. **`users`:** Holds user profiles, credentials, role classifications, lockout counters, and lifecycle statuses.
2. **`accounts`:** Holds individual bank accounts (Savings/Current), current balances, overdraft permissions, and status flags. Enforces `CHECK (balance >= -overdraft_limit)`.
3. **`transactions`:** Immutable ledger recording every monetary movement (from_account, to_account, type, amount, status, timestamp). Enforces `CHECK (amount > 0)`.
4. **`loans`:** Loan applications, sanctioned principals, agreed rates, tenure, EMI schedules, and adjudication state.
5. **`fixed_deposits`:** Term investment contracts, principal debits, maturity amounts, and maturity dates.
6. **`fraud_alerts`:** Security surveillance log documenting flagged transactions, violated rule codes, and review statuses.
7. **`system_settings`:** Dynamic application parameters and audit timestamps.
8. **`audit_log`:** Administrative action log and security audit records feeding operational activity metrics.

### 6.2 Key Indexes
* `transactions(from_account, created_at)`: Optimizes customer account statement extraction and daily limit checks.
* `transactions(to_account, created_at)`: Optimizes credit statement queries.
* `users(email)`: Unique index ensuring instantaneous authentication lookups.
* `accounts(user_id)`: Accelerates customer portfolio aggregation.

---

## 7. Non-Functional Requirements (NFRs)

### 7.1 Concurrency & Deadlock Prevention
* **NFR-CONC-01 (In-Memory Locks):** The system shall manage fine-grained `ReentrantLock` instances per account stored in a `ConcurrentHashMap`. Multi-account transactions must acquire locks in strictly ascending alphanumeric order of account numbers using `tryLock(timeout)` with timeout handling to prevent deadlock.
* **NFR-CONC-02 (Pessimistic Database Locks):** Within the JDBC transaction, row-level pessimistic locks (`SELECT ... FOR UPDATE`) shall be acquired in the identical ascending account number order.

### 7.2 Security & Data Protection
* **NFR-SEC-01 (Password Hashing):** All passwords must be hashed using BCrypt with minimum cost factor 10. No plaintext passwords shall ever be persisted or logged.
* **NFR-SEC-02 (Injection Defense):** All database interactions must use parameterized `PreparedStatement` queries. No dynamic SQL concatenation is permitted.
* **NFR-SEC-03 (XSS Mitigation):** All JSP output rendering user or database data must be filtered through JSTL `<c:out value="${...}"/>` tags. Direct scriptlets (`<% ... %>`) are strictly prohibited.
* **NFR-SEC-04 (Session Fixation):** The HTTP session identifier must be rotated upon login using `request.changeSessionId()`.

### 7.3 Transaction Management & Reliability
* **NFR-REL-01 (Transaction Demarcation):** Service layer methods shall manage transaction boundaries explicitly via JDBC `connection.setAutoCommit(false)`, `commit()`, and `rollback()`.
* **NFR-REL-02 (Failure Isolation):** Any runtime or checked exception encountered during fund transfer must trigger an immediate rollback. Failed transfer logs must be recorded in an independent transaction.

### 7.4 Maintainability & Code Quality
* **NFR-MAINT-01 (Strict Layering):** Flow is strictly `Servlet` &rarr; `Service` &rarr; `DAO` &rarr; `MySQL`. Servlets must never import `java.sql.*` or execute SQL.
* **NFR-MAINT-02 (Method Size & Cleanliness):** Methods shall not exceed approximately 40 lines. All public service methods and classes must include clear Javadoc comments.

---

## 8. Assumptions & Dependencies

1. **Deployment Environment:** Runs on Apache Tomcat 10.1.x with Java 17 LTS runtime.
2. **Database System:** MySQL Community Server 8.0 or 8.4 LTS configured with InnoDB and UTF-8 (`utf8mb4`).
3. **Single-Node Execution:** LockManager and SettingsService caching operate inside a single JVM instance for this academic implementation.
4. **Time Synchronization:** System timestamps rely on the server's synchronized clock.

---

## 9. Out of Scope

1. Multi-currency foreign exchange (all transactions are denominated in INR ₹).
2. Integration with external clearing networks (e.g., NEFT/RTGS/IMPS/SWIFT gateway simulators).
3. Physical debit/credit card issuance and ATM PIN verification.
4. Two-Factor Authentication via SMS/TOTP hardware tokens (deferred to post-evaluation extensions).

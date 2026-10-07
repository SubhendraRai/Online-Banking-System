# Architecture Decision Records (ADRs)

This document records the architectural and engineering decisions made during the design and development of the **Apex Online Banking System**.

---

## ADR-001: Deadlock Avoidance and Hybrid Concurrency Control (Java ReentrantLock + MySQL FOR UPDATE)

### Context & Problem Statement
In peer-to-peer retail banking transfers, two distinct accounts ($A$ and $B$) must be mutated simultaneously within an atomic unit of work. Under high concurrent workloads, if Thread 1 transfers funds from Account $A$ to Account $B$ while Thread 2 concurrently transfers from Account $B$ to Account $A$, a classic circular-wait deadlock can occur both in JVM application memory and at the database engine level (Dijkstra's Coffman conditions). 

Furthermore, relying solely on database row locks (`SELECT ... FOR UPDATE`) risks connection pool exhaustion and database thread starvation when hundreds of requests queue for the same hotspot account. Conversely, relying solely on JVM-level in-memory locks fails to protect data consistency across multi-instance or clustered web servers.

### Decision
We implement a **Two-Tier Hybrid Locking Architecture with Strict Resource Ordering**:

1. **Global Resource Ordering (Deadlock Freedom):**
   Before acquiring any locks, account numbers are strictly sorted in ascending numerical/lexicographical order ($\min(\text{acc}_1, \text{acc}_2) < \max(\text{acc}_1, \text{acc}_2)$). Both the in-memory locks and database row locks are acquired strictly according to this sorted sequence. By imposing a global total ordering on resource acquisition, Dijkstra's circular wait condition is mathematically eliminated, guaranteeing that deadlocks cannot occur.

2. **In-Memory Bounded Locks (`com.bank.concurrent.LockManager`):**
   The application maintains fair `ReentrantLock` instances inside a `ConcurrentHashMap`. Transfers must acquire in-memory locks using `tryLock(5, TimeUnit.SECONDS)`. If a lock cannot be acquired within 5 seconds, all locks acquired during that invocation are released in reverse order, and a `ServiceBusyException` is thrown. This fail-fast barrier protects the JDBC connection pool: threads queue or back off in JVM memory *before* consuming a valuable physical database connection.

3. **Database Pessimistic Row Locking (`SELECT ... FOR UPDATE`):**
   Inside the transactional boundary (`setAutoCommit(false)`), the application queries both account rows using `SELECT ... FOR UPDATE` in the exact same ascending order. This guarantees ACID isolation and prevents race conditions, non-repeatable reads, and lost updates across multiple JVM instances or direct database access paths where local memory locks cannot reach.

### Consequences
- **Positive:** Complete elimination of circular-wait deadlocks in both application and database tiers.
- **Positive:** Protection of JDBC connection pool under peak load via JVM-level bounded queueing (`tryLock` timeout).
- **Positive:** Full ACID multi-node safety and protection against lost updates.
- **Trade-off:** Minimal latency overhead for acquiring in-memory locks prior to opening database connections.

---

## ADR-002: Monetary Representation & Strict Rounding using `BigDecimal`

### Context & Problem Statement
Binary floating-point types (`float` and `double`) represent numbers in IEEE 754 base-2 format. They cannot accurately represent decimal fractions such as `0.10` or `0.01` (e.g., `0.1 + 0.2 = 0.30000000000000004`). In financial accounting, accumulating floating-point rounding errors leads to ledger imbalances, fractional cent discrepancies, and compliance violations.

### Decision
We standardize on `java.math.BigDecimal` across all application tiers, enforced by `com.bank.util.Money`:
1. **Scale and Rounding:** All monetary values maintain a fixed scale of `2` with `RoundingMode.HALF_UP` rounding.
2. **Database Mapping:** MySQL database tables represent money using `DECIMAL(15, 2)`.
3. **Zero Floating-Point Policy:** Primitive `float` and `double` types are strictly prohibited for financial fields.
4. **Immutability & Math Helpers:** Monetary arithmetic operations (add, subtract, multiply, divide, compare) are performed via static helpers in `Money` to eliminate null pointer bugs and guarantee normalization.

### Consequences
- **Positive:** Exact penny-accurate arithmetic matching statutory banking standards.
- **Positive:** Perfect reconciliation between ledger transaction amounts and stored account balances.
- **Trade-off:** Slightly higher memory allocation and CPU overhead compared to primitive float/double calculations.

---

## ADR-003: Strict Layered Architecture & Service-Tier Transaction Ownership

### Context & Problem Statement
In enterprise web applications, business rules and database transaction lifecycles can easily bleed into UI controllers (Servlets) or persistence mappers (DAOs). If Servlets manage JDBC connections, connection leaks become frequent, and testing business workflows requires mock HTTP servlets. Conversely, if DAOs manage transactions, coordinating multi-table operations (such as debiting Account A, crediting Account B, and writing a ledger entry) becomes impossible without leaky abstractions.

### Decision
We enforce a **Strict 4-Layer Unidirectional Architecture**:
$$\text{Servlet} \longrightarrow \text{Service} \longrightarrow \text{DAO} \longrightarrow \text{MySQL}$$

1. **Servlets:** Strictly handle HTTP request unwrapping, parameter extraction, user-friendly error formatting, and Post/Redirect/Get flow. Servlets never import `java.sql.*` and never execute database queries.
2. **Services:** Own all business rules, input validation, permission checks, and **transaction boundaries**. Services open connections, execute `connection.setAutoCommit(false)`, commit upon success, rollback upon any failure, and restore auto-commit in `finally` blocks (via `TxRunner` or direct service orchestration).
3. **DAOs:** Contain pure SQL persistence mechanics. DAO methods participating in a transaction accept an active `java.sql.Connection` parameter and contain no business logic.
4. **Exception Translation:** Low-level checked `SQLException` is wrapped into unchecked `DataAccessException` at the DAO layer. Business rule breaches throw checked `BankingException` subclasses.

### Consequences
- **Positive:** Complete separation of concerns: domain logic is testable without HTTP or container dependencies.
- **Positive:** Transaction boundaries encapsulate all related table mutations atomically.
- **Trade-off:** DAO methods must provide overloads accepting an external `Connection` for transactional joins.

---

## ADR-004: Defense-in-Depth Authentication, BCrypt Hashing, and Session Fixation Defense

### Context & Problem Statement
Web banking applications face credential stuffing, brute-force attacks, session fixation, and unauthorized URL traversal. Storing plaintext or weak SHA-256 hashes exposes credentials during database compromises. Reusing session IDs across authentication boundaries allows session fixation attacks where an attacker tricks a victim into using a known session identifier.

### Decision
We implement a **Multi-Layer Web Security Architecture**:
1. **Salted Adaptive Password Hashing:** User passwords are encrypted using `at.favre.lib:bcrypt` with cost factor 10. Every password receives a cryptographically secure unique salt.
2. **Brute-Force Lockout Defense:** `AuthService` tracks consecutive failed attempts in the database. Upon 5 failed attempts, the user status transitions to `LOCKED`, preventing subsequent logins until operator review.
3. **Session Fixation Defense:** Upon successful authentication, `LoginServlet` invokes `request.changeSessionId()` before establishing the `SessionUser` session principal.
4. **Lightweight Session Principal:** Only `SessionUser` (containing ID, name, email, role) is stored in the session; password hashes are never kept in session memory.
5. **NoCache & Security Headers:** `NoCacheFilter` emits `Cache-Control: no-cache, no-store, must-revalidate` on all private paths to prevent post-logout back-button cache exposure.
6. **Centralized Filter Authorization:** `AuthFilter` and `RoleFilter` centrally guard private routes, preventing privilege escalation.

### Consequences
- **Positive:** Robust resistance to offline dictionary attacks, credential harvesting, and session fixation.
- **Positive:** Immediate revocation of session state on logout with client-side cache invalidation.

---

## ADR-005: 3-Step Idempotent Fund Transfer Protocol with One-Time Session Tokens

### Context & Problem Statement
Fund transfers are the most critical banking operation. If a user double-clicks the transfer button, refreshes the confirmation page, or uses the browser back button, a transfer could execute twice, causing duplicate money loss. Furthermore, transferring funds without recipient name verification risks sending funds to unintended accounts due to mistyped account numbers.

### Decision
We implement a **3-Step Wizard Transfer Protocol with One-Time Token Defense**:
1. **Step 1 (Input Form):** Customer selects source account, inputs beneficiary account number, transfer amount, and optional remarks.
2. **Step 2 (Verification & Staging):** The server verifies source balance and destination account standing, looks up the recipient's verified legal name, generates a cryptographically random UUID token, stages a `TransferDraft` in the HTTP session, and redirects to `?step=confirm`. The user reviews the recipient's real name before confirming.
3. **Step 3 (Execute & Receipt):** On submission, `CustomerTransferServlet` atomically compares the submitted token against the session draft token and immediately consumes (`removeAttribute`) the draft. The transfer executes, a `TransferReceipt` is staged, and the user is redirected via PRG to `?step=receipt`.
4. **Replay Defense:** Any re-submission of the form (via back-button or double-click) finds a missing or mismatched token and is rejected with an error.

### Consequences
- **Positive:** Complete protection against accidental duplicate transfers and replay attacks.
- **Positive:** User visual confirmation of recipient legal name reduces misdirected transfers.

---

## ADR-006: Polymorphic Account Hierarchy & Template Method Pattern

### Context & Problem Statement
Retail banking supports multiple account types (Savings and Current), each with divergent credit constraints: Savings requires a mandatory minimum balance (₹500.00) and accrues interest, while Current accounts permit overdraft balances down to an approved credit limit. Hardcoding `if-else` type checks across the service layer creates code duplication, violating the Open-Closed Principle.

### Decision
We implement an **Object-Oriented Polymorphic Account Model with the Template Method Pattern**:
1. **Abstract Base Class `Account`:** Declares `deposit(BigDecimal)` and `withdraw(BigDecimal)` as final template methods. The template methods enforce common invariants: verifying account operational status (`ACTIVE`, not `FROZEN` or `CLOSED`) and validating positive monetary amounts via `Money.validatePositive`.
2. **Specialized Hooks:** Subclasses implement abstract hooks `doDeposit(BigDecimal)` and `doWithdraw(BigDecimal)`.
3. **Polymorphic Subclasses:**
   - `SavingsAccount`: Verifies that projected balance $\ge \text{minimumBalance}$ (throws `InsufficientFundsException`), and implements `InterestBearing` to calculate monthly interest.
   - `CurrentAccount`: Verifies that projected balance $\ge -\text{overdraftLimit}$ (throws `InsufficientFundsException`).
4. **Dynamic Available Balance:** `getAvailableBalance()` polymorphically returns spendable funds ($\text{balance} - \text{minBalance}$ for Savings, $\text{balance} + \text{overdraft}$ for Current).

### Consequences
- **Positive:** Invariant enforcement is centralized in template methods while business rules remain polymorphic.
- **Positive:** New account types (e.g. Salary, NRI, Fixed Deposit) can be added without modifying existing withdrawal logic.

---

## ADR-007: Paginated Ledger with Dynamic Search and Invariant Filtering

### Context & Problem Statement
Over time, active bank accounts accumulate tens of thousands of ledger entries. Loading an entire transaction history into memory causes high database I/O, JVM heap exhaustion, and sluggish page render times. Customers also need to filter transactions by date range, transaction type (Deposit, Withdrawal, Transfer), amount bounds, and keywords.

### Decision
We implement **Server-Side Dynamic Filtering and SQL Pagination**:
1. **Immutable Domain Object:** `Transaction` is immutable and constructed exclusively via its `Builder`.
2. **Filter Specification Object:** `TxnFilter` encapsulates optional filter criteria (dates, type, amount range, keyword).
3. **Parameterized SQL Construction:** `TransactionDao.findByAccount` constructs a dynamic `WHERE` clause using strictly parameterized `?` placeholders for security, accompanied by a two-phase `COUNT(*)` query for accurate pagination.
4. **Generic Page Container:** `Page<T>` encapsulates the paged slice, page number, page size, total record count, and total page calculations.
5. **Newest-First Ordering:** Transactions are ordered by `created_at DESC, txn_id DESC` in SQL and in Java via `Comparator.comparing(Transaction::getCreatedAt).reversed()`.

### Consequences
- **Positive:** Constant memory footprint and fast response times regardless of total transaction volume.
- **Positive:** Strict protection against SQL injection and cross-site scripting in search results.

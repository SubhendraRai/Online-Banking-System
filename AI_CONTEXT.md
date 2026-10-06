# AI Pair-Programming Context & Hard Engineering Rules

This file documents the architectural guidelines, technology stack invariants, and non-negotiable rules governing the development of the **Online Banking System**. All future phases and AI pair-programming sessions must strictly adhere to these specifications.

---

## 1. Project Specification & Roles

- **Admin Capabilities:**
  - User management (name, email, role; create/update/delete with confirmation flash messages; interactive table with edit/delete actions).
  - Transaction monitoring (search, view, reporting).
  - System settings (configurable parameters with confirmation).
  - Operational metrics (operational reports and graphical visualizations).

- **Customer Capabilities:**
  - Account overview (balances, recent transactions, portfolio summaries).
  - Perform transactions (fund transfers with recipient validation, amount checks, and confirmation).
  - Transaction history (filtering by date/type/amount, full-text search).
  - Banking services (loans and investments with status and schedule details).
  - Profile management (name, email, password update with current verification, contact details, security settings).

---

## 2. Technology Stack (Strict & Fixed)

- **Language & Runtime:** Java 17 LTS
- **Build Tool:** Apache Maven (`war` packaging)
- **Servlet Container:** Apache Tomcat 10.1 (Jakarta EE 10: `jakarta.servlet.*`, **never** `javax.servlet.*`)
- **Database:** MySQL 8.x (`mysql-connector-j`)
- **View Layer:** JSP + JSTL (XML taglib URI: `jakarta.tags.core`)
- **Unit Testing:** JUnit 5 (Jupiter) with `maven-surefire-plugin` (v3.x)
- **Logging:** SLF4J API (`org.slf4j:slf4j-api`) with Logback (`ch.qos.logback:logback-classic`)
- **Password Hashing:** Maintained BCrypt library (`at.favre.lib:bcrypt`)
- **Base Package:** `com.bank`

---

## 3. Hard Architectural & Implementation Rules

1. **Strict Layering:**
   - Flow: `Servlet` &rarr; `Service` &rarr; `DAO` &rarr; `MySQL`.
   - Servlets **never** import `java.sql.*` or execute JDBC operations.
   - DAOs contain pure persistence mechanics (SQL + PreparedStatement mapping) and hold **no** business rules.
   - Services orchestrate business validations, permissions, and transaction boundaries.

2. **Monetary Precision:**
   - **Java:** Always `java.math.BigDecimal` (scale 2, `RoundingMode.HALF_UP`).
   - **MySQL:** Always `DECIMAL(15, 2)`.
   - **Strictly prohibited:** `double`, `float`, or rounding shortcuts.

3. **JDBC & Transaction Ownership:**
   - `PreparedStatement` only—no raw statement concatenation.
   - Always enclose `Connection`, `PreparedStatement`, and `ResultSet` in `try-with-resources`.
   - **Services own transactions:** `connection.setAutoCommit(false)`, `connection.commit()` on success, `connection.rollback()` in `catch`, and restore `connection.setAutoCommit(true)` in `finally`.
   - DAO methods participating in a transaction must accept an active `java.sql.Connection` parameter.

4. **Thread Safety & Concurrency Control:**
   - One `ReentrantLock` per account maintained in a `ConcurrentHashMap`.
   - Multi-account locks must **always** be acquired in strictly ascending order of account number / ID to prevent circular wait deadlocks.
   - Always use `lock.tryLock(...)` with an explicit timeout, releasing held locks in `finally`.
   - Inside the database transaction, acquire pessimistic rows using `SELECT ... FOR UPDATE` in the exact same ascending account order.

5. **Exception Handling:**
   - Checked exceptions: Rooted under a checked `BankingException` hierarchy for recoverable business conditions (e.g., `InsufficientFundsException`, `AccountNotFoundException`).
   - Unchecked exceptions: `DataAccessException` (extending `RuntimeException`) wrapping low-level `SQLException`.
   - **Never swallow exceptions.** Log or rethrow with contextual information.
   - **Never expose stack traces or raw database errors to end users.**

6. **Web Tier & Application Security:**
   - Passwords hashed using salted BCrypt.
   - Server-side role checks enforced centrally using `jakarta.servlet.Filter`.
   - Session fixation defense: regenerate session ID upon successful login (`request.changeSessionId()`).
   - JSP output escaped using `<c:out value="${...}"/>`—**no JSP scriptlets (`<% ... %>`)**.
   - Post/Redirect/Get (PRG) pattern after every POST request to prevent duplicate submissions.
   - One-time flash messages stored in session for user feedback and cleared upon display.

7. **Stateless Servlets:**
   - Servlets must **never** declare mutable instance variables. All state must reside in request, session, or database scope.

8. **Code Cleanliness & Style:**
   - Methods limited to approximately 40 lines maximum.
   - Meaningful, self-documenting naming conventions.
   - Javadoc comments on all public classes, interfaces, and service methods.
   - Zero dead code, unused imports, or copy-pasted logic blocks.

9. **Dependency & Library Integrity:**
   - Never invent imaginary methods or unverified library coordinates.
   - Validate library coordinates against Maven Central.

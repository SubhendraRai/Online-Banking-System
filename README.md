# Online Banking System

[![Java](https://img.shields.io/badge/Java-17%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Jakarta EE](https://img.shields.io/badge/Jakarta%20EE-10%20(Tomcat%2010.1)-F3702A?style=for-the-badge&logo=apachetomcat&logoColor=white)](https://tomcat.apache.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Academic-blue?style=for-the-badge)](LICENSE)

An enterprise-grade, concurrency-safe, dual-portal Online Banking Web Application engineered with **pure Java Servlets 6.0**, **JSP + JSTL**, **native JDBC (PreparedStatement)**, and **MySQL 8.0**, running on **Apache Tomcat 10.1** (Jakarta EE 10).

---

## 1. Project Summary

The **Online Banking System** is designed to demonstrate mission-critical banking operations with strict academic and commercial software engineering principles. It features zero reliance on heavy magic frameworks (such as Spring or Hibernate), providing pure architectural transparency across layered components:
- **Presentation Layer:** Jakarta Servlets and JSPs styled with a responsive design system, completely protected against XSS using JSTL `<c:out>` and CSRF/double-submit using the Post/Redirect/Get (PRG) pattern with session flash messaging.
- **Service & Business Layer:** Transaction-boundary management (`Connection` ownership), multi-account concurrency controls with deterministic deadlock-free locking, and strict monetary math using `BigDecimal`.
- **Data Access Layer (DAO):** Pure JDBC with 100% `PreparedStatement` usage, defensive `try-with-resources`, and database row-level locking (`SELECT ... FOR UPDATE`).
- **Security & Authorization:** Cryptographic password hashing with BCrypt, container-managed session fixation defense (`changeSessionId`), no-cache headers for sensitive banking views, and server-side role validation filters.

---

## 2. Features

### Customer Portal
- **Dashboard & Account Overview:** Real-time checking and savings account balances, masked account numbers, and recent transaction timeline.
- **Deposit & Withdrawal:** Instant funds management with strict validation, remarks tracking, and balance verification.
- **Inter-Account Transfers:** Atomically isolated fund transfers between arbitrary customer accounts with instant receipt generation.
- **Transaction History:** Paginated historical ledger supporting multi-criteria filtering by transaction type (DEPOSIT, WITHDRAWAL, TRANSFER), date ranges, and remarks search.
- **Banking Services:** Loan calculator and investment service modules.
- **Profile Management:** View and update personal profile, email, contact information, and security password with BCrypt verification.

### Administrator Portal
- **Operational Metrics & Analytics:** Real-time KPI summaries including total deposits, total accounts, active users, and transaction volume aggregations.
- **User Management (CRUD):** Searchable user directory, role assignments (Customer, Admin), status toggles (Active, Suspended), and profile updates.
- **Live Transaction Monitoring:** Global transaction audit logs with sorting and detailed transaction trace inspection.
- **System Settings:** Configurable operational parameters (maintenance mode, transaction limits).

---

## 3. Technology Stack

| Component | Technology | Version / Specification | Rationale |
| :--- | :--- | :--- | :--- |
| **Language** | Java SE | 17 LTS | Modern LTS Java features, records, enhanced switches |
| **Web Container** | Apache Tomcat | 10.1.x | Jakarta EE 10 compliance (`jakarta.servlet.*`) |
| **Web Framework** | Servlets & JSP | Jakarta Servlet 6.0 / JSTL 3.0 | Lightweight, transparent web request lifecycle |
| **Database** | MySQL Server | 8.0 / 8.4 | ACID transactional engine (InnoDB) |
| **Database Driver** | MySQL Connector/J | 8.4.0 | Official high-performance Type 4 JDBC driver |
| **Password Hashing** | Favre BCrypt | 0.10.2 | Salted adaptive password hashing (cost factor 12) |
| **Logging** | SLF4J + Logback | 2.0.13 / 1.5.6 | Production-grade structured asynchronous logging |
| **Testing** | JUnit 5 + Mockito | 5.10.2 / 5.11.0 | Unit and concurrency test suites |
| **Build Tool** | Apache Maven | 3.8+ | Standardized lifecycle and artifact packaging |

---

## 4. Architectural Design & Hard Rules

```
Browser (HTTP/HTTPS)
        │
        ▼
   RoleFilter & NoCacheFilter (Authentication & Authorization)
        │
        ▼
   Jakarta Servlets (Auth / Customer / Admin) ── [Post/Redirect/Get]
        │
        ▼
   Banking Services (TransferService, AccountService, UserService)
   ├─ LockManager (JVM Two-Phase Natural Lock Ordering)
   └─ DBUtil Transaction Boundary (Connection setAutoCommit(false))
        │
        ▼
   DAO Layer (AccountDao, TransactionDao, UserDao)
   └─ PreparedStatement (SELECT ... FOR UPDATE)
        │
        ▼
   MySQL Database (InnoDB ACID Engine, DECIMAL(15,2))
```

### Core Design Decisions

#### 1. Concurrency & Deadlock Prevention (Two-Tier Ordering)
- **JVM Tier:** A thread-safe `LockManager` manages account-specific `ReentrantLock` instances stored in a `ConcurrentHashMap`. Whenever a transfer involves multiple accounts (e.g. Account A and Account B), account numbers are sanitized, deduplicated, and sorted in **ascending natural alphanumeric order**. Locks are acquired sequentially with a fail-fast timeout (`tryLock(5s)`).
- **Database Tier:** Inside the active database transaction, accounts are queried and locked using `SELECT ... FOR UPDATE` in the **exact same ascending order**. This two-tier lock ordering mathematically guarantees immunity from Dijkstra deadlocks (dining philosophers problem).

#### 2. Transaction Management (Service-Owned `Connection`)
- Servlets never touch JDBC or SQL.
- DAOs are completely stateless and execute single queries without managing commits or rollbacks.
- Services own transactional units of work. The `Connection` is obtained from `DBUtil`, configured with `setAutoCommit(false)`, passed to DAO operations, explicitly committed on success (`commit()`), rolled back on any business error or SQL exception (`rollback()`), and restored to auto-commit in `finally`.

#### 3. Monetary Representation (`BigDecimal`)
- Floating-point types (`float`, `double`) introduce catastrophic precision degradation due to IEEE 754 binary representation.
- All monetary fields in Java are represented using `BigDecimal` with a fixed scale of 2 and `RoundingMode.HALF_UP`.
- In MySQL, currency values are stored in `DECIMAL(15, 2)` columns.

#### 4. Post/Redirect/Get (PRG) & Web Security
- Every mutating request (POST) redirects (HTTP 302/303) to a GET view. Success and error notifications are propagated via transient flash attributes stored in the `HttpSession` and removed immediately upon consumption.
- Prevents duplicate form submissions upon browser refresh.
- Output escaping via `<c:out>` protects against Cross-Site Scripting (XSS).
- `request.changeSessionId()` upon login neutralizes Session Fixation attacks.

---

## 5. Setup & Installation (Fresh Machine)

### Prerequisites
1. **Java Development Kit 17+**: Verify with `java -version`.
2. **Apache Maven 3.8+**: Verify with `mvn -version`.
3. **MySQL Server 8.0+**: Ensure the MySQL daemon is active.
4. **Apache Tomcat 10.1+**: Installed locally or managed via your IDE.

---

### Step 1: Database Initialization

1. Connect to your local MySQL instance as root (or privileged user):
   ```bash
   mysql -u root -p
   ```

2. Execute the provisioning scripts in sequential order:
   ```bash
   # 1. Create database schemas and application user
   mysql -u root -p < sql/01_init_db.sql

   # 2. Apply table schemas, indexes, and constraints
   mysql -u root -p bankdb < sql/schema.sql

   # 3. Seed demonstration users and initial bank accounts
   mysql -u root -p bankdb < sql/seed.sql
   ```

*(Optional: To reset the database cleanly at any point, run `mysql -u root -p bankdb < sql/reset.sql`)*

---

### Step 2: Database Configuration

1. In the project directory, locate the configuration templates:
   ```bash
   cp src/main/resources/db.properties.example src/main/resources/db.properties
   ```
2. Verify or update credentials in `src/main/resources/db.properties`:
   ```properties
   db.url=jdbc:mysql://localhost:3306/bankdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   db.user=bank_app
   db.password=bank_secret_pass_2026
   db.driver=com.mysql.cj.jdbc.Driver
   ```

---

### Step 3: Build & Package the Application

Run Maven clean package to execute the automated test suite and produce the deployable `.war` archive:
```bash
mvn clean package
```

Upon completion, the deployable WAR is generated at:
```text
target/online-banking-system.war
```

---

### Step 4: Deploy to Apache Tomcat 10.1

#### Method A: Direct Deployment via Tomcat `webapps/`
1. Copy the generated WAR file into Tomcat's deployment folder:
   ```bash
   cp target/online-banking-system.war $CATALINA_HOME/webapps/ROOT.war
   ```
2. Start Apache Tomcat:
   ```bash
   $CATALINA_HOME/bin/startup.sh    # macOS / Linux
   %CATALINA_HOME%\bin\startup.bat   # Windows
   ```
3. Access the web portal at: `http://localhost:8080/`

#### Method B: Running via IDE (IntelliJ IDEA / Eclipse)
1. Add a **Tomcat Server -> Local** run configuration.
2. Configure Tomcat 10.1.x home directory.
3. Under Deployment tab, add `online-banking-system:war exploded`.
4. Set Application Context to `/` or `/online-banking-system`.
5. Run or Debug the application.

---

## 6. Seeded Demo Accounts

| Role | Email Address | Password | Account Number | Initial Balance | Purpose |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Customer** | `customer@bank.com` | `Customer123!` | `ACC-1001-0001` | ₹25,000.00 | Primary customer demo account |
| **Customer** | `jane.doe@bank.com` | `Customer123!` | `ACC-1001-0002` | ₹15,000.00 | Secondary account for transfer demo |
| **Administrator** | `admin@bank.com` | `Admin123!` | *N/A* | *N/A* | Administrative user & metrics portal |

---

## 7. Running Tests & Automated Demos

### Execute All Unit & Concurrency Tests
```bash
mvn test
```

### Run Integration Tests (Requires Active MySQL)
To run end-to-end integration tests against your local test database (`bankdb_test`):
```bash
mvn test -Dtest=*IntegrationTest
```

### Verification Checklist
A comprehensive 35-case manual verification checklist is maintained at [`docs/test-report.md`](docs/test-report.md).

---

## 8. Application Screenshots

Below are placeholders for recording visual evidence of key application flows:

| View Description | Placeholder / Reference |
| :--- | :--- |
| **1. Customer Dashboard & Accounts** | `![Customer Dashboard](docs/screenshots/01_customer_dashboard.png)` |
| **2. Fund Transfer Execution** | `![Fund Transfer Form](docs/screenshots/02_transfer_form.png)` |
| **3. Transfer Confirmation Receipt** | `![Transfer Receipt](docs/screenshots/03_transfer_receipt.png)` |
| **4. Transaction History & Filtering** | `![Transaction History](docs/screenshots/04_transaction_history.png)` |
| **5. Customer Deposit Interface** | `![Customer Deposit](docs/screenshots/05_customer_deposit.png)` |
| **6. Admin Dashboard & Metrics** | `![Admin Dashboard](docs/screenshots/06_admin_dashboard.png)` |
| **7. Admin User Management CRUD** | `![Admin Users](docs/screenshots/07_admin_users.png)` |
| **8. Access Control Guard (HTTP 403)** | `![Security Guard](docs/screenshots/08_unauthorized_access.png)` |

---

## 9. Limitations & Future Enhancements

### Current System Limitations
- **In-Memory Concurrency Locks:** The JVM-level `LockManager` operates inside a single JVM instance. If deployed across a multi-node load-balanced cluster, database row locks (`SELECT ... FOR UPDATE`) provide ACID safety, but JVM lock ordering would need distributed locking (e.g., Redis Redlock).
- **Two-Factor Authentication (2FA):** Currently uses single-factor password authentication with BCrypt hashing; SMS/TOTP token verification is not yet implemented.
- **Connection Pool Tuning:** `DBUtil` provides a robust thread-safe connection manager; in high-load production, a connection pool library like HikariCP can be dropped in seamlessly.

### Future Roadmap
- Implementation of multi-factor OTP authentication via email/SMS.
- Automated generation of downloadable PDF monthly account statements.
- Scheduled recurring payments and standing orders using `ScheduledExecutorService`.
- RESTful OpenAPI JSON endpoints for mobile application clients.

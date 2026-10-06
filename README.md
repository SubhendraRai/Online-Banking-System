# Online Banking System

An enterprise-grade Java web application engineered with Jakarta Servlets 6.0, JSP, JSTL, JDBC, and MySQL running on Apache Tomcat 10.1.

---

## 1. Project Overview

The Online Banking System provides a two-portal architecture:
- **Customer Portal:** Account overview, fund transfers, transaction search/filtering, banking services (loans and investments), and profile management.
- **Admin Console:** User CRUD management, live transaction monitoring, system configuration, and operational metric reporting.

The system emphasizes high concurrency safety, rigorous monetary precision using `BigDecimal`, robust transaction boundaries, and defense-in-depth web security.

---

## 2. Technology Stack & Prerequisites

- **Java Development Kit (JDK):** Version 17 LTS (or higher with release target 17)
- **Build Tool:** Apache Maven 3.8+
- **Application Server:** Apache Tomcat 10.1.x (Jakarta EE 10 compliant)
- **Database:** MySQL 8.x
- **Core Libraries:**
  - Jakarta Servlet API 6.0 (`jakarta.servlet:jakarta.servlet-api:6.0.0`)
  - Jakarta JSTL 3.0 (`jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api:3.0.0` & `org.glassfish.web:jakarta.servlet.jsp.jstl:3.0.1`)
  - MySQL Connector/J (`com.mysql:mysql-connector-j:8.4.0`)
  - Favre BCrypt (`at.favre.lib:bcrypt:0.10.2`)
  - SLF4J 2.0 (`org.slf4j:slf4j-api:2.0.13`) & Logback Classic 1.5 (`ch.qos.logback:logback-classic:1.5.6`)
  - JUnit 5 (`org.junit.jupiter:junit-jupiter:5.10.2`)

---

## 3. Directory & Package Structure

```text
JaveProject/
├── pom.xml
├── .gitignore
├── README.md
├── AI_CONTEXT.md
├── sql/
│   └── 01_init_db.sql
├── docs/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── bank/
    │   │           ├── package-info.java
    │   │           ├── concurrent/
    │   │           ├── dao/
    │   │           ├── exception/
    │   │           ├── filter/
    │   │           ├── listener/
    │   │           ├── model/
    │   │           ├── service/
    │   │           ├── servlet/
    │   │           │   ├── HelloServlet.java
    │   │           │   ├── admin/
    │   │           │   ├── auth/
    │   │           │   └── customer/
    │   │           └── util/
    │   │               └── DbSmokeCheck.java
    │   ├── resources/
    │   │   ├── db.properties.example
    │   │   └── logback.xml
    │   └── webapp/
    │       ├── index.jsp
    │       └── WEB-INF/
    │           ├── web.xml
    │           └── views/
    │               └── error/
    │                   ├── 404.jsp
    │                   └── 500.jsp
    └── test/
        └── java/
            └── com/
                └── bank/
                    └── AppTest.java
```

---

## 4. Database Setup & Provisioning

1. Start your local MySQL 8 server.
2. Log in as an administrator (e.g., `root`):
   ```bash
   mysql -u root -p
   ```
3. Execute the database provisioning script located at `sql/01_init_db.sql`:
   ```bash
   mysql -u root -p < sql/01_init_db.sql
   ```
   This script creates:
   - Schema `bankdb` (UTF-8 mb4)
   - Schema `bankdb_test` (for automated test runs)
   - Application user `bank_app` with restricted privileges limited strictly to `bankdb` and `bankdb_test`.
4. Copy the sample database properties file and adjust credentials if necessary:
   ```bash
   cp src/main/resources/db.properties.example src/main/resources/db.properties
   ```

---

## 5. Build & Test Commands

### Compile and Run Unit Tests
```bash
mvn clean test
```

### Package Application WAR
```bash
mvn clean package
```
Generates the deployable web archive at `target/bank.war`.

### Run Database Smoke Check (Ping Diagnostic)
```bash
mvn compile exec:java -Dexec.mainClass="com.bank.util.DbSmokeCheck"
```
Or execute the class directly via your IDE or `java` command with the compiled classpath:
```bash
mvn test-compile
java -cp "target/classes:$(mvn dependency:build-classpath | grep -v '\[INFO\]' | tr '\n' ':')" com.bank.util.DbSmokeCheck
```

---

## 6. Tomcat 10.1 Deployment Guide (Context Path `/bank`)

### Option A: Deployment in IntelliJ IDEA Ultimate
1. Navigate to **Run** &rarr; **Edit Configurations...**.
2. Click **+** (Add New Configuration) &rarr; **Tomcat Server** &rarr; **Local**.
3. In the **Server** tab:
   - Configure **Application Server** by pointing to your extracted Tomcat 10.1 folder.
   - Set **HTTP port** to `8080`.
4. In the **Deployment** tab:
   - Click **+** &rarr; **Artifact...** &rarr; Select `bank-app:war` (or `bank-app:war exploded`).
   - Under **Application context**, set the path to: `/bank`.
5. Click **Apply** and **OK**.
6. Run the configuration. Access the app at:
   - Welcome page: `http://localhost:8080/bank/`
   - Health check: `http://localhost:8080/bank/hello`

### Option B: Deployment in Eclipse IDE for Enterprise Java (EE)
1. Open the **Servers** view (`Window` &rarr; `Show View` &rarr; `Servers`).
2. Right-click &rarr; **New** &rarr; **Server**.
3. Select **Apache** &rarr; **Tomcat v10.1 Server**, click **Next**, and point to the Tomcat 10.1 installation directory.
4. Add the `bank-app` project from the left panel to the right panel (**Configured**), click **Finish**.
5. Double-click the newly created Tomcat server in the Servers view to open its configuration.
6. Open the **Modules** tab at the bottom:
   - Select the `bank-app` module and click **Edit...**.
   - Change the **Path** to `/bank`.
7. Save changes (`Ctrl+S` / `Cmd+S`) and start the server.
8. Access:
   - Welcome page: `http://localhost:8080/bank/`
   - Health check: `http://localhost:8080/bank/hello`

### Option C: Manual Deployment to Standalone Tomcat 10.1
1. Build the WAR: `mvn clean package`.
2. Copy `target/bank.war` into the `$CATALINA_HOME/webapps/` directory.
3. Start Tomcat: `$CATALINA_HOME/bin/startup.sh` (or `startup.bat` on Windows).
4. Tomcat will automatically unpack `bank.war` into a context accessible at `http://localhost:8080/bank/`.

---

## 7. Development Roadmap

- **Phase 1: Project Skeleton (Completed)** &mdash; Maven configuration, package scaffolding, servlet/JSP/error page wiring, database smoke check.
- **Phase 2: Database Schema & Migration** &mdash; DDL tables for Users, Accounts, Transactions, Loans, Investments, Audit Logs.
- **Phase 3: Core Domain Models & Exception Hierarchy** &mdash; Immutable DTOs, entities with `BigDecimal`, custom checked/unchecked exceptions.
- **Phase 4: Connection Pool & DAO Tier** &mdash; HikariCP, PreparedStatement mapping, CRUD operations.
- **Phase 5: Concurrency Engine & Service Tier** &mdash; Ordered account locking with ReentrantLock timeouts, transactional fund transfers.
- **Phase 6: Authentication & Security Filters** &mdash; Salted BCrypt, session regeneration, role-based URL guards.
- **Phase 7: Customer Portal Servlets & JSPs** &mdash; Account overview, transfers with PRG, statement filtering.
- **Phase 8: Admin Console Servlets & JSPs** &mdash; User management, audit log search, operational reports.

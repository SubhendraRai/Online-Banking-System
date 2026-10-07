# Technical Documentation Index & Architecture Guide

Welcome to the **Online Banking System** technical documentation repository. This directory contains detailed architectural specifications, database models, UML/Mermaid diagrams, decision records, and automated test reports.

---

## 📚 Documentation Table of Contents

| Document / Diagram | Description | Target Audience |
| :--- | :--- | :--- |
| 📖 [System Requirements](requirements.md) | Functional & non-functional requirements, role matrix, and security policies | Product Owners, Engineers, QA |
| 🏗️ [Architecture Decisions](DECISIONS.md) | Architectural Decision Records (ADRs) explaining concurrency locks, `BigDecimal` math, and Jakarta EE migration | Software Architects, Developers |
| 📊 [Entity-Relationship Diagram](er_diagram.mmd) | Database tables, relationships, foreign keys, and indexes | Database Administrators, Backend Engineers |
| 🏛️ [Class Diagram](class_diagram.mmd) | Object-oriented domain model, DAO abstractions, services, and servlets | Backend Developers |
| 🔄 [Fund Transfer Sequence Diagram](sequence_transfer.mmd) | Step-by-step trace of atomic inter-account transfer execution and locking | Concurrency & Security Engineers |
| 👤 [Customer Journey Flow](customer_journey.mmd) | End-to-end user navigation flow for account management, transfers, and transactions | UX Designers, Frontend Engineers |
| 🛡️ [Admin Journey Flow](admin_journey.mmd) | Administrative workflow for user management, system metrics, and transaction auditing | Operations, DevOps, Admins |
| 🎯 [Use Case Diagram](use_cases.mmd) | High-level actor interactions for Customer and Administrator roles | System Analysts, Testers |
| 🧪 [Test Report Checklist](test-report.md) | Manual verification matrix, automated test cases, and test run summaries | QA Engineers, Release Managers |

---

## 🛠️ Quick Developer Quickstart

To build and run tests locally using the included Maven Wrapper:

```bash
# 1. Run unit and concurrency test suite
./mvnw clean test

# 2. Package the application WAR archive
./mvnw clean package
```

---

## 📐 Key Architectural Highlights

1. **Two-Tier Lock Ordering:** Prevent deadlocks at both JVM level (`ReentrantLock`) and DB level (`SELECT ... FOR UPDATE`) using natural alphanumeric account ordering.
2. **Jakarta EE 10:** Built using modern Servlets 6.0 (`jakarta.servlet.*`) running on Tomcat 10.1+.
3. **Monetary Precision:** Pure `java.math.BigDecimal` (scale 2, `HALF_UP`) and MySQL `DECIMAL(15,2)`.
4. **Clean Layering:** Strict `Servlet` → `Service` → `DAO` → `MySQL` separation with zero JDBC leakage in presentation or web layers.

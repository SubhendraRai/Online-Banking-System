# Architecture Decision Records (ADRs)

## ADR-001: Deadlock Avoidance and Hybrid Concurrency Control (Java ReentrantLock + MySQL FOR UPDATE)

### Context & Problem Statement
In peer-to-peer retail banking transfers, two heterogeneous accounts ($A$ and $B$) must be mutated simultaneously within an atomic unit of work. Under high concurrent workloads, if Thread 1 transfers funds from Account $A$ to Account $B$ while Thread 2 concurrently transfers from Account $B$ to Account $A$, a classic circular-wait deadlock can occur both in JVM application memory and at the database engine level (Dijkstra's Coffman conditions). 

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

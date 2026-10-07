package com.bank.service.fakes;

import com.bank.dao.TransactionDao;
import com.bank.dao.TxnFilter;
import com.bank.model.Transaction;
import com.bank.model.TxnStatus;
import com.bank.model.TxnType;
import com.bank.util.Money;
import com.bank.util.Page;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory test double for {@link TransactionDao} supporting transaction rollback staging.
 */
public class FakeTransactionDao extends TransactionDao {

    public static final Map<Connection, FakeTransactionDao> ACTIVE_DAOS = new ConcurrentHashMap<>();

    private final List<Transaction> storage = Collections.synchronizedList(new ArrayList<>());
    private final Map<Connection, List<Transaction>> uncommitted = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1);

    @Override
    public Optional<Transaction> findById(Connection conn, Long id) {
        return findById(id);
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        if (id == null) return Optional.empty();
        synchronized (storage) {
            return storage.stream().filter(t -> id.equals(t.getTxnId())).findFirst();
        }
    }

    @Override
    public List<Transaction> findAll(Connection conn) {
        return findAll();
    }

    @Override
    public List<Transaction> findAll() {
        synchronized (storage) {
            return new ArrayList<>(storage);
        }
    }

    @Override
    public Transaction save(Connection conn, Transaction entity) {
        return insert(conn, entity);
    }

    @Override
    public Transaction insert(Connection conn, Transaction entity) {
        Transaction inserted = insert(entity);
        if (conn != null) {
            ACTIVE_DAOS.put(conn, this);
            try {
                if (!conn.getAutoCommit()) {
                    synchronized (storage) {
                        storage.remove(inserted);
                    }
                    uncommitted.computeIfAbsent(conn, k -> new ArrayList<>()).add(inserted);
                }
            } catch (SQLException ignored) {
            }
        }
        return inserted;
    }

    @Override
    public Transaction insert(Transaction entity) {
        Long id = (entity.getTxnId() != null) ? entity.getTxnId() : sequence.getAndIncrement();
        LocalDateTime created = (entity.getCreatedAt() != null) ? entity.getCreatedAt() : LocalDateTime.now();

        Transaction saved = Transaction.builder()
                .txnId(id)
                .fromAccount(entity.getFromAccount())
                .toAccount(entity.getToAccount())
                .txnType(entity.getTxnType())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .createdAt(created)
                .build();

        storage.add(saved);
        return saved;
    }

    @Override
    public Page<Transaction> findByAccount(Connection conn, String accountNo, TxnFilter filter, int page, int size) {
        return findByAccount(accountNo, filter, page, size);
    }

    @Override
    public Page<Transaction> findByAccount(String accountNo, TxnFilter filter, int page, int size) {
        List<Transaction> matched;
        synchronized (storage) {
            matched = storage.stream()
                    .filter(t -> accountNo.equals(t.getFromAccount()) || accountNo.equals(t.getToAccount()))
                    .toList();
        }
        int curPage = Math.max(1, page);
        int pageSize = Math.max(1, size);
        return new Page<>(matched, curPage, pageSize, (long) matched.size());
    }

    @Override
    public BigDecimal sumTransfersToday(Connection conn, String accountNo) {
        return sumTransfersToday(accountNo);
    }

    @Override
    public BigDecimal sumTransfersToday(String accountNo) {
        LocalDate today = LocalDate.now();
        synchronized (storage) {
            return storage.stream()
                    .filter(t -> accountNo.equals(t.getFromAccount()))
                    .filter(t -> t.getTxnType() == TxnType.TRANSFER)
                    .filter(t -> t.getStatus() == TxnStatus.SUCCESS)
                    .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().equals(today))
                    .map(Transaction::getAmount)
                    .reduce(Money.ZERO, Money::add);
        }
    }

    public void commit(Connection conn) {
        if (conn != null) {
            List<Transaction> txns = uncommitted.remove(conn);
            if (txns != null) {
                storage.addAll(txns);
            }
        }
    }

    public void rollback(Connection conn) {
        if (conn != null) {
            uncommitted.remove(conn);
        }
    }

    public void clear() {
        storage.clear();
        uncommitted.clear();
        sequence.set(1);
    }
}

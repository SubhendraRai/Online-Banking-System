package com.bank.dao;

import com.bank.exception.DataAccessException;
import com.bank.model.Transaction;
import com.bank.model.TxnStatus;
import com.bank.model.TxnType;
import com.bank.util.DBUtil;
import com.bank.util.Money;
import com.bank.util.Page;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link Transaction} ledger records.
 * <p>
 * Enforces immutable double-entry ledger persistence, dynamic transaction search with
 * counterparty resolution and pagination, and daily debit transfer velocity sums.
 * </p>
 */
public class TransactionDao implements Repository<Transaction, Long> {

    private static final String SELECT_BASE =
            "SELECT txn_id, from_account, to_account, txn_type, amount, status, remarks, created_at FROM transactions ";

    @Override
    public Optional<Transaction> findById(Connection conn, Long id) {
        String sql = SELECT_BASE + "WHERE txn_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find transaction by ID: " + id, e);
        }
    }

    @Override
    public List<Transaction> findAll(Connection conn) {
        String sql = SELECT_BASE + "ORDER BY created_at DESC, txn_id DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<Transaction> txns = new ArrayList<>();
            while (rs.next()) {
                txns.add(mapRow(rs));
            }
            return txns;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch all transactions", e);
        }
    }

    @Override
    public Transaction save(Connection conn, Transaction entity) {
        return insert(conn, entity);
    }

    @Override
    public boolean update(Connection conn, Transaction entity) {
        String sql = "UPDATE transactions SET status = ?, remarks = ? WHERE txn_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entity.getStatus().name());
            stmt.setString(2, entity.getRemarks());
            stmt.setLong(3, entity.getTxnId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update transaction ID: " + entity.getTxnId(), e);
        }
    }

    @Override
    public boolean delete(Connection conn, Long id) {
        String sql = "DELETE FROM transactions WHERE txn_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete transaction ID: " + id, e);
        }
    }

    /**
     * Inserts an immutable transaction record using an acquired connection.
     *
     * @param txn transaction to insert
     * @return newly persisted {@link Transaction} with generated ID populated
     */
    public Transaction insert(Transaction txn) {
        try (Connection conn = DBUtil.getConnection()) {
            return insert(conn, txn);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for insert", e);
        }
    }

    /**
     * Inserts an immutable transaction record within an active transaction connection.
     *
     * @param conn active transaction connection
     * @param txn transaction to insert
     * @return newly persisted {@link Transaction} with generated ID populated
     */
    public Transaction insert(Connection conn, Transaction txn) {
        String sql = "INSERT INTO transactions (from_account, to_account, txn_type, amount, "
                + "status, remarks, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, txn.getFromAccount());
            stmt.setString(2, txn.getToAccount());
            stmt.setString(3, txn.getTxnType().name());
            stmt.setBigDecimal(4, Money.of(txn.getAmount()));
            stmt.setString(5, txn.getStatus().name());
            stmt.setString(6, txn.getRemarks());
            LocalDateTime created = txn.getCreatedAt() != null ? txn.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(7, Timestamp.valueOf(created));
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return Transaction.builder()
                            .txnId(keys.getLong(1))
                            .fromAccount(txn.getFromAccount())
                            .toAccount(txn.getToAccount())
                            .txnType(txn.getTxnType())
                            .amount(txn.getAmount())
                            .status(txn.getStatus())
                            .remarks(txn.getRemarks())
                            .createdAt(created)
                            .build();
                }
            }
            return txn;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert transaction record", e);
        }
    }

    /**
     * Finds paginated transactions for an account with dynamic filtering using an acquired connection.
     *
     * @param accountNo bank account number
     * @param filter filter criteria
     * @param page 1-indexed page number
     * @param size page size
     * @return paginated {@link Page} of transactions
     */
    public Page<Transaction> findByAccount(String accountNo, TxnFilter filter, int page, int size) {
        try (Connection conn = DBUtil.getConnection()) {
            return findByAccount(conn, accountNo, filter, page, size);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for findByAccount", e);
        }
    }

    /**
     * Finds paginated transactions for an account with dynamic filtering within a transaction connection.
     *
     * @param conn active database connection
     * @param accountNo bank account number
     * @param filter filter criteria
     * @param page 1-indexed page number
     * @param size page size
     * @return paginated {@link Page} of transactions
     */
    public Page<Transaction> findByAccount(Connection conn, String accountNo, TxnFilter filter, int page, int size) {
        int currentPage = Math.max(1, page);
        int pageSize = Math.max(1, size);
        int offset = (currentPage - 1) * pageSize;

        StringBuilder whereSql = new StringBuilder(" WHERE (from_account = ? OR to_account = ?)");
        List<Object> params = new ArrayList<>();
        params.add(accountNo);
        params.add(accountNo);

        appendFilterConditions(whereSql, params, accountNo, filter);

        long totalItems = executeCountQuery(conn, "SELECT COUNT(*) FROM transactions" + whereSql, params);
        if (totalItems == 0) {
            return new Page<>(Collections.emptyList(), currentPage, pageSize, 0L);
        }

        String dataSql = SELECT_BASE + whereSql + " ORDER BY created_at DESC, txn_id DESC LIMIT ? OFFSET ?";
        List<Transaction> items = executePagedQuery(conn, dataSql, params, pageSize, offset);
        return new Page<>(items, currentPage, pageSize, totalItems);
    }

    /**
     * Computes the cumulative monetary sum of all successful debit transfers today using an acquired connection.
     *
     * @param accountNo source account number
     * @return cumulative debit transfer amount for today
     */
    public BigDecimal sumTransfersToday(String accountNo) {
        try (Connection conn = DBUtil.getConnection()) {
            return sumTransfersToday(conn, accountNo);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for sumTransfersToday", e);
        }
    }

    /**
     * Computes the cumulative monetary sum of all successful debit transfers today within a transaction connection.
     *
     * @param conn active database connection
     * @param accountNo source account number
     * @return cumulative debit transfer amount for today
     */
    public BigDecimal sumTransfersToday(Connection conn, String accountNo) {
        String sql = "SELECT COALESCE(SUM(amount), 0.00) FROM transactions "
                + "WHERE from_account = ? AND txn_type = 'TRANSFER' AND status = 'SUCCESS' "
                + "AND created_at >= CURDATE() AND created_at < CURDATE() + INTERVAL 1 DAY";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountNo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal sum = rs.getBigDecimal(1);
                    return sum != null ? Money.of(sum) : Money.ZERO;
                }
                return Money.ZERO;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed calculating daily transfer sum for account: " + accountNo, e);
        }
    }

    private void appendFilterConditions(StringBuilder sql, List<Object> params, String accountNo, TxnFilter filter) {
        if (filter == null) return;
        if (filter.getFromDate() != null) {
            sql.append(" AND created_at >= ?");
            params.add(Timestamp.valueOf(filter.getFromDate().atStartOfDay()));
        }
        if (filter.getToDate() != null) {
            sql.append(" AND created_at < ?");
            params.add(Timestamp.valueOf(filter.getToDate().plusDays(1).atStartOfDay()));
        }
        if (filter.getType() != null) {
            sql.append(" AND txn_type = ?");
            params.add(filter.getType().name());
        }
        if (filter.getMinAmount() != null) {
            sql.append(" AND amount >= ?");
            params.add(Money.of(filter.getMinAmount()));
        }
        if (filter.getMaxAmount() != null) {
            sql.append(" AND amount <= ?");
            params.add(Money.of(filter.getMaxAmount()));
        }
        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            String pattern = "%" + filter.getKeyword().trim().toLowerCase() + "%";
            sql.append(" AND (LOWER(COALESCE(remarks, '')) LIKE ? OR "
                    + "(CASE WHEN from_account = ? THEN to_account ELSE from_account END) LIKE ?)");
            params.add(pattern);
            params.add(accountNo);
            params.add("%" + filter.getKeyword().trim() + "%");
        }
    }

    private long executeCountQuery(Connection conn, String sql, List<Object> params) {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute transaction count query", e);
        }
    }

    private List<Transaction> executePagedQuery(Connection conn, String sql, List<Object> params, int limit, int offset) {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Object param : params) {
                stmt.setObject(idx++, param);
            }
            stmt.setInt(idx++, limit);
            stmt.setInt(idx, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Transaction> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
                return items;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute paged transaction query", e);
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return Transaction.builder()
                .txnId(rs.getLong("txn_id"))
                .fromAccount(rs.getString("from_account"))
                .toAccount(rs.getString("to_account"))
                .txnType(TxnType.valueOf(rs.getString("txn_type")))
                .amount(Money.of(rs.getBigDecimal("amount")))
                .status(TxnStatus.valueOf(rs.getString("status")))
                .remarks(rs.getString("remarks"))
                .createdAt(ts != null ? ts.toLocalDateTime() : null)
                .build();
    }
}

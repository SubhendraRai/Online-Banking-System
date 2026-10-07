package com.bank.dao;

import com.bank.exception.DataAccessException;
import com.bank.model.Account;
import com.bank.model.AccountStatus;
import com.bank.model.AccountType;
import com.bank.model.CurrentAccount;
import com.bank.model.SavingsAccount;
import com.bank.util.DBUtil;
import com.bank.util.Money;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link Account} entities and polymorphic subtypes
 * ({@link SavingsAccount} and {@link CurrentAccount}).
 * <p>
 * Supports row-level pessimistic locking via {@code SELECT ... FOR UPDATE},
 * dynamic account number generation, and balance adjustments within transactional boundaries.
 * </p>
 */
public class AccountDao implements Repository<Account, String> {

    private static final String SELECT_BASE =
            "SELECT account_no, user_id, account_type, balance, overdraft_limit, status, opened_at FROM accounts ";

    @Override
    public Optional<Account> findById(Connection conn, String accountNo) {
        String sql = SELECT_BASE + "WHERE account_no = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountNo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find account: " + accountNo, e);
        }
    }

    @Override
    public List<Account> findAll(Connection conn) {
        String sql = SELECT_BASE + "ORDER BY account_no ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<Account> accounts = new ArrayList<>();
            while (rs.next()) {
                accounts.add(mapRow(rs));
            }
            return accounts;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch all accounts", e);
        }
    }

    @Override
    public Account save(Connection conn, Account account) {
        if (account.getAccountNo() == null || account.getAccountNo().trim().isEmpty()) {
            account.setAccountNo(nextAccountNumber(conn));
        }
        String sql = "INSERT INTO accounts (account_no, user_id, account_type, balance, "
                + "overdraft_limit, status, opened_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindAccountParameters(stmt, account);
            stmt.executeUpdate();
            return account;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert account: " + account.getAccountNo(), e);
        }
    }

    @Override
    public boolean update(Connection conn, Account account) {
        String sql = "UPDATE accounts SET balance = ?, overdraft_limit = ?, status = ? WHERE account_no = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, Money.of(account.getBalance()));
            BigDecimal overdraft = (account instanceof CurrentAccount ca) ? ca.getOverdraftLimit() : Money.ZERO;
            stmt.setBigDecimal(2, Money.of(overdraft));
            stmt.setString(3, account.getStatus().name());
            stmt.setString(4, account.getAccountNo());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update account: " + account.getAccountNo(), e);
        }
    }

    @Override
    public boolean delete(Connection conn, String accountNo) {
        String sql = "DELETE FROM accounts WHERE account_no = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountNo);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete account: " + accountNo, e);
        }
    }

    /**
     * Retrieves all accounts owned by a specific user using an acquired connection.
     *
     * @param userId user identifier
     * @return list of bank accounts
     */
    public List<Account> findByUser(Long userId) {
        try (Connection conn = DBUtil.getConnection()) {
            return findByUser(conn, userId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for findByUser", e);
        }
    }

    /**
     * Retrieves all accounts owned by a specific user within a transaction connection.
     *
     * @param conn active database connection
     * @param userId user identifier
     * @return list of bank accounts
     */
    public List<Account> findByUser(Connection conn, Long userId) {
        String sql = SELECT_BASE + "WHERE user_id = ? ORDER BY account_no ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Account> accounts = new ArrayList<>();
                while (rs.next()) {
                    accounts.add(mapRow(rs));
                }
                return accounts;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find accounts for user ID: " + userId, e);
        }
    }

    /**
     * Retrieves an account locking the row exclusively with {@code SELECT ... FOR UPDATE}
     * using an acquired connection.
     *
     * @param accountNo account number
     * @return {@link Optional} containing the locked account
     */
    public Optional<Account> findByIdForUpdate(String accountNo) {
        try (Connection conn = DBUtil.getConnection()) {
            return findByIdForUpdate(conn, accountNo);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for findByIdForUpdate", e);
        }
    }

    /**
     * Retrieves an account locking the row exclusively with {@code SELECT ... FOR UPDATE}
     * within an active transaction connection.
     *
     * @param conn active transaction connection
     * @param accountNo account number
     * @return {@link Optional} containing the locked account
     */
    public Optional<Account> findByIdForUpdate(Connection conn, String accountNo) {
        String sql = SELECT_BASE + "WHERE account_no = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountNo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute SELECT FOR UPDATE on account: " + accountNo, e);
        }
    }

    /**
     * Updates an account's monetary balance using an acquired connection.
     *
     * @param account account with updated balance
     * @return {@code true} if update succeeded, {@code false} otherwise
     */
    public boolean updateBalance(Account account) {
        try (Connection conn = DBUtil.getConnection()) {
            return updateBalance(conn, account);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for updateBalance", e);
        }
    }

    /**
     * Updates an account's monetary balance within an active transaction connection.
     *
     * @param conn active transaction connection
     * @param account account with updated balance
     * @return {@code true} if update succeeded, {@code false} otherwise
     */
    public boolean updateBalance(Connection conn, Account account) {
        String sql = "UPDATE accounts SET balance = ? WHERE account_no = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, Money.of(account.getBalance()));
            stmt.setString(2, account.getAccountNo());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update balance for account: " + account.getAccountNo(), e);
        }
    }

    /**
     * Generates a new unique 12-digit account number using an acquired connection.
     *
     * @return unique 12-digit string
     */
    public String nextAccountNumber() {
        try (Connection conn = DBUtil.getConnection()) {
            return nextAccountNumber(conn);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to acquire connection for nextAccountNumber", e);
        }
    }

    /**
     * Generates a new unique 12-digit account number within a transaction connection.
     *
     * @param conn active database connection
     * @return unique 12-digit string
     */
    public String nextAccountNumber(Connection conn) {
        String sql = "SELECT MAX(account_no) FROM accounts WHERE LENGTH(account_no) = 12";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                String maxAcc = rs.getString(1);
                if (maxAcc != null && maxAcc.matches("\\d{12}")) {
                    long nextVal = Long.parseLong(maxAcc) + 1;
                    return String.format("%012d", nextVal);
                }
            }
            return "100000000001";
        } catch (SQLException e) {
            throw new DataAccessException("Failed to generate next account number", e);
        }
    }

    private void bindAccountParameters(PreparedStatement stmt, Account account) throws SQLException {
        stmt.setString(1, account.getAccountNo());
        stmt.setLong(2, account.getOwnerId());
        stmt.setString(3, account.getAccountType().name());
        stmt.setBigDecimal(4, Money.of(account.getBalance()));
        BigDecimal overdraft = (account instanceof CurrentAccount ca) ? ca.getOverdraftLimit() : Money.ZERO;
        stmt.setBigDecimal(5, Money.of(overdraft));
        stmt.setString(6, account.getStatus().name());
        LocalDateTime openedAt = account.getOpenedAt() != null ? account.getOpenedAt() : LocalDateTime.now();
        stmt.setTimestamp(7, Timestamp.valueOf(openedAt));
    }

    private Account mapRow(ResultSet rs) throws SQLException {
        String accountNo = rs.getString("account_no");
        Long userId = rs.getLong("user_id");
        AccountType type = AccountType.valueOf(rs.getString("account_type"));
        BigDecimal balance = Money.of(rs.getBigDecimal("balance"));
        BigDecimal overdraft = Money.of(rs.getBigDecimal("overdraft_limit"));
        AccountStatus status = AccountStatus.valueOf(rs.getString("status"));
        Timestamp openedTs = rs.getTimestamp("opened_at");

        Account account = (type == AccountType.SAVINGS)
                ? new SavingsAccount(accountNo, userId, balance, status, SavingsAccount.DEFAULT_MINIMUM_BALANCE)
                : new CurrentAccount(accountNo, userId, balance, status, overdraft);

        if (openedTs != null) {
            account.setOpenedAt(openedTs.toLocalDateTime());
        }
        return account;
    }
}

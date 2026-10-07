package com.bank.service.fakes;

import com.bank.dao.AccountDao;
import com.bank.model.Account;
import com.bank.model.CurrentAccount;
import com.bank.model.SavingsAccount;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory test double for {@link AccountDao} supporting transaction rollback staging.
 */
public class FakeAccountDao extends AccountDao {

    public static final Map<Connection, FakeAccountDao> ACTIVE_DAOS = new ConcurrentHashMap<>();

    private final Map<String, Account> storage = new ConcurrentHashMap<>();
    private final Map<Connection, Map<String, Account>> uncommitted = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(100000000000L);

    @Override
    public Optional<Account> findById(Connection conn, String accountNo) {
        return findById(accountNo);
    }

    @Override
    public Optional<Account> findById(String accountNo) {
        if (accountNo == null) return Optional.empty();
        return Optional.ofNullable(copyAccount(storage.get(accountNo)));
    }

    @Override
    public Optional<Account> findByIdForUpdate(Connection conn, String accountNo) {
        if (accountNo == null) return Optional.empty();
        if (conn != null) {
            Map<String, Account> staged = uncommitted.get(conn);
            if (staged != null && staged.containsKey(accountNo)) {
                return Optional.of(copyAccount(staged.get(accountNo)));
            }
        }
        return Optional.ofNullable(copyAccount(storage.get(accountNo)));
    }

    @Override
    public Optional<Account> findByIdForUpdate(String accountNo) {
        return findById(accountNo);
    }

    @Override
    public List<Account> findByUser(Connection conn, Long userId) {
        return findByUser(userId);
    }

    @Override
    public List<Account> findByUser(Long userId) {
        if (userId == null) return new ArrayList<>();
        return storage.values().stream()
                .filter(a -> userId.equals(a.getOwnerId()))
                .map(this::copyAccount)
                .toList();
    }

    @Override
    public String nextAccountNumber(Connection conn) {
        return nextAccountNumber();
    }

    @Override
    public String nextAccountNumber() {
        return String.format("%012d", sequence.incrementAndGet());
    }

    @Override
    public Account save(Connection conn, Account account) {
        return save(account);
    }

    @Override
    public Account save(Account account) {
        if (account.getAccountNo() == null || account.getAccountNo().isBlank()) {
            account.setAccountNo(nextAccountNumber());
        }
        Account copy = copyAccount(account);
        storage.put(account.getAccountNo(), copy);
        return copyAccount(copy);
    }

    @Override
    public boolean update(Connection conn, Account account) {
        return updateBalance(conn, account);
    }

    @Override
    public boolean update(Account account) {
        return updateBalance(null, account);
    }

    @Override
    public boolean updateBalance(Connection conn, Account account) {
        if (account == null || account.getAccountNo() == null) {
            return false;
        }
        Account copy = copyAccount(account);
        if (conn != null) {
            ACTIVE_DAOS.put(conn, this);
            try {
                if (!conn.getAutoCommit()) {
                    uncommitted.computeIfAbsent(conn, k -> new ConcurrentHashMap<>()).put(account.getAccountNo(), copy);
                    return true;
                }
            } catch (SQLException ignored) {
            }
        }
        storage.put(account.getAccountNo(), copy);
        return true;
    }

    @Override
    public boolean updateBalance(Account account) {
        if (account == null || account.getAccountNo() == null) return false;
        storage.put(account.getAccountNo(), copyAccount(account));
        return true;
    }

    @Override
    public boolean delete(Connection conn, String accountNo) {
        return delete(accountNo);
    }

    @Override
    public boolean delete(String accountNo) {
        return storage.remove(accountNo) != null;
    }

    @Override
    public List<Account> findAll(Connection conn) {
        return findAll();
    }

    @Override
    public List<Account> findAll() {
        return storage.values().stream().map(this::copyAccount).toList();
    }

    public void commit(Connection conn) {
        if (conn != null) {
            Map<String, Account> staged = uncommitted.remove(conn);
            if (staged != null) {
                storage.putAll(staged);
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
        sequence.set(100000000000L);
    }

    private Account copyAccount(Account a) {
        if (a == null) return null;
        if (a instanceof SavingsAccount sa) {
            SavingsAccount copy = new SavingsAccount(sa.getAccountNo(), sa.getOwnerId(), sa.getBalance(),
                    sa.getStatus(), sa.getMinimumBalance());
            copy.setOpenedAt(sa.getOpenedAt());
            return copy;
        } else if (a instanceof CurrentAccount ca) {
            CurrentAccount copy = new CurrentAccount(ca.getAccountNo(), ca.getOwnerId(), ca.getBalance(),
                    ca.getStatus(), ca.getOverdraftLimit());
            copy.setOpenedAt(ca.getOpenedAt());
            return copy;
        }
        return a;
    }
}

package com.bank.service.fakes;

import com.bank.dao.AccountDao;
import com.bank.model.Account;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory test double for {@link AccountDao}.
 */
public class FakeAccountDao extends AccountDao {

    private final Map<String, Account> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(100000000000L);

    @Override
    public Optional<Account> findById(Connection conn, String accountNo) {
        return findById(accountNo);
    }

    @Override
    public Optional<Account> findById(String accountNo) {
        if (accountNo == null) return Optional.empty();
        return Optional.ofNullable(storage.get(accountNo));
    }

    @Override
    public Optional<Account> findByIdForUpdate(Connection conn, String accountNo) {
        return findById(accountNo);
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
        storage.put(account.getAccountNo(), account);
        return account;
    }

    @Override
    public boolean update(Connection conn, Account account) {
        return update(account);
    }

    @Override
    public boolean update(Account account) {
        if (account.getAccountNo() == null || !storage.containsKey(account.getAccountNo())) {
            return false;
        }
        storage.put(account.getAccountNo(), account);
        return true;
    }

    @Override
    public boolean updateBalance(Connection conn, Account account) {
        return updateBalance(account);
    }

    @Override
    public boolean updateBalance(Account account) {
        return update(account);
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
        return new ArrayList<>(storage.values());
    }

    public void clear() {
        storage.clear();
        sequence.set(100000000000L);
    }
}

package com.bank.service.fakes;

import com.bank.dao.UserDao;
import com.bank.model.Role;
import com.bank.model.User;
import com.bank.model.UserStatus;
import com.bank.util.Page;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory test double for {@link UserDao}.
 */
public class FakeUserDao extends UserDao {

    private final Map<Long, User> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1);

    @Override
    public Optional<User> findById(Connection conn, Long id) {
        return findById(id);
    }

    @Override
    public Optional<User> findById(Long id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<User> findByEmail(Connection conn, String email) {
        return findByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return storage.values().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst();
    }

    @Override
    public boolean existsByEmail(Connection conn, String email) {
        return existsByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public User save(Connection conn, User user) {
        return save(user);
    }

    @Override
    public User save(User user) {
        if (user.getUserId() == null) {
            user.setUserId(sequence.getAndIncrement());
        }
        storage.put(user.getUserId(), user);
        return user;
    }

    @Override
    public boolean update(Connection conn, User user) {
        return update(user);
    }

    @Override
    public boolean update(User user) {
        if (user.getUserId() == null || !storage.containsKey(user.getUserId())) {
            return false;
        }
        storage.put(user.getUserId(), user);
        return true;
    }

    @Override
    public boolean delete(Connection conn, Long id) {
        return delete(id);
    }

    @Override
    public boolean delete(Long id) {
        return storage.remove(id) != null;
    }

    @Override
    public List<User> findAll(Connection conn) {
        return findAll();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public long countByRole(Connection conn, Role role) {
        return countByRole(role);
    }

    @Override
    public long countByRole(Role role) {
        return storage.values().stream()
                .filter(u -> role == null || u.getRole() == role)
                .count();
    }

    @Override
    public Page<User> search(Connection conn, String keyword, Role role, UserStatus status, int page, int size) {
        return search(keyword, role, status, page, size);
    }

    @Override
    public Page<User> search(String keyword, Role role, UserStatus status, int page, int size) {
        List<User> list = storage.values().stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> status == null || u.getStatus() == status)
                .toList();
        return new Page<>(list, page, size, (long) list.size());
    }

    public void clear() {
        storage.clear();
        sequence.set(1);
    }
}

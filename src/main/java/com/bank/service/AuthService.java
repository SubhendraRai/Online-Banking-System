package com.bank.service;

import com.bank.dao.AccountDao;
import com.bank.dao.UserDao;
import com.bank.exception.AuthenticationException;
import com.bank.exception.BankingException;
import com.bank.exception.DuplicateEmailException;
import com.bank.exception.ErrorCode;
import com.bank.exception.ValidationException;
import com.bank.model.AccountStatus;
import com.bank.model.Customer;
import com.bank.model.Role;
import com.bank.model.SavingsAccount;
import com.bank.model.SessionUser;
import com.bank.model.User;
import com.bank.model.UserStatus;
import com.bank.util.Money;
import com.bank.util.PasswordUtil;
import com.bank.util.TxRunner;
import com.bank.util.Validator;
import java.math.BigDecimal;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service managing user authentication, security access control, and retail customer onboarding.
 * <p>
 * Implements business transactions for:
 * <ul>
 *   <li>Customer registration: Atomically creates a {@link Customer} record and provisions
 *       a default 12-digit {@link SavingsAccount} within a managed ACID transaction boundary.</li>
 *   <li>Customer & Admin login: Enforces BCrypt verification, rejects locked or deleted users,
 *       implements 5-attempt brute-force account locking, and returns an authenticated {@link SessionUser}.</li>
 * </ul>
 * </p>
 */
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserDao userDao;
    private final AccountDao accountDao;
    private final SettingsProvider settingsProvider;

    /**
     * Default constructor wiring production DAO and settings dependencies.
     */
    public AuthService() {
        this(new UserDao(), new AccountDao(), new DefaultSettingsProvider());
    }

    /**
     * Parameterized constructor for dependency injection and unit test isolation.
     *
     * @param userDao user repository DAO
     * @param accountDao account repository DAO
     * @param settingsProvider configuration provider
     */
    public AuthService(UserDao userDao, AccountDao accountDao, SettingsProvider settingsProvider) {
        this.userDao = Objects.requireNonNull(userDao, "userDao cannot be null");
        this.accountDao = Objects.requireNonNull(accountDao, "accountDao cannot be null");
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider cannot be null");
    }

    /**
     * Registers a new retail customer with zero opening balance.
     *
     * @param fullName full legal name
     * @param email unique email address
     * @param phone telephone number
     * @param address physical address
     * @param password plaintext password
     * @return newly persisted {@link Customer} entity
     * @throws ValidationException if any input fields fail validation rules
     * @throws DuplicateEmailException if email is already registered
     * @throws BankingException on database transaction error
     */
    public Customer register(String fullName, String email, String phone,
                            String address, String password)
            throws ValidationException, DuplicateEmailException, BankingException {
        return register(fullName, email, phone, address, password, BigDecimal.ZERO);
    }

    /**
     * Atomically registers a new customer and provisions a default 12-digit {@link SavingsAccount}.
     *
     * @param fullName full legal name
     * @param email unique email address
     * @param phone telephone number
     * @param address physical address
     * @param password plaintext password
     * @param initialDeposit initial opening balance deposit
     * @return newly persisted {@link Customer} entity
     * @throws ValidationException if input fails validation or initial deposit is negative
     * @throws DuplicateEmailException if email is already registered
     * @throws BankingException on transactional or persistence failure
     */
    public Customer register(String fullName, String email, String phone,
                            String address, String password, BigDecimal initialDeposit)
            throws ValidationException, DuplicateEmailException, BankingException {

        Validator.validateRegistration(fullName, email, phone, address, password);
        String normalizedEmail = email.trim().toLowerCase();

        if (userDao.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        String passwordHash = PasswordUtil.hash(password);
        BigDecimal minBalance = settingsProvider.getDecimal("savings.min_balance", SavingsAccount.DEFAULT_MINIMUM_BALANCE);
        BigDecimal openingBal = (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) > 0)
                ? Money.of(initialDeposit) : Money.ZERO;

        return TxRunner.execute(conn -> {
            if (userDao.existsByEmail(conn, normalizedEmail)) {
                throw new DuplicateEmailException(normalizedEmail);
            }

            Customer customer = new Customer();
            customer.setFullName(fullName.trim());
            customer.setEmail(normalizedEmail);
            customer.setPhone(phone.trim());
            customer.setAddress(address.trim());
            customer.setPasswordHash(passwordHash);
            customer.setRole(Role.CUSTOMER);
            customer.setStatus(UserStatus.ACTIVE);

            userDao.save(conn, customer);

            String accountNo = accountDao.nextAccountNumber(conn);
            SavingsAccount savings = new SavingsAccount(accountNo, customer.getUserId(),
                    openingBal, AccountStatus.ACTIVE, minBalance);
            accountDao.save(conn, savings);

            LOGGER.info("Successfully registered customer ID {} with default savings account {}",
                    customer.getUserId(), accountNo);
            return customer;
        });
    }

    /**
     * Authenticates a user, enforcing security status checks and password verification.
     *
     * @param email candidate email address
     * @param password candidate plaintext password
     * @return authenticated {@link SessionUser} principal
     * @throws ValidationException if email or password parameters are invalid
     * @throws AuthenticationException if credentials mismatch or user is locked/deleted
     */
    public SessionUser login(String email, String password)
            throws ValidationException, AuthenticationException {

        if (!Validator.isNotBlank(email) || !Validator.isNotBlank(password)) {
            throw new ValidationException("email", "Email and password are required.");
        }
        if (!Validator.isValidEmail(email)) {
            throw new ValidationException("email", "Invalid email address format.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userDao.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthenticationException("Invalid email address or password."));

        if (user.getStatus() == UserStatus.LOCKED) {
            LOGGER.warn("Login attempt for LOCKED user ID {}", user.getUserId());
            throw new AuthenticationException(ErrorCode.ACCOUNT_LOCKED,
                    "Account is locked due to repeated failed login attempts.");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            LOGGER.warn("Login attempt for DELETED user ID {}", user.getUserId());
            throw new AuthenticationException(ErrorCode.AUTHENTICATION_FAILED,
                    "Account has been deactivated. Please contact support.");
        }

        boolean passwordMatches = PasswordUtil.verify(password, user.getPasswordHash());
        if (!passwordMatches) {
            handleFailedLogin(user);
            throw new AuthenticationException("Invalid email address or password.");
        }

        if (user.getFailedAttempts() > 0) {
            user.setFailedAttempts(0);
            userDao.update(user);
        }

        LOGGER.info("Successful login for user ID {} [{}]", user.getUserId(), user.getRole());
        return new SessionUser(user.getUserId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    private void handleFailedLogin(User user) throws AuthenticationException {
        int failedAttempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(failedAttempts);

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            user.setStatus(UserStatus.LOCKED);
            userDao.update(user);
            LOGGER.warn("User ID {} locked after {} consecutive failed login attempts",
                    user.getUserId(), failedAttempts);
            throw new AuthenticationException(ErrorCode.ACCOUNT_LOCKED,
                    "Account locked after 5 consecutive failed login attempts.");
        }

        userDao.update(user);
        LOGGER.warn("Failed password attempt for user ID {} (attempt {} of {})",
                user.getUserId(), failedAttempts, MAX_FAILED_ATTEMPTS);
    }
}

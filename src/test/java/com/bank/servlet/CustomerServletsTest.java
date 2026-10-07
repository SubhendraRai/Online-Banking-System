package com.bank.servlet;

import com.bank.concurrent.LockManager;
import com.bank.model.Account;
import com.bank.model.AccountFactory;
import com.bank.model.AccountType;
import com.bank.model.Customer;
import com.bank.model.Role;
import com.bank.model.SessionUser;
import com.bank.model.TransferDraft;
import com.bank.model.TransferReceipt;
import com.bank.service.AccountService;
import com.bank.service.BaseServiceTest;
import com.bank.service.NoOpFraudChecker;
import com.bank.service.TransferService;
import com.bank.service.fakes.FakeAccountDao;
import com.bank.service.fakes.FakeSettingsProvider;
import com.bank.service.fakes.FakeTransactionDao;
import com.bank.service.fakes.FakeUserDao;
import com.bank.servlet.customer.CustomerAccountsServlet;
import com.bank.servlet.customer.CustomerDepositServlet;
import com.bank.servlet.customer.CustomerHistoryServlet;
import com.bank.servlet.customer.CustomerTransferServlet;
import com.bank.servlet.customer.CustomerWithdrawServlet;
import com.bank.util.FlashMessage;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Customer Portal Servlets Unit & Integration Tests")
class CustomerServletsTest extends BaseServiceTest {

    private FakeAccountDao fakeAccountDao;
    private FakeTransactionDao fakeTransactionDao;
    private FakeUserDao fakeUserDao;
    private FakeSettingsProvider fakeSettingsProvider;
    private AccountService accountService;
    private TransferService transferService;

    private static final Long CUST1_ID = 1L;
    private static final Long CUST2_ID = 2L;
    private static final String ACC1_NO = "100000000001";
    private static final String ACC2_NO = "100000000002";

    @BeforeEach
    void setUpCustomerContext() {
        fakeAccountDao = new FakeAccountDao();
        fakeTransactionDao = new FakeTransactionDao();
        fakeUserDao = new FakeUserDao();
        fakeSettingsProvider = new FakeSettingsProvider();

        fakeSettingsProvider.put("savings.min_balance", "500.00");
        fakeSettingsProvider.put("transfer.per_txn_limit", "50000.00");
        fakeSettingsProvider.put("transfer.daily_limit", "100000.00");

        accountService = new AccountService(fakeAccountDao, fakeTransactionDao, fakeSettingsProvider);
        transferService = new TransferService(
                fakeAccountDao, fakeTransactionDao, fakeUserDao, fakeSettingsProvider,
                LockManager.getInstance(), new NoOpFraudChecker());

        // Create Users
        Customer user1 = new Customer();
        user1.setUserId(CUST1_ID);
        user1.setFullName("Alice Customer");
        user1.setEmail("alice@bank.com");
        fakeUserDao.save(user1);

        Customer user2 = new Customer();
        user2.setUserId(CUST2_ID);
        user2.setFullName("Bob Recipient");
        user2.setEmail("bob@bank.com");
        fakeUserDao.save(user2);

        // Create Initial Accounts
        Account acc1 = AccountFactory.createSavingsAccount(ACC1_NO, CUST1_ID, new BigDecimal("5000.00"), new BigDecimal("500.00"));
        fakeAccountDao.save(acc1);

        Account acc2 = AccountFactory.createSavingsAccount(ACC2_NO, CUST2_ID, new BigDecimal("1000.00"), new BigDecimal("500.00"));
        fakeAccountDao.save(acc2);
    }

    @Test
    @DisplayName("CustomerAccountsServlet: GET forwards to accounts JSP and lists accounts")
    void testAccountsServletGet() throws Exception {
        CustomerAccountsServlet servlet = new CustomerAccountsServlet(accountService);

        Map<String, Object> reqAttrs = new HashMap<>();
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("user", new SessionUser(CUST1_ID, "Alice Customer", Role.CUSTOMER));

        HttpServletRequest request = createRequestProxy(reqAttrs, Map.of(), sessionAttrs);
        HttpServletResponse response = createResponseProxy(new HashMap<>());

        servlet.doGet(request, response);

        assertEquals("/WEB-INF/views/customer/accounts.jsp", reqAttrs.get("forwardedView"));
        assertNotNull(reqAttrs.get("accounts"));
    }

    @Test
    @DisplayName("CustomerAccountsServlet: POST opens a new savings account with PRG redirect")
    void testAccountsServletPostSuccess() throws Exception {
        CustomerAccountsServlet servlet = new CustomerAccountsServlet(accountService);

        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("user", new SessionUser(CUST1_ID, "Alice Customer", Role.CUSTOMER));

        Map<String, String> params = Map.of(
                "accountType", "SAVINGS",
                "initialDeposit", "1500.00"
        );

        Map<String, Object> respState = new HashMap<>();
        HttpServletRequest request = createRequestProxy(new HashMap<>(), params, sessionAttrs);
        HttpServletResponse response = createResponseProxy(respState);

        servlet.doPost(request, response);

        assertEquals("/context/customer/accounts", respState.get("redirectUrl"));
        FlashMessage flash = (FlashMessage) sessionAttrs.get(FlashMessage.SESSION_KEY);
        assertNotNull(flash);
        assertTrue(flash.isSuccess());
        assertTrue(flash.getMessage().contains("Successfully opened new SAVINGS account"));
    }

    @Test
    @DisplayName("CustomerDepositServlet: POST deposits money and increments account balance")
    void testDepositServletPost() throws Exception {
        CustomerDepositServlet servlet = new CustomerDepositServlet(accountService);

        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("user", new SessionUser(CUST1_ID, "Alice Customer", Role.CUSTOMER));

        Map<String, String> params = Map.of(
                "accountNo", ACC1_NO,
                "amount", "1000.00",
                "remarks", "Counter Cash Deposit"
        );

        Map<String, Object> respState = new HashMap<>();
        HttpServletRequest request = createRequestProxy(new HashMap<>(), params, sessionAttrs);
        HttpServletResponse response = createResponseProxy(respState);

        servlet.doPost(request, response);

        assertEquals("/context/customer/deposit", respState.get("redirectUrl"));
        FlashMessage flash = (FlashMessage) sessionAttrs.get(FlashMessage.SESSION_KEY);
        assertNotNull(flash);
        assertTrue(flash.isSuccess());

        Account updated = fakeAccountDao.findById(ACC1_NO).orElseThrow();
        assertEquals(new BigDecimal("6000.00"), updated.getBalance());
    }

    @Test
    @DisplayName("CustomerWithdrawServlet: POST withdraws money and validates available balance")
    void testWithdrawServletPost() throws Exception {
        CustomerWithdrawServlet servlet = new CustomerWithdrawServlet(accountService);

        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("user", new SessionUser(CUST1_ID, "Alice Customer", Role.CUSTOMER));

        Map<String, String> params = Map.of(
                "accountNo", ACC1_NO,
                "amount", "1000.00",
                "remarks", "ATM Cash"
        );

        Map<String, Object> respState = new HashMap<>();
        HttpServletRequest request = createRequestProxy(new HashMap<>(), params, sessionAttrs);
        HttpServletResponse response = createResponseProxy(respState);

        servlet.doPost(request, response);

        assertEquals("/context/customer/withdraw", respState.get("redirectUrl"));
        FlashMessage flash = (FlashMessage) sessionAttrs.get(FlashMessage.SESSION_KEY);
        assertNotNull(flash);
        assertTrue(flash.isSuccess());

        Account updated = fakeAccountDao.findById(ACC1_NO).orElseThrow();
        assertEquals(new BigDecimal("4000.00"), updated.getBalance());
    }

    @Test
    @DisplayName("CustomerWithdrawServlet: POST exceeding available balance triggers flash error")
    void testWithdrawServletInsufficientFunds() throws Exception {
        CustomerWithdrawServlet servlet = new CustomerWithdrawServlet(accountService);

        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("user", new SessionUser(CUST1_ID, "Alice Customer", Role.CUSTOMER));

        // Available balance is 5000 - 500 (min balance) = 4500. Attempt 4800.
        Map<String, String> params = Map.of(
                "accountNo", ACC1_NO,
                "amount", "4800.00",
                "remarks", "Excessive withdrawal"
        );

        Map<String, Object> respState = new HashMap<>();
        HttpServletRequest request = createRequestProxy(new HashMap<>(), params, sessionAttrs);
        HttpServletResponse response = createResponseProxy(respState);

        servlet.doPost(request, response);

        assertEquals("/context/customer/withdraw", respState.get("redirectUrl"));
        FlashMessage flash = (FlashMessage) sessionAttrs.get(FlashMessage.SESSION_KEY);
        assertNotNull(flash);
        assertTrue(flash.isDanger());
    }

    @Test
    @DisplayName("CustomerTransferServlet: Complete 3-step transfer workflow with double-submission blocking")
    void testTransferServletThreeStepAndTokenProtection() throws Exception {
        CustomerTransferServlet servlet = new CustomerTransferServlet(accountService, transferService);

        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("user", new SessionUser(CUST1_ID, "Alice Customer", Role.CUSTOMER));

        // STEP 1: POST form to review and stage transfer
        Map<String, String> step1Params = Map.of(
                "fromAccount", ACC1_NO,
                "toAccount", ACC2_NO,
                "amount", "1200.00",
                "remarks", "Project fee"
        );
        Map<String, Object> resp1 = new HashMap<>();
        HttpServletRequest req1 = createRequestProxy(new HashMap<>(), step1Params, sessionAttrs);
        HttpServletResponse res1 = createResponseProxy(resp1);

        servlet.doPost(req1, res1);

        assertEquals("/context/customer/transfer?step=confirm", resp1.get("redirectUrl"));
        TransferDraft draft = (TransferDraft) sessionAttrs.get(TransferDraft.SESSION_KEY);
        assertNotNull(draft, "Pending transfer draft must be staged in session");
        assertEquals("Bob Recipient", draft.getRecipientName());
        assertNotNull(draft.getToken(), "One-time session token must be present");
        String originalToken = draft.getToken();

        // STEP 2: GET confirm view
        Map<String, Object> req2Attrs = new HashMap<>();
        HttpServletRequest req2 = createRequestProxy(req2Attrs, Map.of("step", "confirm"), sessionAttrs);
        HttpServletResponse res2 = createResponseProxy(new HashMap<>());

        servlet.doGet(req2, res2);
        assertEquals("/WEB-INF/views/customer/transfer-confirm.jsp", req2Attrs.get("forwardedView"));
        assertEquals(draft, req2Attrs.get("draft"));

        // STEP 3: POST execute with valid token
        Map<String, String> step3Params = Map.of(
                "action", "execute",
                "token", originalToken
        );
        Map<String, Object> resp3 = new HashMap<>();
        HttpServletRequest req3 = createRequestProxy(new HashMap<>(), step3Params, sessionAttrs);
        HttpServletResponse res3 = createResponseProxy(resp3);

        servlet.doPost(req3, res3);

        assertEquals("/context/customer/transfer?step=receipt", resp3.get("redirectUrl"));
        assertNull(sessionAttrs.get(TransferDraft.SESSION_KEY), "Token and draft must be consumed from session");
        TransferReceipt receipt = (TransferReceipt) sessionAttrs.get(TransferReceipt.SESSION_KEY);
        assertNotNull(receipt, "Receipt must be available in session");
        assertEquals(new BigDecimal("1200.00"), receipt.getAmount());

        // Verify balances after transfer
        Account debited = fakeAccountDao.findById(ACC1_NO).orElseThrow();
        Account credited = fakeAccountDao.findById(ACC2_NO).orElseThrow();
        assertEquals(new BigDecimal("3800.00"), debited.getBalance());
        assertEquals(new BigDecimal("2200.00"), credited.getBalance());

        // DOUBLE-SUBMISSION DEFENSE: Re-submitting the same token must be blocked!
        Map<String, Object> resp4 = new HashMap<>();
        HttpServletRequest req4 = createRequestProxy(new HashMap<>(), step3Params, sessionAttrs);
        HttpServletResponse res4 = createResponseProxy(resp4);

        servlet.doPost(req4, res4);

        assertEquals("/context/customer/transfer", resp4.get("redirectUrl"));
        FlashMessage duplicateFlash = (FlashMessage) sessionAttrs.get(FlashMessage.SESSION_KEY);
        assertNotNull(duplicateFlash);
        assertTrue(duplicateFlash.isDanger());
        assertTrue(duplicateFlash.getMessage().contains("already been submitted") || duplicateFlash.getMessage().contains("expired"));

        // Balances must remain unchanged
        assertEquals(new BigDecimal("3800.00"), debited.getBalance());
    }

    @Test
    @DisplayName("CustomerHistoryServlet: GET parses filters and populates paginated transactions")
    void testHistoryServletGet() throws Exception {
        CustomerHistoryServlet servlet = new CustomerHistoryServlet(accountService);

        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("user", new SessionUser(CUST1_ID, "Alice Customer", Role.CUSTOMER));

        Map<String, Object> reqAttrs = new HashMap<>();
        Map<String, String> params = Map.of(
                "accountNo", ACC1_NO,
                "page", "1",
                "size", "10"
        );

        HttpServletRequest request = createRequestProxy(reqAttrs, params, sessionAttrs);
        HttpServletResponse response = createResponseProxy(new HashMap<>());

        servlet.doGet(request, response);

        assertEquals("/WEB-INF/views/customer/transactions.jsp", reqAttrs.get("forwardedView"));
        assertNotNull(reqAttrs.get("pagedTxns"));
        assertEquals(ACC1_NO, reqAttrs.get("selectedAccountNo"));
    }

    // Helper Dynamic Proxies
    private HttpServletRequest createRequestProxy(Map<String, Object> reqAttrs, Map<String, String> params, Map<String, Object> sessionAttrs) {
        AtomicBoolean invalidated = new AtomicBoolean(false);
        HttpSession session = createSessionProxy(sessionAttrs, invalidated);

        RequestDispatcher dispatcher = (RequestDispatcher) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{RequestDispatcher.class},
                (proxy, method, args) -> {
                    if ("forward".equals(method.getName())) {
                        return null;
                    }
                    return null;
                }
        );

        return (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContextPath" -> "/context";
                    case "getParameter" -> params.get((String) args[0]);
                    case "getSession" -> session;
                    case "setAttribute" -> {
                        reqAttrs.put((String) args[0], args[1]);
                        yield null;
                    }
                    case "getAttribute" -> reqAttrs.get((String) args[0]);
                    case "getRequestDispatcher" -> {
                        reqAttrs.put("forwardedView", args[0]);
                        yield dispatcher;
                    }
                    default -> null;
                }
        );
    }

    private HttpServletResponse createResponseProxy(Map<String, Object> respState) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "sendRedirect" -> {
                        respState.put("redirectUrl", args[0]);
                        yield null;
                    }
                    default -> null;
                }
        );
    }

    private HttpSession createSessionProxy(Map<String, Object> attributes, AtomicBoolean invalidated) {
        return (HttpSession) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setAttribute" -> {
                        attributes.put((String) args[0], args[1]);
                        yield null;
                    }
                    case "getAttribute" -> attributes.get((String) args[0]);
                    case "removeAttribute" -> attributes.remove((String) args[0]);
                    case "invalidate" -> {
                        invalidated.set(true);
                        attributes.clear();
                        yield null;
                    }
                    default -> null;
                }
        );
    }
}

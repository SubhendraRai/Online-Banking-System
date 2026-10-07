package com.bank.servlet.customer;

import com.bank.dao.TxnFilter;
import com.bank.exception.BankingException;
import com.bank.model.Account;
import com.bank.model.SessionUser;
import com.bank.model.Transaction;
import com.bank.model.TxnType;
import com.bank.service.AccountService;
import com.bank.util.ErrorMessageResolver;
import com.bank.util.FlashMessage;
import com.bank.util.Page;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customer servlet managing transaction history filtering, search, and pagination.
 * <p>
 * Supports filtering by account, date range, transaction type, amount bounds, and keyword search.
 * Leverages {@link Page} to render structured newest-first ledger items with page controls.
 * </p>
 */
@WebServlet(name = "CustomerHistoryServlet", urlPatterns = {"/customer/transactions", "/customer/history"})
public class CustomerHistoryServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerHistoryServlet.class);

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;

    private final AccountService accountService;

    public CustomerHistoryServlet() {
        this(new AccountService());
    }

    public CustomerHistoryServlet(AccountService accountService) {
        this.accountService = Objects.requireNonNull(accountService, "accountService cannot be null");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof SessionUser user)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        List<Account> accounts = accountService.getAccountsForUser(user.getId());
        if (accounts.isEmpty()) {
            request.setAttribute("accounts", List.of());
            request.setAttribute("pagedTxns", Page.<Transaction>empty(1, DEFAULT_SIZE));
            request.getRequestDispatcher("/WEB-INF/views/customer/transactions.jsp").forward(request, response);
            return;
        }

        String accountNoParam = request.getParameter("accountNo");
        String selectedAccountNo = (accountNoParam != null && !accountNoParam.isBlank())
                ? accountNoParam.trim() : accounts.get(0).getAccountNo();

        // Server-side ownership validation
        try {
            accountService.getUserAccount(selectedAccountNo, user.getId());
        } catch (BankingException be) {
            LOGGER.warn("Unauthorized attempt to view statement for account {} by user {}", selectedAccountNo, user.getId());
            FlashMessage.error(session, "Selected account does not belong to your profile.");
            selectedAccountNo = accounts.get(0).getAccountNo();
        }

        TxnFilter filter = buildFilter(request);
        int page = parsePositiveInt(request.getParameter("page"), DEFAULT_PAGE);
        int size = parsePositiveInt(request.getParameter("size"), DEFAULT_SIZE);

        try {
            Page<Transaction> pagedTxns = accountService.getStatement(selectedAccountNo, filter, page, size);
            request.setAttribute("accounts", accounts);
            request.setAttribute("selectedAccountNo", selectedAccountNo);
            request.setAttribute("filter", filter);
            request.setAttribute("pagedTxns", pagedTxns);
        } catch (BankingException be) {
            LOGGER.error("Failed loading transaction history for account {}", selectedAccountNo, be);
            FlashMessage.error(session, ErrorMessageResolver.resolve(be));
            request.setAttribute("pagedTxns", Page.<Transaction>empty(page, size));
        }

        request.getRequestDispatcher("/WEB-INF/views/customer/transactions.jsp").forward(request, response);
    }

    private TxnFilter buildFilter(HttpServletRequest request) {
        LocalDate fromDate = parseDate(request.getParameter("fromDate"));
        LocalDate toDate = parseDate(request.getParameter("toDate"));
        TxnType type = parseTxnType(request.getParameter("type"));
        BigDecimal minAmount = parseAmount(request.getParameter("minAmount"));
        BigDecimal maxAmount = parseAmount(request.getParameter("maxAmount"));
        String keyword = request.getParameter("keyword");

        return TxnFilter.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .type(type)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .keyword(keyword)
                .build();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private TxnType parseTxnType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) {
            return null;
        }
        try {
            return TxnType.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) {
            return null;
        }
        try {
            BigDecimal bd = new BigDecimal(amountStr.trim());
            return bd.compareTo(BigDecimal.ZERO) >= 0 ? bd : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parsePositiveInt(String intStr, int defaultValue) {
        if (intStr == null || intStr.isBlank()) {
            return defaultValue;
        }
        try {
            int val = Integer.parseInt(intStr.trim());
            return Math.max(1, val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

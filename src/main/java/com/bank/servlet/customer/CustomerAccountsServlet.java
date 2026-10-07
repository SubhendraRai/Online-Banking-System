package com.bank.servlet.customer;

import com.bank.exception.BankingException;
import com.bank.model.Account;
import com.bank.model.AccountType;
import com.bank.model.SessionUser;
import com.bank.service.AccountService;
import com.bank.util.ErrorMessageResolver;
import com.bank.util.FlashMessage;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customer servlet managing bank accounts portfolio listing and opening new accounts.
 * <p>
 * Supports creating new Savings or Current accounts with server-side validation,
 * transactional persistence, and Post/Redirect/Get feedback messages.
 * </p>
 */
@WebServlet(name = "CustomerAccountsServlet", urlPatterns = {"/customer/accounts"})
public class CustomerAccountsServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerAccountsServlet.class);

    private final AccountService accountService;

    public CustomerAccountsServlet() {
        this(new AccountService());
    }

    public CustomerAccountsServlet(AccountService accountService) {
        this.accountService = Objects.requireNonNull(accountService, "accountService cannot be null");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") instanceof SessionUser user) {
            List<Account> accounts = accountService.getAccountsForUser(user.getId());
            request.setAttribute("accounts", accounts);
        }

        request.getRequestDispatcher("/WEB-INF/views/customer/accounts.jsp").forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof SessionUser user)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String typeStr = request.getParameter("accountType");
        String depositStr = request.getParameter("initialDeposit");

        try {
            AccountType type = parseAccountType(typeStr);
            BigDecimal deposit = parseDepositAmount(depositStr);

            Account account = accountService.openAccount(user.getId(), type, deposit);
            LOGGER.info("Customer ID {} opened {} account {}", user.getId(), type, account.getAccountNo());

            FlashMessage.success(session, "Successfully opened new " + type + " account: " + account.getAccountNo());
        } catch (BankingException be) {
            LOGGER.warn("Account opening failed for customer ID {}: {}", user.getId(), be.getMessage());
            FlashMessage.error(session, ErrorMessageResolver.resolve(be));
        } catch (IllegalArgumentException iae) {
            LOGGER.warn("Invalid input opening account for customer ID {}: {}", user.getId(), iae.getMessage());
            FlashMessage.error(session, iae.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error opening account for customer ID {}", user.getId(), e);
            FlashMessage.error(session, ErrorMessageResolver.resolve(e));
        }

        response.sendRedirect(request.getContextPath() + "/customer/accounts");
    }

    private AccountType parseAccountType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) {
            throw new IllegalArgumentException("Please choose an account type (Savings or Current).");
        }
        try {
            return AccountType.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid account type selected: " + typeStr);
        }
    }

    private BigDecimal parseDepositAmount(String depositStr) {
        if (depositStr == null || depositStr.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal amount = new BigDecimal(depositStr.trim());
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Initial deposit cannot be negative.");
            }
            return amount;
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Please enter a valid numeric opening deposit amount.");
        }
    }
}

package com.bank.servlet.customer;

import com.bank.exception.BankingException;
import com.bank.model.Account;
import com.bank.model.SessionUser;
import com.bank.model.Transaction;
import com.bank.service.AccountService;
import com.bank.util.ErrorMessageResolver;
import com.bank.util.FlashMessage;
import com.bank.util.Money;
import com.bank.util.Validator;
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
 * Customer servlet managing cash and debit withdrawals.
 * <p>
 * Enforces ownership verification on the debited account, verifies minimum balance and
 * overdraft constraints polymorphically, and coordinates transaction logging via {@link AccountService#withdraw(String, BigDecimal, String)}.
 * </p>
 */
@WebServlet(name = "CustomerWithdrawServlet", urlPatterns = {"/customer/withdraw"})
public class CustomerWithdrawServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerWithdrawServlet.class);

    private final AccountService accountService;

    public CustomerWithdrawServlet() {
        this(new AccountService());
    }

    public CustomerWithdrawServlet(AccountService accountService) {
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

        request.getRequestDispatcher("/WEB-INF/views/customer/withdraw.jsp").forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof SessionUser user)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String accountNo = request.getParameter("accountNo");
        String amountStr = request.getParameter("amount");
        String remarks = request.getParameter("remarks");

        try {
            if (accountNo == null || accountNo.isBlank()) {
                throw new IllegalArgumentException("Please select an account for withdrawal.");
            }

            // Verify account ownership
            Account account = accountService.getUserAccount(accountNo.trim(), user.getId());

            BigDecimal amount = parseAmount(amountStr);

            // Server-side balance check for fast user feedback
            if (amount.compareTo(account.getAvailableBalance()) > 0) {
                throw new IllegalArgumentException(String.format(
                        "Withdrawal amount %s exceeds available balance %s.",
                        Money.formatInr(amount), Money.formatInr(account.getAvailableBalance())));
            }

            String cleanRemarks = Validator.validateRemarks(remarks);
            Transaction txn = accountService.withdraw(accountNo.trim(), amount, cleanRemarks);

            LOGGER.info("Withdrawal of {} from account {} succeeded for customer ID {}",
                    Money.formatInr(amount), accountNo, user.getId());
            FlashMessage.success(session, "Successfully withdrew " + Money.formatInr(amount)
                    + " from account " + accountNo + ". Reference Txn ID: #" + txn.getTxnId());
        } catch (BankingException be) {
            LOGGER.warn("Withdrawal failed for customer ID {}: {}", user.getId(), be.getMessage());
            FlashMessage.error(session, ErrorMessageResolver.resolve(be));
        } catch (IllegalArgumentException iae) {
            LOGGER.warn("Invalid withdrawal request from customer ID {}: {}", user.getId(), iae.getMessage());
            FlashMessage.error(session, iae.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error during withdrawal for customer ID {}", user.getId(), e);
            FlashMessage.error(session, ErrorMessageResolver.resolve(e));
        }

        response.sendRedirect(request.getContextPath() + "/customer/withdraw");
    }

    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) {
            throw new IllegalArgumentException("Withdrawal amount cannot be empty.");
        }
        try {
            BigDecimal amount = new BigDecimal(amountStr.trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Withdrawal amount must be strictly greater than 0.00.");
            }
            if (amount.scale() > 2) {
                throw new IllegalArgumentException("Withdrawal amount cannot exceed 2 decimal places.");
            }
            return amount;
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Please enter a valid numeric monetary amount (e.g., 500.00).");
        }
    }
}

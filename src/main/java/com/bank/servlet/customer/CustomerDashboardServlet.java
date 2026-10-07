package com.bank.servlet.customer;

import com.bank.model.DashboardSummary;
import com.bank.model.SessionUser;
import com.bank.service.AccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customer dashboard servlet loading portfolio balances and recent activity.
 * <p>
 * Fetches aggregated accounts and ledger transactions via {@link AccountService#getDashboardSummary(Long)}.
 * </p>
 */
@WebServlet(name = "CustomerDashboardServlet", urlPatterns = {"/customer/dashboard"})
public class CustomerDashboardServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerDashboardServlet.class);

    private final AccountService accountService;

    public CustomerDashboardServlet() {
        this(new AccountService());
    }

    public CustomerDashboardServlet(AccountService accountService) {
        this.accountService = Objects.requireNonNull(accountService, "accountService cannot be null");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") instanceof SessionUser user) {
            try {
                DashboardSummary summary = accountService.getDashboardSummary(user.getId());
                request.setAttribute("summary", summary);
            } catch (Exception e) {
                LOGGER.warn("Could not load dashboard summary for customer ID {}: {}", user.getId(), e.getMessage());
            }
        }

        request.getRequestDispatcher("/WEB-INF/views/customer/dashboard.jsp").forward(request, response);
    }
}

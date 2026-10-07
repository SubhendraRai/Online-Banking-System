package com.bank.servlet.auth;

import com.bank.exception.BankingException;
import com.bank.exception.DuplicateEmailException;
import com.bank.exception.ValidationException;
import com.bank.model.Customer;
import com.bank.model.SessionUser;
import com.bank.service.AuthService;
import com.bank.util.FlashMessage;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customer registration servlet provisioning new user credentials and default accounts.
 * <p>
 * Implements strict Post/Redirect/Get flow with flash messaging, input validation,
 * session rotation defense, and non-sensitive form state retention upon error.
 * </p>
 */
@WebServlet(name = "RegisterServlet", urlPatterns = {"/register"})
public class RegisterServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterServlet.class);

    private final AuthService authService;

    public RegisterServlet() {
        this(new AuthService());
    }

    public RegisterServlet(AuthService authService) {
        this.authService = Objects.requireNonNull(authService, "authService cannot be null");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") instanceof SessionUser user) {
            String redirectUrl = user.isAdmin() ? "/admin/dashboard" : "/customer/dashboard";
            response.sendRedirect(request.getContextPath() + redirectUrl);
            return;
        }

        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String initialDepositStr = request.getParameter("initialDeposit");

        preserveFormInputs(request, fullName, email, phone, address, initialDepositStr);

        if (password == null || !password.equals(confirmPassword)) {
            redirectWithError(request, response, "Passwords do not match. Please verify.");
            return;
        }

        BigDecimal initialDeposit = parseDeposit(initialDepositStr, request, response);
        if (initialDeposit == null) {
            return;
        }

        try {
            Customer customer = authService.register(fullName, email, phone, address, password, initialDeposit);

            // Rotate session ID on registration to guard against session fixation
            request.changeSessionId();

            HttpSession session = request.getSession(true);
            clearPreservedFormInputs(session);
            FlashMessage.success(session, "Account registered successfully! Please sign in with your email.");
            session.setAttribute("savedEmail", customer.getEmail());
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (ValidationException | DuplicateEmailException e) {
            redirectWithError(request, response, e.getMessage());
        } catch (BankingException e) {
            LOGGER.error("Banking failure registering customer: {}", email, e);
            redirectWithError(request, response, "Registration failed: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected failure registering customer: {}", email, e);
            redirectWithError(request, response, "An unexpected system error occurred during registration.");
        }
    }

    private BigDecimal parseDeposit(String raw, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal amount = new BigDecimal(raw.trim());
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                redirectWithError(req, resp, "Initial deposit amount cannot be negative.");
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            redirectWithError(req, resp, "Invalid initial deposit format. Please enter a valid number.");
            return null;
        }
    }

    private void preserveFormInputs(HttpServletRequest req, String name, String email,
                                    String phone, String address, String deposit) {
        HttpSession session = req.getSession(true);
        if (name != null) session.setAttribute("savedFullName", name.trim());
        if (email != null) session.setAttribute("savedEmail", email.trim());
        if (phone != null) session.setAttribute("savedPhone", phone.trim());
        if (address != null) session.setAttribute("savedAddress", address.trim());
        if (deposit != null) session.setAttribute("savedDeposit", deposit.trim());
    }

    private void clearPreservedFormInputs(HttpSession session) {
        if (session != null) {
            session.removeAttribute("savedFullName");
            session.removeAttribute("savedPhone");
            session.removeAttribute("savedAddress");
            session.removeAttribute("savedDeposit");
        }
    }

    private void redirectWithError(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
        HttpSession session = req.getSession(true);
        FlashMessage.error(session, msg);
        resp.sendRedirect(req.getContextPath() + "/register");
    }
}

package com.bank.servlet.auth;

import com.bank.exception.AuthenticationException;
import com.bank.exception.ValidationException;
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
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication servlet managing user sign-in workflows and session initiation.
 * <p>
 * Enforces session fixation defense via {@link HttpServletRequest#changeSessionId()},
 * adheres strictly to Post/Redirect/Get (PRG), and assigns authenticated session principals.
 * </p>
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginServlet.class);

    private final AuthService authService;

    public LoginServlet() {
        this(new AuthService());
    }

    public LoginServlet(AuthService authService) {
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

        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            SessionUser sessionUser = authService.login(email, password);

            // Defend against session fixation attack by rotating the session identifier
            request.changeSessionId();

            HttpSession session = request.getSession(true);
            session.setAttribute("user", sessionUser);
            FlashMessage.success(session, "Welcome back, " + sessionUser.getName() + "!");

            String target = sessionUser.isAdmin() ? "/admin/dashboard" : "/customer/dashboard";
            response.sendRedirect(request.getContextPath() + target);
        } catch (ValidationException | AuthenticationException e) {
            handleLoginError(request, response, email, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error during login attempt for email: {}", email, e);
            handleLoginError(request, response, email, "An unexpected system error occurred. Please try again.");
        }
    }

    private void handleLoginError(HttpServletRequest request, HttpServletResponse response,
                                  String email, String errorMessage) throws IOException {
        HttpSession session = request.getSession(true);
        FlashMessage.error(session, errorMessage);
        if (email != null && !email.isBlank()) {
            session.setAttribute("savedEmail", email.trim());
        }
        response.sendRedirect(request.getContextPath() + "/login");
    }
}

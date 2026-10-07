package com.bank.filter;

import com.bank.model.SessionUser;
import com.bank.util.FlashMessage;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Gatekeeper authentication filter verifying active session credentials.
 * <p>
 * Permits access to public endpoints (login, registration, static assets).
 * Redirects unauthenticated requests to {@code /login} with an informative flash message.
 * </p>
 */
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getServletPath();
        if (httpRequest.getPathInfo() != null) {
            path += httpRequest.getPathInfo();
        }

        if (isPublicResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        SessionUser user = (session != null) ? (SessionUser) session.getAttribute("user") : null;

        if (user != null) {
            chain.doFilter(request, response);
            return;
        }

        FlashMessage.error(httpRequest.getSession(true), "Please sign in to access that page.");
        httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
    }

    private boolean isPublicResource(String path) {
        if (path == null || path.isEmpty() || "/".equals(path) || "/index.jsp".equals(path)) {
            return true;
        }
        return path.equals("/login")
                || path.startsWith("/login/")
                || path.equals("/register")
                || path.startsWith("/register/")
                || path.equals("/logout")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || "/favicon.ico".equals(path)
                || "/hello".equals(path);
    }
}

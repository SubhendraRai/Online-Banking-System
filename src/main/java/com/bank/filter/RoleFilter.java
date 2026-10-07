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
 * Role-based authorization filter enforcing strict separation between Customer and Admin domains.
 * <p>
 * Requests to {@code /admin} or {@code /admin/*} mandate {@link com.bank.model.Role#ADMIN}; otherwise customer
 * users are redirected to their portal. Requests to {@code /customer} or {@code /customer/*} mandate
 * {@link com.bank.model.Role#CUSTOMER}; administrative users attempting customer operations
 * are redirected to the admin console. Exact matches for root portal paths seamlessly redirect
 * to their corresponding dashboard views.
 * </p>
 */
public class RoleFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getServletPath();
        if (httpRequest.getPathInfo() != null) {
            path += httpRequest.getPathInfo();
        }

        HttpSession session = httpRequest.getSession(false);
        SessionUser user = (session != null) ? (SessionUser) session.getAttribute("user") : null;

        if (path.startsWith("/admin")) {
            if (user == null) {
                FlashMessage.error(httpRequest.getSession(true), "Please sign in to access the administrator portal.");
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
                return;
            }
            if (!user.isAdmin()) {
                FlashMessage.error(httpRequest.getSession(), "Access denied: Administrator privileges required.");
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/customer/dashboard");
                return;
            }
            if ("/admin".equals(path) || "/admin/".equals(path)) {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/admin/dashboard");
                return;
            }
        } else if (path.startsWith("/customer")) {
            if (user == null) {
                FlashMessage.error(httpRequest.getSession(true), "Please sign in to access your customer account.");
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
                return;
            }
            if (!user.isCustomer()) {
                FlashMessage.error(httpRequest.getSession(), "Access denied: Customer privileges required.");
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/admin/dashboard");
                return;
            }
            if ("/customer".equals(path) || "/customer/".equals(path)) {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/customer/dashboard");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}

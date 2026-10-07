package com.bank.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter preventing downstream caching of authenticated or sensitive banking pages.
 * <p>
 * Emits HTTP 1.1 {@code Cache-Control: no-cache, no-store, must-revalidate}, HTTP 1.0 {@code Pragma: no-cache},
 * and legacy proxy {@code Expires: 0} headers. Prevents back-button information leakage after logout.
 * </p>
 */
public class NoCacheFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setDateHeader("Expires", 0);
        }

        chain.doFilter(request, response);
    }
}

package com.bank.filter;

import com.bank.util.FlashMessage;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Filter ensuring uniform UTF-8 character encoding across all incoming HTTP requests
 * and outgoing responses.
 * <p>
 * Additionally bridges session-scoped flash messages into the request scope for one-time
 * rendering in target JSP views following Post/Redirect/Get workflows.
 * </p>
 */
public class EncodingFilter implements Filter {

    private static final String ENCODING = "UTF-8";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding(ENCODING);
        response.setCharacterEncoding(ENCODING);

        if (request instanceof HttpServletRequest httpRequest) {
            FlashMessage.transfer(httpRequest);
        }

        chain.doFilter(request, response);
    }
}

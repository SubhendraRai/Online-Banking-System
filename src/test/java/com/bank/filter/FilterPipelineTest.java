package com.bank.filter;

import com.bank.model.Role;
import com.bank.model.SessionUser;
import com.bank.util.FlashMessage;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Filter Pipeline Unit Tests")
class FilterPipelineTest {

    @Test
    @DisplayName("EncodingFilter sets UTF-8 encoding on request and response")
    void testEncodingFilter() throws Exception {
        EncodingFilter filter = new EncodingFilter();

        Map<String, Object> reqState = new HashMap<>();
        Map<String, Object> respState = new HashMap<>();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        HttpServletRequest request = createRequestProxy("/login", reqState, null);
        HttpServletResponse response = createResponseProxy(respState);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertEquals("UTF-8", reqState.get("characterEncoding"));
        assertEquals("UTF-8", respState.get("characterEncoding"));
        assertTrue(chainCalled.get());
    }

    @Test
    @DisplayName("NoCacheFilter sets cache prevention headers")
    void testNoCacheFilter() throws Exception {
        NoCacheFilter filter = new NoCacheFilter();

        Map<String, Object> respHeaders = new HashMap<>();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        HttpServletRequest request = createRequestProxy("/customer/dashboard", new HashMap<>(), null);
        HttpServletResponse response = createResponseProxy(respHeaders);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertEquals("no-cache, no-store, must-revalidate", respHeaders.get("Cache-Control"));
        assertEquals("no-cache", respHeaders.get("Pragma"));
        assertEquals(0L, respHeaders.get("Expires"));
        assertTrue(chainCalled.get());
    }

    @Test
    @DisplayName("AuthFilter permits public resources without session")
    void testAuthFilterAllowsPublicPaths() throws Exception {
        AuthFilter filter = new AuthFilter();

        for (String path : new String[]{"/login", "/register", "/css/style.css", "/js/app.js", "/", "/index.jsp"}) {
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            HttpServletRequest request = createRequestProxy(path, new HashMap<>(), null);
            HttpServletResponse response = createResponseProxy(new HashMap<>());
            FilterChain chain = (req, resp) -> chainCalled.set(true);

            filter.doFilter(request, response, chain);
            assertTrue(chainCalled.get(), "Public path should pass through: " + path);
        }
    }

    @Test
    @DisplayName("AuthFilter redirects unauthenticated user on protected route")
    void testAuthFilterRedirectsUnauthenticated() throws Exception {
        AuthFilter filter = new AuthFilter();

        Map<String, Object> sessionState = new HashMap<>();
        HttpSession session = createSessionProxy(sessionState);
        Map<String, Object> respState = new HashMap<>();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        HttpServletRequest request = createRequestProxy("/customer/dashboard", new HashMap<>(), session);
        HttpServletResponse response = createResponseProxy(respState);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertFalse(chainCalled.get());
        assertEquals("/context/login", respState.get("redirectUrl"));
        assertNotNull(sessionState.get(FlashMessage.SESSION_KEY));
    }

    @Test
    @DisplayName("RoleFilter prevents Customer from accessing /admin endpoints")
    void testRoleFilterCustomerAccessingAdmin() throws Exception {
        RoleFilter filter = new RoleFilter();

        Map<String, Object> sessionState = new HashMap<>();
        SessionUser customerUser = new SessionUser(1L, "Alice Customer", Role.CUSTOMER);
        sessionState.put("user", customerUser);
        HttpSession session = createSessionProxy(sessionState);

        Map<String, Object> respState = new HashMap<>();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        HttpServletRequest request = createRequestProxy("/admin/users", new HashMap<>(), session);
        HttpServletResponse response = createResponseProxy(respState);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertFalse(chainCalled.get());
        assertEquals("/context/customer/dashboard", respState.get("redirectUrl"));
    }

    @Test
    @DisplayName("RoleFilter allows Admin to access /admin endpoints")
    void testRoleFilterAdminAccessingAdmin() throws Exception {
        RoleFilter filter = new RoleFilter();

        Map<String, Object> sessionState = new HashMap<>();
        SessionUser adminUser = new SessionUser(99L, "System Admin", Role.ADMIN);
        sessionState.put("user", adminUser);
        HttpSession session = createSessionProxy(sessionState);

        Map<String, Object> respState = new HashMap<>();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        HttpServletRequest request = createRequestProxy("/admin/dashboard", new HashMap<>(), session);
        HttpServletResponse response = createResponseProxy(respState);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(chainCalled.get());
        assertNull(respState.get("redirectUrl"));
    }

    private HttpServletRequest createRequestProxy(String servletPath, Map<String, Object> reqState, HttpSession session) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getServletPath" -> servletPath;
                    case "getPathInfo" -> null;
                    case "getContextPath" -> "/context";
                    case "setCharacterEncoding" -> {
                        reqState.put("characterEncoding", args[0]);
                        yield null;
                    }
                    case "getSession" -> session != null ? session : createSessionProxy(new HashMap<>());
                    case "setAttribute" -> {
                        reqState.put((String) args[0], args[1]);
                        yield null;
                    }
                    case "getAttribute" -> reqState.get((String) args[0]);
                    default -> null;
                }
        );
    }

    private HttpServletResponse createResponseProxy(Map<String, Object> respState) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setCharacterEncoding" -> {
                        respState.put("characterEncoding", args[0]);
                        yield null;
                    }
                    case "setHeader" -> {
                        respState.put((String) args[0], args[1]);
                        yield null;
                    }
                    case "setDateHeader" -> {
                        respState.put((String) args[0], args[1]);
                        yield null;
                    }
                    case "sendRedirect" -> {
                        respState.put("redirectUrl", args[0]);
                        yield null;
                    }
                    default -> null;
                }
        );
    }

    private HttpSession createSessionProxy(Map<String, Object> sessionState) {
        return (HttpSession) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setAttribute" -> {
                        sessionState.put((String) args[0], args[1]);
                        yield null;
                    }
                    case "getAttribute" -> sessionState.get((String) args[0]);
                    case "removeAttribute" -> sessionState.remove((String) args[0]);
                    default -> null;
                }
        );
    }
}

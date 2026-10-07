package com.bank.servlet;

import com.bank.model.Role;
import com.bank.model.SessionUser;
import com.bank.service.AuthService;
import com.bank.servlet.auth.LoginServlet;
import com.bank.servlet.auth.LogoutServlet;
import com.bank.servlet.auth.RegisterServlet;
import com.bank.util.FlashMessage;
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

@DisplayName("Authentication Servlets Unit Tests")
class AuthServletsTest {

    @Test
    @DisplayName("LogoutServlet invalidates session, creates flash message, and redirects to login")
    void testLogoutServlet() throws Exception {
        LogoutServlet servlet = new LogoutServlet();

        AtomicBoolean invalidated = new AtomicBoolean(false);
        Map<String, Object> newSessionAttributes = new HashMap<>();

        HttpSession activeSession = createSessionProxy(new HashMap<>(), invalidated);
        HttpSession freshSession = createSessionProxy(newSessionAttributes, new AtomicBoolean(false));

        Map<String, Object> respState = new HashMap<>();
        HttpServletRequest request = createRequestProxy(new HashMap<>(), activeSession, freshSession, new AtomicBoolean(false));
        HttpServletResponse response = createResponseProxy(respState);

        servlet.doGet(request, response);

        assertTrue(invalidated.get(), "Session must be invalidated on logout");
        assertEquals("/context/login", respState.get("redirectUrl"));
        assertNotNull(newSessionAttributes.get(FlashMessage.SESSION_KEY));
    }

    @Test
    @DisplayName("LoginServlet redirects already logged-in customer to customer dashboard on GET")
    void testLoginServletAlreadyLoggedIn() throws Exception {
        LoginServlet servlet = new LoginServlet();

        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("user", new SessionUser(10L, "Bob", Role.CUSTOMER));
        HttpSession session = createSessionProxy(sessionAttributes, new AtomicBoolean(false));

        Map<String, Object> respState = new HashMap<>();
        HttpServletRequest request = createRequestProxy(new HashMap<>(), session, session, new AtomicBoolean(false));
        HttpServletResponse response = createResponseProxy(respState);

        servlet.doGet(request, response);

        assertEquals("/context/customer/dashboard", respState.get("redirectUrl"));
    }

    @Test
    @DisplayName("RegisterServlet detects mismatched passwords and redirects with flash error")
    void testRegisterServletMismatchedPasswords() throws Exception {
        RegisterServlet servlet = new RegisterServlet();

        Map<String, Object> sessionAttributes = new HashMap<>();
        HttpSession session = createSessionProxy(sessionAttributes, new AtomicBoolean(false));

        Map<String, String> params = new HashMap<>();
        params.put("fullName", "Charlie");
        params.put("email", "charlie@test.com");
        params.put("phone", "1234567890");
        params.put("address", "123 Main St");
        params.put("password", "Pass12345!");
        params.put("confirmPassword", "DifferentPass!");

        Map<String, Object> respState = new HashMap<>();
        HttpServletRequest request = createRequestProxyWithParams(params, session);
        HttpServletResponse response = createResponseProxy(respState);

        servlet.doPost(request, response);

        assertEquals("/context/register", respState.get("redirectUrl"));
        FlashMessage flash = (FlashMessage) sessionAttributes.get(FlashMessage.SESSION_KEY);
        assertNotNull(flash);
        assertTrue(flash.isDanger());
        assertTrue(flash.getMessage().contains("match"));
    }

    private HttpServletRequest createRequestProxy(Map<String, Object> reqAttrs,
                                                  HttpSession currentSession,
                                                  HttpSession nextSession,
                                                  AtomicBoolean sessionChanged) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContextPath" -> "/context";
                    case "changeSessionId" -> {
                        sessionChanged.set(true);
                        yield null;
                    }
                    case "getSession" -> {
                        if (args != null && args.length == 1 && Boolean.FALSE.equals(args[0])) {
                            yield currentSession;
                        }
                        yield (currentSession != null && !((Boolean) true).equals(args != null ? args[0] : null))
                                ? currentSession : nextSession;
                    }
                    case "setAttribute" -> {
                        reqAttrs.put((String) args[0], args[1]);
                        yield null;
                    }
                    case "getAttribute" -> reqAttrs.get((String) args[0]);
                    default -> null;
                }
        );
    }

    private HttpServletRequest createRequestProxyWithParams(Map<String, String> params, HttpSession session) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContextPath" -> "/context";
                    case "getParameter" -> params.get((String) args[0]);
                    case "getSession" -> session;
                    default -> null;
                }
        );
    }

    private HttpServletResponse createResponseProxy(Map<String, Object> respState) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "sendRedirect" -> {
                        respState.put("redirectUrl", args[0]);
                        yield null;
                    }
                    default -> null;
                }
        );
    }

    private HttpSession createSessionProxy(Map<String, Object> attributes, AtomicBoolean invalidated) {
        return (HttpSession) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setAttribute" -> {
                        attributes.put((String) args[0], args[1]);
                        yield null;
                    }
                    case "getAttribute" -> attributes.get((String) args[0]);
                    case "removeAttribute" -> attributes.remove((String) args[0]);
                    case "invalidate" -> {
                        invalidated.set(true);
                        attributes.clear();
                        yield null;
                    }
                    default -> null;
                }
        );
    }
}

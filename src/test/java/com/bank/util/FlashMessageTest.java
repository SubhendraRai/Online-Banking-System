package com.bank.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FlashMessage Utility Unit Tests")
class FlashMessageTest {

    @Test
    @DisplayName("Set and consume flash message removes it from session")
    void testSetAndConsume() {
        Map<String, Object> sessionAttributes = new HashMap<>();
        HttpSession session = createSessionProxy(sessionAttributes);

        FlashMessage.success(session, "Operation successful!");
        assertEquals(1, sessionAttributes.size());

        FlashMessage consumed = FlashMessage.consume(session);
        assertNotNull(consumed);
        assertEquals(FlashMessage.TYPE_SUCCESS, consumed.getType());
        assertEquals("Operation successful!", consumed.getMessage());
        assertTrue(consumed.isSuccess());
        assertFalse(consumed.isDanger());

        // Consumed again returns null (one-time read)
        assertNull(FlashMessage.consume(session));
        assertTrue(sessionAttributes.isEmpty());
    }

    @Test
    @DisplayName("Transfer flash message moves attribute from session to request")
    void testTransferToRequest() {
        Map<String, Object> sessionAttributes = new HashMap<>();
        Map<String, Object> requestAttributes = new HashMap<>();

        HttpSession session = createSessionProxy(sessionAttributes);
        HttpServletRequest request = createRequestProxy(requestAttributes, session);

        FlashMessage.error(session, "Invalid input data provided.");
        assertEquals(1, sessionAttributes.size());

        FlashMessage.transfer(request);

        // Cleared from session
        assertTrue(sessionAttributes.isEmpty());

        // Present in request scope
        FlashMessage transferred = (FlashMessage) requestAttributes.get(FlashMessage.REQUEST_KEY);
        assertNotNull(transferred);
        assertTrue(transferred.isDanger());
        assertEquals("Invalid input data provided.", transferred.getMessage());
    }

    @Test
    @DisplayName("FlashMessage null session does not throw exception")
    void testNullSessionSafe() {
        assertDoesNotThrow(() -> FlashMessage.success(null, "Test"));
        assertNull(FlashMessage.consume(null));
        assertDoesNotThrow(() -> FlashMessage.transfer(null));
    }

    @SuppressWarnings("unchecked")
    private HttpSession createSessionProxy(Map<String, Object> attributes) {
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
                    default -> null;
                }
        );
    }

    private HttpServletRequest createRequestProxy(Map<String, Object> attributes, HttpSession session) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setAttribute" -> {
                        attributes.put((String) args[0], args[1]);
                        yield null;
                    }
                    case "getAttribute" -> attributes.get((String) args[0]);
                    case "getSession" -> session;
                    default -> null;
                }
        );
    }
}

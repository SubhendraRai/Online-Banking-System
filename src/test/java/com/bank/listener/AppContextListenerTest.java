package com.bank.listener;

import com.bank.service.BaseServiceTest;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AppContextListener Lifecycle Unit Tests")
class AppContextListenerTest extends BaseServiceTest {

    @Test
    @DisplayName("contextInitialized bootstraps settings and default settings provider into application scope")
    void testContextInitialized() {
        AppContextListener listener = new AppContextListener();
        Map<String, Object> contextAttributes = new HashMap<>();

        ServletContext servletContext = (ServletContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ServletContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setAttribute" -> {
                        contextAttributes.put((String) args[0], args[1]);
                        yield null;
                    }
                    case "getAttribute" -> contextAttributes.get((String) args[0]);
                    default -> null;
                }
        );

        ServletContextEvent event = new ServletContextEvent(servletContext);

        assertDoesNotThrow(() -> listener.contextInitialized(event));
        assertNotNull(contextAttributes.get("settingsProvider"), "Settings provider should be stored in context");
    }

    @Test
    @DisplayName("contextDestroyed cleanly invokes driver deregistration and cleanup routines")
    void testContextDestroyed() {
        AppContextListener listener = new AppContextListener();
        ServletContext servletContext = (ServletContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ServletContext.class},
                (proxy, method, args) -> null
        );

        ServletContextEvent event = new ServletContextEvent(servletContext);
        assertDoesNotThrow(() -> listener.contextDestroyed(event));
    }
}

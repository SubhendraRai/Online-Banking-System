package com.bank.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Objects;

/**
 * Transient notification message stored in HTTP session and displayed once.
 * <p>
 * Follows the Post/Redirect/Get (PRG) flash message pattern: a message is staged
 * into the session prior to an HTTP redirect, transferred to request attributes
 * upon the subsequent request, and automatically cleared from the session.
 * </p>
 */
public class FlashMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SESSION_KEY = "FLASH_MESSAGE_SESSION";
    public static final String REQUEST_KEY = "flash";
    public static final String LEGACY_REQUEST_KEY = "flashMessage";

    public static final String TYPE_SUCCESS = "success";
    public static final String TYPE_DANGER = "danger";
    public static final String TYPE_WARNING = "warning";
    public static final String TYPE_INFO = "info";

    private final String type;
    private final String message;

    public FlashMessage(String type, String message) {
        this.type = (type != null && !type.isBlank()) ? type : TYPE_INFO;
        this.message = (message != null) ? message : "";
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return TYPE_SUCCESS.equalsIgnoreCase(type);
    }

    public boolean isDanger() {
        return TYPE_DANGER.equalsIgnoreCase(type) || "error".equalsIgnoreCase(type);
    }

    public static void success(HttpSession session, String message) {
        set(session, TYPE_SUCCESS, message);
    }

    public static void error(HttpSession session, String message) {
        set(session, TYPE_DANGER, message);
    }

    public static void warning(HttpSession session, String message) {
        set(session, TYPE_WARNING, message);
    }

    public static void info(HttpSession session, String message) {
        set(session, TYPE_INFO, message);
    }

    public static void set(HttpSession session, String type, String message) {
        if (session != null) {
            session.setAttribute(SESSION_KEY, new FlashMessage(type, message));
        }
    }

    /**
     * Reads and atomically removes the flash message from the session.
     *
     * @param session active HTTP session
     * @return current {@link FlashMessage} or {@code null} if none exists
     */
    public static FlashMessage consume(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object obj = session.getAttribute(SESSION_KEY);
        if (obj instanceof FlashMessage flash) {
            session.removeAttribute(SESSION_KEY);
            return flash;
        }
        return null;
    }

    /**
     * Transfers any pending flash message from session scope to request scope.
     *
     * @param request current HTTP servlet request
     */
    public static void transfer(HttpServletRequest request) {
        if (request == null) {
            return;
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            FlashMessage flash = consume(session);
            if (flash != null) {
                request.setAttribute(REQUEST_KEY, flash);
                request.setAttribute(LEGACY_REQUEST_KEY, flash);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlashMessage that = (FlashMessage) o;
        return Objects.equals(type, that.type) && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, message);
    }

    @Override
    public String toString() {
        return "FlashMessage{type='" + type + "', message='" + message + "'}";
    }
}

package com.bank.util;

import com.bank.exception.BankingException;
import com.bank.exception.ErrorCode;
import com.bank.exception.InsufficientFundsException;
import com.bank.exception.ValidationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Central ErrorCode to Friendly Message Resolver Tests")
class ErrorMessageResolverTest {

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("Every ErrorCode maps to a descriptive friendly message")
    void testEveryErrorCodeHasFriendlyMessage(ErrorCode code) {
        String msg = ErrorMessageResolver.resolve(code);
        assertNotNull(msg, "Resolved message for " + code + " must not be null");
        assertFalse(msg.isBlank(), "Resolved message for " + code + " must not be blank");
        assertFalse(msg.contains("null"), "Resolved message should not contain literal 'null'");
    }

    @Test
    @DisplayName("Null ErrorCode falls back to INTERNAL_ERROR friendly message")
    void testNullErrorCodeFallback() {
        String fallback = ErrorMessageResolver.resolve((ErrorCode) null);
        assertNotNull(fallback);
        assertTrue(fallback.contains("system error") || fallback.contains("safe"));
    }

    @Test
    @DisplayName("BankingException with custom message preserves specific message")
    void testBankingExceptionCustomMessage() {
        ValidationException vex = new ValidationException("amount", "Amount must be greater than zero.");
        String msg = ErrorMessageResolver.resolve(vex);
        assertTrue(msg.contains("Amount must be greater than zero."));
    }

    @Test
    @DisplayName("BankingException without message uses registered friendly message for code")
    void testBankingExceptionCodeMessage() {
        BankingException be = new BankingException(ErrorCode.INSUFFICIENT_FUNDS);
        String msg = ErrorMessageResolver.resolve(be);
        assertTrue(msg.contains("Insufficient"));
    }

    @Test
    @DisplayName("Generic Throwable maps safely without leaking internal details")
    void testGenericThrowable() {
        RuntimeException ex = new RuntimeException("DB Connection Refused 192.168.1.1");
        String msg = ErrorMessageResolver.resolve(ex);
        assertFalse(msg.contains("192.168.1.1"));
        assertTrue(msg.contains("error"));
    }
}

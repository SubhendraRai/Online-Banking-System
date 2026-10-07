package com.bank.util;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Indian Rupee (INR) Formatting Tests")
class MoneyInrTest {

    @Test
    @DisplayName("Null or zero amounts format properly to ₹0.00")
    void testNullAndZero() {
        assertEquals("₹0.00", Money.formatInr(null));
        assertEquals("₹0.00", Money.formatInr(BigDecimal.ZERO));
        assertEquals("0.00", Money.formatInrWithoutSymbol(null));
        assertEquals("0.00", Money.formatInrWithoutSymbol(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Amounts up to 3 digits have no comma separation")
    void testSmallAmounts() {
        assertEquals("₹500.00", Money.formatInr(new BigDecimal("500")));
        assertEquals("₹99.50", Money.formatInr(new BigDecimal("99.5")));
        assertEquals("500.00", Money.formatInrWithoutSymbol(new BigDecimal("500")));
    }

    @Test
    @DisplayName("Amounts in thousands (4 and 5 digits) group last 3 digits")
    void testThousands() {
        assertEquals("₹1,500.00", Money.formatInr(new BigDecimal("1500")));
        assertEquals("₹50,000.00", Money.formatInr(new BigDecimal("50000.00")));
        assertEquals("50,000.00", Money.formatInrWithoutSymbol(new BigDecimal("50000.00")));
    }

    @Test
    @DisplayName("Lakhs and Crores group in Indian 2-2-3 digit system")
    void testLakhsAndCrores() {
        // 12,34,567.89 (12 Lakhs, 34 Thousand, 567 Rupees and 89 Paise)
        assertEquals("₹12,34,567.89", Money.formatInr(new BigDecimal("1234567.89")));
        assertEquals("12,34,567.89", Money.formatInrWithoutSymbol(new BigDecimal("1234567.89")));

        // 1,00,00,000.00 (1 Crore Rupees)
        assertEquals("₹1,00,00,000.00", Money.formatInr(new BigDecimal("10000000")));
        assertEquals("1,00,00,000.00", Money.formatInrWithoutSymbol(new BigDecimal("10000000")));
    }

    @Test
    @DisplayName("Negative amounts preserve minus sign before rupee symbol")
    void testNegativeAmounts() {
        assertEquals("-₹1,500.75", Money.formatInr(new BigDecimal("-1500.75")));
        assertEquals("-1,500.75", Money.formatInrWithoutSymbol(new BigDecimal("-1500.75")));
    }
}

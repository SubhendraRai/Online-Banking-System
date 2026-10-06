package com.bank.util;

import com.bank.exception.InvalidAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying financial arithmetic precision, rounding, and validation in {@link Money}.
 */
class MoneyTest {

    @Test
    @DisplayName("Money normalizes amounts to scale 2 with RoundingMode.HALF_UP")
    void testScaleAndHalfUpRounding() {
        assertEquals(new BigDecimal("100.00"), Money.of(new BigDecimal("100")));
        assertEquals(new BigDecimal("100.56"), Money.of(new BigDecimal("100.555")));
        assertEquals(new BigDecimal("100.55"), Money.of(new BigDecimal("100.554")));
        assertEquals(new BigDecimal("0.00"), Money.of((BigDecimal) null));
    }

    @Test
    @DisplayName("Money parses string amounts and cent representations correctly")
    void testStringParsingAndCentConversion() throws InvalidAmountException {
        assertEquals(new BigDecimal("250.75"), Money.of("250.75"));
        assertEquals(new BigDecimal("15.00"), Money.of("15"));
        assertEquals(new BigDecimal("12.34"), Money.of(1234L));
        assertEquals(new BigDecimal("0.50"), Money.of(50L));

        assertThrows(InvalidAmountException.class, () -> Money.of("abc"));
        assertThrows(InvalidAmountException.class, () -> Money.of(""));
        assertThrows(InvalidAmountException.class, () -> Money.of((String) null));
    }

    @Test
    @DisplayName("Money formatting outputs plain 2-decimal string representation")
    void testFormat() {
        assertEquals("1234.50", Money.format(new BigDecimal("1234.5")));
        assertEquals("0.00", Money.format(null));
        assertEquals("0.00", Money.format(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Money arithmetic performs exact additions, subtractions, multiplications, and divisions")
    void testArithmeticAddSubtractMultiplyDivide() {
        BigDecimal a = new BigDecimal("100.50");
        BigDecimal b = new BigDecimal("49.50");

        assertEquals(new BigDecimal("150.00"), Money.add(a, b));
        assertEquals(new BigDecimal("51.00"), Money.subtract(a, b));

        // Multiplication with rounding
        BigDecimal mult = Money.multiply(new BigDecimal("10.00"), new BigDecimal("1.05"));
        assertEquals(new BigDecimal("10.50"), mult);

        // Division with scale 2 HALF_UP: 100 / 3 = 33.33
        BigDecimal div = Money.divide(new BigDecimal("100.00"), new BigDecimal("3.00"));
        assertEquals(new BigDecimal("33.33"), div);

        assertThrows(IllegalArgumentException.class, () -> Money.divide(a, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Money comparison helpers correctly evaluate ordering and equality")
    void testComparisonHelpers() {
        BigDecimal low = new BigDecimal("50.00");
        BigDecimal high = new BigDecimal("100.00");
        BigDecimal equalLow = new BigDecimal("50.000");

        assertTrue(Money.isGreaterThan(high, low));
        assertFalse(Money.isGreaterThan(low, high));
        assertTrue(Money.isLessThan(low, high));
        assertTrue(Money.isGreaterThanOrEqual(low, equalLow));
        assertTrue(Money.isEqual(low, equalLow));
        assertEquals(0, Money.compare(low, equalLow));
        assertEquals(-1, Money.compare(low, high));
        assertEquals(1, Money.compare(high, low));
    }

    @Test
    @DisplayName("Money sign checkers correctly identify positive, negative, and zero values")
    void testSignChecksAndZero() {
        assertTrue(Money.isPositive(new BigDecimal("0.01")));
        assertFalse(Money.isPositive(BigDecimal.ZERO));
        assertFalse(Money.isPositive(new BigDecimal("-0.01")));

        assertTrue(Money.isNegative(new BigDecimal("-5.00")));
        assertFalse(Money.isNegative(BigDecimal.ZERO));

        assertTrue(Money.isZero(BigDecimal.ZERO));
        assertTrue(Money.isZero(new BigDecimal("0.0000")));
        assertFalse(Money.isZero(new BigDecimal("0.01")));

        assertTrue(Money.isZeroOrPositive(BigDecimal.ZERO));
        assertTrue(Money.isZeroOrPositive(new BigDecimal("10.00")));
        assertFalse(Money.isZeroOrPositive(new BigDecimal("-1.00")));
    }

    @Test
    @DisplayName("validatePositive throws InvalidAmountException for zero, negative, and null values")
    void testValidatePositiveThrowsOnZeroOrNegative() {
        assertThrows(InvalidAmountException.class, () -> Money.validatePositive(null, "Test"));
        assertThrows(InvalidAmountException.class, () -> Money.validatePositive(BigDecimal.ZERO, "Test"));
        assertThrows(InvalidAmountException.class, () -> Money.validatePositive(new BigDecimal("-0.01"), "Test"));
    }
}

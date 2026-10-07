package com.bank.util;

import com.bank.exception.InvalidAmountException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for rigorous financial computations, scale normalization, and amount validation.
 * <p>
 * Enforces Architectural Rule 2:
 * <ul>
 *   <li>All monetary calculations maintain a scale of 2 with {@link RoundingMode#HALF_UP}.</li>
 *   <li>Floating-point primitives ({@code double}, {@code float}) are strictly forbidden.</li>
 *   <li>Safe comparison and arithmetic helpers guarantee consistency across domain and service tiers.</li>
 * </ul>
 * </p>
 */
public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);

    private Money() {
        // Prevent instantiation
    }

    /**
     * Normalizes a {@link BigDecimal} to the standard 2-decimal scale with HALF_UP rounding.
     *
     * @param amount the raw amount, or null
     * @return scaled BigDecimal, or {@link #ZERO} if amount was null
     */
    public static BigDecimal of(BigDecimal amount) {
        if (amount == null) {
            return ZERO;
        }
        return amount.setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Parses a string into a normalized monetary {@link BigDecimal}.
     *
     * @param amountStr the string representation of the amount
     * @return scaled BigDecimal
     * @throws InvalidAmountException if string is null, blank, or malformed
     */
    public static BigDecimal of(String amountStr) throws InvalidAmountException {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            throw new InvalidAmountException(null, "Monetary amount string cannot be null or empty.");
        }
        try {
            return new BigDecimal(amountStr.trim()).setScale(SCALE, ROUNDING_MODE);
        } catch (NumberFormatException nfe) {
            throw new InvalidAmountException(null, "Invalid monetary amount format: " + amountStr);
        }
    }

    /**
     * Creates a normalized monetary {@link BigDecimal} from a whole integer or cent value.
     *
     * @param cents total value in cents (e.g. 1050 produces 10.50)
     * @return scaled BigDecimal
     */
    public static BigDecimal of(long cents) {
        return BigDecimal.valueOf(cents, SCALE);
    }

    /**
     * Formats a monetary amount to a standard two-decimal string representation.
     *
     * @param amount the monetary amount
     * @return formatted plain string (e.g., "1250.50")
     */
    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(SCALE, ROUNDING_MODE).toPlainString();
    }

    /**
     * Formats a monetary amount in Indian Rupees (INR) with Indian numbering grouping (e.g., "₹12,34,567.89").
     *
     * @param amount the monetary amount
     * @return formatted INR string with rupee symbol
     */
    public static String formatInr(BigDecimal amount) {
        if (amount == null) {
            return "₹0.00";
        }
        boolean isNegative = amount.compareTo(BigDecimal.ZERO) < 0;
        String grouped = formatIndianGrouping(amount.abs());
        return (isNegative ? "-₹" : "₹") + grouped;
    }

    /**
     * Formats a monetary amount with Indian numbering grouping without currency symbol (e.g., "12,34,567.89").
     *
     * @param amount the monetary amount
     * @return formatted grouped string
     */
    public static String formatInrWithoutSymbol(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        boolean isNegative = amount.compareTo(BigDecimal.ZERO) < 0;
        String grouped = formatIndianGrouping(amount.abs());
        return isNegative ? "-" + grouped : grouped;
    }

    private static String formatIndianGrouping(BigDecimal absAmount) {
        BigDecimal scaled = absAmount.setScale(SCALE, ROUNDING_MODE);
        String str = scaled.toPlainString();
        int dotIndex = str.indexOf('.');
        String intPart = (dotIndex >= 0) ? str.substring(0, dotIndex) : str;
        String decPart = (dotIndex >= 0) ? str.substring(dotIndex + 1) : "00";

        if (intPart.length() <= 3) {
            return intPart + "." + decPart;
        }

        String lastThree = intPart.substring(intPart.length() - 3);
        String remaining = intPart.substring(0, intPart.length() - 3);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < remaining.length(); i++) {
            if (i > 0 && (remaining.length() - i) % 2 == 0) {
                sb.append(",");
            }
            sb.append(remaining.charAt(i));
        }
        sb.append(",").append(lastThree).append(".").append(decPart);
        return sb.toString();
    }


    /**
     * Checks if the amount is non-null and strictly greater than zero.
     *
     * @param amount the monetary amount
     * @return true if strictly positive, false otherwise
     */
    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if the amount is non-null and greater than or equal to zero.
     *
     * @param amount the monetary amount
     * @return true if zero or positive, false otherwise
     */
    public static boolean isZeroOrPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Checks if the amount is non-null and strictly negative.
     *
     * @param amount the monetary amount
     * @return true if negative, false otherwise
     */
    public static boolean isNegative(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Checks if the amount is non-null and numerically equal to zero.
     *
     * @param amount the monetary amount
     * @return true if zero, false otherwise
     */
    public static boolean isZero(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Performs a null-safe numerical comparison of two monetary values.
     *
     * @param a the first amount (null treated as {@link #ZERO})
     * @param b the second amount (null treated as {@link #ZERO})
     * @return -1, 0, or 1 as {@code a} is numerically less than, equal to, or greater than {@code b}
     */
    public static int compare(BigDecimal a, BigDecimal b) {
        BigDecimal safeA = a != null ? a : ZERO;
        BigDecimal safeB = b != null ? b : ZERO;
        return safeA.compareTo(safeB);
    }

    /**
     * Evaluates whether {@code a} is strictly greater than {@code b}.
     *
     * @param a first amount
     * @param b second amount
     * @return true if {@code a > b}
     */
    public static boolean isGreaterThan(BigDecimal a, BigDecimal b) {
        return compare(a, b) > 0;
    }

    /**
     * Evaluates whether {@code a} is greater than or equal to {@code b}.
     *
     * @param a first amount
     * @param b second amount
     * @return true if {@code a >= b}
     */
    public static boolean isGreaterThanOrEqual(BigDecimal a, BigDecimal b) {
        return compare(a, b) >= 0;
    }

    /**
     * Evaluates whether {@code a} is strictly less than {@code b}.
     *
     * @param a first amount
     * @param b second amount
     * @return true if {@code a < b}
     */
    public static boolean isLessThan(BigDecimal a, BigDecimal b) {
        return compare(a, b) < 0;
    }

    /**
     * Evaluates whether {@code a} is less than or equal to {@code b}.
     *
     * @param a first amount
     * @param b second amount
     * @return true if {@code a <= b}
     */
    public static boolean isLessThanOrEqual(BigDecimal a, BigDecimal b) {
        return compare(a, b) <= 0;
    }

    /**
     * Evaluates whether {@code a} is numerically equal to {@code b}.
     *
     * @param a first amount
     * @param b second amount
     * @return true if {@code a == b}
     */
    public static boolean isEqual(BigDecimal a, BigDecimal b) {
        return compare(a, b) == 0;
    }

    /**
     * Adds two monetary values, scaling the sum to 2 decimal places with HALF_UP rounding.
     *
     * @param a first operand
     * @param b second operand
     * @return sum
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        BigDecimal safeA = a != null ? a : ZERO;
        BigDecimal safeB = b != null ? b : ZERO;
        return safeA.add(safeB).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Subtracts {@code b} from {@code a}, scaling the result to 2 decimal places with HALF_UP rounding.
     *
     * @param a minuend
     * @param b subtrahend
     * @return difference
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        BigDecimal safeA = a != null ? a : ZERO;
        BigDecimal safeB = b != null ? b : ZERO;
        return safeA.subtract(safeB).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Multiplies an amount by a factor, rounding to 2 decimal places with HALF_UP rounding.
     *
     * @param a monetary amount
     * @param factor multiplication factor
     * @return product
     */
    public static BigDecimal multiply(BigDecimal a, BigDecimal factor) {
        if (a == null || factor == null) {
            return ZERO;
        }
        return a.multiply(factor).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Divides an amount by a divisor, scaling the result to 2 decimal places with HALF_UP rounding.
     *
     * @param a dividend
     * @param divisor divisor
     * @return quotient
     * @throws IllegalArgumentException if divisor is null or zero
     */
    public static BigDecimal divide(BigDecimal a, BigDecimal divisor) {
        if (divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Divisor cannot be null or zero.");
        }
        BigDecimal safeA = a != null ? a : ZERO;
        return safeA.divide(divisor, SCALE, ROUNDING_MODE);
    }

    /**
     * Validates that an amount is non-null and strictly greater than zero.
     *
     * @param amount the monetary amount to test
     * @param fieldName descriptive name of the field for error reporting
     * @throws InvalidAmountException if the amount is null or non-positive
     */
    public static void validatePositive(BigDecimal amount, String fieldName) throws InvalidAmountException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            String label = (fieldName != null && !fieldName.trim().isEmpty()) ? fieldName : "Amount";
            throw new InvalidAmountException(amount, label + " must be strictly greater than 0.00.");
        }
    }
}

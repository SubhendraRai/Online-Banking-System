package com.bank.dao;

import com.bank.model.TxnType;
import com.bank.util.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TxnFilterTest {

    @Test
    @DisplayName("TxnFilter builder populates and normalizes all fields correctly")
    void testFilterBuilder() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        BigDecimal min = new BigDecimal("100.5");
        BigDecimal max = new BigDecimal("5000");

        TxnFilter filter = TxnFilter.builder()
                .fromDate(start)
                .toDate(end)
                .type(TxnType.TRANSFER)
                .minAmount(min)
                .maxAmount(max)
                .keyword("  Salary  ")
                .build();

        assertEquals(start, filter.getFromDate());
        assertEquals(end, filter.getToDate());
        assertEquals(TxnType.TRANSFER, filter.getType());
        assertEquals(Money.of(min), filter.getMinAmount());
        assertEquals(Money.of(max), filter.getMaxAmount());
        assertEquals("Salary", filter.getKeyword());
        assertTrue(filter.hasFilters());
    }

    @Test
    @DisplayName("Empty TxnFilter detects absence of filters and handles null keywords")
    void testEmptyFilter() {
        TxnFilter filter = new TxnFilter();
        assertFalse(filter.hasFilters());
        assertNull(filter.getKeyword());

        filter.setKeyword("   ");
        assertNull(filter.getKeyword());
        assertFalse(filter.hasFilters());

        assertNotNull(filter.toString());
    }
}

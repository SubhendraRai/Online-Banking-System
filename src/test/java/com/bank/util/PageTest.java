package com.bank.util;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageTest {

    @Test
    @DisplayName("Page correctly calculates total pages and pagination navigation states")
    void testPageCalculationsAndNavigation() {
        List<String> items = List.of("A", "B", "C", "D", "E");
        Page<String> page1 = new Page<>(items, 1, 5, 12);

        assertEquals(1, page1.getPage());
        assertEquals(5, page1.getSize());
        assertEquals(12, page1.getTotalItems());
        assertEquals(3, page1.getTotalPages());
        assertTrue(page1.isFirst());
        assertFalse(page1.isLast());
        assertTrue(page1.hasNext());
        assertFalse(page1.hasPrevious());
        assertFalse(page1.isEmpty());
        assertEquals(5, page1.getItems().size());
    }

    @Test
    @DisplayName("Last page flags navigation correctly")
    void testLastPageNavigation() {
        List<String> items = List.of("K", "L");
        Page<String> page3 = new Page<>(items, 3, 5, 12);

        assertEquals(3, page3.getPage());
        assertEquals(3, page3.getTotalPages());
        assertFalse(page3.isFirst());
        assertTrue(page3.isLast());
        assertFalse(page3.hasNext());
        assertTrue(page3.hasPrevious());
    }

    @Test
    @DisplayName("Empty page contains zero items and zero total pages")
    void testEmptyPage() {
        Page<String> empty = Page.empty(1, 10);

        assertTrue(empty.isEmpty());
        assertEquals(0, empty.getTotalItems());
        assertEquals(0, empty.getTotalPages());
        assertEquals(0, empty.getItems().size());
    }

    @Test
    @DisplayName("Returned items list is immutable")
    void testItemsListImmutability() {
        Page<Integer> page = new Page<>(List.of(1, 2, 3), 1, 10, 3);
        List<Integer> list = page.getItems();

        assertThrows(UnsupportedOperationException.class, () -> list.add(4));
    }
}

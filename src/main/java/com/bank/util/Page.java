package com.bank.util;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Generic container for paginated query results.
 * <p>
 * Holds a page slice of domain items alongside pagination metadata including
 * the current 1-indexed page number, requested page size, and total element count.
 * All collections returned by {@link #getItems()} are unmodifiable.
 * </p>
 *
 * @param <T> element type contained within the page slice
 */
public final class Page<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final long totalItems;
    private final int totalPages;

    /**
     * Constructs an immutable pagination slice.
     *
     * @param items non-null list of elements for the current page
     * @param page 1-indexed current page number
     * @param size page size capacity
     * @param totalItems total count of items matching the query across all pages
     */
    public Page(List<T> items, int page, int size, long totalItems) {
        this.items = items != null ? List.copyOf(items) : List.of();
        this.page = Math.max(1, page);
        this.size = Math.max(1, size);
        this.totalItems = Math.max(0, totalItems);
        this.totalPages = this.totalItems == 0 ? 0 : (int) Math.ceil((double) this.totalItems / this.size);
    }

    /**
     * Factory method creating an empty page slice.
     *
     * @param <E> item type
     * @param page 1-indexed page number
     * @param size page size capacity
     * @return empty {@link Page}
     */
    public static <E> Page<E> empty(int page, int size) {
        return new Page<>(Collections.emptyList(), page, size, 0L);
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean hasNext() {
        return page < totalPages;
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean isFirst() {
        return page <= 1;
    }

    public boolean isLast() {
        return page >= totalPages;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page<?> page1 = (Page<?>) o;
        return page == page1.page && size == page1.size &&
                totalItems == page1.totalItems && Objects.equals(items, page1.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items, page, size, totalItems);
    }

    @Override
    public String toString() {
        return "Page{" +
                "page=" + page +
                ", size=" + size +
                ", totalPages=" + totalPages +
                ", totalItems=" + totalItems +
                ", itemsCount=" + items.size() +
                '}';
    }
}

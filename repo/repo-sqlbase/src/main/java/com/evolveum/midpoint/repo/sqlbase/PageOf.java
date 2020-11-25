/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

/**
 * Result object for potentially paginated content.
 *
 * @param <T> type of the result row
 */
public class PageOf<T> implements Iterable<T> {

    public static final int PAGE_NO_PAGINATION = 0;
    public static final int FIRST_PAGE = 1;
    public static final int TOTAL_COUNT_UNKNOWN = -1;

    private final List<T> content;

    /**
     * First page number is defined {@link #FIRST_PAGE} (value {@value #FIRST_PAGE}).
     * No pagination is indicated by {@link #PAGE_NO_PAGINATION} (value {@value #PAGE_NO_PAGINATION}).
     */
    private final int pageNumber;

    private final int pageSize;
    private final long totalCount;

    public PageOf(@NotNull List<T> content, int pageNumber, int pageSize, long totalCount) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
    }

    public PageOf(@NotNull List<T> content, int pageNumber, int pageSize) {
        this(content, pageNumber, pageSize, TOTAL_COUNT_UNKNOWN);
    }

    /**
     * Empty result.
     */
    public PageOf() {
        this(Collections.emptyList(), PAGE_NO_PAGINATION, 0, 0);
    }

    public PageOf(@NotNull List<T> content) {
        this(content, PAGE_NO_PAGINATION, 0, TOTAL_COUNT_UNKNOWN);
    }

    public <R> PageOf<R> map(Function<T, R> mappingFunction) {
        return new PageOf<>(
                content.stream().map(mappingFunction).collect(Collectors.toList()),
                this.pageNumber, pageSize, totalCount);
    }

    @Override
    @NotNull
    public Iterator<T> iterator() {
        return content.iterator();
    }

    public Stream<T> stream() {
        return content.stream();
    }

    public List<T> content() {
        return Collections.unmodifiableList(content);
    }

    public T get(int index) {
        return content.get(index);
    }

    public int pageNumber() {
        return pageNumber;
    }

    public int pageSize() {
        return pageSize;
    }

    public long totalCount() {
        return totalCount;
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    public boolean isPaginated() {
        return pageNumber != PAGE_NO_PAGINATION;
    }

    public boolean isKnownTotalCount() {
        return totalCount != TOTAL_COUNT_UNKNOWN;
    }

    @Override
    public String toString() {
        return "PageOf{" +
                "content=" + content +
                ", page=" + pageNumber +
                ", pageSize=" + pageSize +
                ", totalCount=" + totalCount +
                '}';
    }
}

/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.hibernate.ScrollableResults;

/**
 * @author lazyman
 */
public class ScrollableResultsIterator<T> implements Iterator<T> {

    private final ScrollableResults results;
    private Boolean hasNext;

    public ScrollableResultsIterator(ScrollableResults results) {
        Objects.requireNonNull(results, "Scrollable results must not be null.");

        this.results = results;
    }

    @Override
    public boolean hasNext() {
        if (hasNext == null) {
            hasNext = results.next();
        }
        return hasNext;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        hasNext = null;
        return (T) results.get();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove operation not supported.");
    }
}

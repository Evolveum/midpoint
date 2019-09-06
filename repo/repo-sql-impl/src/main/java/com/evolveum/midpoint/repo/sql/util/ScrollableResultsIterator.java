/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.apache.commons.lang.Validate;
import org.hibernate.ScrollableResults;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author lazyman
 */
public class ScrollableResultsIterator<T> implements Iterator<T> {

    private final ScrollableResults results;
    private Boolean hasNext;

    public ScrollableResultsIterator(ScrollableResults results) {
        Validate.notNull(results, "Scrollable results must not be null.");

        this.results = results;
    }

    public boolean hasNext() {
        if (hasNext == null) {
            hasNext = results.next();
        }
        return hasNext;
    }

    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        hasNext = null;
        return (T) results.get(0);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove operation not supported.");
    }
}

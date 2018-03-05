/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        return (T) results.get(0);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove operation not supported.");
    }
}

/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
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

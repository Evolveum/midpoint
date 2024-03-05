/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping;

import java.util.Collection;
import java.util.List;

import com.querydsl.core.Tuple;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Helps with row transformation of result lists, possibly in stateful context.
 * This kind of transformation still has access to low-level row data if necessary.
 * Typical example is container's owner OID which is lost in the container itself.
 *
 * For example, row-by-row transformation can collect information for some additional processing
 * which can be executed by implementing {@link #finishTransformation()}.
 *
 * Alternatively, the result list of tuples can be processed first with {@link #beforeTransformation},
 * for instance to fetch owner objects for the whole container list in a single query.
 * The reason for this is that {@link #transform} already needs to return actual containers from
 * the owner object.
 * Leaving this fetch to {@link #finishTransformation} would require this method to transform the list
 * again and replace "bare" containers with the real ones from owner objects.
 * Finish method is useful when the objects in the final result list needs to be enriched, but not
 * when they need to be replaced.
 * (If this is necessary later, just add result list as parameter and returned value as well.)
 *
 * @param <S> schema type
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public interface ResultListRowTransformer<S, Q extends FlexibleRelationalPathBase<R>, R> {

    /**
     * This allows preprocessing of results before the transformation.
     * One example is to load owner objects for containers.
     */
    default void beforeTransformation(List<Tuple> rowTuples, Q entityPath) throws SchemaException {
        // nothing by default
    }

    /**
     * Transforms row tuple to the midPoint object.
     */
    S transform(Tuple rowTuple, Q entityPath);

    /**
     * This method is called after all the rows were transformed and allows for additional
     * final shake-up for more complex stateful transformations.
     */
    default void finishTransformation() throws SchemaException {
        // nothing by default
    }
}

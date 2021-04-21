/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.function.Function;

import com.querydsl.core.types.Path;

import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * @param <T> type of real value, the same like the column (attribute) type in the row bean (M-type)
 * @param <P> type of the corresponding path in the Q-type
 */
// TODO necessary if this bare?
public class SimpleItemDeltaProcessor<T, P extends Path<T>>
        extends SinglePathItemDeltaProcessor<T, P> {

    /**
     * @param <Q> entity query type from which the attribute is resolved
     * @param <R> row type related to {@link Q}
     */
    public <Q extends FlexibleRelationalPathBase<R>, R> SimpleItemDeltaProcessor(
            SqaleUpdateContext<?, Q, R> context, Function<Q, P> rootToQueryItem) {
        super(context, rootToQueryItem);
    }
}

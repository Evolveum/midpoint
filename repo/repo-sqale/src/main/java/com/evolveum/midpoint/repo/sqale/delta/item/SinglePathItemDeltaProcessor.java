/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.function.Function;

import com.querydsl.core.types.Path;

import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Processor for items represented by a single column (query path).
 *
 * @param <T> type of real value after optional conversion ({@link #convertRealValue(Object)}
 * to match the column (attribute) type in the row bean (M-type)
 * @param <P> type of the corresponding path in the Q-type
 */
public class SinglePathItemDeltaProcessor<T, P extends Path<T>>
        extends ItemDeltaSingleValueProcessor<T> {

    protected final P path;

    /**
     * @param <Q> entity query type from which the attribute is resolved
     * @param <R> row type related to {@link Q}
     */
    public <Q extends FlexibleRelationalPathBase<R>, R> SinglePathItemDeltaProcessor(
            SqaleUpdateContext<?, Q, R> context, Function<Q, P> rootToQueryItem) {
        super(context);
        this.path = rootToQueryItem.apply(context.entityPath());
    }

    @Override
    public void setValue(T value) {
        context.set(path, value);
    }

    @Override
    public void delete() {
        context.setNull(path);
    }
}

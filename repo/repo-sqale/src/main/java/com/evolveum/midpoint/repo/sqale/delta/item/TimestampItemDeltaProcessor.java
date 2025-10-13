/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.function.Function;

import com.querydsl.core.types.dsl.DateTimePath;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.QuerydslUtils;

public class TimestampItemDeltaProcessor<T extends Comparable<T>>
        extends SinglePathItemDeltaProcessor<T, DateTimePath<T>> {

    /**
     * @param <Q> entity query type from which the attribute is resolved
     * @param <R> row type related to {@link Q}
     */
    public <Q extends FlexibleRelationalPathBase<R>, R> TimestampItemDeltaProcessor(
            SqaleUpdateContext<?, Q, R> context,
            Function<Q, DateTimePath<T>> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    @Nullable
    public T convertRealValue(Object realValue) {
        return QuerydslUtils.convertTimestampToPathType(realValue, path);
    }
}

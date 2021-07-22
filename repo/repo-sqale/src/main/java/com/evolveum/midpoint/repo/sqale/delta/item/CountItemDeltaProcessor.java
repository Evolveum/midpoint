/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.Collection;
import java.util.function.Function;

import com.querydsl.core.types.dsl.NumberPath;

import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Delta processor for columns storing size of multi-value item.
 *
 * @param <T> expected type of the real value, but we don't care in this class
 */
public class CountItemDeltaProcessor<T> extends FinalValueDeltaProcessor<T> {

    private final NumberPath<Integer> path;

    /**
     * @param <Q> entity query type from which the attribute is resolved
     * @param <R> row type related to {@link Q}
     */
    public <Q extends FlexibleRelationalPathBase<R>, R> CountItemDeltaProcessor(
            SqaleUpdateContext<?, Q, R> context,
            Function<Q, NumberPath<Integer>> rootToQueryItem) {
        super(context);
        this.path = rootToQueryItem.apply(context.entityPath());
    }

    @Override
    public void setRealValues(Collection<?> values) {
        context.set(path, values.size());
    }

    @Override
    public void delete() {
        context.set(path, 0);
    }
}

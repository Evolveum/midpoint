/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.function.Function;

import com.querydsl.core.types.dsl.NumberPath;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

public class UriItemDeltaProcessor
        extends SinglePathItemDeltaProcessor<Integer, NumberPath<Integer>> {

    /**
     * @param <Q> entity query type from which the attribute is resolved
     * @param <R> row type related to {@link Q}
     */
    public <Q extends FlexibleRelationalPathBase<R>, R> UriItemDeltaProcessor(
            SqaleUpdateContext<?, Q, R> context,
            Function<Q, NumberPath<Integer>> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    public @Nullable Integer convertRealValue(Object realValue) {
        return context.repositoryContext().processCacheableUri(realValue);
    }
}

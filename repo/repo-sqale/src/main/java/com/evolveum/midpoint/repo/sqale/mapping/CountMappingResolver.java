/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.function.Function;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.NumberPath;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Resolver that maps the container item to the count column.
 *
 * @param <Q> query type of entity where the mapping is nested and declared
 * @param <R> row type of {@link Q}
 */
public class CountMappingResolver<Q extends FlexibleRelationalPathBase<R>, R>
        implements SqaleItemRelationResolver<Q, R, Q, R> {

    private final Function<Q, NumberPath<Integer>> rootToCount;

    public CountMappingResolver(Function<Q, NumberPath<Integer>> rootToCount) {
        this.rootToCount = rootToCount;
    }

    /** Should not be called, the count must be treated before this happens. */
    @Override
    public ResolutionResult<Q, R> resolve(SqlQueryContext<?, Q, R> context, boolean parent) {
        throw new UnsupportedOperationException("resolution not supported for count mapping");
    }

    /** This creates the predicate representing the EXISTS filter. */
    public Predicate createExistsPredicate(SqaleQueryContext<?, Q, R> context) {
        return rootToCount.apply(context.path()).gt(0);
    }

    @Override
    public SqaleUpdateContext<?, ?, ?> resolve(
            SqaleUpdateContext<?, Q, R> context, ItemPath itemPath) {
        return null; // indicates ignore
    }
}

/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping.item;

import java.util.function.BiFunction;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Resolver that knows how to add {@code JOIN} for the specified target query type.
 *
 * @param <Q> type of source entity path
 * @param <DQ> type of target entity path
 * @param <DR> row type related to the target entity path {@link DQ}
 */
public class TableRelationResolver<
        Q extends FlexibleRelationalPathBase<?>, DQ extends FlexibleRelationalPathBase<DR>, DR>
        implements ItemRelationResolver {

    private final Class<DQ> targetQueryType;
    private final BiFunction<Q, DQ, Predicate> joinPredicate;

    public TableRelationResolver(
            @NotNull Class<DQ> targetQueryType,
            @NotNull BiFunction<Q, DQ, Predicate> joinPredicate) {
        this.targetQueryType = targetQueryType;
        this.joinPredicate = joinPredicate;
    }

    /**
     * Creates the JOIN using provided query context.
     * This does not use the mapping parameter as it is useless for JOIN creation.
     *
     * @param context query context used for JOIN creation
     * @return result with context for JOINed entity path and its mapping
     */
    @Override
    public ResolutionResult resolve(SqlQueryContext<?, ?, ?> context) {
        //noinspection unchecked
        SqlQueryContext<?, DQ, DR> joinContext =
                ((SqlQueryContext<?, Q, ?>) context).leftJoin(targetQueryType, joinPredicate);

        return new ResolutionResult(joinContext, joinContext.mapping());
    }
}

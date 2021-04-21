/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.function.BiFunction;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.RootUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Resolver that knows how to add {@code JOIN} for the specified target query type.
 *
 * @param <Q> type of source entity path
 * @param <TQ> type of target entity path
 * @param <TR> row type related to the target entity path {@link TQ}
 */
public class TableRelationResolver<
        Q extends FlexibleRelationalPathBase<?>, TQ extends FlexibleRelationalPathBase<TR>, TR>
        implements SqaleItemRelationResolver {

    private final Class<TQ> targetQueryType;
    private final BiFunction<Q, TQ, Predicate> joinPredicate;

    public TableRelationResolver(
            @NotNull Class<TQ> targetQueryType,
            @NotNull BiFunction<Q, TQ, Predicate> joinPredicate) {
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
        SqlQueryContext<?, TQ, TR> joinContext =
                ((SqlQueryContext<?, Q, ?>) context).leftJoin(targetQueryType, joinPredicate);

        return new ResolutionResult(joinContext, joinContext.mapping());
    }

    @Override
    public UpdateResolutionResult resolve(RootUpdateContext<?, ?, ?> context) {
        // TODO for query above we can hop to another table with join, still using SqlQueryContext
        //  (just a new instance), but right now SqaleUpdateContext is not built for that.
        //  Options - superclass? Common interface? Parametrized to Flexible... instead of QObject?

        return null; // TODO: now fails outside with NPE
    }
}

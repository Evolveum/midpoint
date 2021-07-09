/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping;

import java.util.function.BiFunction;

import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Resolver that knows how to traverse to the specified target query type.
 * Traversal can be LEFT JOIN or EXISTS which is the default for multi-value table stored items.
 *
 * @param <Q> type of source entity path (where the mapping is)
 * @param <R> row type for {@link Q}
 * @param <TS> schema type for the target entity, can be owning container or object
 * @param <TQ> type of target entity path
 * @param <TR> row type related to the target entity path {@link TQ}
 */
public class TableRelationResolver<
        Q extends FlexibleRelationalPathBase<R>, R,
        TS, TQ extends FlexibleRelationalPathBase<TR>, TR>
        implements ItemRelationResolver<Q, R, TQ, TR> {

    protected final QueryTableMapping<TS, TQ, TR> targetMapping;
    protected final BiFunction<Q, TQ, Predicate> correlationPredicate;

    public TableRelationResolver(
            @NotNull QueryTableMapping<TS, TQ, TR> targetMapping,
            @NotNull BiFunction<Q, TQ, Predicate> correlationPredicate) {
        this.targetMapping = targetMapping;
        this.correlationPredicate = correlationPredicate;
    }

    /**
     * Creates the EXISTS subquery using provided query context.
     *
     * @param context query context used for subquery creation
     * @return result with context for subquery entity path and its mapping
     */
    @Override
    public ResolutionResult<TQ, TR> resolve(SqlQueryContext<?, Q, R> context) {
        SqlQueryContext<TS, TQ, TR> subcontext = context.subquery(targetMapping);
        SQLQuery<?> subquery = subcontext.sqlQuery();
        subquery.where(correlationPredicate.apply(context.path(), subcontext.path()));

        return new ResolutionResult<>(subcontext, subcontext.mapping(), true);
    }
}

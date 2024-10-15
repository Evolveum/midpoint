/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Resolver that knows how to traverse to the specified target query type.
 * By default, EXISTS subquery is used which is better for multi-value table stored items
 * to avoid result multiplication.
 * The resolver supports mapping supplier to avoid call cycles during mapping initialization.
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

    protected final Supplier<QueryTableMapping<TS, TQ, TR>> targetMappingSupplier;
    protected final BiFunction<Q, TQ, Predicate> correlationPredicateFunction;
    private final boolean useSubquery;

    public Supplier<QueryTableMapping<TS, TQ, TR>> getTargetMappingSupplier() {
        return targetMappingSupplier;
    }

    public static <Q extends FlexibleRelationalPathBase<R>, R, TS, TQ extends FlexibleRelationalPathBase<TR>, TR>
    TableRelationResolver<Q, R, TS, TQ, TR> usingSubquery(
            @NotNull QueryTableMapping<TS, TQ, TR> targetMapping,
            @NotNull BiFunction<Q, TQ, Predicate> correlationPredicateFunction) {
        return new TableRelationResolver<>(targetMapping, correlationPredicateFunction);
    }

    /**
     * Currently the decision to use `JOIN` is static in the mapping, but it can be more flexible.
     * If the query does not order by such a path, `EXISTS` is more efficient and should be used.
     * This would require order examination first and then using this info in {@link #resolve(SqlQueryContext)},
     * perhaps accessible via the context parameter.
     */
    public static <Q extends FlexibleRelationalPathBase<R>, R, TS, TQ extends FlexibleRelationalPathBase<TR>, TR>
    TableRelationResolver<Q, R, TS, TQ, TR> usingJoin(
            @NotNull Supplier<QueryTableMapping<TS, TQ, TR>> targetMappingSupplier,
            @NotNull BiFunction<Q, TQ, Predicate> correlationPredicateFunction) {
        return new TableRelationResolver<>(targetMappingSupplier, correlationPredicateFunction);
    }

    /**
     * Constructor for relation resolver using `EXISTS` subquery to the table.
     * This is good for multi-value containers.
     */
    protected TableRelationResolver(
            @NotNull QueryTableMapping<TS, TQ, TR> targetMapping,
            @NotNull BiFunction<Q, TQ, Predicate> correlationPredicateFunction) {
        this.targetMappingSupplier = () -> targetMapping;
        this.correlationPredicateFunction = correlationPredicateFunction;
        this.useSubquery = true;
    }

    /**
     * Constructor for table-stored relation resolver using `LEFT JOIN`.
     * This is good when we know only one result will match the joining condition,
     * e.g. owning object or object referenced by embedded (single-value) reference.
     * Using `JOIN` is necessary if ordering by the target is required.
     */
    private TableRelationResolver(
            @NotNull Supplier<QueryTableMapping<TS, TQ, TR>> targetMappingSupplier,
            @NotNull BiFunction<Q, TQ, Predicate> correlationPredicateFunction) {
        this.targetMappingSupplier = targetMappingSupplier;
        this.correlationPredicateFunction = correlationPredicateFunction;
        this.useSubquery = false;
    }

    /**
     * Creates the EXISTS subquery using provided query context.
     *
     * @param context query context used for subquery creation
     * @return result with context for subquery entity path and its mapping
     */
    @Override
    public ResolutionResult<TQ, TR> resolve(SqlQueryContext<?, Q, R> context, boolean parent) {
        if (useSubquery) {
            SqlQueryContext<TS, TQ, TR> subcontext = context.subquery(targetMappingSupplier.get());
            SQLQuery<?> subquery = subcontext.sqlQuery();
            subquery.where(correlationPredicateFunction.apply(context.path(), subcontext.path()));

            return new ResolutionResult<>(subcontext, subcontext.mapping(), true);
        }
        return resolveUsingJoin(context, parent);
    }

    @Override
    public ResolutionResult<TQ, TR> resolveUsingJoin(SqlQueryContext<?, Q, R> context) {
        return resolveUsingJoin(context, false);
    }

    private ResolutionResult<TQ, TR> resolveUsingJoin(SqlQueryContext<?, Q, R> context, boolean parent) {
        SqlQueryContext<TS, TQ, TR> subcontext;

        if (!parent) {
            subcontext = context.leftJoin(targetMappingSupplier.get(), correlationPredicateFunction);
            return new ResolutionResult<>(subcontext, subcontext.mapping());
        }

        if (context.getParentItemContext() != null) {
            // noinspection unchecked
            subcontext = (SqlQueryContext<TS, TQ, TR>) context.getParentItemContext();
            return new ResolutionResult<>(subcontext, subcontext.mapping());
        }

        subcontext = context.leftJoin(targetMappingSupplier.get(), correlationPredicateFunction);
        context.setParentItemContext(subcontext);

        return new ResolutionResult<>(subcontext, subcontext.mapping());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public TableRelationResolver<Q, R, TS, TQ, TR> replaceTable(QueryTableMapping<? extends TS, TQ, TR> target) {
        // FIXME: Add check

        return new TableRelationResolver(() -> target, correlationPredicateFunction);
    }

    public TableRelationResolver<Q, R, TS, TQ, TR> withSubquery() {
        return usingSubquery(targetMappingSupplier.get(), correlationPredicateFunction);
    }

    public <AQ extends FlexibleRelationalPathBase<AR>, AS, AR>
    TableRelationResolver<TQ, TR, AS, AQ, AR> reverse(
            @NotNull QueryTableMapping<AS, AQ, AR> targetMapping) {
        //noinspection unchecked
        return new TableRelationResolver<>(targetMapping, (t, a) -> correlationPredicateFunction.apply((Q) a, t));
    }

    public QueryTableMapping<TS, TQ, TR> mapping() {
        return targetMappingSupplier.get();
    }
}

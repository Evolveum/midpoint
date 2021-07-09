/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;

/**
 * Resolver that knows how to traverse from reference table to the reference target.
 * The resolver uses mapping lazily via supplier to avoid call cycles during mapping initialization,
 * otherwise it's functionally equivalent to {@link TableRelationResolver}.
 *
 * Ideal mapping type provided by the supplier is to be found in the schema inside
 * `<a:objectReferenceTargetType>` element for the reference, using more generic type means
 * Postgres has to search through more sub-tables than necessary and also doesn't provide
 * the right implicit type for further conditions on the target.
 *
 * @param <Q> type of reference entity path (where the mapping is)
 * @param <R> row type for {@link Q}
 * @param <TQ> type of target entity path, that is referenced object
 * @param <TR> row type related to the target entity path {@link TQ}
 */
public class RefTableTargetResolver<
        Q extends QReference<R, ?>, R extends MReference,
        TQ extends QObject<TR>, TR extends MObject>
        implements ItemRelationResolver<Q, R, TQ, TR> {

    private final Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier;
    private final BiFunction<Q, TQ, Predicate> correlationPredicate;

    public RefTableTargetResolver(
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier,
            @NotNull BiFunction<Q, TQ, Predicate> correlationPredicate) {
        this.targetMappingSupplier = targetMappingSupplier;
        this.correlationPredicate = correlationPredicate;
    }

    @Override
    public ResolutionResult<TQ, TR> resolve(SqlQueryContext<?, Q, R> context) {
        SqlQueryContext<?, TQ, TR> subcontext = context.subquery(
                targetMappingSupplier.get());
        SQLQuery<?> subquery = subcontext.sqlQuery();
        subquery.where(correlationPredicate.apply(context.path(), subcontext.path()));

        return new ResolutionResult<>(subcontext, subcontext.mapping(), true);
    }
}

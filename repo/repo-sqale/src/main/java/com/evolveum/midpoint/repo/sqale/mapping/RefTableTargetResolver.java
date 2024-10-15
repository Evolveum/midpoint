/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;

/**
 * Resolver that knows how to traverse from reference table to the reference target.
 * The resolver uses mapping lazily via supplier to avoid call cycles during mapping initialization.
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

    public RefTableTargetResolver(
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier) {
        this.targetMappingSupplier = targetMappingSupplier;
        // correlation predicate is always the same, so it's hard-coded in resolve()
    }

    @Override
    public ResolutionResult<TQ, TR> resolve(SqlQueryContext<?, Q, R> context, boolean parent) {
        /*
        Currently, the JOIN does not reuse existing alias for the same mapping - which would be better for targetOid.
        Also, this is used for ordering instructions, but not for WHERE clauses where EXISTS subquery is still used.
        See also: com.evolveum.midpoint.repo.sqale.filtering.RefItemFilterProcessor#targetFilterPredicate()
        */
        SqlQueryContext<?, TQ, TR> subcontext = context.leftJoin(
                targetMappingSupplier.get(), (q, p) -> q.targetOid.eq(p.oid));
        return new ResolutionResult<>(subcontext, subcontext.mapping());

        /*
        EXISTS is also possible - and perhaps even preferable for WHERE, but does not allow
        to use the target of the reference for ordering.
        Ordering by the ref target is not possible for multi-value refs with object/container search.
        It is possible for reference search, as each ref has its own role and a single target, so
        LEFT JOIN is actually beneficial there.

        SqlQueryContext<?, TQ, TR> subcontext = context.subquery(targetMappingSupplier.get());
        SQLQuery<?> subquery = subcontext.sqlQuery();
        subquery.where(context.path().targetOid.eq(subcontext.path().oid));

        return new ResolutionResult<>(subcontext, subcontext.mapping(), true);
        */
    }
}

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

import com.evolveum.midpoint.repo.sqale.qmodel.QOwnedBy;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Resolver that knows how to add {@code JOIN} for the specified target query type.
 *
 * @param <Q> type of source entity path
 * @param <R> row type for {@link Q}, this is the owner of the target table
 * @param <TS> schema type for the target entity
 * @param <TQ> type of target entity path
 * @param <TR> row type related to the target entity path {@link TQ}
 */
// TODO: Can we have just this one time for both container and references?
//  If not (probably because of modify), subclass it.
public class TableRelationResolver<
        Q extends FlexibleRelationalPathBase<R>, R,
        TS, TQ extends FlexibleRelationalPathBase<TR> & QOwnedBy<R>, TR>
        // TODO how to add & QOwnedByMapping without clashing on transformer? perhaps it will not be necessary to capture here
//        M extends SqaleTableMapping<?, TQ, TR>> // without & the M is not necessary
        implements SqaleItemRelationResolver<Q, R> {

    private final SqaleTableMapping<TS, TQ, TR> targetMapping;
    private final BiFunction<Q, TQ, Predicate> joinPredicate;

    public TableRelationResolver(
            @NotNull SqaleTableMapping<TS, TQ, TR> targetMapping,
            @NotNull BiFunction<Q, TQ, Predicate> joinPredicate) {
        this.targetMapping = targetMapping;
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
    public ResolutionResult resolve(SqlQueryContext<?, Q, R> context) {
        SqlQueryContext<TS, TQ, TR> joinContext =
                context.leftJoin(targetMapping.queryType(), joinPredicate);

        return new ResolutionResult(joinContext, joinContext.mapping());
    }

    @Override
    public UpdateResolutionResult resolve(SqaleUpdateContext<?, Q, R> context) {
        // TODO for query above we can hop to another table with join, still using SqlQueryContext
        //  (just a new instance), but right now SqaleUpdateContext is not built for that.
        //  Options - superclass? Common interface? Parametrized to Flexible... instead of QObject?

//        return null; // TODO: now fails outside with NPE
//        new ContainerTableDeltaProcessor<>
        return new UpdateResolutionResult(context, targetMapping);
    }
}

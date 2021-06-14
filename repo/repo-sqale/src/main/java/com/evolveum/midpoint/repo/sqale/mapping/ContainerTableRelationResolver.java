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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.qmodel.QOwnedBy;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.update.ContainerTableUpdateContext;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Resolver that knows how to join to the specified target query type (can be JOIN or EXISTS).
 *
 * @param <Q> type of source entity path (where the mapping is)
 * @param <R> row type for {@link Q}, this is the owner of the target table
 * @param <TS> schema type for the target entity
 * @param <TQ> type of target entity path
 * @param <TR> row type related to the target entity path {@link TQ}
 */
public class ContainerTableRelationResolver<
        Q extends FlexibleRelationalPathBase<R>, R,
        TS extends Containerable, TQ extends QContainer<TR, R> & QOwnedBy<R>, TR extends MContainer>
        implements SqaleItemRelationResolver<Q, R> {

    private final QContainerMapping<TS, TQ, TR, R> targetMapping;
    private final BiFunction<Q, TQ, Predicate> joinPredicate;

    public ContainerTableRelationResolver(
            @NotNull QContainerMapping<TS, TQ, TR, R> targetMapping,
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
    public ContainerTableUpdateContext<TS, TQ, TR, R> resolve(
            SqaleUpdateContext<?, Q, R> context, ItemPath itemPath) {
        if (itemPath == null || itemPath.size() != 2 || !(itemPath.getSegment(1) instanceof Long)) {
            throw new IllegalArgumentException(
                    "Item path provided for container table relation resolver must have two"
                            + " segments with PCV ID as the second");
        }
        TR row = targetMapping.newRowObject(context.row());
        //noinspection ConstantConditions
        row.cid = (long) itemPath.getSegment(1);
        return new ContainerTableUpdateContext<>(context, targetMapping, row);
    }
}

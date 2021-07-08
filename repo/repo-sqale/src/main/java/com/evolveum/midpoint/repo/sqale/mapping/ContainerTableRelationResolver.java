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
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Resolver that knows how to traverse to the specified target query type (can be JOIN or EXISTS).
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
        extends TableRelationResolver<Q, R, TS, TQ, TR>
        implements SqaleItemRelationResolver<Q, R, TQ, TR> {

    public ContainerTableRelationResolver(
            @NotNull QContainerMapping<TS, TQ, TR, R> targetMapping,
            @NotNull BiFunction<Q, TQ, Predicate> correlationPredicate) {
        super(targetMapping, correlationPredicate);
    }

    @Override
    public ContainerTableUpdateContext<TS, TQ, TR, R> resolve(
            SqaleUpdateContext<?, Q, R> context, ItemPath itemPath) {
        if (itemPath == null || itemPath.size() != 2 || !(itemPath.getSegment(1) instanceof Long)) {
            throw new IllegalArgumentException(
                    "Item path provided for container table relation resolver must have two"
                            + " segments with PCV ID as the second");
        }
        QContainerMapping<TS, TQ, TR, R> containerMapping =
                (QContainerMapping<TS, TQ, TR, R>) this.targetMapping;
        TR row = containerMapping.newRowObject(context.row());
        //noinspection ConstantConditions
        row.cid = (long) itemPath.getSegment(1);
        // TODO check actual container existence? E.g. run test332ModifiedCertificationCaseStoresIt
        //  isolated and it ignores the missing container here and later fails with NPE.
        //  Funny thing is, that modification.applyTo(prism) only logs WARN and doesn't care anymore.
        return new ContainerTableUpdateContext<>(context, containerMapping, row);
    }
}

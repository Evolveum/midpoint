/*
 * Copyright (C) 2010-2022 Evolveum and contributors
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
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Resolver that knows how to traverse to the specified container table.
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
            SqaleUpdateContext<?, Q, R> context, ItemPath itemPath) throws RepositoryException {
        if (itemPath == null || itemPath.size() != 2 || !(ItemPath.isId(itemPath.getSegment(1)))) {
            throw new IllegalArgumentException(
                    "Item path provided for container table relation resolver must have two"
                            + " segments with PCV ID as the second");
        }
        // TODO: this works for top level containers, but not for nested, e.g. this tries to resolve
        //  workItem/3 on root object for modification on case/2/workItem/3 (during the resolution).
        //  Normally context.findValueOrItem is used with modification.getPath(), but here it's more
        //  relative, which obviously can't work.
        //  Perhaps findValueOrItemRelative() is needed, ContainerTableUpdateContext should capture
        //  the container value it resolves and find in that one, not delegate to the root context.
        //  TestCertificationBasic will fail if this is not fixed, but it's OK to leave it out - it
        //  only makes errors down the line more confusing.
//        if (context.findValueOrItem(itemPath) == null) {
//            throw new RepositoryException("Container for path '" + itemPath + "' does not exist!");
//        }

        QContainerMapping<TS, TQ, TR, R> containerMapping =
                (QContainerMapping<TS, TQ, TR, R>) targetMappingSupplier.get();
        TR row = containerMapping.newRowObject(context.row());
        row.cid = ItemPath.toId(itemPath.getSegment(1));
        return new ContainerTableUpdateContext<>(context, containerMapping, row);
    }
}

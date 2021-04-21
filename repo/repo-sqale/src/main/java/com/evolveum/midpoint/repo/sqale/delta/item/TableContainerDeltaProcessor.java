/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.ContainerSqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Delta processor for multi-value containers stored in separate tables.
 *
 * @param <T> schema type for container
 * @param <Q> query type for container table
 * @param <R> row type for container table, related to {@link Q}
 * @param <OQ> query type of the table owning this container
 * @param <OR> row type of the table owning this container, related to {@link OQ}
 */
public class TableContainerDeltaProcessor<
        T extends Containerable, Q extends QContainer<R>, R extends MContainer,
        OQ extends FlexibleRelationalPathBase<OR>, OR>
        extends ItemDeltaValueProcessor<T> {

    private final SqaleUpdateContext<?, OQ, OR> context;
    private final QContainerMapping<T, Q, R> containerTableMapping;

    public TableContainerDeltaProcessor(
            @NotNull SqaleUpdateContext<?, OQ, OR> context,
            @NotNull QContainerMapping<T, Q, R> containerTableMapping) {
        super(context);
        this.context = context;
        this.containerTableMapping = containerTableMapping;
    }

    @Override
    public void addValues(Collection<T> values) {
        OR ownerRow = context.row(); // TODO what about other owner type?
        ContainerSqlTransformer<T, Q, R> transformer =
                containerTableMapping.createTransformer(context.transformerSupport());

        // It looks like the insert belongs to context, but there is no common insert contract.
        // Each transformer has different types and needs. What? No, I don't want to introduce
        // owner row type as another parametrized type on the transformer, thank you.
        // TODO introduce owner type to sub-tables and revisit also RefTableItemDeltaProcessor
        //  perhaps it can go under context after all?
//        values.forEach(ref -> transformer.insert(ref, ownerRow, context.jdbcSession()));
    }

    @Override
    public void deleteValues(Collection<T> values) {
        Q c = containerTableMapping.defaultAlias();
        for (T container : values) {
            /*
            context.jdbcSession().newDelete(c)
                    // TODO - this is ok for containers under object directly, but not under containers
                    .where(c.ownerOid.eq(context.objectOid())
                            .and(c.cid.eq(container.asPrismContainerValue().getId())))
                    .execute();
             */
        }
    }

    @Override
    public void delete() {
        QContainer<?> r = containerTableMapping.defaultAlias();
        /*
        context.jdbcSession().newDelete(r)
                // TODO - this is ok for containers under object directly, but not under containers
                .where(r.ownerOid.eq(context.objectOid()))
                .execute();
         */
    }
}

/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.update;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.sql.dml.SQLUpdateClause;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Update context for multi-value containers stored in separate table.
 * This can be owned by the root object or another container.
 *
 * @param <S> schema type of the container stored in the owned table
 * @param <Q> type of entity path for the owned (child) table
 * @param <R> row type related to the {@link Q}
 * @param <OR> owner row type
 */
public class ContainerTableUpdateContext<S extends Containerable, Q extends QContainer<R, OR>, R extends MContainer, OR>
        extends SqaleUpdateContext<S, Q, R> {

    private final Q path;
    private final SQLUpdateClause update;
    private final QContainerMapping<S, Q, R, OR> mapping;

    /**
     * Creates the context for container component of the path, skeleton/fake row of the container
     * with pre-filled CID and FK referencing the owner row must be provided.
     */
    public ContainerTableUpdateContext(
            SqaleUpdateContext<?, ?, OR> parentContext,
            QContainerMapping<S, Q, R, OR> mapping,
            R row) {
        super(parentContext, row);
        this.mapping = mapping;

        path = mapping.defaultAlias();
        // we create the update, but only use it if set methods are used
        update = jdbcSession.newUpdate(path)
                .where(path.isOwnedBy(parentContext.row())
                        .and(path.cid.eq(row.cid)));
    }

    @Override
    public Q entityPath() {
        return path;
    }

    @Override
    public QContainerMapping<S, Q, R, OR> mapping() {
        return mapping;
    }

    public SQLUpdateClause update() {
        return update;
    }

    @Override
    public <P extends Path<T>, T> void set(P path, T value) {
        update.set(path, value);
    }

    @Override
    public <P extends Path<T>, T> void set(P path, Expression<T> value) {
        update.set(path, value);
    }

    @Override
    public <P extends Path<T>, T> void setNull(P path) {
        update.setNull(path);
    }

    /** Executes updates if applicable, nothing is done if set methods were not used. */
    @Override
    protected void finishExecutionOwn() throws SchemaException {
        mapping.afterModify(this);
        if (!update.isEmpty()) {
            update.execute();
        }
    }
}

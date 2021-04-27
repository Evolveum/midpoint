/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.update;

import com.querydsl.core.types.Path;
import com.querydsl.sql.dml.SQLUpdateClause;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;

/**
 * Update context for owned containers stored in tables.
 * This can be owned by the root object or another container.
 * TODO - this is theory, before implementation:
 * Updates are collected as the modifications are processed and then executed by the root context.
 * Inserts are executed immediately to allow nested inserts (e.g. container inside the container).
 *
 * @param <S> schema type of the object stored in the owned (child) table
 * @param <Q> type of entity path for the owned (child) table
 * @param <R> row type related to the {@link Q}
 * @param <OR> owner row type
 */
public class ContainerTableUpdateContext<S extends Containerable, Q extends QContainer<R, OR>, R extends MContainer, OR>
        extends SqaleUpdateContext<S, Q, R> {

    private final Q path;
    private final SQLUpdateClause update;
    private final SqaleTableMapping<S, Q, R> mapping;

    public ContainerTableUpdateContext(
            SqaleUpdateContext<?, ?, OR> parentContext,
            SqaleTableMapping<S, Q, R> mapping) {
        super(parentContext, null); // TODO what is row? fake row with owner + id
        this.mapping = mapping;

        path = mapping.defaultAlias();
        // we create the update, but only use it if set methods are used
        update = jdbcSession.newUpdate(path)
                .where(path.isOwnedBy(parentContext.row()));
        // TODO add CID condition after writing test that checks only the right container is changed :-)
    }

    public Q path() {
        return path;
    }

    @Override
    public SqaleTableMapping<S, Q, R> mapping() {
        return mapping;
    }

    public SQLUpdateClause update() {
        return update;
    }

    public <P extends Path<T>, T> void set(P path, T value) {
        update.set(path, value);
    }

    /** Executes updates if applicable, nothing is done if set methods were not used. */
    @Override
    protected void finishExecutionOwn() {
        if (!update.isEmpty()) {
            update.execute();
        }
    }
}

/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.update;

import javax.xml.namespace.QName;

import com.querydsl.core.types.Path;

import com.evolveum.midpoint.repo.sqale.SqaleTransformerSupport;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.delta.item.UriItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QOwnedByMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.TransformerForOwnedBy;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Update context manages state information related to the currently executed modify operation.
 * Contexts can be nested for any non-trivial updates where each instance matches the changed item
 * and its mapping.
 * {@link RootUpdateContext} is the top level parent context that holds the main object.
 *
 * For example, given a path `assignment/1/metadata/channel`:
 *
 * * {@link RootUpdateContext} is created for the object, imagine it at the root of the path.
 * * {@link ContainerUpdateContext} is created for `assignment/1` part under the root context.
 * * {@link NestedContainerUpdateContext} is created for `metadata` part.
 * * One of {@link ItemDeltaValueProcessor} is then created for `channel` based on the type of
 * the property (in this case {@link UriItemDeltaProcessor}) which processes actual values and
 * uses the context above to do so.
 *
 * In this example channel is single-valued property and will be changed adding a `SET` clause
 * to the `UPDATE` held by the container table update context.
 * This `UPDATE` will use proper `WHERE` clause negotiated between this context and its parent,
 * which in this case is the root context, using also the container ID provided in the item path.
 * Nested container context merely "focuses" from assignment container to its metadata, but the
 * table is still the same.
 *
 * @param <S> schema type of the mapped object (potentially in nested mapping)
 * @param <Q> query entity type
 * @param <R> row type related to the {@link Q}
 */
public abstract class SqaleUpdateContext<S, Q extends FlexibleRelationalPathBase<R>, R> {

    protected final Trace logger = TraceManager.getTrace(getClass());

    protected final SqaleTransformerSupport transformerSupport;
    protected final SqaleTableMapping<S, Q, R> mapping;
    protected final JdbcSession jdbcSession;
    protected final S object;
    protected final R row;

    public SqaleUpdateContext(SqaleTransformerSupport sqlTransformerSupport,
            SqaleTableMapping<S, Q, R> mapping,
            JdbcSession jdbcSession, S object, R row) {
        this.transformerSupport = sqlTransformerSupport;
        this.mapping = mapping;
        this.jdbcSession = jdbcSession;
        this.object = object;
        this.row = row;
    }

    public SqaleUpdateContext(SqaleUpdateContext<?, ?, ?> parentContext, S object, R row) {
        this.transformerSupport = parentContext.transformerSupport;
        this.mapping = transformerSupport.sqlRepoContext()
                .getMappingBySchemaType(SqaleUtils.getClass(object));
        this.jdbcSession = parentContext.jdbcSession();
        this.object = object;
        this.row = row;
    }

    public SqaleTransformerSupport transformerSupport() {
        return transformerSupport;
    }

    public Integer processCacheableRelation(QName relation) {
        return transformerSupport.processCacheableRelation(relation);
    }

    public Integer processCacheableUri(String uri) {
        return transformerSupport.processCacheableUri(uri);
    }

    public JdbcSession jdbcSession() {
        return jdbcSession;
    }

    public S schemaObject() {
        return object;
    }

    public R row() {
        return row;
    }

    public abstract Q path();

    public abstract <P extends Path<T>, T> void set(P path, T value);

    @SuppressWarnings("UnusedReturnValue")
    public <TS, TR> TR insertOwnedRow(QOwnedByMapping<TS, TR, R> mapping, TS schemaObject) {
        TransformerForOwnedBy<TS, TR, R> transformer =
                mapping.createTransformer(transformerSupport());
        return transformer.insert(schemaObject, row, jdbcSession);
    }
}

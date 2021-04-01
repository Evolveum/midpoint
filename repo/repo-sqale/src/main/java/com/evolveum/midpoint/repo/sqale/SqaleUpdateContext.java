/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.util.UUID;
import javax.xml.namespace.QName;

import com.querydsl.sql.dml.SQLUpdateClause;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqale.delta.SqaleItemSqlMapper;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemSqlMapper;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * TODO
 *
 * @param <S> schema type
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public class SqaleUpdateContext<S extends ObjectType, Q extends QObject<R>, R extends MObject> {

    protected final SqaleTableMapping<S, Q, R> mapping;
    protected final JdbcSession jdbcSession;
    protected final PrismObject<S> prismObject;
    protected final Q rootPath;
    private final SQLUpdateClause update;

    public SqaleUpdateContext(
            SqaleTableMapping<S, Q, R> mapping,
            JdbcSession jdbcSession,
            PrismObject<S> prismObject) {
        this.mapping = mapping;
        this.jdbcSession = jdbcSession;
        this.prismObject = prismObject;
        rootPath = mapping.defaultAlias();
        update = jdbcSession.newUpdate(rootPath)
                .where(rootPath.oid.eq(UUID.fromString(prismObject.getOid())));
    }

    public Q path() {
        return rootPath;
    }

    public void processModification(ItemDelta<?, ?> modification) throws RepositoryException {
        QName itemPath = modification.getPath().asSingleName();
        // TODO later resolution of complex paths just like for filters
        ItemSqlMapper itemSqlMapper = mapping.itemMapper(itemPath);
        if (itemSqlMapper instanceof SqaleItemSqlMapper) {
            ((SqaleItemSqlMapper) itemSqlMapper)
                    .createItemDeltaProcessor(this)
                    .process(modification);
        } else {
            throw new IllegalArgumentException("No delta processor available for " + itemPath
                    + " in mapping " + mapping + "! (Only query mapping is available.)");
        }
    }

    /** Updates version in enclosed {@link #prismObject} and adds corresponding set clause. */
    public void incrementVersion() {
        int newVersion = SqaleUtils.objectVersionAsInt(prismObject) + 1;
        prismObject.setVersion(String.valueOf(newVersion));
        update.set(rootPath.version, newVersion);
    }

    /**
     * Serializes enclosed {@link #prismObject} and adds set clause to the update.
     * This should be the last update otherwise following changes are not reflected in the stored
     * full object.
     */
    public void updateFullObject(SqlTransformerSupport transformerSupport) throws SchemaException {
        ObjectSqlTransformer<S, Q, R> transformer =
                (ObjectSqlTransformer<S, Q, R>) mapping.createTransformer(transformerSupport);
        update.set(rootPath.fullObject, transformer.createFullObject(prismObject.asObjectable()));
    }

    public void execute() {
        update.execute();
    }
}

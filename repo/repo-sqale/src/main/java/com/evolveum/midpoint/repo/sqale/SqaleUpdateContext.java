/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import static com.evolveum.midpoint.repo.sqale.SqaleUtils.objectVersionAsInt;

import java.util.UUID;
import javax.xml.namespace.QName;

import com.querydsl.core.types.Path;
import com.querydsl.sql.dml.SQLUpdateClause;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqale.mapping.delta.SqaleItemSqlMapper;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
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

    private final SqaleTransformerSupport transformerSupport;
    private final JdbcSession jdbcSession;
    private final PrismObject<S> prismObject;

    private final SqaleTableMapping<S, Q, R> mapping;
    private final Q rootPath;
    private final SQLUpdateClause update;
    private final UUID objectOid;
    private final int objectVersion;

    public SqaleUpdateContext(
            SqaleTransformerSupport sqlTransformerSupport,
            JdbcSession jdbcSession,
            PrismObject<S> prismObject) {
        this.transformerSupport = sqlTransformerSupport;
        this.jdbcSession = jdbcSession;
        this.prismObject = prismObject;

        this.mapping = transformerSupport.sqlRepoContext()
                .getMappingBySchemaType(prismObject.getCompileTimeClass());
        rootPath = mapping.defaultAlias();
        objectOid = UUID.fromString(prismObject.getOid());
        objectVersion = objectVersionAsInt(prismObject);
        update = jdbcSession.newUpdate(rootPath)
                .where(rootPath.oid.eq(objectOid)
                        .and(rootPath.version.eq(objectVersion)));
    }

    public Q path() {
        return rootPath;
    }

    public void processModification(ItemDelta<?, ?> modification) throws RepositoryException {
        QName itemPath = modification.getPath().asSingleName();
        if (itemPath == null) {
            return; // TODO no action now, we don't want NPE
        }

        // TODO later resolution of complex paths just like for filters
        ItemSqlMapper itemSqlMapper = mapping.getItemMapper(itemPath);
        if (itemSqlMapper instanceof SqaleItemSqlMapper) {
            ((SqaleItemSqlMapper) itemSqlMapper)
                    .createItemDeltaProcessor(this)
                    .process(modification);
        } else if (itemSqlMapper != null) {
            throw new IllegalArgumentException("No delta processor available for " + itemPath
                    + " in mapping " + mapping + "! (Only query mapping is available.)");
        }

        // if the mapper null it is not indexed ("externalized") attribute, no action
    }

    /**
     * Executes all necessary SQL updates (including sub-entity inserts/deletes)
     * for the enclosed {@link #prismObject}.
     * This also increments the version information and serializes `fullObject`.
     */
    public void execute() throws SchemaException, RepositoryException {
        int newVersion = objectVersionAsInt(prismObject) + 1;
        prismObject.setVersion(String.valueOf(newVersion));
        update.set(rootPath.version, newVersion);

        ObjectSqlTransformer<S, Q, R> transformer =
                (ObjectSqlTransformer<S, Q, R>) mapping.createTransformer(transformerSupport);
        update.set(rootPath.fullObject, transformer.createFullObject(prismObject.asObjectable()));

        long rows = update.execute();
        if (rows != 1) {
            throw new RepositoryException("Object " + objectOid + " with supposed version "
                    + objectVersion + " could not be updated (concurrent access?).");
        }
    }

    public SQLUpdateClause update() {
        return update;
    }

    public <P extends Path<T>, T> void set(P path, T value) {
        update.set(path, value);
    }

    public SqaleTransformerSupport transformerSupport() {
        return transformerSupport;
    }

    public Integer processCacheableRelation(QName relation) {
        return transformerSupport.processCacheableRelation(relation, jdbcSession);
    }

    public Integer processCacheableUri(String uri) {
        return transformerSupport.processCacheableUri(uri, jdbcSession);
    }
}

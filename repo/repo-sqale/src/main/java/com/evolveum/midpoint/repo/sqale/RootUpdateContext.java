/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import static com.evolveum.midpoint.repo.sqale.SqaleUtils.objectVersionAsInt;

import java.util.Collection;
import java.util.UUID;
import javax.xml.namespace.QName;

import com.querydsl.core.types.Path;
import com.querydsl.sql.dml.SQLUpdateClause;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.repo.sqale.delta.DelegatingItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * TODO
 *
 * @param <S> schema type
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public class RootUpdateContext<S extends ObjectType, Q extends QObject<R>, R extends MObject> {

    private static final Trace LOGGER = TraceManager.getTrace(RootUpdateContext.class);

    private final SqaleTransformerSupport transformerSupport;
    private final JdbcSession jdbcSession;
    private final S object;
    private final R rootRow;

    private final SqaleTableMapping<S, Q, R> mapping;
    private final Q rootPath;
    private final SQLUpdateClause update;
    private final int objectVersion;

    private ContainerValueIdGenerator cidGenerator;

    public RootUpdateContext(SqaleTransformerSupport sqlTransformerSupport,
            JdbcSession jdbcSession, S object, R rootRow) {
        this.transformerSupport = sqlTransformerSupport;
        this.jdbcSession = jdbcSession;
        this.object = object;
        this.rootRow = rootRow;

        //noinspection unchecked
        this.mapping = transformerSupport.sqlRepoContext()
                .getMappingBySchemaType((Class<S>) object.getClass());
        rootPath = mapping.defaultAlias();
        objectVersion = objectVersionAsInt(object);
        update = jdbcSession.newUpdate(rootPath)
                .where(rootPath.oid.eq(rootRow.oid)
                        .and(rootPath.version.eq(objectVersion)));
    }

    public Q path() {
        return rootPath;
    }

    /** Applies modifications, executes necessary updates and returns narrowed modifications. */
    public Collection<? extends ItemDelta<?, ?>> execute(
            Collection<? extends ItemDelta<?, ?>> modifications)
            throws SchemaException, RepositoryException {

        PrismObject<S> prismObject = getPrismObject();

        // I reassign here, we DON'T want original modifications to be used further by accident
        modifications = prismObject.narrowModifications(
                modifications, EquivalenceStrategy.DATA,
                EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS, true);
        LOGGER.trace("Narrowed modifications:\n{}", DebugUtil.debugDumpLazily(modifications));

        if (modifications.isEmpty()) {
            return modifications; // no need to execute any update
        }

        cidGenerator = new ContainerValueIdGenerator()
                .forModifyObject(getPrismObject(), rootRow.containerIdSeq);

        for (ItemDelta<?, ?> modification : modifications) {
            try {
                processModification(modification);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Modification failed/not implemented yet: {}", e.toString());
            }
        }

        transformerSupport.normalizeAllRelations(prismObject);
        finishExecution();

        return modifications;
    }

    private void processModification(ItemDelta<?, ?> modification)
            throws RepositoryException, SchemaException {
        cidGenerator.processModification(modification);
        modification.applyTo(getPrismObject());

        new DelegatingItemDeltaProcessor(this, mapping)
                .process(modification);
    }

    /**
     * Executes all necessary SQL updates (including sub-entity inserts/deletes)
     * for the enclosed {@link #object}.
     * This also increments the version information and serializes `fullObject`.
     */
    public void finishExecution() throws SchemaException, RepositoryException {
        int newVersion = objectVersionAsInt(object) + 1;
        object.setVersion(String.valueOf(newVersion));
        update.set(rootPath.version, newVersion);

        update.set(rootPath.containerIdSeq, cidGenerator.lastUsedId() + 1);

        ObjectSqlTransformer<S, Q, R> transformer =
                (ObjectSqlTransformer<S, Q, R>) mapping.createTransformer(transformerSupport);
        update.set(rootPath.fullObject, transformer.createFullObject(object));

        long rows = update.execute();
        if (rows != 1) {
            throw new RepositoryException("Object " + objectOid() + " with supposed version "
                    + objectVersion + " could not be updated (concurrent access?).");
        }
    }

    public SQLUpdateClause update() {
        return update;
    }

    public <P extends Path<T>, T> void set(P path, T value) {
        update.set(path, value);
    }

    public UUID objectOid() {
        return rootRow.oid;
    }

    public R row() {
        return rootRow;
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

    public S getObject() {
        return object;
    }

    public PrismObject<S> getPrismObject() {
        //noinspection unchecked
        return (PrismObject<S>) object.asPrismObject();
    }
}

/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.object;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectionHolderType.*;

import java.util.Objects;

import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectionHolderType;


/**
 * Mapping between {@link QProjectionHolder} and {@link ProjectionHolderType}.
 *
 * <p>Projection holder is an abstract type, its table is a parent of the focus and case tables
 * (both of which can hold projections). Searching this type therefore returns all foci and cases.
 *
 * @param <S> schema type for the projection holder object
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public class QProjectionHolderMapping<
        S extends ProjectionHolderType, Q extends QProjectionHolder<R>, R extends MObject>
        extends QAssignmentHolderMapping<S, Q, R> {

    public static final String DEFAULT_ALIAS_NAME = "ph";
    private static QProjectionHolderMapping<ProjectionHolderType, QProjectionHolder<MObject>, MObject> instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QProjectionHolderMapping<?, ?, ?> initProjectionHolderMapping(
            @NotNull SqaleRepoContext repositoryContext) {
        instance = new QProjectionHolderMapping<>(QProjectionHolder.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ProjectionHolderType.class, QProjectionHolder.CLASS,
                repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QProjectionHolderMapping<?, ?, ?> getProjectionHolderMapping() {
        return Objects.requireNonNull(instance);
    }

    protected QProjectionHolderMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType,
            @NotNull SqaleRepoContext repositoryContext) {
        super(tableName, defaultAliasName, schemaType, queryType, repositoryContext);

        addRefMapping(F_LINK_REF, QObjectReferenceMapping.initForProjection(repositoryContext));
    }

    @Override
    protected Q newAliasInstance(String alias) {
        //noinspection unchecked
        return (Q) new QProjectionHolder<>(MObject.class, alias);
    }

    @Override
    public void storeRelatedEntities(@NotNull R row, @NotNull S schemaObject, @NotNull JdbcSession jdbcSession) throws SchemaException {
        super.storeRelatedEntities(row, schemaObject, jdbcSession);
        storeRefs(row, schemaObject.getLinkRef(),
                QObjectReferenceMapping.getForProjection(), jdbcSession);
    }
}

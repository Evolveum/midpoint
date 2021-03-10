/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import java.util.Collection;
import java.util.UUID;
import javax.xml.namespace.QName;

import com.querydsl.core.Tuple;
import com.querydsl.sql.ColumnMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evolveum.midpoint.repo.sqale.MObjectType;
import com.evolveum.midpoint.repo.sqale.SqaleTransformerSupport;
import com.evolveum.midpoint.repo.sqale.UriCache;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.SqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public abstract class SqaleTransformerBase<S, Q extends FlexibleRelationalPathBase<R>, R>
        implements SqlTransformer<S, Q, R> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final SqaleTransformerSupport transformerSupport;
    protected final QueryTableMapping<S, Q, R> mapping;

    /**
     * Constructor uses {@link SqlTransformerSupport} type even when it really is
     * {@link SqaleTransformerSupport}, but this way we can cast it just once here; otherwise cast
     * would be needed in each implementation of {@link QueryTableMapping#createTransformer)}.
     * (Alternative is to parametrize {@link QueryTableMapping} with various {@link SqlTransformer}
     * types which is not convenient at all. This little downcast is low price to pay.)
     */
    protected SqaleTransformerBase(
            SqlTransformerSupport transformerSupport,
            QueryTableMapping<S, Q, R> mapping) {
        this.transformerSupport = (SqaleTransformerSupport) transformerSupport;
        this.mapping = mapping;
    }

    @Override
    public S toSchemaObject(R row) {
        throw new UnsupportedOperationException("Use toSchemaObject(Tuple,...)");
    }

    /**
     * Transforms row Tuple containing {@link R} under entity path and extension columns.
     */
    @Override
    public S toSchemaObject(Tuple tuple, Q entityPath,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException {
        S schemaObject = toSchemaObject(tuple.get(entityPath));
        processExtensionColumns(schemaObject, tuple, entityPath);
        return schemaObject;
    }

    @SuppressWarnings("unused")
    protected void processExtensionColumns(S schemaObject, Tuple tuple, Q entityPath) {
        // empty by default, can be overridden
    }

    /**
     * Returns {@link ObjectReferenceType} with specified oid, proper type based on
     * {@link MObjectType} and, optionally, target name/description.
     * Returns {@code null} if OID is null.
     * Fails if OID is not null and {@code repoObjectType} is null.
     */
    @Nullable
    protected ObjectReferenceType objectReferenceType(
            @Nullable String oid, MObjectType repoObjectType, String targetName) {
        if (oid == null) {
            return null;
        }
        if (repoObjectType == null) {
            throw new IllegalArgumentException(
                    "NULL object type provided for object reference with OID " + oid);
        }

        return new ObjectReferenceType()
                .oid(oid)
                .type(transformerSupport.schemaClassToQName(repoObjectType.getSchemaType()))
                .description(targetName)
                .targetName(targetName);
    }

    /**
     * Trimming the value to the column size from column metadata (must be specified).
     */
    protected @Nullable String trim(
            @Nullable String value, @NotNull ColumnMetadata columnMetadata) {
        if (!columnMetadata.hasSize()) {
            throw new IllegalArgumentException(
                    "trimString with column metadata without specified size: " + columnMetadata);
        }
        return MiscUtil.trimString(value, columnMetadata.getSize());
    }

    /**
     * Returns ID for relation QName without going ot database.
     * Relation is normalized before consulting {@link UriCache}.
     */
    protected Integer resolveRelationToId(QName qName) {
        return qName != null
                ? resolveUriToId(QNameUtil.qNameToUri(transformerSupport.normalizeRelation(qName)))
                : null;
    }

    /** Returns ID for cached URI without going ot database. */
    protected Integer resolveUriToId(String uri) {
        return transformerSupport.resolveUriToId(uri);
    }

    /**
     * Returns ID for relation QName creating new {@link QUri} row in DB as needed.
     * Relation is normalized before consulting the cache.
     */
    protected Integer processCacheableRelation(QName qName, JdbcSession jdbcSession) {
        return qName == null ? null : processCacheableUri(
                QNameUtil.qNameToUri(transformerSupport.normalizeRelation(qName)),
                jdbcSession);
    }

    /** Returns ID for URI creating new cache row in DB as needed. */
    protected Integer processCacheableUri(String uri, JdbcSession jdbcSession) {
        return transformerSupport.processCachedUri(uri, jdbcSession);
    }

    protected @Nullable UUID oidToUUid(@Nullable String oid) {
        return oid != null ? UUID.fromString(oid) : null;
    }

    protected MObjectType schemaTypeToObjectType(QName schemaType) {
        return schemaType == null ? null :
                MObjectType.fromSchemaType(
                        transformerSupport.qNameToSchemaClass(schemaType));
    }
}

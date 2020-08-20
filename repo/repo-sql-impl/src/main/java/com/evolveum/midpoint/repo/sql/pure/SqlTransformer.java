/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure;

import com.querydsl.core.Tuple;
import com.querydsl.sql.ColumnMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Base class for SQL transformers translating from query beans or tuples to model types.
 *
 * @param <S> schema type
 * @param <R> type of the transformed data, a row bean
 */
public abstract class SqlTransformer<S, Q extends FlexibleRelationalPathBase<R>, R> {

    protected final PrismContext prismContext;
    protected final QueryModelMapping<S, Q, R> mapping;

    protected SqlTransformer(PrismContext prismContext, QueryModelMapping<S, Q, R> mapping) {
        this.prismContext = prismContext;
        this.mapping = mapping;
    }

    /**
     * Transforms row of R to M type - typically a model/schema object.
     * If pre-generated bean is used as row it does not include extension (dynamic) columns,
     * which is OK if extension columns are used only for query and their information
     * is still contained in the object somehow else (e.g. full object LOB).
     * <p>
     * Alternative would be dynamically generated list of select expressions and transforming
     * row to M object directly from {@link com.querydsl.core.Tuple}.
     */
    public abstract S toSchemaObject(R row) throws SchemaException;

    /**
     * Transforms row Tuple containing {@link R} under entity path and extension columns.
     */
    public S toSchemaObject(Tuple tuple, Q entityPath) throws SchemaException {
        S schemaObject = toSchemaObject(tuple.get(entityPath));
        processExtensionColumns(schemaObject, tuple, entityPath);
        return schemaObject;
    }

    protected void processExtensionColumns(S schemaObject, Tuple tuple, Q entityPath) {
        // empty by default, can be overridden
    }

    /**
     * Version of {@link #toSchemaObject(Object)} rethrowing checked exceptions as unchecked
     * {@link SqlTransformationException} - this is useful for lambda/method references usages.
     */
    public S toSchemaObjectSafe(R row) {
        try {
            return toSchemaObject(row);
        } catch (SchemaException e) {
            throw new SqlTransformationException(e);
        }
    }

    public S toSchemaObjectSafe(Tuple row, Q entityPath) {
        try {
            return toSchemaObject(row, entityPath);
        } catch (SchemaException e) {
            throw new SqlTransformationException(e);
        }
    }

    public static class SqlTransformationException extends RuntimeException {
        public SqlTransformationException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Returns {@link ObjectReferenceType} with specified oid, proper type based on
     * {@link RObjectType} and, optionally, description.
     * Returns {@code null} if OID is null.
     * Fails if OID is not null and {@code repoObjectType} is null.
     */
    @Nullable
    protected ObjectReferenceType objectReferenceType(
            @Nullable String oid, RObjectType repoObjectType, String description) {
        if (oid == null) {
            return null;
        }
        if (repoObjectType == null) {
            throw new IllegalArgumentException(
                    "NULL object type provided for object reference with OID " + oid);
        }

        return new ObjectReferenceType()
                .oid(oid)
                .type(prismContext.getSchemaRegistry().determineTypeForClass(
                        repoObjectType.getJaxbClass()))
                .description(description);
    }

    /**
     * Returns {@link RObjectType} from ordinal Integer or specified default value.
     */
    protected @NotNull RObjectType repoObjectType(
            @Nullable Integer repoObjectTypeId, @NotNull RObjectType defaultValue) {
        return repoObjectTypeId != null
                ? RObjectType.fromOrdinal(repoObjectTypeId)
                : defaultValue;
    }

    /**
     * Returns nullable {@link RObjectType} from ordinal Integer.
     * If null is returned it will not fail immediately unlike {@link RObjectType#fromOrdinal(int)}.
     * This is practical for eager argument resolution for
     * {@link #objectReferenceType(String, RObjectType, String)}.
     * Null may still be OK if OID is null as well - which means no reference.
     */
    protected @Nullable RObjectType repoObjectType(
            @Nullable Integer repoObjectTypeId) {
        return repoObjectTypeId != null
                ? RObjectType.fromOrdinal(repoObjectTypeId)
                : null;
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
        return RUtil.trimString(value, columnMetadata.getSize());
    }
}

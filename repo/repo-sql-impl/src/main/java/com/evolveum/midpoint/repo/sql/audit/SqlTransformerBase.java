/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit;

import com.querydsl.core.Tuple;
import com.querydsl.sql.ColumnMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sqlbase.SqlConfiguration;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.SqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

// TODO probably can be moved to sqlbase (now abstracted as interface).
//  We need to get rid of RObjectType for that if possible, part can be solved by interface,
//  but then there are static reverse conversions from ordinal number to instance.
//  Probably the whole RObjectType should be replaced by more dynamic registry.
public abstract class SqlTransformerBase<S, Q extends FlexibleRelationalPathBase<R>, R>
        implements SqlTransformer<S, Q, R> {

    protected final PrismContext prismContext;
    protected final QueryModelMapping<S, Q, R> mapping;
    protected final SqlConfiguration sqlConfiguration;

    protected SqlTransformerBase(PrismContext prismContext,
            QueryModelMapping<S, Q, R> mapping, SqlConfiguration sqlConfiguration) {
        this.prismContext = prismContext;
        this.mapping = mapping;
        this.sqlConfiguration = sqlConfiguration;
    }

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

    @Override
    public S toSchemaObjectSafe(Tuple row, Q entityPath) {
        try {
            return toSchemaObject(row, entityPath);
        } catch (SchemaException e) {
            throw new SqlTransformationException(e);
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

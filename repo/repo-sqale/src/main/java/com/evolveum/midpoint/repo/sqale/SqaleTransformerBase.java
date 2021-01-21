/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.util.Collection;

import com.querydsl.core.Tuple;
import com.querydsl.sql.ColumnMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.SqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public abstract class SqaleTransformerBase<S, Q extends FlexibleRelationalPathBase<R>, R>
        implements SqlTransformer<S, Q, R> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final PrismContext prismContext;
    protected final QueryModelMapping<S, Q, R> mapping;
    protected final SqlRepoContext sqlRepoContext;

    protected SqaleTransformerBase(PrismContext prismContext,
            QueryModelMapping<S, Q, R> mapping, SqlRepoContext sqlRepoContext) {
        this.prismContext = prismContext;
        this.mapping = mapping;
        this.sqlRepoContext = sqlRepoContext;
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

    protected void processExtensionColumns(S schemaObject, Tuple tuple, Q entityPath) {
        // empty by default, can be overridden
    }

    /**
     * Returns {@link ObjectReferenceType} with specified oid, proper type based on
     * {@link MObjectTypeMapping} and, optionally, target name/description.
     * Returns {@code null} if OID is null.
     * Fails if OID is not null and {@code repoObjectType} is null.
     */
    @Nullable
    protected ObjectReferenceType objectReferenceType(
            @Nullable String oid, MObjectTypeMapping repoObjectType, String targetName) {
        if (oid == null) {
            return null;
        }
        if (repoObjectType == null) {
            throw new IllegalArgumentException(
                    "NULL object type provided for object reference with OID " + oid);
        }

        return new ObjectReferenceType()
                .oid(oid)
                .type(prismContext.getSchemaRegistry()
                        .determineTypeForClass(repoObjectType.getSchemaType()))
                .description(targetName)
                .targetName(targetName);
    }

    /**
     * Returns {@link MObjectTypeMapping} from ordinal Integer or specified default value.
     */
    protected @NotNull MObjectTypeMapping objectTypeMapping(
            @Nullable Integer repoObjectTypeId, @NotNull MObjectTypeMapping defaultValue) {
        return repoObjectTypeId != null
                ? MObjectTypeMapping.fromCode(repoObjectTypeId)
                : defaultValue;
    }

    /**
     * Returns nullable {@link MObjectTypeMapping} from ordinal Integer.
     * If null is returned it will not fail immediately unlike {@link MObjectTypeMapping#fromCode(int)}.
     * This is practical for eager argument resolution for
     * {@link #objectReferenceType(String, MObjectTypeMapping, String)}.
     * Null may still be OK if OID is null as well - which means no reference.
     */
    protected @Nullable MObjectTypeMapping objectTypeMapping(
            @Nullable Integer repoObjectTypeId) {
        return repoObjectTypeId != null
                ? MObjectTypeMapping.fromCode(repoObjectTypeId)
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
        return MiscUtil.trimString(value, columnMetadata.getSize());
    }
}

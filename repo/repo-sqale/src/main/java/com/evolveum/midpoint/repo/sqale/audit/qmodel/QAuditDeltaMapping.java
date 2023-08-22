/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.audit.qmodel;

import static com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditDelta.TABLE_NAME;

import java.util.Collection;
import java.util.Objects;

import com.querydsl.core.Tuple;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType.*;

/**
 * Mapping between {@link QAuditDelta} and {@link ObjectDeltaOperationType}.
 */
public class QAuditDeltaMapping
        extends SqaleTableMapping<ObjectDeltaOperationType, QAuditDelta, MAuditDelta> {

    public static final String DEFAULT_ALIAS_NAME = "ad";

    private static QAuditDeltaMapping instance;

    public static QAuditDeltaMapping init(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QAuditDeltaMapping(repositoryContext);
        return instance;
    }

    public static QAuditDeltaMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QAuditDeltaMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                ObjectDeltaOperationType.class, QAuditDelta.class, repositoryContext);

        addItemMapping(F_OBJECT_NAME, polyStringMapper(r -> r.objectNameOrig, r-> r.objectNameNorm));
        addItemMapping(F_RESOURCE_NAME, polyStringMapper(r -> r.resourceNameOrig, r -> r.resourceNameNorm));
        addItemMapping(F_RESOURCE_OID, uuidMapper(r -> r.resourceOid));
        addItemMapping(F_SHADOW_KIND, enumMapper(r -> r.shadowKind));
        addItemMapping(F_SHADOW_INTENT, stringMapper(r -> r.shadowIntent));
    }

    @Override
    protected QAuditDelta newAliasInstance(String alias) {
        return new QAuditDelta(alias);
    }

    public ObjectDeltaOperationType toSchemaObject(MAuditDelta row) {
        ObjectDeltaOperationType odo = new ObjectDeltaOperationType();

        String deltaId = row.recordId + "-" + row.checksum;
        odo.setObjectDelta(parseBytes(row.delta, "delta-" + deltaId, ObjectDeltaType.class));
        odo.setExecutionResult(parseBytes(row.fullResult, "result-" + deltaId, OperationResultType.class));

        if (row.objectNameOrig != null || row.objectNameNorm != null) {
            odo.setObjectName(new PolyStringType(
                    new PolyString(row.objectNameOrig, row.objectNameNorm)));
        }
        odo.setResourceOid(row.resourceOid != null ? row.resourceOid.toString() : null);
        if (row.resourceNameOrig != null || row.resourceNameNorm != null) {
            odo.setResourceName(new PolyStringType(
                    new PolyString(row.resourceNameOrig, row.resourceNameNorm)));
        }
        odo.setShadowKind(row.shadowKind);
        odo.setShadowIntent(row.shadowIntent);

        return odo;
    }

    private <T> T parseBytes(byte[] bytes, String identifier, Class<T> clazz) {
        if (bytes == null) {
            return null;
        }

        try {
            return parseSchemaObject(bytes, identifier, clazz);
        } catch (SchemaException e) {
            // If it's "just" schema error, we ignore it. No reason not to return the audit record.
            // Also, parseSchemaObject logs this on ERROR, so we don't need to do it here.
            return null;
        }
    }

    @Override
    public ObjectDeltaOperationType toSchemaObject(
            @NotNull Tuple row, @NotNull QAuditDelta entityPath, @NotNull JdbcSession jdbcSession, Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException {
        throw new UnsupportedOperationException();
    }
}

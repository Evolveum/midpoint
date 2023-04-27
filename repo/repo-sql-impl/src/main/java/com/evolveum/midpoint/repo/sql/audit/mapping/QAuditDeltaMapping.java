/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit.mapping;

import static com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditItem.TABLE_NAME;

import java.util.Objects;

import com.querydsl.sql.SQLServerTemplates;
import com.querydsl.sql.SQLTemplates;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.audit.beans.MAuditDelta;
import com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditDelta;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Mapping between {@link QAuditDelta} and {@link ObjectDeltaOperationType}.
 */
public class QAuditDeltaMapping
        extends AuditTableMapping<ObjectDeltaOperationType, QAuditDelta, MAuditDelta> {

    public static final String DEFAULT_ALIAS_NAME = "ad";

    private static QAuditDeltaMapping instance;

    public static QAuditDeltaMapping init(@NotNull SqlRepoContext repositoryContext) {
        instance = new QAuditDeltaMapping(repositoryContext);
        return instance;
    }

    public static QAuditDeltaMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QAuditDeltaMapping(@NotNull SqlRepoContext repositoryContext) {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                ObjectDeltaOperationType.class, QAuditDelta.class, repositoryContext);
    }

    @Override
    protected QAuditDelta newAliasInstance(String alias) {
        return new QAuditDelta(alias);
    }

    public ObjectDeltaOperationType toSchemaObject(MAuditDelta row) {
        ObjectDeltaOperationType odo = new ObjectDeltaOperationType();
        SQLTemplates querydslTemplates = repositoryContext().getQuerydslTemplates();

        boolean usingSqlServer = querydslTemplates instanceof SQLServerTemplates;
        odo.setObjectDelta(parseBytes(row.delta, usingSqlServer, ObjectDeltaType.class));
        odo.setExecutionResult(parseBytes(row.fullResult, usingSqlServer, OperationResultType.class));

        if (row.objectNameOrig != null || row.objectNameNorm != null) {
            odo.setObjectName(new PolyStringType(
                    new PolyString(row.objectNameOrig, row.objectNameNorm)));
        }
        odo.setResourceOid(row.resourceOid);
        if (row.resourceNameOrig != null || row.resourceNameNorm != null) {
            odo.setResourceName(new PolyStringType(
                    new PolyString(row.resourceNameOrig, row.resourceNameNorm)));
        }

        return odo;
    }

    private <T> T parseBytes(byte[] bytes, boolean usingSqlServer, Class<T> clazz) {
        if (bytes == null) {
            return null;
        }

        try {
            return repositoryContext()
                    .createStringParser(RUtil.getSerializedFormFromBytes(bytes, usingSqlServer))
                    .compat()
                    .fastAddOperations()
                    .parseRealValue(clazz);
        } catch (SchemaException e) {
            logger.error("Cannot parse {}: {}", clazz.getSimpleName(), e.getMessage(), e);
            return null;
        }
    }
}

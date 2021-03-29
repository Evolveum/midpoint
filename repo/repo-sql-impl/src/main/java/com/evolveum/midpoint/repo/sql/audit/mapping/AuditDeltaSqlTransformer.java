/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit.mapping;

import com.querydsl.sql.SQLServerTemplates;
import com.querydsl.sql.SQLTemplates;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.audit.AuditSqlTransformerBase;
import com.evolveum.midpoint.repo.sql.audit.beans.MAuditDelta;
import com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditDelta;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Simple class with methods for audit event transformation between repo and Prism world.
 */
public class AuditDeltaSqlTransformer
        extends AuditSqlTransformerBase<ObjectDeltaOperationType, QAuditDelta, MAuditDelta> {

    private static final Trace LOGGER = TraceManager.getTrace(AuditDeltaSqlTransformer.class);

    public AuditDeltaSqlTransformer(
            SqlTransformerSupport transformerSupport, QAuditDeltaMapping mapping) {
        super(transformerSupport, mapping);
    }

    public ObjectDeltaOperationType toSchemaObject(MAuditDelta row) throws SchemaException {
        ObjectDeltaOperationType odo = new ObjectDeltaOperationType();
        SQLTemplates querydslTemplates = transformerSupport.sqlRepoContext().getQuerydslTemplates();
        boolean usingSqlServer = querydslTemplates instanceof SQLServerTemplates;
        odo.setObjectDelta(parseDelta(row.delta, usingSqlServer));
        if (row.fullResult != null) {
            String serializedResult =
                    RUtil.getSerializedFormFromBytes(row.fullResult, usingSqlServer);

            OperationResultType resultType = transformerSupport.parseRealValue(
                    serializedResult, OperationResultType.class);
            odo.setExecutionResult(resultType);
        }
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

    private ObjectDeltaType parseDelta(byte[] rowDelta, boolean usingSqlServer) {
        if (rowDelta == null) {
            return null;
        }
        String serializedDelta = RUtil.getSerializedFormFromBytes(rowDelta, usingSqlServer);

        try {
            return transformerSupport.parseRealValue(
                    serializedDelta, ObjectDeltaType.class);
        } catch (SchemaException e) {
            LOGGER.error("Cannot parse delta: {}", e.getMessage(), e);
        }
        return null;

    }
}

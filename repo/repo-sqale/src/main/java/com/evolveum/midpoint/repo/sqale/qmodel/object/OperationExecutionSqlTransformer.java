/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;

public class OperationExecutionSqlTransformer
        extends ContainerSqlTransformer<OperationExecutionType, QOperationExecution, MOperationExecution> {

    public OperationExecutionSqlTransformer(
            SqlTransformerSupport transformerSupport, QOperationExecutionMapping mapping) {
        super(transformerSupport, mapping);
    }

    public void insert(OperationExecutionType schemaObject, MObject ownerRow, JdbcSession jdbcSession) {
        MOperationExecution row = initRowObject(schemaObject, ownerRow.oid);

        row.status = schemaObject.getStatus();
        setReference(schemaObject.getInitiatorRef(), jdbcSession,
                o -> row.initiatorRefTargetOid = o,
                t -> row.initiatorRefTargetType = t,
                r -> row.initiatorRefRelationId = r);
        setReference(schemaObject.getTaskRef(), jdbcSession,
                o -> row.taskRefTargetOid = o,
                t -> row.taskRefTargetType = t,
                r -> row.taskRefRelationId = r);
        row.timestampValue = MiscUtil.asInstant(schemaObject.getTimestamp());

        insert(row, jdbcSession);
    }
}

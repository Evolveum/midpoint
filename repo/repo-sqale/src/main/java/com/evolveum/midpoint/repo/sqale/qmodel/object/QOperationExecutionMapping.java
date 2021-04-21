/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType.*;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;

/**
 * Mapping between {@link QOperationExecution} and {@link OperationExecutionType}.
 */
public class QOperationExecutionMapping
        extends QContainerMapping<OperationExecutionType, QOperationExecution, MOperationExecution> {

    public static final String DEFAULT_ALIAS_NAME = "opex";

    public static final QOperationExecutionMapping INSTANCE = new QOperationExecutionMapping();

    private QOperationExecutionMapping() {
        super(QOperationExecution.TABLE_NAME, DEFAULT_ALIAS_NAME,
                OperationExecutionType.class, QOperationExecution.class);

        addItemMapping(F_STATUS, enumMapper(q -> q.status));
        addItemMapping(F_RECORD_TYPE, enumMapper(q -> q.recordType));
        addItemMapping(F_INITIATOR_REF, refMapper(
                q -> q.initiatorRefTargetOid,
                q -> q.initiatorRefTargetType,
                q -> q.initiatorRefRelationId));
        addItemMapping(F_TASK_REF, refMapper(
                q -> q.taskRefTargetOid,
                q -> q.taskRefTargetType,
                q -> q.taskRefRelationId));
        addItemMapping(OperationExecutionType.F_TIMESTAMP,
                timestampMapper(q -> q.timestampValue));
    }

    @Override
    protected QOperationExecution newAliasInstance(String alias) {
        return new QOperationExecution(alias);
    }

    @Override
    public OperationExecutionSqlTransformer createTransformer(SqlTransformerSupport transformerSupport) {
        return new OperationExecutionSqlTransformer(transformerSupport, this);
    }

    @Override
    public MOperationExecution newRowObject() {
        return new MOperationExecution();
    }
}

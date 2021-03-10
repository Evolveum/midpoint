/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit.mapping;

import static com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditEventRecord.TABLE_NAME;
import static com.evolveum.midpoint.repo.sqlbase.mapping.item.SimpleItemFilterProcessor.stringMapper;
import static com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.*;

import com.evolveum.midpoint.repo.sql.audit.beans.MAuditEventRecord;
import com.evolveum.midpoint.repo.sql.audit.querymodel.*;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventStage;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.SqlDetailFetchMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.CanonicalItemPathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.DetailTableItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.EnumOrdinalItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.TimestampItemFilterProcessor;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Mapping between {@link QAuditEventRecord} and {@link AuditEventRecordType}.
 */
public class QAuditEventRecordMapping
        extends QueryTableMapping<AuditEventRecordType, QAuditEventRecord, MAuditEventRecord> {

    public static final String DEFAULT_ALIAS_NAME = "aer";

    public static final QAuditEventRecordMapping INSTANCE = new QAuditEventRecordMapping();

    private QAuditEventRecordMapping() {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                AuditEventRecordType.class, QAuditEventRecord.class);

        addItemMapping(F_CHANNEL, stringMapper(path(q -> q.channel)));
        addItemMapping(F_EVENT_IDENTIFIER, stringMapper(path(q -> q.eventIdentifier)));
        addItemMapping(F_EVENT_STAGE, EnumOrdinalItemFilterProcessor.mapper(
                path(q -> q.eventStage), RAuditEventStage::fromSchemaValue));
        addItemMapping(F_EVENT_TYPE, EnumOrdinalItemFilterProcessor.mapper(
                path(q -> q.eventType), RAuditEventType::fromSchemaValue));
        addItemMapping(F_HOST_IDENTIFIER, stringMapper(path(q -> q.hostIdentifier)));
        addItemMapping(F_MESSAGE, stringMapper(path(q -> q.message)));
        addItemMapping(F_NODE_IDENTIFIER, stringMapper(path(q -> q.nodeIdentifier)));
        addItemMapping(F_OUTCOME, EnumOrdinalItemFilterProcessor.mapper(
                path(q -> q.outcome), ROperationResultStatus::fromSchemaValue));
        addItemMapping(F_PARAMETER, stringMapper(path(q -> q.parameter)));
        addItemMapping(F_REMOTE_HOST_ADDRESS, stringMapper(path(q -> q.remoteHostAddress)));
        addItemMapping(F_REQUEST_IDENTIFIER, stringMapper(path(q -> q.requestIdentifier)));
        addItemMapping(F_RESULT, stringMapper(path(q -> q.result)));
        addItemMapping(F_SESSION_IDENTIFIER, stringMapper(path(q -> q.sessionIdentifier)));
        addItemMapping(F_TASK_IDENTIFIER, stringMapper(path(q -> q.taskIdentifier)));
        addItemMapping(F_TASK_OID, stringMapper(path(q -> q.taskOid)));
        addItemMapping(F_TIMESTAMP, TimestampItemFilterProcessor.mapper(path(q -> q.timestamp)));

        addItemMapping(F_CHANGED_ITEM, DetailTableItemFilterProcessor.mapper(
                QAuditItem.class,
                joinOn((r, i) -> r.id.eq(i.recordId)),
                CanonicalItemPathItemFilterProcessor.mapper(
                        path(QAuditItem.class, ai -> ai.changedItemPath))));

        addItemMapping(F_RESOURCE_OID, DetailTableItemFilterProcessor.mapper(
                QAuditResource.class,
                joinOn((r, i) -> r.id.eq(i.recordId)),
                stringMapper(
                        path(QAuditResource.class, ai -> ai.resourceOid))));

        /*
         * No need to map initiatorName, initiatorType, attorneyName for query, OID should suffice.
         * There is also no F_ATTORNEY_NAME and similar paths - unless these are "extension" columns?
         */
        addItemMapping(F_INITIATOR_REF, AuditRefItemFilterProcessor.mapper(
                path(q -> q.initiatorOid), path(q -> q.initiatorType)));
        addItemMapping(F_ATTORNEY_REF, AuditRefItemFilterProcessor.mapper(path(q -> q.attorneyOid)));
        addItemMapping(F_TARGET_REF, AuditRefItemFilterProcessor.mapper(
                path(q -> q.targetOid), path(q -> q.targetType)));
        addItemMapping(F_TARGET_OWNER_REF, AuditRefItemFilterProcessor.mapper(
                path(q -> q.targetOwnerOid), path(q -> q.targetOwnerType)));

        addItemMapping(F_CUSTOM_COLUMN_PROPERTY, AuditCustomColumnItemFilterProcessor.mapper());

        // lambdas use lowercase names matching the type parameters from SqlDetailFetchMapper
        addDetailFetchMapper(F_PROPERTY, new SqlDetailFetchMapper<>(
                r -> r.id,
                QAuditPropertyValue.class,
                dq -> dq.recordId,
                dr -> dr.recordId,
                (r, dr) -> r.addProperty(dr)));
        addDetailFetchMapper(F_CHANGED_ITEM, new SqlDetailFetchMapper<>(
                r -> r.id,
                QAuditItem.class,
                dq -> dq.recordId,
                dr -> dr.recordId,
                (r, dr) -> r.addChangedItem(dr)));
        addDetailFetchMapper(F_DELTA, new SqlDetailFetchMapper<>(
                r -> r.id,
                QAuditDelta.class,
                dq -> dq.recordId,
                dr -> dr.recordId,
                (r, dr) -> r.addDelta(dr)));
        addDetailFetchMapper(F_REFERENCE, new SqlDetailFetchMapper<>(
                r -> r.id,
                QAuditRefValue.class,
                dq -> dq.recordId,
                dr -> dr.recordId,
                (r, dr) -> r.addRefValue(dr)));
        addDetailFetchMapper(F_RESOURCE_OID, new SqlDetailFetchMapper<>(
                r -> r.id,
                QAuditResource.class,
                dq -> dq.recordId,
                dr -> dr.recordId,
                (r, dr) -> r.addResourceOid(dr)));
    }

    @Override
    protected QAuditEventRecord newAliasInstance(String alias) {
        return new QAuditEventRecord(alias);
    }

    @Override
    public AuditEventRecordSqlTransformer createTransformer(
            SqlTransformerSupport transformerContext) {
        return new AuditEventRecordSqlTransformer(transformerContext, this);
    }
}

package com.evolveum.midpoint.repo.sql.pure.mapping;

import static com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditEventRecord.*;
import static com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.*;

import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventStage;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditEventRecord;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Mapping between {@link QAuditEventRecord} and {@link AuditEventRecordType}.
 */
public class QAuditEventRecordMapping
        extends QueryModelMapping<AuditEventRecordType, QAuditEventRecord> {

    public static final String TABLE_NAME = "M_AUDIT_EVENT";
    public static final String DEFAULT_ALIAS_NAME = "aer";

    public static final QAuditEventRecordMapping INSTANCE = new QAuditEventRecordMapping();

    private QAuditEventRecordMapping() {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                AuditEventRecordType.class, QAuditEventRecord.class,
                ID, CHANNEL, ATTORNEY_NAME, ATTORNEY_OID,
                EVENT_IDENTIFIER, EVENT_STAGE, EVENT_TYPE,
                HOST_IDENTIFIER, INITIATOR_NAME, INITIATOR_OID, INITIATOR_TYPE,
                MESSAGE, NODE_IDENTIFIER, OUTCOME, PARAMETER,
                REMOTE_HOST_ADDRESS, REQUEST_IDENTIFIER, RESULT, SESSION_IDENTIFIER,
                TARGET_NAME, TARGET_OID, TARGET_TYPE,
                TARGET_OWNER_NAME, TARGET_OWNER_OID, TARGET_OWNER_TYPE,
                TASK_IDENTIFIER, TASK_OID, TIMESTAMP);

        addItemMapping(F_CHANNEL, StringItemFilterProcessor.mapper(path(q -> q.channel)));
        addItemMapping(F_EVENT_IDENTIFIER, StringItemFilterProcessor.mapper(path(q -> q.eventIdentifier)));
        addItemMapping(F_EVENT_STAGE, EnumOrdinalItemFilterProcessor.mapper(
                path(q -> q.eventStage), RAuditEventStage::fromSchemaValue));
        addItemMapping(F_EVENT_TYPE, EnumOrdinalItemFilterProcessor.mapper(
                path(q -> q.eventType), RAuditEventType::fromSchemaValue));
        addItemMapping(F_HOST_IDENTIFIER, StringItemFilterProcessor.mapper(path(q -> q.hostIdentifier)));
        addItemMapping(F_MESSAGE, StringItemFilterProcessor.mapper(path(q -> q.message)));
        addItemMapping(F_NODE_IDENTIFIER, StringItemFilterProcessor.mapper(path(q -> q.nodeIdentifier)));
        addItemMapping(F_OUTCOME, EnumOrdinalItemFilterProcessor.mapper(
                path(q -> q.outcome), ROperationResultStatus::fromSchemaValue));
        addItemMapping(F_PARAMETER, StringItemFilterProcessor.mapper(path(q -> q.parameter)));
        addItemMapping(F_REMOTE_HOST_ADDRESS, StringItemFilterProcessor.mapper(path(q -> q.remoteHostAddress)));
        addItemMapping(F_REQUEST_IDENTIFIER, StringItemFilterProcessor.mapper(path(q -> q.requestIdentifier)));
        addItemMapping(F_RESULT, StringItemFilterProcessor.mapper(path(q -> q.result)));
        addItemMapping(F_SESSION_IDENTIFIER, StringItemFilterProcessor.mapper(path(q -> q.sessionIdentifier)));
        addItemMapping(F_TASK_IDENTIFIER, StringItemFilterProcessor.mapper(path(q -> q.taskIdentifier)));
        addItemMapping(F_TASK_OID, StringItemFilterProcessor.mapper(path(q -> q.taskOid)));
        addItemMapping(F_TIMESTAMP, TimestampItemFilterProcessor.mapper(path(q -> q.timestamp)));

        /*
         * No need to map initiatorName, initiatorType, attorneyName for query, OID should suffice.
         * There is also no F_ATTORNEY_NAME and similar paths - unless these are "extension" columns?
         */
        addItemMapping(F_INITIATOR_REF, RefItemFilterProcessor.mapper(path(q -> q.initiatorOid)));
        addItemMapping(F_ATTORNEY_REF, RefItemFilterProcessor.mapper(path(q -> q.attorneyOid)));
        addItemMapping(F_TARGET_REF, RefItemFilterProcessor.mapper(path(q -> q.targetOid)));
        addItemMapping(F_TARGET_OWNER_REF, RefItemFilterProcessor.mapper(path(q -> q.targetOwnerOid)));

        // TODO rest of items/attrs/columns

    }
}

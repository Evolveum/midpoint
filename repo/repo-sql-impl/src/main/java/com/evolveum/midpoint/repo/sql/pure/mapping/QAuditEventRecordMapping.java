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

        addItemMapping(F_EVENT_TYPE, q -> q.eventType,
                EnumOrdinalItemFilterProcessor.withValueFunction(RAuditEventType::fromSchemaValue));
        addItemMapping(F_EVENT_STAGE, q -> q.eventStage,
                EnumOrdinalItemFilterProcessor.withValueFunction(RAuditEventStage::fromSchemaValue));
        addItemMapping(F_MESSAGE, q -> q.message, StringItemFilterProcessor::new);
        addItemMapping(F_CHANNEL, q -> q.channel, StringItemFilterProcessor::new);
        addItemMapping(F_TIMESTAMP, q -> q.timestamp, TimestampItemFilterProcessor::new);
        addItemMapping(F_OUTCOME, q -> q.outcome,
                EnumOrdinalItemFilterProcessor.withValueFunction(ROperationResultStatus::fromSchemaValue));
        addItemMapping(F_RESULT, q -> q.result, StringItemFilterProcessor::new);
        addItemMapping(F_INITIATOR_REF, q -> q.initiatoroid);
    }
}

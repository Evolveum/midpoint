package com.evolveum.midpoint.repo.sql.pure.mapping;

import static com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditEventRecord.*;

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
                TASK_IDENTIFIER, TASK_OID, TIMESTAMP_VALUE);

        // TODO how to do this better with generics?
        addProcessorForItem(AuditEventRecordType.F_EVENT_TYPE,
                EnumOrdinalItemMapper.factory(q -> ((QAuditEventRecord) q).eventtype));
    }
}

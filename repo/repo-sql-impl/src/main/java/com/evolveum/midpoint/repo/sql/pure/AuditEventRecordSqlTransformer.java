package com.evolveum.midpoint.repo.sql.pure;

import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventStage;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditEventRecord;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordPropertyType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/**
 * Simple class with methods for audit event transformation between repo and Prism world.
 */
public class AuditEventRecordSqlTransformer extends SqlTransformerBase {

    public AuditEventRecordSqlTransformer(PrismContext prismContext) {
        super(prismContext);
    }

    public AuditEventRecordType toAuditEventRecordType(MAuditEventRecord row) {
        AuditEventRecordType record = new AuditEventRecordType()
                .channel(row.channel)
                .eventIdentifier(row.eventIdentifier)
                .eventStage(auditEventStageTypeFromRepo(row.eventStage))
                .eventType(auditEventTypeTypeFromRepo(row.eventType))
                .hostIdentifier(row.hostIdentifier)
                .message(row.message)
                .nodeIdentifier(row.nodeIdentifier)
                .outcome(operationResultStatusTypeFromRepo(row.outcome))
                .parameter(row.parameter)
                .remoteHostAddress(row.remoteHostAddress)
                .requestIdentifier(row.requestIdentifier)
                .result(row.result)
                .sessionIdentifier(row.sessionIdentifier)
                .taskIdentifier(row.taskIdentifier)
                .taskOID(row.taskOid)
                .timestamp(MiscUtil.asXMLGregorianCalendar(row.timestamp))
                .initiatorRef(objectReferenceType(
                        row.initiatorOid,
                        repoObjectType(row.initiatorType, RObjectType.FOCUS),
                        row.initiatorName))
                .attorneyRef(objectReferenceType(
                        row.attorneyOid, RObjectType.FOCUS, row.attorneyName))
                .targetRef(objectReferenceType(
                        row.targetOid,
                        // targetType must not be null if targetOid is not
                        repoObjectType(row.targetType),
                        row.targetName))
                .targetOwnerRef(objectReferenceType(
                        row.targetOwnerOid,
                        repoObjectType(row.targetOwnerType),
                        row.targetOwnerName))
//                .customColumnProperty() // TODO how does this work?
                ;
        for (Map.Entry<String, List<String>> entry : row.properties.entrySet()) {
            AuditEventRecordPropertyType propType = new AuditEventRecordPropertyType()
                    .name(entry.getKey());
            propType.getValue().addAll(entry.getValue());
            record.property(propType);
        }

        return record;
    }

    private AuditEventTypeType auditEventTypeTypeFromRepo(Integer ordinal) {
        RAuditEventType eventType = MiscUtil.enumFromOrdinal(RAuditEventType.class, ordinal);
        return eventType != null
                ? eventType.getSchemaValue()
                : null;
    }

    private AuditEventStageType auditEventStageTypeFromRepo(Integer ordinal) {
        RAuditEventStage stage = MiscUtil.enumFromOrdinal(RAuditEventStage.class, ordinal);
        return stage != null
                ? stage.getSchemaValue()
                : null;
    }

    private OperationResultStatusType operationResultStatusTypeFromRepo(Integer ordinal) {
        ROperationResultStatus status =
                MiscUtil.enumFromOrdinal(ROperationResultStatus.class, ordinal);
        return status != null
                ? status.getSchemaValue()
                : null;
    }
}

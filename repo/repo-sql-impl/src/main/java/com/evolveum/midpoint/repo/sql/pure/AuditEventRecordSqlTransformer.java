package com.evolveum.midpoint.repo.sql.pure;

import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventStage;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditEventRecord;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/**
 * Simple class with static methods for audit event transformation between repo and Prism world.
 */
public class AuditEventRecordSqlTransformer {

    public static AuditEventRecordType toAuditEventRecordType(MAuditEventRecord row) {
        AuditEventRecordType rv = new AuditEventRecordType()
                .eventType(auditEventTypeTypeFromRepo(row.eventType))
                .eventStage(auditEventStageTypeFromRepo(row.eventStage))
                .message(row.message)
                .timestamp(MiscUtil.asXMLGregorianCalendar(row.timestamp))
                .outcome(operationResultStatusTypeFromRepo(row.outcome))
                .result(row.result)
                .channel(row.channel)
//                .initiatorRef() // TODO MID-6319 how to map refs?
                //
                ;
        // todo transformation from whatever input
//        rv.set where is repo ID? is it not necessary?

        return rv;
    }

    public static AuditEventTypeType auditEventTypeTypeFromRepo(Integer ordinal) {
        RAuditEventType rAuditEventType = MiscUtil.enumFromOrdinal(RAuditEventType.class, ordinal);
        return rAuditEventType != null
                ? rAuditEventType.getSchemaValue()
                : null;

    }

    public static AuditEventStageType auditEventStageTypeFromRepo(Integer ordinal) {
        RAuditEventStage rAuditEventStage = MiscUtil.enumFromOrdinal(RAuditEventStage.class, ordinal);
        return rAuditEventStage != null
                ? rAuditEventStage.getSchemaValue()
                : null;
    }

    public static OperationResultStatusType operationResultStatusTypeFromRepo(Integer ordinal) {
        ROperationResultStatus rOperationResultStatus = MiscUtil.enumFromOrdinal(ROperationResultStatus.class, ordinal);
        return rOperationResultStatus != null
                ? rOperationResultStatus.getSchemaValue()
                : null;
    }
}

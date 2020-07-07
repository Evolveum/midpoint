package com.evolveum.midpoint.repo.sql.pure;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventStage;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditEventRecord;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/**
 * Simple class with methods for audit event transformation between repo and Prism world.
 */
public class AuditEventRecordSqlTransformer {

    // TODO: protected and to common transformer superclass later
    private final PrismContext prismContext;

    public AuditEventRecordSqlTransformer(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public AuditEventRecordType toAuditEventRecordType(MAuditEventRecord row) {
        return new AuditEventRecordType()
                .eventType(auditEventTypeTypeFromRepo(row.eventType))
                .eventStage(auditEventStageTypeFromRepo(row.eventStage))
                .message(row.message)
                .timestamp(MiscUtil.asXMLGregorianCalendar(row.timestamp))
                .outcome(operationResultStatusTypeFromRepo(row.outcome))
                .result(row.result)
                .channel(row.channel)
                .initiatorRef(objectReferenceType(
                        row.initiatorOid,
                        repoObjectType(row.initiatorType, RObjectType.FOCUS),
                        row.initiatorName))
                .attorneyRef(objectReferenceType(
                        row.attorneyOid, RObjectType.FOCUS, row.attorneyName))
                .targetRef(objectReferenceType(
                        row.targetOid,
                        // targetType must not be null if targetOid is not
                        repoObjectType(row.targetType, null),
                        row.targetName))
                .targetOwnerRef(objectReferenceType(
                        row.targetOwnerOid,
                        repoObjectType(row.targetOwnerType, null),
                        row.targetOwnerName))
                .hostIdentifier(row.hostIdentifier)
                .remoteHostAddress(row.remoteHostAddress)
                .requestIdentifier(row.requestIdentifier)
                .parameter(row.parameter)
                //
                ;
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

    // TODO: To common superclass later - if transformer concept stays

    /**
     * Returns {@link ObjectReferenceType} with specified oid, proper type based on
     * {@link RObjectType} and, optionally, description.
     * Returns {@code null} if OID is null.
     * Fails if OID is not null and {@code repoObjectType} is null.
     */
    private @Nullable ObjectReferenceType objectReferenceType(
            @Nullable String oid, RObjectType repoObjectType, String description) {
        if (oid == null) {
            return null;
        }
        if (repoObjectType == null) {
            throw new IllegalArgumentException(
                    "NULL object type provided for object reference with OID " + oid);
        }

        return new ObjectReferenceType()
                .oid(oid)
                .type(prismContext.getSchemaRegistry().determineTypeForClass(
                        repoObjectType.getJaxbClass()))
                .description(description);
    }

    /**
     * Returns nullable {@link RObjectType} from ordinal Integer with optional default value.
     * If no default value is provided and null is returned it will not fail immediately
     * (as the call to {@link RObjectType#fromOrdinal(int)} would) which is practical for eager
     * argument resolution for {@link #objectReferenceType(String, RObjectType, String)}.
     * Null may still be OK if OID is null as well - which means no reference.
     */
    private RObjectType repoObjectType(
            @Nullable Integer repoObjectTypeId, RObjectType defaultValue) {
        return repoObjectTypeId != null
                ? RObjectType.fromOrdinal(repoObjectTypeId)
                : defaultValue;
    }
}

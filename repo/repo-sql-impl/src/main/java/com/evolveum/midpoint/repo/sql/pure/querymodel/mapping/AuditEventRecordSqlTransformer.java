package com.evolveum.midpoint.repo.sql.pure.querymodel.mapping;

import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventStage;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.pure.SqlTransformer;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditDelta;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditEventRecord;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditRefValue;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Simple class with methods for audit event transformation between repo and Prism world.
 */
public class AuditEventRecordSqlTransformer
        extends SqlTransformer<AuditEventRecordType, MAuditEventRecord> {

    public AuditEventRecordSqlTransformer(PrismContext prismContext) {
        super(prismContext);
    }

    public AuditEventRecordType toSchemaObject(MAuditEventRecord row) throws SchemaException {
        AuditEventRecordType record = mapSimpleAttributes(row);
        mapDeltas(record, row.deltas);
        mapChangedItems(record, row.changedItemPaths);
        mapRefValues(record, row.refValues);
        mapProperties(record, row.properties);
        mapResourceOids(record, row.resourceOids);
        return record;
    }

    private AuditEventRecordType mapSimpleAttributes(MAuditEventRecord row) {
        return new AuditEventRecordType()
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
                        row.targetOwnerName));
    }

    private void mapDeltas(AuditEventRecordType record, List<MAuditDelta> deltas)
            throws SchemaException {
        if (deltas == null) {
            return;
        }

        SqlTransformer<ObjectDeltaOperationType, MAuditDelta> transformer =
                new AuditDeltaSqlTransformer(prismContext);
        for (MAuditDelta delta : deltas) {
            record.delta(transformer.toSchemaObject(delta));
        }
    }

    // the rest of sub-entities do not deserve the dedicated transformer classes (yet)
    private void mapChangedItems(AuditEventRecordType record, List<String> changedItemPaths) {
        if (changedItemPaths == null) {
            return;
        }

        for (String changedItemPath : changedItemPaths) {
            ItemPath itemPath = ItemPath.create(changedItemPath);
            record.getChangedItem().add(new ItemPathType(itemPath));
        }
    }

    private void mapRefValues(
            AuditEventRecordType record, Map<String, List<MAuditRefValue>> refValues) {
        if (refValues == null) {
            return;
        }

        for (Map.Entry<String, List<MAuditRefValue>> entry : refValues.entrySet()) {
            AuditEventRecordReferenceType referenceValues =
                    new AuditEventRecordReferenceType().name(entry.getKey());
            for (MAuditRefValue refValue : entry.getValue()) {
                AuditEventRecordReferenceValueType value = new AuditEventRecordReferenceValueType()
                        .oid(refValue.oid)
                        .type(QName.valueOf(refValue.type));
                if (refValue.targetNameOrig != null) {
                    value.targetName(new PolyStringType(
                            new PolyString(refValue.targetNameOrig, refValue.targetNameNorm)));
                }
                referenceValues.value(value);
            }
            record.reference(referenceValues);
        }
    }

    private void mapProperties(AuditEventRecordType record, Map<String, List<String>> properties) {
        if (properties == null) {
            return;
        }

        for (Map.Entry<String, List<String>> entry : properties.entrySet()) {
            AuditEventRecordPropertyType propType =
                    new AuditEventRecordPropertyType().name(entry.getKey());
            propType.getValue().addAll(entry.getValue());
            record.property(propType);
        }
    }

    private void mapResourceOids(
            AuditEventRecordType record, List<String> resourceOids) {
        if (resourceOids == null) {
            return;
        }

        record.getResourceOid().addAll(resourceOids);
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

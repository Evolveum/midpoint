/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.audit.qmodel;

import static com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditEventRecord.TABLE_NAME;
import static com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.*;

import java.util.Objects;

import com.querydsl.core.Tuple;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.*;

/**
 * Mapping between {@link QAuditEventRecord} and {@link AuditEventRecordType}.
 */
public class QAuditEventRecordMapping
        extends SqaleTableMapping<AuditEventRecordType, QAuditEventRecord, MAuditEventRecord> {

    public static final String DEFAULT_ALIAS_NAME = "aer";

    private static QAuditEventRecordMapping instance;

    public static QAuditEventRecordMapping init(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QAuditEventRecordMapping(repositoryContext);
        return instance;
    }

    public static QAuditEventRecordMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QAuditEventRecordMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                AuditEventRecordType.class, QAuditEventRecord.class, repositoryContext);

        addItemMapping(F_REPO_ID, longMapper(q -> q.id));
        addItemMapping(F_TIMESTAMP, timestampMapper(q -> q.timestamp));
        addItemMapping(F_EVENT_IDENTIFIER, stringMapper(q -> q.eventIdentifier));
        addItemMapping(F_EVENT_TYPE, enumMapper(q -> q.eventType));
        addItemMapping(F_EVENT_STAGE, enumMapper(q -> q.eventStage));
        addItemMapping(F_SESSION_IDENTIFIER, stringMapper(q -> q.sessionIdentifier));
        addItemMapping(F_REQUEST_IDENTIFIER, stringMapper(q -> q.requestIdentifier));
        addItemMapping(F_TASK_IDENTIFIER, stringMapper(q -> q.taskIdentifier));
        addItemMapping(F_TASK_OID, uuidMapper(q -> q.taskOid));
        addItemMapping(F_HOST_IDENTIFIER, stringMapper(q -> q.hostIdentifier));
        addItemMapping(F_NODE_IDENTIFIER, stringMapper(q -> q.nodeIdentifier));
        addItemMapping(F_REMOTE_HOST_ADDRESS, stringMapper(q -> q.remoteHostAddress));

        addItemMapping(F_OUTCOME, enumMapper(q -> q.outcome));

        addItemMapping(F_CHANNEL, stringMapper(q -> q.channel));
        addItemMapping(F_PARAMETER, stringMapper(q -> q.parameter));
        addItemMapping(F_RESULT, stringMapper(q -> q.result));
        addItemMapping(F_MESSAGE, stringMapper(q -> q.message));

        /* TODO the rest
        addItemMapping(F_CHANGED_ITEM, DetailTableItemFilterProcessor.mapper(
                QAuditItem.class,
                joinOn((r, i) -> r.id.eq(i.recordId)),
                CanonicalItemPathItemFilterProcessor.mapper(ai -> ai.changedItemPath)));

        Function<QAuditResource, StringPath> rootToQueryItem = ai -> ai.resourceOid;
        addItemMapping(F_RESOURCE_OID, DetailTableItemFilterProcessor.mapper(
                QAuditResource.class,
                joinOn((r, i) -> r.id.eq(i.recordId)),
                new DefaultItemSqlMapper<>(
                        ctx -> new SimpleItemFilterProcessor<>(ctx, rootToQueryItem),
                        rootToQueryItem)));
        */

        /* ref mapping
        addItemMapping(F_INITIATOR_REF, AuditRefItemFilterProcessor.mapper(
                q -> q.initiatorOid, q -> q.initiatorName, q -> q.initiatorType));
        addItemMapping(F_ATTORNEY_REF, AuditRefItemFilterProcessor.mapper(q -> q.attorneyOid,
                q -> q.attorneyName, null));
        addItemMapping(F_TARGET_REF, AuditRefItemFilterProcessor.mapper(
                q -> q.targetOid, q -> q.targetName, q -> q.targetType));
        addItemMapping(F_TARGET_OWNER_REF, AuditRefItemFilterProcessor.mapper(
                q -> q.targetOwnerOid, q -> q.targetOwnerName, q -> q.targetOwnerType));

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
        */
    }

    @Override
    protected QAuditEventRecord newAliasInstance(String alias) {
        return new QAuditEventRecord(alias);
    }

    public AuditEventRecordType toSchemaObject(MAuditEventRecord row) {
        // prismContext in constructor ensures complex type definition
        AuditEventRecordType record = new AuditEventRecordType(prismContext())
                .repoId(row.id)
                .channel(row.channel)
                .eventIdentifier(row.eventIdentifier)
                .eventStage(row.eventStage)
                .eventType(row.eventType)
                .hostIdentifier(row.hostIdentifier)
                .message(row.message)
                .nodeIdentifier(row.nodeIdentifier)
                .outcome(row.outcome)
                .parameter(row.parameter)
                .remoteHostAddress(row.remoteHostAddress)
                .requestIdentifier(row.requestIdentifier)
                .result(row.result)
                .sessionIdentifier(row.sessionIdentifier)
                .taskIdentifier(row.taskIdentifier)
                .taskOID(row.taskOid != null ? row.taskOid.toString() : null)
                .timestamp(MiscUtil.asXMLGregorianCalendar(row.timestamp))
                .initiatorRef(objectReference(row.initiatorOid, row.initiatorType, row.initiatorName))
                .attorneyRef(objectReference(row.attorneyOid, MObjectType.FOCUS, row.attorneyName))
                .targetRef(objectReference(row.targetOid, row.targetType, row.targetName))
                .targetOwnerRef(objectReference(row.targetOwnerOid, row.targetOwnerType, row.targetOwnerName));

        // TODO
//        mapDeltas(record, row.deltas);
//        mapChangedItems(record, row.changedItemPaths);
//        mapRefValues(record, row.refValues);
//        mapProperties(record, row.properties);
//        mapResourceOids(record, row.resourceOids);
        return record;
    }

    /*
    private void mapDeltas(AuditEventRecordType record, List<MAuditDelta> deltas) {
        if (deltas == null) {
            return;
        }

        for (MAuditDelta delta : deltas) {
            record.delta(QAuditDeltaMapping.get().toSchemaObject(delta));
        }
    }

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

    /**
     * Transforms {@link AuditEventRecord} to {@link MAuditEventRecord} without any subentities.
     */
    public MAuditEventRecord toRowObject(AuditEventRecord record) {
        MAuditEventRecord bean = new MAuditEventRecord();
        bean.id = record.getRepoId(); // this better be null if we want to insert
        bean.eventIdentifier = record.getEventIdentifier();
        bean.timestamp = MiscUtil.asInstant(record.getTimestamp());
        bean.channel = record.getChannel();
        bean.eventStage = AuditEventStage.toSchemaValue(record.getEventStage());
        bean.eventType = AuditEventType.toSchemaValue(record.getEventType());
        bean.hostIdentifier = record.getHostIdentifier();

        PrismReferenceValue attorney = record.getAttorneyRef();
        if (attorney != null) {
            bean.attorneyOid = oidToUUid(attorney.getOid());
            bean.attorneyName = attorney.getDescription();
        }

        PrismReferenceValue initiator = record.getInitiatorRef();
        if (initiator != null) {
            bean.initiatorOid = oidToUUid(initiator.getOid());
            bean.initiatorType = Objects.requireNonNullElse(
                    MObjectType.fromTypeQName(initiator.getTargetType()),
                    MObjectType.FOCUS);
            bean.initiatorName = initiator.getDescription();
        }

        bean.message = record.getMessage();
        bean.nodeIdentifier = record.getNodeIdentifier();
        bean.outcome = OperationResultStatus.createStatusType(record.getOutcome());
        bean.parameter = record.getParameter();
        bean.remoteHostAddress = record.getRemoteHostAddress();
        bean.requestIdentifier = record.getRequestIdentifier();
        bean.result = record.getResult();
        bean.sessionIdentifier = record.getSessionIdentifier();

        PrismReferenceValue target = record.getTargetRef();
        if (target != null) {
            bean.targetOid = oidToUUid(target.getOid());
            bean.targetType = MObjectType.fromTypeQName(target.getTargetType());
            bean.targetName = target.getDescription();
        }
        PrismReferenceValue targetOwner = record.getTargetOwnerRef();
        if (targetOwner != null) {
            bean.targetOwnerOid = oidToUUid(targetOwner.getOid());
            bean.targetOwnerType = MObjectType.fromTypeQName(targetOwner.getTargetType());
            bean.targetOwnerName = targetOwner.getDescription();
        }
        bean.taskIdentifier = record.getTaskIdentifier();
        bean.taskOid = oidToUUid(record.getTaskOid());
        return bean;
    }

    @Override
    protected void processExtensionColumns(
            AuditEventRecordType schemaObject, Tuple tuple, QAuditEventRecord entityPath) {
        for (String propertyName : getExtensionColumns().keySet()) {
            Object customColumnValue = tuple.get(entityPath.getPath(propertyName));
            schemaObject.getCustomColumnProperty().add(
                    new AuditEventRecordCustomColumnPropertyType()
                            .name(propertyName).value((String) customColumnValue));
        }
    }
}

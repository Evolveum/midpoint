/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit.mapping;

import static com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditEventRecord.MESSAGE;
import static com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditEventRecord.TABLE_NAME;
import static com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.xml.namespace.QName;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.StringPath;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.audit.beans.MAuditDelta;
import com.evolveum.midpoint.repo.sql.audit.beans.MAuditEventRecord;
import com.evolveum.midpoint.repo.sql.audit.beans.MAuditRefValue;
import com.evolveum.midpoint.repo.sql.audit.querymodel.*;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventStage;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.CanonicalItemPathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.DetailTableItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.EnumOrdinalItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SimpleItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.DefaultItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.SqlDetailFetchMapper;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Mapping between {@link QAuditEventRecord} and {@link AuditEventRecordType}.
 */
public class QAuditEventRecordMapping
        extends AuditTableMapping<AuditEventRecordType, QAuditEventRecord, MAuditEventRecord> {

    public static final String DEFAULT_ALIAS_NAME = "aer";

    private static QAuditEventRecordMapping instance;

    public static QAuditEventRecordMapping init(@NotNull SqlRepoContext repositoryContext) {
        instance = new QAuditEventRecordMapping(repositoryContext);
        return instance;
    }

    public static QAuditEventRecordMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QAuditEventRecordMapping(@NotNull SqlRepoContext repositoryContext) {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                AuditEventRecordType.class, QAuditEventRecord.class, repositoryContext);

        addItemMapping(F_REPO_ID, longMapper(q -> q.id));
        addItemMapping(F_CHANNEL, stringMapper(q -> q.channel));
        addItemMapping(F_EVENT_IDENTIFIER, stringMapper(q -> q.eventIdentifier));
        addItemMapping(F_EVENT_STAGE, EnumOrdinalItemFilterProcessor.mapper(
                q -> q.eventStage, RAuditEventStage::fromSchemaValue));
        addItemMapping(F_EVENT_TYPE, EnumOrdinalItemFilterProcessor.mapper(
                q -> q.eventType, RAuditEventType::fromSchemaValue));
        addItemMapping(F_HOST_IDENTIFIER, stringMapper(q -> q.hostIdentifier));
        addItemMapping(F_MESSAGE, stringMapper(q -> q.message));
        addItemMapping(F_NODE_IDENTIFIER, stringMapper(q -> q.nodeIdentifier));
        addItemMapping(F_OUTCOME, EnumOrdinalItemFilterProcessor.mapper(
                q -> q.outcome, ROperationResultStatus::fromSchemaValue));
        addItemMapping(F_PARAMETER, stringMapper(q -> q.parameter));
        addItemMapping(F_REMOTE_HOST_ADDRESS, stringMapper(q -> q.remoteHostAddress));
        addItemMapping(F_REQUEST_IDENTIFIER, stringMapper(q -> q.requestIdentifier));
        addItemMapping(F_RESULT, stringMapper(q -> q.result));
        addItemMapping(F_SESSION_IDENTIFIER, stringMapper(q -> q.sessionIdentifier));
        addItemMapping(F_TASK_IDENTIFIER, stringMapper(q -> q.taskIdentifier));
        addItemMapping(F_TASK_OID, stringMapper(q -> q.taskOid));
        addItemMapping(F_TIMESTAMP, timestampMapper(q -> q.timestamp));

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
    }

    @Override
    protected QAuditEventRecord newAliasInstance(String alias) {
        return new QAuditEventRecord(alias);
    }

    public AuditEventRecordType toSchemaObject(MAuditEventRecord row) {
        AuditEventRecordType record = mapSimpleAttributes(row);
        mapDeltas(record, row.deltas);
        mapRefValues(record, row.refValues);
        mapProperties(record, row.properties);
        mapResourceOids(record, row.resourceOids);
        return record;
    }

    private AuditEventRecordType mapSimpleAttributes(MAuditEventRecord row) {
        // prismContext in constructor ensures complex type definition
        return new AuditEventRecordType()
                .repoId(row.id)
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

    private void mapDeltas(AuditEventRecordType record, List<MAuditDelta> deltas) {
        if (deltas == null) {
            return;
        }

        for (MAuditDelta delta : deltas) {
            record.delta(QAuditDeltaMapping.get().toSchemaObject(delta));
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

    /**
     * Transforms {@link AuditEventRecord} to {@link MAuditEventRecord} without any subentities.
     */
    public MAuditEventRecord toRowObject(AuditEventRecord record) {
        MAuditEventRecord bean = new MAuditEventRecord();
        bean.id = record.getRepoId(); // this better be null if we want to insert
        bean.eventIdentifier = record.getEventIdentifier();
        // We definitely want this set, otherwise we can't enforce NOT NULL column, which is
        // important - e.g. Oracle doesn't use index for nullable column.
        bean.timestamp = MiscUtil.asInstant(
                Objects.requireNonNullElse(record.getTimestamp(), System.currentTimeMillis()));
        bean.channel = record.getChannel();
        bean.eventStage = MiscUtil.enumOrdinal(RAuditEventStage.from(record.getEventStage()));
        bean.eventType = MiscUtil.enumOrdinal(RAuditEventType.from(record.getEventType()));
        bean.hostIdentifier = record.getHostIdentifier();

        PrismReferenceValue attorney = record.getAttorneyRef();
        if (attorney != null) {
            bean.attorneyName = attorney.getDescription();
            bean.attorneyOid = attorney.getOid();
        }

        PrismReferenceValue initiator = record.getInitiatorRef();
        if (initiator != null) {
            bean.initiatorName = initiator.getDescription();
            bean.initiatorOid = initiator.getOid();
            bean.initiatorType = targetTypeToRepoOrdinal(initiator);
        }

        bean.message = trim(record.getMessage(), MESSAGE);
        bean.nodeIdentifier = record.getNodeIdentifier();
        bean.outcome = MiscUtil.enumOrdinal(ROperationResultStatus.from(record.getOutcome()));
        bean.parameter = record.getParameter();
        bean.remoteHostAddress = record.getRemoteHostAddress();
        bean.requestIdentifier = record.getRequestIdentifier();
        bean.result = record.getResult();
        bean.sessionIdentifier = record.getSessionIdentifier();

        PrismReferenceValue target = record.getTargetRef();
        if (target != null) {
            bean.targetName = target.getDescription();
            bean.targetOid = target.getOid();
            bean.targetType = targetTypeToRepoOrdinal(target);
        }
        PrismReferenceValue targetOwner = record.getTargetOwnerRef();
        if (targetOwner != null) {
            bean.targetOwnerName = targetOwner.getDescription();
            bean.targetOwnerOid = targetOwner.getOid();
            bean.targetOwnerType = targetTypeToRepoOrdinal(targetOwner);
        }
        bean.taskIdentifier = record.getTaskIdentifier();
        bean.taskOid = record.getTaskOid();
        return bean;
    }

    public MAuditEventRecord toRowObject(AuditEventRecordType record) {
        MAuditEventRecord bean = new MAuditEventRecord();
        bean.id = record.getRepoId(); // this better be null if we want to insert
        bean.eventIdentifier = record.getEventIdentifier();
        bean.timestamp = MiscUtil.asInstant(record.getTimestamp());
        bean.channel = record.getChannel();
        bean.eventStage = MiscUtil.enumOrdinal(RAuditEventStage.from(
                AuditEventStage.fromSchemaValue(record.getEventStage())));
        bean.eventType = MiscUtil.enumOrdinal(RAuditEventType.from(
                AuditEventType.fromSchemaValue(record.getEventType())));
        bean.hostIdentifier = record.getHostIdentifier();

        ObjectReferenceType attorney = record.getAttorneyRef();
        if (attorney != null) {
            bean.attorneyName = attorney.getDescription();
            bean.attorneyOid = attorney.getOid();
        }

        ObjectReferenceType initiator = record.getInitiatorRef();
        if (initiator != null) {
            bean.initiatorName = initiator.getDescription();
            bean.initiatorOid = initiator.getOid();
            bean.initiatorType = targetTypeToRepoOrdinal(initiator);
        }

        bean.message = trim(record.getMessage(), MESSAGE);
        bean.nodeIdentifier = record.getNodeIdentifier();
        bean.outcome = MiscUtil.enumOrdinal(ROperationResultStatus.from(
                OperationResultStatus.parseStatusType(record.getOutcome())));
        bean.parameter = record.getParameter();
        bean.remoteHostAddress = record.getRemoteHostAddress();
        bean.requestIdentifier = record.getRequestIdentifier();
        bean.result = record.getResult();
        bean.sessionIdentifier = record.getSessionIdentifier();

        ObjectReferenceType target = record.getTargetRef();
        if (target != null) {
            bean.targetName = target.getDescription();
            bean.targetOid = target.getOid();
            bean.targetType = targetTypeToRepoOrdinal(target);
        }
        ObjectReferenceType targetOwner = record.getTargetOwnerRef();
        if (targetOwner != null) {
            bean.targetOwnerName = targetOwner.getDescription();
            bean.targetOwnerOid = targetOwner.getOid();
            bean.targetOwnerType = targetTypeToRepoOrdinal(targetOwner);
        }
        bean.taskIdentifier = record.getTaskIdentifier();
        bean.taskOid = record.getTaskOID();
        return bean;
    }

    private Integer targetTypeToRepoOrdinal(@NotNull PrismReferenceValue targetOwner) {
        //noinspection rawtypes
        Class objectClass = repositoryContext().qNameToSchemaClass(targetOwner.getTargetType());
        //noinspection unchecked
        return MiscUtil.enumOrdinal(RObjectType.getByJaxbType(objectClass));
    }

    private Integer targetTypeToRepoOrdinal(@NotNull ObjectReferenceType targetOwner) {
        //noinspection rawtypes
        Class objectClass = repositoryContext().qNameToSchemaClass(targetOwner.getType());
        //noinspection unchecked
        return MiscUtil.enumOrdinal(RObjectType.getByJaxbType(objectClass));
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

/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.audit.qmodel;

import static com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditEventRecord.TABLE_NAME;
import static com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.*;

import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;

import com.google.common.collect.Table;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ArrayPath;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.audit.filtering.AuditCustomColumnItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.audit.filtering.AuditPropertiesItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.ArrayPathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocusMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.DefaultItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.ResultListRowTransformer;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Mapping between {@link QAuditEventRecord} and {@link AuditEventRecordType}.
 * This often uses mapping supporting both query and update, but update is not intended.
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
        addAuditRefMapping(F_INITIATOR_REF,
                q -> q.initiatorOid, q -> q.initiatorType, q -> q.initiatorName,
                QFocusMapping::getFocusMapping);
        addAuditRefMapping(F_ATTORNEY_REF,
                q -> q.attorneyOid, null, q -> q.attorneyName,
                QFocusMapping::getFocusMapping);
        addAuditRefMapping(F_EFFECTIVE_PRINCIPAL_REF,
                q -> q.effectivePrincipalOid, q -> q.effectivePrincipalType, q -> q.effectivePrincipalName,
                QFocusMapping::getFocusMapping);
        addItemMapping(F_EFFECTIVE_PRIVILEGES_MODIFICATION, enumMapper(q -> q.effectivePrivilegesModification));
        addAuditRefMapping(F_TARGET_REF,
                q -> q.targetOid, q -> q.targetType, q -> q.targetName,
                QObjectMapping::getObjectMapping);
        addAuditRefMapping(F_TARGET_OWNER_REF,
                q -> q.targetOwnerOid, q -> q.targetOwnerType, q -> q.targetOwnerName,
                QObjectMapping::getObjectMapping);
        addItemMapping(F_CHANNEL, stringMapper(q -> q.channel));
        addItemMapping(F_OUTCOME, enumMapper(q -> q.outcome));
        addItemMapping(F_PARAMETER, stringMapper(q -> q.parameter));
        addItemMapping(F_RESULT, stringMapper(q -> q.result));
        addItemMapping(F_MESSAGE, stringMapper(q -> q.message));

        Function<QAuditEventRecord, ArrayPath<String[], String>> rootToQueryItem = q -> q.changedItemPaths;
        addItemMapping(F_CHANGED_ITEM, new DefaultItemSqlMapper<>(
                ctx -> new ArrayPathItemFilterProcessor<>(ctx, rootToQueryItem, "TEXT", String.class,
                        (ItemPathType i) -> prismContext().createCanonicalItemPath(i.getItemPath()).asString())));
        // Mapped as TEXT[], because UUID[] is harder to work with. PG driver doesn't convert Java Uuid[] directly,
        // so we would have to send String[] anyway. So we just keep it simple with String+TEXT here.
        addItemMapping(F_RESOURCE_OID, multiStringMapper(q -> q.resourceOids));
        addItemMapping(F_PROPERTY, new DefaultItemSqlMapper<>(
                ctx -> new AuditPropertiesItemFilterProcessor(ctx, q -> q.properties)));

        addItemMapping(F_CUSTOM_COLUMN_PROPERTY, AuditCustomColumnItemFilterProcessor.mapper());
        addRelationResolver(F_DELTA,
                TableRelationResolver.usingSubquery(
                        QAuditDeltaMapping.init(repositoryContext),
                        (e, d) -> e.id.eq(d.recordId)));
    }

    @Override
    protected QAuditEventRecord newAliasInstance(String alias) {
        return new QAuditEventRecord(alias);
    }

    public AuditEventRecordType toSchemaObject(MAuditEventRecord row) {
        // prismContext in constructor ensures complex type definition
        AuditEventRecordType record = new AuditEventRecordType()
                .repoId(row.id)
                .timestamp(MiscUtil.asXMLGregorianCalendar(row.timestamp))
                .eventIdentifier(row.eventIdentifier)
                .eventType(row.eventType)
                .eventStage(row.eventStage)
                .sessionIdentifier(row.sessionIdentifier)
                .requestIdentifier(row.requestIdentifier)
                .taskIdentifier(row.taskIdentifier)
                .taskOID(row.taskOid != null ? row.taskOid.toString() : null)
                .hostIdentifier(row.hostIdentifier)
                .nodeIdentifier(row.nodeIdentifier)
                .remoteHostAddress(row.remoteHostAddress)
                .initiatorRef(objectReference(row.initiatorOid, row.initiatorType, row.initiatorName))
                .attorneyRef(objectReference(row.attorneyOid, MObjectType.FOCUS, row.attorneyName))
                .effectivePrincipalRef(objectReference(row.effectivePrincipalOid, row.effectivePrincipalType, row.effectivePrincipalName))
                .effectivePrivilegesModification(row.effectivePrivilegesModification)
                .targetRef(objectReference(row.targetOid, row.targetType, row.targetName))
                .targetOwnerRef(objectReference(row.targetOwnerOid, row.targetOwnerType, row.targetOwnerName))
                .channel(row.channel)
                .outcome(row.outcome)
                .parameter(row.parameter)
                .result(row.result)
                .message(row.message);

        mapDeltas(record, row.deltas);
        // Changed items are only for searching, we don't want to reconstruct them, they are even "canonicalized".
        mapRefValues(record, row.refValues);
        mapProperties(record, row.properties);
        mapResourceOids(record, row.resourceOids);
        return record;
    }

    private void mapDeltas(AuditEventRecordType record, Collection<MAuditDelta> deltas) {
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
                        .oid(refValue.targetOid != null ? refValue.targetOid.toString() : null)
                        .type(objectTypeToQName(refValue.targetType));
                if (refValue.targetNameOrig != null) {
                    value.targetName(new PolyStringType(
                            new PolyString(refValue.targetNameOrig, refValue.targetNameNorm)));
                }
                referenceValues.value(value);
            }
            record.reference(referenceValues);
        }
    }

    private void mapProperties(AuditEventRecordType record, Jsonb properties) {
        if (properties == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : Jsonb.toMap(properties).entrySet()) {
            AuditEventRecordPropertyType propType =
                    new AuditEventRecordPropertyType().name(entry.getKey());
            //noinspection unchecked
            propType.getValue().addAll((Collection<String>) entry.getValue());
            record.property(propType);
        }
    }

    private void mapResourceOids(AuditEventRecordType record, String[] resourceOids) {
        if (resourceOids == null) {
            return;
        }

        record.getResourceOid().addAll(List.of(resourceOids));
    }

    /**
     * Transforms {@link AuditEventRecord} to {@link MAuditEventRecord} without any subentities.
     */
    public MAuditEventRecord toRowObject(AuditEventRecord record) {
        MAuditEventRecord row = new MAuditEventRecord();
        row.id = record.getRepoId(); // this better be null if we want to insert
        row.eventIdentifier = record.getEventIdentifier();
        // Timestamp should be set, but this is last resort, as partitioning key it MUST be set.
        row.timestamp = MiscUtil.asInstant(
                Objects.requireNonNullElse(record.getTimestamp(), System.currentTimeMillis()));
        row.channel = record.getChannel();
        row.eventStage = AuditEventStage.toSchemaValue(record.getEventStage());
        row.eventType = AuditEventType.toSchemaValue(record.getEventType());
        row.hostIdentifier = record.getHostIdentifier();

        PrismReferenceValue attorney = record.getAttorneyRef();
        if (attorney != null) {
            row.attorneyOid = SqaleUtils.oidToUuid(attorney.getOid());
            row.attorneyName = attorney.getDescription();
        }

        PrismReferenceValue initiator = record.getInitiatorRef();
        if (initiator != null) {
            row.initiatorOid = SqaleUtils.oidToUuid(initiator.getOid());
            row.initiatorType = Objects.requireNonNullElse(
                    MObjectType.fromTypeQName(initiator.getTargetType()),
                    MObjectType.FOCUS);
            row.initiatorName = initiator.getDescription();
        }

        PrismReferenceValue effectivePrincipal = record.getEffectivePrincipalRef();
        if (effectivePrincipal != null) {
            row.effectivePrincipalOid = SqaleUtils.oidToUuid(effectivePrincipal.getOid());
            row.effectivePrincipalType = Objects.requireNonNullElse(
                    MObjectType.fromTypeQName(effectivePrincipal.getTargetType()),
                    MObjectType.FOCUS);
            row.effectivePrincipalName = effectivePrincipal.getDescription();
        }

        row.effectivePrivilegesModification = record.getEffectivePrivilegesModification();

        row.message = record.getMessage();
        row.nodeIdentifier = record.getNodeIdentifier();
        row.outcome = OperationResultStatus.createStatusType(record.getOutcome());
        row.parameter = record.getParameter();
        row.remoteHostAddress = record.getRemoteHostAddress();
        row.requestIdentifier = record.getRequestIdentifier();
        row.result = record.getResult();
        row.sessionIdentifier = record.getSessionIdentifier();

        PrismReferenceValue target = record.getTargetRef();
        if (target != null) {
            row.targetOid = SqaleUtils.oidToUuid(target.getOid());
            row.targetType = MObjectType.fromTypeQName(target.getTargetType());
            row.targetName = target.getDescription();
        }
        PrismReferenceValue targetOwner = record.getTargetOwnerRef();
        if (targetOwner != null) {
            row.targetOwnerOid = SqaleUtils.oidToUuid(targetOwner.getOid());
            row.targetOwnerType = MObjectType.fromTypeQName(targetOwner.getTargetType());
            row.targetOwnerName = targetOwner.getDescription();
        }
        row.taskIdentifier = record.getTaskIdentifier();
        row.taskOid = SqaleUtils.oidToUuid(record.getTaskOid());

        row.resourceOids = stringsToArray(record.getResourceOids());
        Map<String, ?> properties = record.getProperties();
        if (properties != null) {
            row.properties = Jsonb.fromMap(properties);
        }
        // changedItemPaths are later extracted from deltas

        return row;
    }

    /**
     * Transforms {@link AuditEventRecordType} to {@link MAuditEventRecord} without any subentities.
     */
    public MAuditEventRecord toRowObject(AuditEventRecordType record) {
        MAuditEventRecord row = new MAuditEventRecord();
        row.id = record.getRepoId(); // this better be null if we want to insert
        row.eventIdentifier = record.getEventIdentifier();
        // Timestamp should be set, but this is last resort, as partitioning key it MUST be set.
        row.timestamp = Objects.requireNonNullElse(
                MiscUtil.asInstant(record.getTimestamp()), Instant.now());
        row.channel = record.getChannel();
        row.eventStage = record.getEventStage();
        row.eventType = record.getEventType();
        row.hostIdentifier = record.getHostIdentifier();

        ObjectReferenceType attorney = record.getAttorneyRef();
        if (attorney != null) {
            row.attorneyOid = SqaleUtils.oidToUuid(attorney.getOid());
            row.attorneyName = attorney.getDescription();
        }

        ObjectReferenceType initiator = record.getInitiatorRef();
        if (initiator != null) {
            row.initiatorOid = SqaleUtils.oidToUuid(initiator.getOid());
            row.initiatorType = Objects.requireNonNullElse(
                    MObjectType.fromTypeQName(initiator.getType()),
                    MObjectType.FOCUS);
            row.initiatorName = initiator.getDescription();
        }

        ObjectReferenceType effectivePrincipal = record.getEffectivePrincipalRef();
        if (effectivePrincipal != null) {
            row.effectivePrincipalOid = SqaleUtils.oidToUuid(effectivePrincipal.getOid());
            row.effectivePrincipalType = Objects.requireNonNullElse(
                    MObjectType.fromTypeQName(effectivePrincipal.getType()),
                    MObjectType.FOCUS);
            row.effectivePrincipalName = effectivePrincipal.getDescription();
        }

        row.effectivePrivilegesModification = record.getEffectivePrivilegesModification();

        row.message = record.getMessage();
        row.nodeIdentifier = record.getNodeIdentifier();
        row.outcome = record.getOutcome();
        row.parameter = record.getParameter();
        row.remoteHostAddress = record.getRemoteHostAddress();
        row.requestIdentifier = record.getRequestIdentifier();
        row.result = record.getResult();
        row.sessionIdentifier = record.getSessionIdentifier();

        ObjectReferenceType target = record.getTargetRef();
        if (target != null) {
            row.targetOid = SqaleUtils.oidToUuid(target.getOid());
            row.targetType = MObjectType.fromTypeQName(target.getType());
            row.targetName = target.getDescription();
        }
        ObjectReferenceType targetOwner = record.getTargetOwnerRef();
        if (targetOwner != null) {
            row.targetOwnerOid = SqaleUtils.oidToUuid(targetOwner.getOid());
            row.targetOwnerType = MObjectType.fromTypeQName(targetOwner.getType());
            row.targetOwnerName = targetOwner.getDescription();
        }
        row.taskIdentifier = record.getTaskIdentifier();
        row.taskOid = SqaleUtils.oidToUuid(record.getTaskOID());

        row.resourceOids = stringsToArray(record.getResourceOid());

        List<AuditEventRecordPropertyType> properties = record.getProperty();
        if (properties != null) {
            Map<String, List<String>> propertiesMap = new HashMap<>();
            for (AuditEventRecordPropertyType property : properties) {
                propertiesMap.put(property.getName(), property.getValue());
            }
            row.properties = Jsonb.fromMap(propertiesMap);
        }

        // changedItemPaths are later extracted from deltas
        // custom properties are treated specially elsewhere

        return row;
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

    @Override
    public ResultListRowTransformer<AuditEventRecordType, QAuditEventRecord, MAuditEventRecord> createRowTransformer(
            SqlQueryContext<AuditEventRecordType, QAuditEventRecord, MAuditEventRecord> sqlQueryContext,
            JdbcSession jdbcSession) {
        return new ResultListRowTransformer<>() {
            @Override
            public void beforeTransformation(List<Tuple> rowTuples, QAuditEventRecord entityPath) {
                if (rowTuples.isEmpty()) {
                    return;
                }

                Instant minTimestamp = Instant.MAX;
                Instant maxTimestamp = Instant.MIN;
                Map<Long, MAuditEventRecord> rowMap = new HashMap<>();
                for (Tuple rowTuple : rowTuples) {
                    MAuditEventRecord row = Objects.requireNonNull(rowTuple.get(entityPath));
                    rowMap.put(row.id, row);
                    if (row.timestamp.isBefore(minTimestamp)) {
                        minTimestamp = row.timestamp;
                    }
                    if (row.timestamp.isAfter(maxTimestamp)) {
                        maxTimestamp = row.timestamp;
                    }
                }

                QAuditDelta qd = QAuditDeltaMapping.get().defaultAlias();
                jdbcSession.newQuery()
                        .select(qd)
                        .from(qd)
                        .where(qd.recordId.in(rowMap.keySet())
                                // here between is OK, it's inclusive on both sides
                                .and(qd.timestamp.between(minTimestamp, maxTimestamp)))
                        .fetch()
                        .forEach(d -> rowMap.get(d.recordId).addDelta(d));

                QAuditRefValue qr = QAuditRefValueMapping.get().defaultAlias();
                jdbcSession.newQuery()
                        .select(qr)
                        .from(qr)
                        .where(qr.recordId.in(rowMap.keySet())
                                // here between is OK, it's inclusive on both sides
                                .and(qr.timestamp.between(minTimestamp, maxTimestamp)))
                        .fetch()
                        .forEach(r -> rowMap.get(r.recordId).addRefValue(r));
            }

            @Override
            public AuditEventRecordType transform(Tuple tuple, QAuditEventRecord entityPath,
                    Collection<SelectorOptions<GetOperationOptions>> options) {
                return toSchemaObjectCompleteSafe(tuple, entityPath, options, jdbcSession, false);
            }
        };
    }
}

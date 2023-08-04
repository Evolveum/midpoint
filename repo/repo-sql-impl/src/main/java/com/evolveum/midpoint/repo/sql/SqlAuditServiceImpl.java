/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.schema.util.SystemConfigurationAuditUtil.isEscapingInvalidCharacters;

import java.sql.Types;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import javax.xml.datatype.Duration;

import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.DefaultMapper;
import com.querydsl.sql.dml.SQLInsertClause;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditReferenceValue;
import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.CanonicalItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.S_ConditionEntry;
import com.evolveum.midpoint.prism.query.builder.S_MatchingRuleEntry;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.sql.audit.AuditSqlQueryContext;
import com.evolveum.midpoint.repo.sql.audit.beans.MAuditDelta;
import com.evolveum.midpoint.repo.sql.audit.beans.MAuditEventRecord;
import com.evolveum.midpoint.repo.sql.audit.mapping.*;
import com.evolveum.midpoint.repo.sql.audit.querymodel.*;
import com.evolveum.midpoint.repo.sql.data.common.enums.RChangeType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sql.util.TemporaryTableDialect;
import com.evolveum.midpoint.repo.sqlbase.*;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Audit service using SQL DB as a store, also allows for searching (see {@link #supportsRetrieval}).
 * This is NOT a managed bean, it is completely created by {@link SqlAuditServiceFactory} and any
 * of the dependencies must be dependencies of that factory to assure proper initialization.
 * <p>
 * Design notes:
 * No repo.sql.data.audit.* entities are used (stage/type enums are OK).
 */
public class SqlAuditServiceImpl extends SqlBaseService implements AuditService {

    private static final Trace LOGGER = TraceManager.getTrace(SqlAuditServiceImpl.class);

    private static final String OP_NAME_PREFIX = SqlAuditServiceImpl.class.getSimpleName() + '.';

    private static final Integer CLEANUP_AUDIT_BATCH_SIZE = 500;

    private final SqlRepoContext sqlRepoContext;
    private final SchemaService schemaService;
    private final SqlQueryExecutor sqlQueryExecutor;

    private volatile SystemConfigurationAuditType auditConfiguration;

    public SqlAuditServiceImpl(
            BaseHelper baseHelper,
            SqlRepoContext sqlRepoContext,
            SchemaService schemaService) {

        super(baseHelper);

        this.sqlRepoContext = sqlRepoContext;
        this.schemaService = schemaService;
        this.sqlQueryExecutor = new SqlQueryExecutor(sqlRepoContext);
    }

    public SqlRepoContext getSqlRepoContext() {
        return sqlRepoContext;
    }

    @Override
    public SqlRepositoryConfiguration sqlConfiguration() {
        return (SqlRepositoryConfiguration) sqlRepoContext.getJdbcRepositoryConfiguration();
    }

    @Override
    public void audit(AuditEventRecord record, Task task, OperationResult result) {
        Objects.requireNonNull(record, "Audit event record must not be null.");

        // TODO convert record to AERType and call that version?
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(OP_AUDIT, AuditEventRecord.class);
        int attempt = 1;

        while (true) {
            try {
                auditAttempt(record);
                return;
            } catch (RuntimeException ex) {
                attempt = baseHelper.logOperationAttempt(null, OP_AUDIT, attempt, ex, result);
                pm.registerOperationNewAttempt(opHandle, attempt);
            } finally {
                pm.registerOperationFinish(opHandle, attempt);
            }
        }
    }

    private void auditAttempt(AuditEventRecord record) {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            try {
                long recordId = insertAuditEventRecord(jdbcSession, record);

                Collection<MAuditDelta> deltas =
                        insertAuditDeltas(jdbcSession, recordId, record.getDeltas());
                insertChangedItemPaths(jdbcSession, recordId, deltas);

                insertProperties(jdbcSession, recordId, record.getProperties());
                insertReferences(jdbcSession, recordId, record.getReferences());
                insertResourceOids(jdbcSession, recordId, record.getResourceOids());
                jdbcSession.commit();
            } catch (RuntimeException ex) {
                baseHelper.handleGeneralRuntimeException(ex, jdbcSession, null);
            }
        }
    }

    /**
     * Inserts audit event record aggregate root without any subentities.
     *
     * @return ID of created audit event record
     */
    private Long insertAuditEventRecord(
            JdbcSession jdbcSession, AuditEventRecord record) {
        QAuditEventRecordMapping aerMapping = QAuditEventRecordMapping.get();
        QAuditEventRecord aer = aerMapping.defaultAlias();
        MAuditEventRecord aerBean = aerMapping.toRowObject(record);
        SQLInsertClause insert = jdbcSession.newInsert(aer).populate(aerBean);

        Map<String, ColumnMetadata> customColumns = aerMapping.getExtensionColumns();
        for (Entry<String, String> property : record.getCustomColumnProperty().entrySet()) {
            String propertyName = property.getKey();
            if (!customColumns.containsKey(propertyName)) {
                throw new IllegalArgumentException("Audit event record table doesn't"
                        + " contains column for property " + propertyName);
            }
            // Like insert.set, but that one is too parameter-type-safe for our generic usage here.
            insert.columns(aer.getPath(propertyName)).values(property.getValue());
        }

        Long returnedId = insert.executeWithKey(aer.id);
        // If returned ID is null, it was provided. If not, it fails, something went bad.
        return returnedId != null ? returnedId : record.getRepoId();
    }

    private Collection<MAuditDelta> insertAuditDeltas(
            JdbcSession jdbcSession, long recordId, Collection<ObjectDeltaOperation<?>> deltas) {
        // we want to keep only unique deltas, checksum is also part of PK
        Map<String, MAuditDelta> deltasByChecksum = new HashMap<>();
        for (ObjectDeltaOperation<?> deltaOperation : deltas) {
            if (deltaOperation == null) {
                continue;
            }

            MAuditDelta mAuditDelta = convertDelta(deltaOperation, recordId);
            deltasByChecksum.put(mAuditDelta.checksum, mAuditDelta);
        }

        if (!deltasByChecksum.isEmpty()) {
            SQLInsertClause insertBatch = jdbcSession.newInsert(
                    QAuditDeltaMapping.get().defaultAlias());
            for (MAuditDelta value : deltasByChecksum.values()) {
                // NULLs are important to keep the value count consistent during the batch
                insertBatch.populate(value, DefaultMapper.WITH_NULL_BINDINGS).addBatch();
            }
            insertBatch.setBatchToBulk(true);
            insertBatch.execute();
        }
        return deltasByChecksum.values();
    }

    private MAuditDelta convertDelta(ObjectDeltaOperation<?> deltaOperation, long recordId) {
        MAuditDelta mAuditDelta = new MAuditDelta();
        mAuditDelta.recordId = recordId;

        try {
            ObjectDelta<? extends ObjectType> delta = deltaOperation.getObjectDelta();
            if (delta != null) {
                DeltaConversionOptions options =
                        DeltaConversionOptions.createSerializeReferenceNames();
                options.setEscapeInvalidCharacters(isEscapingInvalidCharacters(auditConfiguration));
                String serializedDelta = DeltaConvertor.serializeDelta(delta, options, PrismContext.LANG_XML);

                // serializedDelta is transient, needed for changed items later
                mAuditDelta.serializedDelta = serializedDelta;
                mAuditDelta.delta = RUtil.getBytesFromSerializedForm(
                        serializedDelta, sqlConfiguration().isUseZipAudit());
                mAuditDelta.deltaOid = delta.getOid();
                mAuditDelta.deltaType = MiscUtil.enumOrdinal(
                        RUtil.getRepoEnumValue(delta.getChangeType(), RChangeType.class));
            }

            OperationResult executionResult = deltaOperation.getExecutionResult();
            if (executionResult != null) {
                OperationResultType jaxb = executionResult.createOperationResultType();
                if (jaxb != null) {
                    mAuditDelta.status = MiscUtil.enumOrdinal(
                            RUtil.getRepoEnumValue(jaxb.getStatus(), ROperationResultStatus.class));
                    // Note that escaping invalid characters and using toString for unsupported types is safe in the
                    // context of operation result serialization.
                    String full = schemaService.createStringSerializer(PrismContext.LANG_XML)
                            .options(SerializationOptions.createEscapeInvalidCharacters()
                                    .serializeUnsupportedTypesAsString(true))
                            .serializeRealValue(jaxb, SchemaConstantsGenerated.C_OPERATION_RESULT);
                    mAuditDelta.fullResult = RUtil.getBytesFromSerializedForm(
                            full, sqlConfiguration().isUseZipAudit());
                }
            }
            mAuditDelta.resourceOid = deltaOperation.getResourceOid();
            if (deltaOperation.getObjectName() != null) {
                mAuditDelta.objectNameOrig = deltaOperation.getObjectName().getOrig();
                mAuditDelta.objectNameNorm = deltaOperation.getObjectName().getNorm();
            }
            if (deltaOperation.getResourceName() != null) {
                mAuditDelta.resourceNameOrig = deltaOperation.getResourceName().getOrig();
                mAuditDelta.resourceNameNorm = deltaOperation.getResourceName().getNorm();
            }
            mAuditDelta.checksum = RUtil.computeChecksum(mAuditDelta.delta, mAuditDelta.fullResult);
        } catch (Exception ex) {
            throw new SystemException("Problem during audit delta conversion", ex);
        }
        return mAuditDelta;
    }

    private void insertChangedItemPaths(
            JdbcSession jdbcSession, long recordId, Collection<MAuditDelta> deltas) {
        Set<String> changedItemPaths = new HashSet<>();
        for (MAuditDelta delta : deltas) {
            try {
                ObjectDeltaType deltaBean =
                        schemaService.parserFor(delta.serializedDelta)
                                .parseRealValue(ObjectDeltaType.class);
                for (ItemDeltaType itemDelta : deltaBean.getItemDelta()) {
                    ItemPath path = itemDelta.getPath().getItemPath();
                    CanonicalItemPath canonical =
                            schemaService.createCanonicalItemPath(
                                    path, deltaBean.getObjectType());
                    for (int i = 0; i < canonical.size(); i++) {
                        changedItemPaths.add(canonical.allUpToIncluding(i).asString());
                    }
                }
            } catch (SchemaException | SystemException e) {
                // See MID-6446 - we want to throw in tests, old ones should be fixed by now
                if (InternalsConfig.isConsistencyChecks()) {
                    throw new SystemException("Problem during audit delta parse", e);
                }
                LOGGER.warn("Serialized audit delta for recordId={} cannot be parsed."
                        + " No changed items were created. This may cause problem later, but is not"
                        + " critical for storing the audit record.", recordId, e);
            }
        }
        if (!changedItemPaths.isEmpty()) {
            QAuditItem qAuditItem = QAuditItemMapping.get().defaultAlias();
            SQLInsertClause insertBatch = jdbcSession.newInsert(qAuditItem);
            for (String changedItemPath : changedItemPaths) {
                insertBatch.set(qAuditItem.recordId, recordId)
                        .set(qAuditItem.changedItemPath, changedItemPath)
                        .addBatch();
            }
            insertBatch.setBatchToBulk(true);
            insertBatch.execute();
        }
    }

    private void insertProperties(
            JdbcSession jdbcSession, long recordId, Map<String, Set<String>> properties) {
        if (properties.isEmpty()) {
            return;
        }

        QAuditPropertyValue qAuditPropertyValue = QAuditPropertyValueMapping.get().defaultAlias();
        SQLInsertClause insertBatch = jdbcSession.newInsert(qAuditPropertyValue);
        for (String propertyName : properties.keySet()) {
            for (String propertyValue : properties.get(propertyName)) {
                // id will be generated, but we're not interested in those here
                insertBatch.set(qAuditPropertyValue.recordId, recordId)
                        .set(qAuditPropertyValue.name, propertyName)
                        .set(qAuditPropertyValue.value, propertyValue)
                        .addBatch();
            }
        }
        if (insertBatch.getBatchCount() == 0) {
            return; // strange, no values anywhere?
        }

        insertBatch.setBatchToBulk(true);
        insertBatch.execute();
    }

    private void insertReferences(JdbcSession jdbcSession,
            long recordId, Map<String, Set<AuditReferenceValue>> references) {
        if (references.isEmpty()) {
            return;
        }

        QAuditRefValue qAuditRefValue = QAuditRefValueMapping.get().defaultAlias();
        SQLInsertClause insertBatch = jdbcSession.newInsert(qAuditRefValue);
        for (String refName : references.keySet()) {
            for (AuditReferenceValue refValue : references.get(refName)) {
                // id will be generated, but we're not interested in those here
                PolyString targetName = refValue.getTargetName();
                insertBatch.set(qAuditRefValue.recordId, recordId)
                        .set(qAuditRefValue.name, refName)
                        .set(qAuditRefValue.oid, refValue.getOid())
                        .set(qAuditRefValue.type, RUtil.qnameToString(refValue.getType()))
                        .set(qAuditRefValue.targetNameOrig, PolyString.getOrig(targetName))
                        .set(qAuditRefValue.targetNameNorm, PolyString.getNorm(targetName))
                        .addBatch();
            }
        }
        if (insertBatch.getBatchCount() == 0) {
            return; // strange, no values anywhere?
        }

        insertBatch.setBatchToBulk(true);
        insertBatch.execute();
    }

    private void insertResourceOids(
            JdbcSession jdbcSession, long recordId, Collection<String> resourceOids) {
        if (resourceOids.isEmpty()) {
            return;
        }

        QAuditResource qAuditResource = QAuditResourceMapping.get().defaultAlias();
        SQLInsertClause insertBatch = jdbcSession.newInsert(qAuditResource);
        for (String resourceOid : resourceOids) {
            insertBatch.set(qAuditResource.recordId, recordId)
                    .set(qAuditResource.resourceOid, resourceOid)
                    .addBatch();
        }

        insertBatch.setBatchToBulk(true);
        insertBatch.execute();
    }

    @Override
    public void audit(AuditEventRecordType record, OperationResult result) {
        Objects.requireNonNull(record, "Audit event record must not be null.");

        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(OP_AUDIT, AuditEventRecordType.class);
        int attempt = 1;

        while (true) {
            try {
                auditAttempt(record);
                return;
            } catch (RuntimeException ex) {
                attempt = baseHelper.logOperationAttempt(null, OP_AUDIT, attempt, ex, result);
                pm.registerOperationNewAttempt(opHandle, attempt);
            } finally {
                pm.registerOperationFinish(opHandle, attempt);
            }
        }
    }

    private void auditAttempt(AuditEventRecordType record) {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            try {
                MAuditEventRecord auditRow = insertAuditEventRecord(jdbcSession, record);

                insertAuditDeltas(jdbcSession, auditRow, record.getDelta());
                insertChangedItemPaths(jdbcSession, auditRow);

                insertProperties(jdbcSession, auditRow.id, record.getProperty());
                insertReferences(jdbcSession, auditRow.id, record.getReference());
                insertResourceOids(jdbcSession, auditRow.id, record.getResourceOid());
                jdbcSession.commit();
            } catch (RuntimeException ex) {
                baseHelper.handleGeneralRuntimeException(ex, jdbcSession, null);
            }
        }
    }

    /**
     * Inserts audit event record aggregate root without any subentities.
     *
     * @return ID of created audit event record
     */
    private MAuditEventRecord insertAuditEventRecord(JdbcSession jdbcSession, AuditEventRecordType record) {
        QAuditEventRecordMapping aerMapping = QAuditEventRecordMapping.get();
        QAuditEventRecord aer = aerMapping.defaultAlias();
        MAuditEventRecord row = aerMapping.toRowObject(record);
        SQLInsertClause insert = jdbcSession.newInsert(aer).populate(row);

        Map<String, ColumnMetadata> customColumns = aerMapping.getExtensionColumns();
        for (AuditEventRecordCustomColumnPropertyType property : record.getCustomColumnProperty()) {
            String propertyName = property.getName();
            if (!customColumns.containsKey(propertyName)) {
                throw new IllegalArgumentException("Audit event record table doesn't"
                        + " contains column for property " + propertyName);
            }
            // Like insert.set, but that one is too parameter-type-safe for our generic usage here.
            insert.columns(aer.getPath(propertyName)).values(property.getValue());
        }

        Long returnedId = insert.executeWithKey(aer.id);
        // If returned ID is null, it was provided. If not, it fails, something went bad.
        row.id = returnedId != null ? returnedId : record.getRepoId();
        return row;
    }

    private void insertAuditDeltas(
            JdbcSession jdbcSession, MAuditEventRecord auditRow, List<ObjectDeltaOperationType> deltas) {
        // we want to keep only unique deltas, checksum is also part of PK
        Map<String, MAuditDelta> deltasByChecksum = new HashMap<>();
        for (ObjectDeltaOperationType delta : deltas) {
            if (delta == null) {
                continue;
            }

            MAuditDelta mAuditDelta = convertDelta(delta, auditRow);
            deltasByChecksum.put(mAuditDelta.checksum, mAuditDelta);
        }

        if (!deltasByChecksum.isEmpty()) {
            SQLInsertClause insertBatch = jdbcSession.newInsert(
                    QAuditDeltaMapping.get().defaultAlias());
            for (MAuditDelta value : deltasByChecksum.values()) {
                // NULLs are important to keep the value count consistent during the batch
                insertBatch.populate(value, DefaultMapper.WITH_NULL_BINDINGS).addBatch();
            }
            insertBatch.setBatchToBulk(true);
            insertBatch.execute();
        }
    }

    private MAuditDelta convertDelta(ObjectDeltaOperationType deltaOperation, MAuditEventRecord auditRow) {
        MAuditDelta mAuditDelta = new MAuditDelta();
        mAuditDelta.recordId = auditRow.id;

        try {
            ObjectDeltaType delta = deltaOperation.getObjectDelta();
            if (delta != null) {
                DeltaConversionOptions options =
                        DeltaConversionOptions.createSerializeReferenceNames();
                options.setEscapeInvalidCharacters(isEscapingInvalidCharacters(auditConfiguration));
                String serializedDelta = DeltaConvertor.serializeDelta(delta, options, PrismContext.LANG_XML);

                // serializedDelta is transient, needed for changed items later
                mAuditDelta.serializedDelta = serializedDelta;
                mAuditDelta.delta = RUtil.getBytesFromSerializedForm(
                        serializedDelta, sqlConfiguration().isUseZipAudit());
                mAuditDelta.deltaOid = delta.getOid();
                mAuditDelta.deltaType = MiscUtil.enumOrdinal(
                        RUtil.getRepoEnumValue(ChangeType.toChangeType(delta.getChangeType()), RChangeType.class));

                for (ItemDeltaType itemDelta : delta.getItemDelta()) {
                    ItemPath path = itemDelta.getPath().getItemPath();
                    CanonicalItemPath canonical =
                            schemaService.createCanonicalItemPath(path, delta.getObjectType());
                    for (int i = 0; i < canonical.size(); i++) {
                        auditRow.addChangedItem(canonical.allUpToIncluding(i).asString());
                    }
                }
            }

            OperationResultType executionResult = deltaOperation.getExecutionResult();
            if (executionResult != null) {
                mAuditDelta.status = MiscUtil.enumOrdinal(
                        RUtil.getRepoEnumValue(executionResult.getStatus(), ROperationResultStatus.class));
                // Note that escaping invalid characters and using toString for unsupported types is safe in the
                // context of operation result serialization.
                String full = schemaService.createStringSerializer(PrismContext.LANG_XML)
                        .options(SerializationOptions.createEscapeInvalidCharacters()
                                .serializeUnsupportedTypesAsString(true))
                        .serializeRealValue(executionResult, SchemaConstantsGenerated.C_OPERATION_RESULT);
                mAuditDelta.fullResult = RUtil.getBytesFromSerializedForm(
                        full, sqlConfiguration().isUseZipAudit());
            }
            mAuditDelta.resourceOid = deltaOperation.getResourceOid();
            if (deltaOperation.getObjectName() != null) {
                mAuditDelta.objectNameOrig = deltaOperation.getObjectName().getOrig();
                mAuditDelta.objectNameNorm = deltaOperation.getObjectName().getNorm();
            }
            if (deltaOperation.getResourceName() != null) {
                mAuditDelta.resourceNameOrig = deltaOperation.getResourceName().getOrig();
                mAuditDelta.resourceNameNorm = deltaOperation.getResourceName().getNorm();
            }
            mAuditDelta.checksum = RUtil.computeChecksum(mAuditDelta.delta, mAuditDelta.fullResult);
        } catch (Exception ex) {
            throw new SystemException("Problem during audit delta conversion", ex);
        }
        return mAuditDelta;
    }

    private void insertChangedItemPaths(JdbcSession jdbcSession, MAuditEventRecord auditRow) {
        if (auditRow.changedItemPaths != null && !auditRow.changedItemPaths.isEmpty()) {
            QAuditItem qAuditItem = QAuditItemMapping.get().defaultAlias();
            SQLInsertClause insertBatch = jdbcSession.newInsert(qAuditItem);
            for (String changedItemPath : auditRow.changedItemPaths) {
                insertBatch.set(qAuditItem.recordId, auditRow.id)
                        .set(qAuditItem.changedItemPath, changedItemPath)
                        .addBatch();
            }
            insertBatch.setBatchToBulk(true);
            insertBatch.execute();
        }
    }

    private void insertProperties(
            JdbcSession jdbcSession, long recordId, List<AuditEventRecordPropertyType> properties) {
        if (properties.isEmpty()) {
            return;
        }

        QAuditPropertyValue qAuditPropertyValue = QAuditPropertyValueMapping.get().defaultAlias();
        SQLInsertClause insertBatch = jdbcSession.newInsert(qAuditPropertyValue);
        for (AuditEventRecordPropertyType propertySet : properties) {
            for (String value : propertySet.getValue()) {
                // id will be generated, but we're not interested in those here
                insertBatch.set(qAuditPropertyValue.recordId, recordId)
                        .set(qAuditPropertyValue.name, propertySet.getName())
                        .set(qAuditPropertyValue.value, value)
                        .addBatch();
            }
        }
        if (insertBatch.getBatchCount() == 0) {
            return; // strange, no values anywhere?
        }

        insertBatch.setBatchToBulk(true);
        insertBatch.execute();
    }

    private void insertReferences(JdbcSession jdbcSession,
            long recordId, List<AuditEventRecordReferenceType> references) {
        if (references.isEmpty()) {
            return;
        }

        QAuditRefValue qAuditRefValue = QAuditRefValueMapping.get().defaultAlias();
        SQLInsertClause insertBatch = jdbcSession.newInsert(qAuditRefValue);
        for (AuditEventRecordReferenceType refSet : references) {
            for (AuditEventRecordReferenceValueType refValue : refSet.getValue()) {
                // id will be generated, but we're not interested in those here
                PolyStringType targetName = refValue.getTargetName();
                insertBatch.set(qAuditRefValue.recordId, recordId)
                        .set(qAuditRefValue.name, refSet.getName())
                        .set(qAuditRefValue.oid, refValue.getOid())
                        .set(qAuditRefValue.type, RUtil.qnameToString(refValue.getType()))
                        .set(qAuditRefValue.targetNameOrig, PolyString.getOrig(targetName))
                        .set(qAuditRefValue.targetNameNorm, PolyString.getNorm(targetName))
                        .addBatch();
            }
        }
        if (insertBatch.getBatchCount() == 0) {
            return; // strange, no values anywhere?
        }

        insertBatch.setBatchToBulk(true);
        insertBatch.execute();
    }

    @Override
    public void cleanupAudit(CleanupPolicyType policy, OperationResult parentResult) {
        Objects.requireNonNull(policy, "Cleanup policy must not be null.");
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        // TODO review monitoring performance of these cleanup operations
        // It looks like the attempts (and wasted time) are not counted correctly
        cleanupAuditMaxRecords(policy, parentResult);
        cleanupAuditMaxAge(policy, parentResult);
    }

    private void cleanupAuditMaxAge(CleanupPolicyType policy, OperationResult parentResult) {
        if (policy.getMaxAge() == null) {
            return;
        }

        final String operation = "deletingMaxAge";

        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(OP_CLEANUP_AUDIT_MAX_AGE, AuditEventRecord.class);
        int attempt = 1;

        Duration duration = policy.getMaxAge();
        if (duration.getSign() > 0) {
            duration = duration.negate();
        }
        Date minValue = new Date();
        duration.addTo(minValue);

        checkTemporaryTablesSupport();

        long start = System.currentTimeMillis();
        boolean first = true;
        Holder<Integer> totalCountHolder = new Holder<>(0);
        try {
            while (true) {
                try {
                    LOGGER.info("{} audit cleanup, deleting up to {} (duration '{}'), batch size {}{}.",
                            first ? "Starting" : "Continuing with ",
                            minValue, duration, CLEANUP_AUDIT_BATCH_SIZE,
                            first ? "" : ", up to now deleted " + totalCountHolder.getValue() + " entries");
                    first = false;
                    int count;
                    do {
                        // the following method may restart due to concurrency
                        // (or any other) problem - in any iteration
                        long batchStart = System.currentTimeMillis();
                        LOGGER.debug(
                                "Starting audit cleanup batch, deleting up to {} (duration '{}'),"
                                        + " batch size {}, up to now deleted {} entries.",
                                minValue, duration, CLEANUP_AUDIT_BATCH_SIZE, totalCountHolder.getValue());

                        count = batchDeletionAttempt(
                                (session, tempTable) -> selectRecordsByMaxAge(session, tempTable, minValue),
                                totalCountHolder, batchStart, parentResult);
                    } while (count > 0);
                    return;
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, parentResult);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
            LOGGER.info("Audit cleanup based on age finished; deleted {} entries in {} seconds.",
                    totalCountHolder.getValue(), (System.currentTimeMillis() - start) / 1000L);
        }
    }

    private void cleanupAuditMaxRecords(CleanupPolicyType policy, OperationResult parentResult) {
        if (policy.getMaxRecords() == null) {
            return;
        }

        final String operation = "deletingMaxRecords";

        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(
                OP_CLEANUP_AUDIT_MAX_RECORDS, AuditEventRecord.class);
        int attempt = 1;

        int recordsToKeep = policy.getMaxRecords();

        checkTemporaryTablesSupport();

        long start = System.currentTimeMillis();
        boolean first = true;
        Holder<Integer> totalCountHolder = new Holder<>(0);
        try {
            while (true) {
                try {
                    LOGGER.info("{} audit cleanup, keeping at most {} records, batch size {}{}.",
                            first ? "Starting" : "Continuing with ", recordsToKeep, CLEANUP_AUDIT_BATCH_SIZE,
                            first ? "" : ", up to now deleted " + totalCountHolder.getValue() + " entries");
                    first = false;
                    int count;
                    do {
                        // the following method may restart due to concurrency
                        // (or any other) problem - in any iteration
                        long batchStart = System.currentTimeMillis();
                        LOGGER.debug(
                                "Starting audit cleanup batch, keeping at most {} records,"
                                        + " batch size {}, up to now deleted {} entries.",
                                recordsToKeep, CLEANUP_AUDIT_BATCH_SIZE, totalCountHolder.getValue());

                        count = batchDeletionAttempt(
                                (session, tempTable) -> selectRecordsByNumberToKeep(session, tempTable, recordsToKeep),
                                totalCountHolder, batchStart, parentResult);
                    } while (count > 0);
                    return;
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, parentResult);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
            LOGGER.info("Audit cleanup based on record count finished; deleted {} entries in {} seconds.",
                    totalCountHolder.getValue(), (System.currentTimeMillis() - start) / 1000L);
        }
    }

    private void checkTemporaryTablesSupport() {
        SupportedDatabase database = sqlConfiguration().getDatabaseType();
        try {
            TemporaryTableDialect.getTempTableDialect(database);
        } catch (SystemException e) {
            LOGGER.error(
                    "Database type {} doesn't support temporary tables, couldn't cleanup audit logs.",
                    database);
            throw new SystemException("Database type " + database
                    + " doesn't support temporary tables, couldn't cleanup audit logs.");
        }
    }

    /**
     * Deletes one batch of records using recordsSelector to select records
     * according to particular cleanup policy.
     */
    private int batchDeletionAttempt(
            BiFunction<JdbcSession, String, Integer> recordsSelector,
            Holder<Integer> totalCountHolder, long batchStart, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult("batchDeletionAttempt");

        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            try {
                TemporaryTableDialect ttDialect = TemporaryTableDialect
                        .getTempTableDialect(sqlConfiguration().getDatabaseType());

                // create temporary table
                final String tempTable =
                        ttDialect.generateTemporaryTableName(QAuditEventRecord.TABLE_NAME);
                createTemporaryTable(jdbcSession, tempTable);
                LOGGER.trace("Created temporary table '{}'.", tempTable);

                int count = recordsSelector.apply(jdbcSession, tempTable);
                LOGGER.trace("Inserted {} audit record ids ready for deleting.", count);

                // drop records from m_audit_item, m_audit_event, m_audit_delta, and others
                jdbcSession.executeStatement(
                        createDeleteQuery(QAuditItem.TABLE_NAME,
                                tempTable, QAuditItem.RECORD_ID));
                jdbcSession.executeStatement(
                        createDeleteQuery(QAuditDelta.TABLE_NAME,
                                tempTable, QAuditDelta.RECORD_ID));
                jdbcSession.executeStatement(
                        createDeleteQuery(QAuditPropertyValue.TABLE_NAME,
                                tempTable, QAuditPropertyValue.RECORD_ID));
                jdbcSession.executeStatement(
                        createDeleteQuery(QAuditRefValue.TABLE_NAME,
                                tempTable, QAuditRefValue.RECORD_ID));
                jdbcSession.executeStatement(
                        createDeleteQuery(QAuditResource.TABLE_NAME,
                                tempTable, QAuditResource.RECORD_ID));
                jdbcSession.executeStatement(
                        createDeleteQuery(QAuditEventRecord.TABLE_NAME,
                                tempTable, QAuditEventRecord.ID));

                // drop temporary table
                if (ttDialect.dropTemporaryTableAfterUse()) {
                    LOGGER.debug("Dropping temporary table.");
                    jdbcSession.executeStatement(
                            ttDialect.getDropTemporaryTableString() + ' ' + tempTable);
                }

                jdbcSession.commit();
                // commit would happen automatically, but if it fails, we don't change the numbers
                int totalCount = totalCountHolder.getValue() + count;
                totalCountHolder.setValue(totalCount);
                LOGGER.debug("Audit cleanup batch finishing successfully in {} milliseconds; total count = {}",
                        System.currentTimeMillis() - batchStart, totalCount);

                return count;
            } catch (RuntimeException ex) {
                LOGGER.debug("Audit cleanup batch finishing with exception in {} milliseconds; exception = {}",
                        System.currentTimeMillis() - batchStart, ex.getMessage());
                baseHelper.handleGeneralRuntimeException(ex, jdbcSession, result);
                throw new AssertionError("We shouldn't get here.");
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private int selectRecordsByMaxAge(
            JdbcSession jdbcSession, String tempTable, Date minValue) {

        QAuditEventRecord aer = QAuditEventRecordMapping.get().defaultAlias();
        SQLQuery<Long> populateQuery = jdbcSession.newQuery()
                .select(aer.id)
                .from(aer)
                .where(aer.timestamp.lt(Instant.ofEpochMilli(minValue.getTime())))
                // we limit the query, but we don't care about order, eventually we'll get them all
                .limit(CLEANUP_AUDIT_BATCH_SIZE);

        QAuditTemp tmp = new QAuditTemp("tmp", tempTable);
        return (int) jdbcSession.newInsert(tmp).select(populateQuery).execute();
    }

    private int selectRecordsByNumberToKeep(
            JdbcSession jdbcSession, String tempTable, int recordsToKeep) {

        QAuditEventRecord aer = QAuditEventRecordMapping.get().defaultAlias();
        long totalAuditRecords = jdbcSession.newQuery().from(aer).fetchCount();

        // we will find the number to delete and limit it to range [0,CLEANUP_AUDIT_BATCH_SIZE]
        long recordsToDelete = Math.max(0,
                Math.min(totalAuditRecords - recordsToKeep, CLEANUP_AUDIT_BATCH_SIZE));
        LOGGER.debug("Total audit records: {}, records to keep: {} => records to delete in this batch: {}",
                totalAuditRecords, recordsToKeep, recordsToDelete);
        if (recordsToDelete == 0) {
            return 0;
        }

        SQLQuery<Long> populateQuery = jdbcSession.newQuery()
                .select(aer.id)
                .from(aer)
                .orderBy(aer.timestamp.asc())
                .limit(recordsToDelete);

        QAuditTemp tmp = new QAuditTemp("tmp", tempTable);
        return (int) jdbcSession.newInsert(tmp).select(populateQuery).execute();
    }

    /**
     * This method creates temporary table for cleanup audit method.
     */
    private void createTemporaryTable(JdbcSession jdbcSession, final String tempTable) {
        // check if table exists
        if (!sqlConfiguration().isUsingPostgreSQL()) {
            try {
                jdbcSession.executeStatement("select id from " + tempTable + " where id = 1");
                // table already exists
                return;
            } catch (Exception ex) {
                // we expect this on the first time
            }
        }

        TemporaryTableDialect ttDialect =
                TemporaryTableDialect.getTempTableDialect(sqlConfiguration().getDatabaseType());

        jdbcSession.executeStatement(ttDialect.getCreateTemporaryTableString()
                + ' ' + tempTable + " (id "
                + jdbcSession.getNativeTypeName(Types.BIGINT)
                + " not null)"
                + ttDialect.getCreateTemporaryTablePostfix());
    }

    private String createDeleteQuery(
            String objectTable, String tempTable, ColumnMetadata idColumn) {
        if (sqlConfiguration().isUsingMySqlCompatible()) {
            return createDeleteQueryAsJoin(objectTable, tempTable, idColumn);
        } else if (sqlConfiguration().isUsingPostgreSQL()) {
            return createDeleteQueryAsJoinPostgreSQL(objectTable, tempTable, idColumn);
        } else {
            // todo consider using join for other databases as well
            return createDeleteQueryAsSubquery(objectTable, tempTable, idColumn);
        }
    }

    private String createDeleteQueryAsJoin(
            String objectTable, String tempTable, ColumnMetadata idColumn) {
        return "DELETE FROM main, temp USING " + objectTable + " AS main"
                + " INNER JOIN " + tempTable + " as temp"
                + " WHERE main." + idColumn.getName() + " = temp.id";
    }

    private String createDeleteQueryAsJoinPostgreSQL(
            String objectTable, String tempTable, ColumnMetadata idColumn) {
        return "delete from " + objectTable + " main using " + tempTable
                + " temp where main." + idColumn.getName() + " = temp.id";
    }

    private String createDeleteQueryAsSubquery(
            String objectTable, String tempTable, ColumnMetadata idColumn) {
        return "delete from " + objectTable
                + " where " + idColumn.getName() + " in (select id from " + tempTable
                + ')';
    }

    @Override
    public boolean supportsRetrieval() {
        return true;
    }

    @Override
    public void applyAuditConfiguration(SystemConfigurationAuditType configuration) {
        this.auditConfiguration = CloneUtil.clone(configuration);
    }

    @Override
    public int countObjects(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) {
        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + OP_COUNT_OBJECTS)
                .addParam("query", query)
                .build();

        try {
            var queryContext = AuditSqlQueryContext.from(
                    AuditEventRecordType.class, sqlRepoContext);
            return sqlQueryExecutor.count(queryContext, query, options);
        } catch (RepositoryException | RuntimeException e) {
            baseHelper.handleGeneralException(e, operationResult);
            throw new SystemException(e);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    @Override
    @NotNull
    public SearchResultList<AuditEventRecordType> searchObjects(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult)
            throws SchemaException {
        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + OP_SEARCH_OBJECTS)
                .addParam("query", query)
                .build();

        try {
            var queryContext = AuditSqlQueryContext.from(
                    AuditEventRecordType.class, sqlRepoContext);
            SearchResultList<AuditEventRecordType> result =
                    sqlQueryExecutor.list(queryContext, query, options);
            return result;
        } catch (RepositoryException | RuntimeException e) {
            baseHelper.handleGeneralException(e, operationResult);
            throw new SystemException(e);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    @Override
    public SearchResultMetadata searchObjectsIterative(
            @Nullable ObjectQuery query,
            @NotNull AuditResultHandler handler,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) throws SchemaException {
        // TODO: supported for Ninja, implementation is crude rework from new repo
//        throw new UnsupportedOperationException("searchObjectsIterative not supported in old repository audit");
        Validate.notNull(handler, "Result handler must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + OP_SEARCH_OBJECTS_ITERATIVE)
                .addParam("type", AuditEventRecordType.class.getName())
                .addParam("query", query)
                .build();

        try {
            if (query == null) {
                return new SearchResultMetadata().approxNumberOfAllResults(0);
            }

            return executeSearchObjectsIterative(query, handler, options, operationResult);
        } catch (RepositoryException | RuntimeException e) {
            operationResult.recordFatalError(e);
            throw new SystemException(e);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    private SearchResultMetadata executeSearchObjectsIterative(
            ObjectQuery originalQuery,
            AuditResultHandler handler,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult operationResult) throws SchemaException, RepositoryException {

        ObjectPaging originalPaging = originalQuery != null ? originalQuery.getPaging() : null;
        // this is total requested size of the search
        Integer maxSize = originalPaging != null ? originalPaging.getMaxSize() : null;

        List<? extends ObjectOrdering> providedOrdering = originalPaging != null
                ? originalPaging.getOrderingInstructions()
                : null;
        if (providedOrdering != null && providedOrdering.size() > 1) {
            throw new RepositoryException("searchObjectsIterative() does not support ordering"
                    + " by multiple paths (yet): " + providedOrdering);
        }

        ObjectQuery pagedQuery = schemaService.prismContext().queryFactory().createQuery();
        ObjectPaging paging = schemaService.prismContext().queryFactory().createPaging();
        if (originalPaging != null && originalPaging.getOrderingInstructions() != null) {
            originalPaging.getOrderingInstructions().forEach(o ->
                    paging.addOrderingInstruction(o.getOrderBy(), o.getDirection()));
        }
        // TODO check of provided ordering
        paging.addOrderingInstruction(AuditEventRecordType.F_REPO_ID, OrderDirection.ASCENDING);
        pagedQuery.setPaging(paging);

        int pageSize = Math.min(
                sqlConfiguration().getIterativeSearchByPagingBatchSize(),
                defaultIfNull(maxSize, Integer.MAX_VALUE));
        pagedQuery.getPaging().setMaxSize(pageSize);

        AuditEventRecordType lastProcessedObject = null;
        int handledObjectsTotal = 0;

        while (true) {
            if (maxSize != null && maxSize - handledObjectsTotal < pageSize) {
                // relevant only for the last page
                pagedQuery.getPaging().setMaxSize(maxSize - handledObjectsTotal);
            }

            // filterAnd() is quite null safe, even for both nulls
            ObjectFilter originalFilter = originalQuery != null ? originalQuery.getFilter() : null;
            pagedQuery.setFilter(ObjectQueryUtil.filterAndImmutable(
                    originalFilter, iterativeSearchCondition(lastProcessedObject, providedOrdering)));

            // we don't call public searchObject to avoid subresults and query simplification
            List<AuditEventRecordType> resultPage = searchObjects(
                    pagedQuery, options, operationResult);

            // process page results
            for (AuditEventRecordType auditEvent : resultPage) {
                lastProcessedObject = auditEvent;
                if (!handler.handle(auditEvent, operationResult)) {
                    return new SearchResultMetadata()
                            .approxNumberOfAllResults(handledObjectsTotal + 1)
                            .pagingCookie(lastProcessedObject.getRepoId().toString())
                            .partialResults(true);
                }
                handledObjectsTotal += 1;

                if (maxSize != null && handledObjectsTotal >= maxSize) {
                    return new SearchResultMetadata()
                            .approxNumberOfAllResults(handledObjectsTotal)
                            .pagingCookie(lastProcessedObject.getRepoId().toString());
                }
            }

            if (resultPage.isEmpty() || resultPage.size() < pageSize) {
                return new SearchResultMetadata()
                        .approxNumberOfAllResults(handledObjectsTotal)
                        .pagingCookie(lastProcessedObject != null
                                ? lastProcessedObject.getRepoId().toString() : null);
            }
        }
    }

    /**
     * See {@code SqaleRepositoryService.iterativeSearchCondition()} for more info.
     * This is unsupported version only for Ninja usage.
     */
    @Nullable
    private ObjectFilter iterativeSearchCondition(
            @Nullable AuditEventRecordType lastProcessedObject,
            List<? extends ObjectOrdering> providedOrdering) {
        if (lastProcessedObject == null) {
            return null;
        }

        Long lastProcessedId = lastProcessedObject.getRepoId();
        if (providedOrdering == null || providedOrdering.isEmpty()) {
            return schemaService.prismContext()
                    .queryFor(AuditEventRecordType.class)
                    .item(AuditEventRecordType.F_REPO_ID).gt(lastProcessedId)
                    .buildFilter();
        }

        if (providedOrdering.size() == 1) {
            ObjectOrdering objectOrdering = providedOrdering.get(0);
            ItemPath orderByPath = objectOrdering.getOrderBy();
            boolean asc = objectOrdering.getDirection() != OrderDirection.DESCENDING; // null => asc
            S_ConditionEntry filter = schemaService.prismContext()
                    .queryFor(AuditEventRecordType.class)
                    .item(orderByPath);
            @SuppressWarnings("unchecked")
            Item<PrismValue, ItemDefinition<?>> item =
                    lastProcessedObject.asPrismContainerValue().findItem(orderByPath);
            if (item.size() > 1) {
                throw new IllegalArgumentException(
                        "Multi-value property for ordering is forbidden - item: " + item);
            } else if (item.isEmpty()) {
                // TODO what if it's nullable? is it null-first or last?
                // See: https://www.postgresql.org/docs/13/queries-order.html
                // "By default, null values sort as if larger than any non-null value; that is,
                // NULLS FIRST is the default for DESC order, and NULLS LAST otherwise."
            } else {
                S_MatchingRuleEntry matchingRuleEntry =
                        asc ? filter.gt(item.getRealValue()) : filter.lt(item.getRealValue());
                filter = matchingRuleEntry.or()
                        .block()
                        .item(orderByPath).eq(item.getRealValue())
                        .and()
                        .item(AuditEventRecordType.F_REPO_ID);
                return (asc ? filter.gt(lastProcessedId) : filter.lt(lastProcessedId))
                        .endBlock()
                        .buildFilter();
            }
        }

        throw new IllegalArgumentException(
                "Shouldn't get here with check in executeSearchObjectsIterative()");
    }
}

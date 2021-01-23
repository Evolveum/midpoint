/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditEventRecord.*;
import static com.evolveum.midpoint.schema.util.SystemConfigurationAuditUtil.isEscapingInvalidCharacters;

import java.sql.*;
import java.time.Instant;
import java.util.Date;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import javax.xml.datatype.Duration;

import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.DefaultMapper;
import com.querydsl.sql.dml.SQLInsertClause;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditReferenceValue;
import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.CanonicalItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.sql.audit.AuditSqlQueryContext;
import com.evolveum.midpoint.repo.sql.audit.beans.MAuditDelta;
import com.evolveum.midpoint.repo.sql.audit.beans.MAuditEventRecord;
import com.evolveum.midpoint.repo.sql.audit.mapping.*;
import com.evolveum.midpoint.repo.sql.audit.querymodel.*;
import com.evolveum.midpoint.repo.sql.data.SelectQueryBuilder;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventStage;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventType;
import com.evolveum.midpoint.repo.sql.data.common.enums.RChangeType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sql.perf.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sql.util.TemporaryTableDialect;
import com.evolveum.midpoint.repo.sqlbase.*;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectType;

/**
 * Audit service using SQL DB as a store, also allows for searching (see {@link #supportsRetrieval}.
 * This is NOT a managed bean, it is completely created by {@link SqlAuditServiceFactory} and any
 * of the dependencies must be dependencies of that factory to assure proper initialization.
 * <p>
 * Design notes:
 * No repo.sql.data.audit.* entities are used (stage/type enums are OK).
 */
public class SqlAuditServiceImpl extends SqlBaseService implements AuditService {

    private static final Trace LOGGER = TraceManager.getTrace(SqlAuditServiceImpl.class);

    private static final String OP_NAME_PREFIX = SqlAuditServiceImpl.class.getSimpleName() + '.';

    private static final String OP_CLEANUP_AUDIT_MAX_AGE = "cleanupAuditMaxAge";
    private static final String OP_CLEANUP_AUDIT_MAX_RECORDS = "cleanupAuditMaxRecords";
    private static final String OP_LIST_RECORDS = "listRecords";
    private static final String OP_LIST_RECORDS_ATTEMPT = "listRecordsAttempt";
    private static final String OP_LOAD_AUDIT_DELTA = "loadAuditDelta";

    private static final Integer CLEANUP_AUDIT_BATCH_SIZE = 500;

    private static final String QUERY_MAX_RESULT = "setMaxResults";
    private static final String QUERY_FIRST_RESULT = "setFirstResult";

    private final BaseHelper baseHelper; // only for logging/exception handling
    private final SqlRepoContext sqlRepoContext;
    private final PrismContext prismContext;

    private final SqlQueryExecutor sqlQueryExecutor;
    private final SqlTransformerContext sqlTransformerContext;

    private volatile SystemConfigurationAuditType auditConfiguration;

    public SqlAuditServiceImpl(
            BaseHelper baseHelper,
            SqlRepoContext sqlRepoContext,
            PrismContext prismContext) {
        this.baseHelper = baseHelper;
        this.sqlRepoContext = sqlRepoContext;
        this.prismContext = prismContext;
        this.sqlQueryExecutor = new SqlQueryExecutor(sqlRepoContext);
        // null is safe here, audit transformations don't use RelationRegistry
        this.sqlTransformerContext = new SqlTransformerContext(prismContext, null);
    }

    public SqlRepoContext getSqlRepoContext() {
        return sqlRepoContext;
    }

    @Override
    public SqlRepositoryConfiguration sqlConfiguration() {
        return (SqlRepositoryConfiguration) sqlRepoContext.getJdbcRepositoryConfiguration();
    }

    @Override
    public void audit(AuditEventRecord record, Task task) {
        Objects.requireNonNull(record, "Audit event record must not be null.");
        Objects.requireNonNull(task, "Task must not be null.");

        final String operation = "audit";
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(operation, AuditEventRecord.class);
        int attempt = 1;

        while (true) {
            try {
                auditAttempt(record);
                return;
            } catch (RuntimeException ex) {
                attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, null);
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
            } catch (RuntimeException ex) {
                jdbcSession.handleGeneralException(ex, null);
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
        QAuditEventRecordMapping aerMapping = QAuditEventRecordMapping.INSTANCE;
        QAuditEventRecord aer = aerMapping.defaultAlias();
        MAuditEventRecord aerBean = aerMapping
                .createTransformer(sqlTransformerContext, sqlRepoContext)
                .from(record);
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

        return insert.executeWithKey(aer.id);
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
                    QAuditDeltaMapping.INSTANCE.defaultAlias());
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
                String serializedDelta = DeltaConvertor.toObjectDeltaTypeXml(delta, options);

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
                    String full = prismContext.xmlSerializer()
                            .options(SerializationOptions.createEscapeInvalidCharacters())
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
                        prismContext.parserFor(delta.serializedDelta)
                                .parseRealValue(ObjectDeltaType.class);
                for (ItemDeltaType itemDelta : deltaBean.getItemDelta()) {
                    ItemPath path = itemDelta.getPath().getItemPath();
                    CanonicalItemPath canonical = prismContext.createCanonicalItemPath(
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
            QAuditItem qAuditItem = QAuditItemMapping.INSTANCE.defaultAlias();
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

        QAuditPropertyValue qAuditPropertyValue = QAuditPropertyValueMapping.INSTANCE.defaultAlias();
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

        QAuditRefValue qAuditRefValue = QAuditRefValueMapping.INSTANCE.defaultAlias();
        SQLInsertClause insertBatch = jdbcSession.newInsert(qAuditRefValue);
        for (String refName : references.keySet()) {
            for (AuditReferenceValue refValue : references.get(refName)) {
                // id will be generated, but we're not interested in those here
                PolyString targetName = refValue.getTargetName();
                insertBatch.set(qAuditRefValue.recordId, recordId)
                        .set(qAuditRefValue.name, refName)
                        .set(qAuditRefValue.oid, refValue.getOid())
                        .set(qAuditRefValue.type, RUtil.qnameToString(refValue.getType()))
                        .set(qAuditRefValue.targetNameOrig,
                                targetName != null ? targetName.getOrig() : null)
                        .set(qAuditRefValue.targetNameNorm,
                                targetName != null ? targetName.getNorm() : null)
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
            JdbcSession jdbcSession, long recordId, Set<String> resourceOids) {
        if (resourceOids.isEmpty()) {
            return;
        }

        QAuditResource qAuditResource = QAuditResourceMapping.INSTANCE.defaultAlias();
        SQLInsertClause insertBatch = jdbcSession.newInsert(qAuditResource);
        for (String resourceOid : resourceOids) {
            insertBatch.set(qAuditResource.recordId, recordId)
                    .set(qAuditResource.resourceOid, resourceOid)
                    .addBatch();
        }

        insertBatch.setBatchToBulk(true);
        insertBatch.execute();
    }

    /** @deprecated use {@link #searchObjects(ObjectQuery, Collection, OperationResult)} instead */
    @Override
    @Deprecated
    public List<AuditEventRecord> listRecords(
            String query, Map<String, Object> params, OperationResult parentResult) {
        final String operation = "listRecords";
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(operation, AuditEventRecord.class);
        int attempt = 1;

        OperationResult result = parentResult.subresult(OP_LIST_RECORDS)
                .addParam("query", query)
                .build();

        while (true) {
            OperationResult attemptResult = result.createMinorSubresult(OP_LIST_RECORDS_ATTEMPT);
            try {
                final List<AuditEventRecord> auditEventRecords = new ArrayList<>();

                AuditResultHandler handler = new AuditResultHandler() {

                    @Override
                    public boolean handle(AuditEventRecord auditRecord) {
                        auditEventRecords.add(auditRecord);
                        return true;
                    }

                    @Override
                    public boolean handle(AuditEventRecordType auditRecord) {
                        return true;
                    }

                    @Override
                    public int getProgress() {
                        return 0;
                    }
                };
                listRecordsIterativeAttempt(query, params, handler, attemptResult);
                return auditEventRecords;
            } catch (RuntimeException ex) {
                attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, null);
                pm.registerOperationNewAttempt(opHandle, attempt);
                LOGGER.error("Error while trying to list audit records, {}", ex.getMessage(), ex);
                attemptResult.recordFatalError(
                        "Error while trying to list audit records, " + ex.getMessage(), ex);
            } finally {
                pm.registerOperationFinish(opHandle, attempt);
                attemptResult.computeStatus();
                result.computeStatus();
                result.cleanupResult();
            }
        }
    }

    @Deprecated
    private void listRecordsIterativeAttempt(String query,
            Map<String, Object> params, AuditResultHandler handler, OperationResult result) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("List records attempt\n  query: {}\n params:\n{}", query,
                    DebugUtil.debugDump(params, 2));
        }

        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
            try {
                Connection conn = jdbcSession.connection();
                SupportedDatabase database = sqlConfiguration().getDatabaseType();
                int count = 0;
                String basicQuery = query;
                if (StringUtils.isBlank(query)) {
                    basicQuery = "select * from m_audit_event "
                            + (database == SupportedDatabase.ORACLE ? "" : "as ")
                            + "aer where 1=1 order by aer.timestampValue desc";
                }
                SelectQueryBuilder queryBuilder = new SelectQueryBuilder(database, basicQuery);
                setParametersToQuery(queryBuilder, params);

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("List records attempt\n  processed query: {}", queryBuilder);
                }

                try (PreparedStatement stmt = queryBuilder.build().createPreparedStatement(conn)) {
                    ResultSet resultList = stmt.executeQuery();
                    while (resultList.next()) {
                        AuditEventRecord audit =
                                createAuditEventRecordAggregate(resultList, jdbcSession, result);
                        count++;
                        if (!handler.handle(audit)) {
                            LOGGER.trace("Skipping handling of objects after {} was handled. ", audit);
                            break;
                        }
                    }
                } finally {
                    result.computeStatus();
                }

                LOGGER.trace("List records iterative attempt processed {} records", count);
            } catch (Exception ex) {
                jdbcSession.handleGeneralException(ex, result);
            }
        }
    }

    /**
     * Creates {@link AuditEventRecord} with all its subentities.
     */
    @NotNull
    private AuditEventRecord createAuditEventRecordAggregate(
            ResultSet resultList, JdbcSession jdbcSession, OperationResult result)
            throws SQLException {
        AuditEventRecord audit = createAuditEventRecord(resultList);
        final Map<String, ColumnMetadata> customColumns =
                QAuditEventRecordMapping.INSTANCE.getExtensionColumns();
        for (Entry<String, ColumnMetadata> entry : customColumns.entrySet()) {
            audit.getCustomColumnProperty().put(entry.getKey(),
                    resultList.getString(entry.getValue().getName()));
        }

        //query for deltas
        String tableAliasPreposition =
                jdbcSession.databaseType() == SupportedDatabase.ORACLE ? "" : "as ";
        OperationResult deltaResult = result.createMinorSubresult(OP_LOAD_AUDIT_DELTA);
        try (PreparedStatement subStmt = jdbcSession.connection().prepareStatement(
                "select * from m_audit_delta " + tableAliasPreposition
                        + "delta where delta.record_id=?")) {
            subStmt.setLong(1, resultList.getLong(QAuditEventRecord.ID.getName()));

            ResultSet subResultList = subStmt.executeQuery();
            while (subResultList.next()) {
                try {
                    ObjectDeltaOperation<?> odo = createObjectDeltaOperation(subResultList);
                    audit.addDelta(odo);
                } catch (DtoTranslationException ex) {
                    LOGGER.error("Cannot convert stored audit delta. Reason: {}", ex.getMessage(), ex);
                    deltaResult.recordPartialError("Cannot convert stored audit delta. Reason: " + ex.getMessage(), ex);
                    //do not throw an error. rather audit record without delta than fatal error.
                }
            }
        } catch (Throwable t) {
            deltaResult.recordFatalError(t);
            throw t;
        } finally {
            deltaResult.computeStatus();
        }

        //query for properties
        try (PreparedStatement subStmt = jdbcSession.connection().prepareStatement(
                "select * from m_audit_prop_value " + tableAliasPreposition
                        + "prop where prop.record_id=?")) {
            subStmt.setLong(1, resultList.getLong(QAuditEventRecord.ID.getName()));

            ResultSet subResultList = subStmt.executeQuery();
            while (subResultList.next()) {
                audit.addPropertyValue(subResultList.getString(QAuditPropertyValue.NAME.getName()),
                        subResultList.getString(QAuditPropertyValue.VALUE.getName()));
            }
        }

        //query for references
        try (PreparedStatement subStmt = jdbcSession.connection().prepareStatement(
                "select * from m_audit_ref_value " + tableAliasPreposition
                        + "ref where ref.record_id=?")) {
            subStmt.setLong(1, resultList.getLong(QAuditEventRecord.ID.getName()));

            ResultSet subResultList = subStmt.executeQuery();
            while (subResultList.next()) {
                audit.addReferenceValue(subResultList.getString(QAuditRefValue.NAME.getName()),
                        createAuditReferenceValue(subResultList));
            }
        }

        //query for target resource oids
        try (PreparedStatement subStmt = jdbcSession.connection().prepareStatement(
                "select * from m_audit_resource " + tableAliasPreposition
                        + "res where res.record_id=?")) {
            subStmt.setLong(1, resultList.getLong(QAuditEventRecord.ID.getName()));
            ResultSet subResultList = subStmt.executeQuery();

            while (subResultList.next()) {
                audit.addResourceOid(subResultList.getString(QAuditResource.RESOURCE_OID.getName()));
            }
        }

        audit.setInitiatorRef(prismRefValue(
                resultList.getString(QAuditEventRecord.INITIATOR_OID.getName()),
                resultList.getString(QAuditEventRecord.INITIATOR_NAME.getName()),
                resultList.getString(QAuditEventRecord.INITIATOR_NAME.getName()),
                Objects.requireNonNullElse(
                        repoObjectType(resultList, QAuditEventRecord.INITIATOR_TYPE.getName()),
                        RObjectType.FOCUS)));
        audit.setAttorneyRef(prismRefValue(
                resultList.getString(QAuditEventRecord.ATTORNEY_OID.getName()),
                resultList.getString(QAuditEventRecord.ATTORNEY_NAME.getName()),
                resultList.getString(QAuditEventRecord.ATTORNEY_NAME.getName()),
                RObjectType.FOCUS));
        audit.setTargetRef(prismRefValue(
                resultList.getString(QAuditEventRecord.TARGET_OID.getName()),
                resultList.getString(QAuditEventRecord.TARGET_TYPE.getName()),
                resultList.getString(QAuditEventRecord.TARGET_NAME.getName()),
                repoObjectType(resultList, QAuditEventRecord.TARGET_TYPE.getName())));
        audit.setTargetOwnerRef(prismRefValue(
                resultList.getString(QAuditEventRecord.TARGET_OWNER_OID.getName()),
                resultList.getString(QAuditEventRecord.TARGET_OWNER_NAME.getName()),
                resultList.getString(QAuditEventRecord.TARGET_OWNER_NAME.getName()),
                repoObjectType(resultList, QAuditEventRecord.TARGET_OWNER_TYPE.getName())));
        return audit;
    }

    private AuditEventRecord createAuditEventRecord(ResultSet resultSet) throws SQLException {
        AuditEventRecord audit = new AuditEventRecord();
        audit.setChannel(resultSet.getString(CHANNEL.getName()));
        audit.setEventIdentifier(resultSet.getString(EVENT_IDENTIFIER.getName()));
        if (resultSet.getObject(EVENT_STAGE.getName()) != null) {
            audit.setEventStage(RAuditEventStage.values()[resultSet.getInt(EVENT_STAGE.getName())].getStage());
        }
        if (resultSet.getObject(EVENT_TYPE.getName()) != null) {
            audit.setEventType(RAuditEventType.values()[resultSet.getInt(EVENT_TYPE.getName())].getType());
        }
        audit.setHostIdentifier(resultSet.getString(HOST_IDENTIFIER.getName()));
        audit.setRemoteHostAddress(resultSet.getString(REMOTE_HOST_ADDRESS.getName()));
        audit.setNodeIdentifier(resultSet.getString(NODE_IDENTIFIER.getName()));
        audit.setMessage(resultSet.getString(MESSAGE.getName()));

        if (resultSet.getObject(OUTCOME.getName()) != null) {
            audit.setOutcome(
                    ROperationResultStatus.values()[resultSet.getInt(OUTCOME.getName())].getStatus());
        }
        audit.setParameter(resultSet.getString(PARAMETER.getName()));
        audit.setResult(resultSet.getString(RESULT.getName()));
        audit.setSessionIdentifier(resultSet.getString(SESSION_IDENTIFIER.getName()));
        audit.setRequestIdentifier(resultSet.getString(REQUEST_IDENTIFIER.getName()));
        audit.setTaskIdentifier(resultSet.getString(TASK_IDENTIFIER.getName()));
        audit.setTaskOid(resultSet.getString(TASK_OID.getName()));
        if (resultSet.getTimestamp(TIMESTAMP.getName()) != null) {
            audit.setTimestamp(resultSet.getTimestamp(TIMESTAMP.getName()).getTime());
        }

        audit.setRepoId(resultSet.getLong(ID.getName()));

        return audit;
    }

    @NotNull
    private ObjectDeltaOperation<?> createObjectDeltaOperation(ResultSet resultSet)
            throws DtoTranslationException {

        ObjectDeltaOperation<?> odo = new ObjectDeltaOperation<>();
        try {
            if (resultSet.getBytes(QAuditDelta.DELTA.getName()) != null) {
                byte[] data = resultSet.getBytes(QAuditDelta.DELTA.getName());
                String serializedDelta = RUtil.getSerializedFormFromBytes(data, sqlConfiguration().isUsingSQLServer());

                ObjectDeltaType delta = prismContext.parserFor(serializedDelta)
                        .parseRealValue(ObjectDeltaType.class);
                odo.setObjectDelta(DeltaConvertor.createObjectDelta(delta, prismContext));
            }
            if (resultSet.getBytes(QAuditDelta.FULL_RESULT.getName()) != null) {
                byte[] data = resultSet.getBytes(QAuditDelta.FULL_RESULT.getName());
                String serializedResult = RUtil.getSerializedFormFromBytes(data, sqlConfiguration().isUsingSQLServer());

                OperationResultType resultType = prismContext.parserFor(serializedResult)
                        .parseRealValue(OperationResultType.class);
                odo.setExecutionResult(OperationResult.createOperationResult(resultType));
            }
            if (resultSet.getString(QAuditDelta.OBJECT_NAME_ORIG.getName()) != null
                    || resultSet.getString(QAuditDelta.OBJECT_NAME_NORM.getName()) != null) {
                odo.setObjectName(new PolyString(
                        resultSet.getString(QAuditDelta.OBJECT_NAME_ORIG.getName()),
                        resultSet.getString(QAuditDelta.OBJECT_NAME_NORM.getName())));
            }
            odo.setResourceOid(resultSet.getString(QAuditDelta.RESOURCE_OID.getName()));
            if (resultSet.getString(QAuditDelta.RESOURCE_NAME_ORIG.getName()) != null
                    || resultSet.getString(QAuditDelta.RESOURCE_NAME_NORM.getName()) != null) {
                odo.setResourceName(new PolyString(
                        resultSet.getString(QAuditDelta.RESOURCE_NAME_ORIG.getName()),
                        resultSet.getString(QAuditDelta.RESOURCE_NAME_NORM.getName())));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        return odo;
    }

    private AuditReferenceValue createAuditReferenceValue(ResultSet resultSet) throws SQLException {
        PolyString targetName = null;
        if (resultSet.getString(QAuditRefValue.TARGET_NAME_ORIG.getName()) != null
                || resultSet.getString(QAuditRefValue.TARGET_NAME_NORM.getName()) != null) {
            targetName = new PolyString(resultSet.getString(QAuditRefValue.TARGET_NAME_ORIG.getName()),
                    resultSet.getString(QAuditRefValue.TARGET_NAME_NORM.getName()));
        }
        return new AuditReferenceValue(resultSet.getString(QAuditRefValue.OID.getName()),
                RUtil.stringToQName(resultSet.getString(QAuditRefValue.TYPE.getName())), targetName);
    }

    @Override
    public void reindexEntry(AuditEventRecord record) {
        LOGGER.warn("Audit reindex does nothing now and probably should not be used.");
        /* TODO: disabled in 2020 during MID-6318, see Javadoc from interface.
         * Consider removal if it doesn't get proper meaning in some not so distant time.
        final String operation = "reindexEntry";
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(operation, AuditEventRecord.class);
        int attempt = 1;

        while (true) {
            try {
                reindexEntryAttempt(record);
                return;
            } catch (RuntimeException ex) {
                attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, null);
                pm.registerOperationNewAttempt(opHandle, attempt);
            } finally {
                pm.registerOperationFinish(opHandle, attempt);
            }
        }
    }

    private void reindexEntryAttempt(AuditEventRecord record) {
        Session session = baseHelper.beginTransaction();
        try {
            RAuditEventRecord reindexed = RAuditEventRecord.toRepo(record, prismContext, null, auditConfiguration);
            //TODO FIXME temporary hack, merge will eventually load the object to the session if there isn't one,
            // but in this case we force loading object because of "objectDeltaOperation". There is some problem probably
            // during serializing/deserializing which causes constraint violation on primary key..
            RAuditEventRecord rRecord = session.load(RAuditEventRecord.class, record.getRepoId());
            rRecord.getChangedItems().clear();
            rRecord.getChangedItems().addAll(reindexed.getChangedItems());
            session.merge(rRecord);

            session.getTransaction().commit();
        } catch (DtoTranslationException ex) {
            baseHelper.handleGeneralCheckedException(ex, session, null);
        } catch (RuntimeException ex) {
            baseHelper.handleGeneralRuntimeException(ex, session, null);
        } finally {
            baseHelper.cleanupSessionAndResult(session, null);
        }
        */
    }

    private RObjectType repoObjectType(ResultSet resultList, String columnName)
            throws SQLException {
        // Yes, to detect null, you have to check again after reading int. No getInteger there.
        int ordinalValue = resultList.getInt(columnName);
        return resultList.wasNull() ? null : RObjectType.fromOrdinal(ordinalValue);
    }

    private PrismReferenceValue prismRefValue(
            String oid, String description, String targetName, RObjectType repoObjectType) {
        if (oid == null) {
            return null;
        }

        PrismReferenceValue prv = prismContext.itemFactory().createReferenceValue(oid,
                prismContext.getSchemaRegistry().determineTypeForClass(
                        repoObjectType.getJaxbClass()));
        prv.setDescription(description);
        if (targetName != null) {
            prv.setTargetName(new PolyString(targetName));
        }
        return prv;
    }

    private void setParametersToQuery(SelectQueryBuilder queryBuilder, Map<String, Object> params) {
        if (params == null) {
            return;
        }

        if (params.containsKey(QUERY_FIRST_RESULT)) {
            queryBuilder.setFirstResult((int) params.get(QUERY_FIRST_RESULT));
            params.remove(QUERY_FIRST_RESULT);
        }
        if (params.containsKey(QUERY_MAX_RESULT)) {
            queryBuilder.setMaxResult((int) params.get(QUERY_MAX_RESULT));
            params.remove(QUERY_MAX_RESULT);
        }
        queryBuilder.addParameters(params);
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
                jdbcSession.handleGeneralRuntimeException(ex, result);
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

        QAuditEventRecord aer = QAuditEventRecordMapping.INSTANCE.defaultAlias();
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

        QAuditEventRecord aer = QAuditEventRecordMapping.INSTANCE.defaultAlias();
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

    public long countObjects(String query, Map<String, Object> params) {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
            try {
                SupportedDatabase database = jdbcSession.databaseType();

                String basicQuery = query;
                if (StringUtils.isBlank(query)) {
                    basicQuery = "select count(*) from m_audit_event "
                            + (database == SupportedDatabase.ORACLE ? "" : "as ")
                            + "aer where 1 = 1";
                }
                SelectQueryBuilder queryBuilder = new SelectQueryBuilder(database, basicQuery);
                setParametersToQuery(queryBuilder, params);

                LOGGER.trace("List records attempt\n  processed query: {}", queryBuilder);

                try (PreparedStatement stmt =
                        queryBuilder.build().createPreparedStatement(jdbcSession.connection())) {
                    ResultSet resultList = stmt.executeQuery();
                    if (!resultList.next()) {
                        throw new IllegalArgumentException(
                                "Result set don't have value for select: " + query);
                    }
                    if (resultList.getMetaData().getColumnCount() > 1) {
                        throw new IllegalArgumentException(
                                "Result have more as one value for select: " + query);
                    }
                    return resultList.getLong(1);
                }
            } catch (RuntimeException | SQLException ex) {
                jdbcSession.handleGeneralException(ex, null);
            }
        }
        // not good, there is not even an operation result to check for error status
        return 0;
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
        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + "countObjects")
                .addParam("query", query)
                .build();

        try {
            var queryContext = AuditSqlQueryContext.from(
                    AuditEventRecordType.class, sqlTransformerContext, sqlRepoContext);
            return sqlQueryExecutor.count(queryContext, query, options);
        } catch (QueryException | RuntimeException e) {
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
        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + "searchObjects")
                .addParam("query", query)
                .build();

        try {
            var queryContext = AuditSqlQueryContext.from(
                    AuditEventRecordType.class, sqlTransformerContext, sqlRepoContext);
            SearchResultList<AuditEventRecordType> result =
                    sqlQueryExecutor.list(queryContext, query, options);
//            addContainerDefinition(AuditEventRecordType.class, result);
            return result;
        } catch (QueryException | RuntimeException e) {
            baseHelper.handleGeneralException(e, operationResult);
            throw new SystemException(e);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    /**
     * This enriches collection of returned values with parent container so that
     * {@link PrismContainer#getDefinition()} is not null.
     * It is a bit questionable whether this is a responsibility of the repository-level service.
     * Even without this the values should have {@link PrismContainer#getComplexTypeDefinition()}
     * if properly created with {@link PrismContext} as constructor parameter.
     */
    @SuppressWarnings("SameParameterValue")
    private <C extends Containerable> void addContainerDefinition(
            Class<C> containerableType, List<C> containerableValues) throws SchemaException {
        PrismContainerDefinition<C> containerDefinition = prismContext.getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(containerableType);
        PrismContainer<C> container = containerDefinition.instantiate();
        for (C containerValue : containerableValues) {
            //noinspection unchecked
            container.add(containerValue.asPrismContainerValue());
        }
    }
}

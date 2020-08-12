/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.sql.*;
import java.time.Instant;
import java.util.Date;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import javax.xml.datatype.Duration;

import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.SQLInsertClause;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditReferenceValue;
import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.CanonicalItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.Database;
import com.evolveum.midpoint.repo.sql.data.BatchSqlQuery;
import com.evolveum.midpoint.repo.sql.data.SelectQueryBuilder;
import com.evolveum.midpoint.repo.sql.data.SingleSqlQuery;
import com.evolveum.midpoint.repo.sql.data.audit.*;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sql.helpers.JdbcSession;
import com.evolveum.midpoint.repo.sql.perf.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.repo.sql.pure.SqlQueryExecutor;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditEventRecord;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditTemp;
import com.evolveum.midpoint.repo.sql.pure.querymodel.mapping.QAuditEventRecordMapping;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sql.util.TemporaryTableDialect;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditType;

/**
 * Audit service using SQL DB as a store, also allows for searching (see {@link #supportsRetrieval}.
 * This is NOT a managed bean, it is completely created by {@link SqlAuditServiceFactory} and any
 * of the dependencies must be dependencies of that factory to assure proper initialization.
 */
public class SqlAuditServiceImpl extends SqlBaseService implements AuditService {

    private static final Trace LOGGER = TraceManager.getTrace(SqlAuditServiceImpl.class);

    private static final String OP_CLEANUP_AUDIT_MAX_AGE = "cleanupAuditMaxAge";
    private static final String OP_CLEANUP_AUDIT_MAX_RECORDS = "cleanupAuditMaxRecords";
    private static final String OP_LIST_RECORDS = "listRecords";
    private static final String OP_LIST_RECORDS_ATTEMPT = "listRecordsAttempt";
    private static final String OP_LOAD_AUDIT_DELTA = "loadAuditDelta";

    private static final Integer CLEANUP_AUDIT_BATCH_SIZE = 500;

    private static final String QUERY_MAX_RESULT = "setMaxResults";
    private static final String QUERY_FIRST_RESULT = "setFirstResult";

    private final BaseHelper baseHelper;
    private final PrismContext prismContext;

    private final SqlQueryExecutor sqlQueryExecutor;

    private final Map<String, String> customColumn = new HashMap<>();

    private volatile SystemConfigurationAuditType auditConfiguration;

    public SqlAuditServiceImpl(
            BaseHelper baseHelper,
            PrismContext prismContext) {
        this.baseHelper = baseHelper;
        this.prismContext = prismContext;
        this.sqlQueryExecutor = new SqlQueryExecutor(baseHelper, prismContext);
    }

    @Override
    public SqlRepositoryConfiguration sqlConfiguration() {
        return baseHelper.getConfiguration();
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

    @Override
    public List<AuditEventRecord> listRecords(String query, Map<String, Object> params, OperationResult parentResult) {
        final String operation = "listRecords";
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(operation, AuditEventRecord.class);
        int attempt = 1;

        OperationResult result = parentResult.createSubresult(OP_LIST_RECORDS);
        result.addParam("query", query);

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

    @Override
    public void listRecordsIterative(String query, Map<String, Object> params, AuditResultHandler handler, OperationResult parentResult) {
        // TODO operation recording ... but beware, this method is called from within listRecords
        //  (fortunately, currently it is not used from the outside, so it does not matter that it skips recording)
        final String operation = "listRecordsIterative";
        int attempt = 1;

        while (true) {
            OperationResult result = parentResult.createMinorSubresult(OP_LIST_RECORDS_ATTEMPT);
            try {
                listRecordsIterativeAttempt(query, params, handler, result);
                result.recordSuccess();
                return;
            } catch (RuntimeException ex) {
                attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, null);
                LOGGER.error("Error while trying to list audit record, {}, attempt: {}", ex.getMessage(), attempt, ex);
                result.recordFatalError("Error while trying to list audit record " + ex.getMessage() + ", attempt: " + attempt, ex);
            }
        }

    }

    // Hibernate-based
    @Override
    public void reindexEntry(AuditEventRecord record) {
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

    // Hibernate-based
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

    }

    // Hibernate-based
    private void listRecordsIterativeAttempt(String query, Map<String, Object> params,
            AuditResultHandler handler, OperationResult result) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("List records attempt\n  query: {}\n params:\n{}", query,
                    DebugUtil.debugDump(params, 2));
        }

        try (JdbcSession jdbcSession = baseHelper.newJdbcSession().startReadOnlyTransaction()) {
            try {
                Connection conn = jdbcSession.connection();
                Database database = sqlConfiguration().getDatabaseType();
                int count = 0;
                String basicQuery = query;
                if (StringUtils.isBlank(query)) {
                    basicQuery = "select * from m_audit_event "
                            + (database.equals(Database.ORACLE) ? "" : "as ")
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
                        AuditEventRecord audit = getAuditEventRecord(resultList, jdbcSession, result);
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

    @NotNull
    private AuditEventRecord getAuditEventRecord(
            ResultSet resultList, JdbcSession jdbcSession, OperationResult result)
            throws SQLException {
        AuditEventRecord audit = RAuditEventRecord.fromRepo(resultList);
        if (!customColumn.isEmpty()) {
            for (Entry<String, String> property : customColumn.entrySet()) {
                audit.getCustomColumnProperty().put(property.getKey(), resultList.getString(property.getValue()));
            }
        }

        //query for deltas
        String tableAliasPreposition =
                jdbcSession.databaseType().equals(Database.ORACLE) ? "" : "as ";
        OperationResult deltaResult = result.createMinorSubresult(OP_LOAD_AUDIT_DELTA);
        try (PreparedStatement subStmt = jdbcSession.connection().prepareStatement(
                "select * from m_audit_delta " + tableAliasPreposition
                        + "delta where delta.record_id=?")) {
            subStmt.setLong(1, resultList.getLong(RAuditEventRecord.ID_COLUMN_NAME));

            ResultSet subResultList = subStmt.executeQuery();
            while (subResultList.next()) {
                try {
                    ObjectDeltaOperation<?> odo = RObjectDeltaOperation.fromRepo(
                            subResultList, prismContext, sqlConfiguration().isUsingSQLServer());
                    audit.addDelta(odo);
                } catch (DtoTranslationException ex) {
                    LOGGER.error("Cannot convert stored audit delta. Reason: {}", ex.getMessage(), ex);
                    deltaResult.recordPartialError("Cannot convert stored audit delta. Reason: " + ex.getMessage(), ex);
                    //do not throw an error. rather audit record without delta than fatal error.
                }
            }
        } finally {
            deltaResult.computeStatus();
        }

        //query for properties
        try (PreparedStatement subStmt = jdbcSession.connection().prepareStatement(
                "select * from m_audit_prop_value " + tableAliasPreposition
                        + "prop where prop.record_id=?")) {
            subStmt.setLong(1, resultList.getLong(RAuditEventRecord.ID_COLUMN_NAME));

            ResultSet subResultList = subStmt.executeQuery();
            while (subResultList.next()) {
                audit.addPropertyValue(subResultList.getString(RAuditPropertyValue.NAME_COLUMN_NAME),
                        subResultList.getString(RAuditPropertyValue.VALUE_COLUMN_NAME));
            }
        }

        //query for references
        try (PreparedStatement subStmt = jdbcSession.connection().prepareStatement(
                "select * from m_audit_ref_value " + tableAliasPreposition
                        + "ref where ref.record_id=?")) {
            subStmt.setLong(1, resultList.getLong(RAuditEventRecord.ID_COLUMN_NAME));

            ResultSet subResultList = subStmt.executeQuery();
            while (subResultList.next()) {
                audit.addReferenceValue(subResultList.getString(RAuditReferenceValue.NAME_COLUMN_NAME),
                        RAuditReferenceValue.fromRepo(subResultList));
            }
        }

        //query for target resource oids
        try (PreparedStatement subStmt = jdbcSession.connection().prepareStatement(
                "select * from m_audit_resource " + tableAliasPreposition
                        + "res where res.record_id=?")) {
            subStmt.setLong(1, resultList.getLong(RAuditEventRecord.ID_COLUMN_NAME));
            ResultSet subResultList = subStmt.executeQuery();

            while (subResultList.next()) {
                audit.addResourceOid(subResultList.getString(RTargetResourceOid.RESOURCE_OID_COLUMN_NAME));
            }
        }

        audit.setInitiatorRef(prismRefValue(
                resultList.getString(RAuditEventRecord.INITIATOR_OID_COLUMN_NAME),
                resultList.getString(RAuditEventRecord.INITIATOR_NAME_COLUMN_NAME),
                // TODO: when JDK-8 is gone use Objects.requireNonNullElse
                defaultIfNull(
                        repoObjectType(resultList, RAuditEventRecord.INITIATOR_TYPE_COLUMN_NAME),
                        RObjectType.FOCUS)));
        audit.setAttorneyRef(prismRefValue(
                resultList.getString(RAuditEventRecord.ATTORNEY_OID_COLUMN_NAME),
                resultList.getString(RAuditEventRecord.ATTORNEY_NAME_COLUMN_NAME),
                RObjectType.FOCUS));
        audit.setTargetRef(prismRefValue(
                resultList.getString(RAuditEventRecord.TARGET_OID_COLUMN_NAME),
                resultList.getString(RAuditEventRecord.TARGET_TYPE_COLUMN_NAME),
                repoObjectType(resultList, RAuditEventRecord.TARGET_TYPE_COLUMN_NAME)));
        audit.setTargetOwnerRef(prismRefValue(
                resultList.getString(RAuditEventRecord.TARGET_OWNER_OID_COLUMN_NAME),
                resultList.getString(RAuditEventRecord.TARGET_OWNER_NAME_COLUMN_NAME),
                repoObjectType(resultList, RAuditEventRecord.TARGET_OWNER_TYPE_COLUMN_NAME)));
        return audit;
    }

    // Yes, to detect null, you have to check again after reading int. No getInteger there.
    private RObjectType repoObjectType(ResultSet resultList, String columnName) throws SQLException {
        int ordinalValue = resultList.getInt(columnName);
        return resultList.wasNull() ? null : RObjectType.fromOrdinal(ordinalValue);
    }

    private PrismReferenceValue prismRefValue(String oid, String description, RObjectType repoObjectType) {
        if (oid == null) {
            return null;
        }

        PrismReferenceValue prv = prismContext.itemFactory().createReferenceValue(oid,
                prismContext.getSchemaRegistry().determineTypeForClass(
                        repoObjectType.getJaxbClass()));
        prv.setDescription(description);
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

    // Hibernate-based
    private void auditAttempt(AuditEventRecord record) {
        Session session = baseHelper.beginTransaction();
        try {
            SingleSqlQuery query = RAuditEventRecord.toRepo(record, customColumn);
            session.doWork(connection -> {
                Database database = sqlConfiguration().getDatabaseType();
                String[] keyColumn = { RAuditEventRecord.ID_COLUMN_NAME };
                PreparedStatement smtp = query.createPreparedStatement(connection, keyColumn);
                Long id = null;
                try {
                    smtp.executeUpdate();
                    ResultSet resultSet = smtp.getGeneratedKeys();

                    if (resultSet.next()) {
                        id = resultSet.getLong(1);

                    }
                } finally {
                    smtp.close();
                }
                if (id == null) {
                    throw new IllegalArgumentException("Returned id of new record is null");
                }

                BatchSqlQuery deltaBatchQuery = new BatchSqlQuery(database);
                BatchSqlQuery itemBatchQuery = new BatchSqlQuery(database);

                for (ObjectDeltaOperation<?> delta : record.getDeltas()) {
                    if (delta == null) {
                        continue;
                    }

                    ObjectDelta<?> objectDelta = delta.getObjectDelta();
                    for (ItemDelta<?, ?> itemDelta : objectDelta.getModifications()) {
                        ItemPath path = itemDelta.getPath();
                        CanonicalItemPath canonical = prismContext.createCanonicalItemPath(path, objectDelta.getObjectTypeClass());
                        for (int i = 0; i < canonical.size(); i++) {

                            SingleSqlQuery itemQuery = RAuditItem.toRepo(id, canonical.allUpToIncluding(i).asString());
                            itemBatchQuery.addQueryForBatch(itemQuery);
                        }
                    }

                    SingleSqlQuery deltaQuery;
                    try {
                        deltaQuery = RObjectDeltaOperation.toRepo(id, delta, prismContext, auditConfiguration);
                        deltaBatchQuery.addQueryForBatch(deltaQuery);
                    } catch (DtoTranslationException e) {
                        baseHelper.handleGeneralCheckedException(e, session, null);
                    }
                }
                if (!deltaBatchQuery.isEmpty()) {
                    deltaBatchQuery.execute(connection);
                }
                if (!itemBatchQuery.isEmpty()) {
                    itemBatchQuery.execute(connection);
                }

                BatchSqlQuery propertyBatchQuery = new BatchSqlQuery(database);
                for (Entry<String, Set<String>> propertyEntry : record.getProperties().entrySet()) {
                    for (String propertyValue : propertyEntry.getValue()) {
                        SingleSqlQuery propertyQuery = RAuditPropertyValue.toRepo(
                                id, propertyEntry.getKey(), RUtil.trimString(propertyValue, AuditService.MAX_PROPERTY_SIZE));
                        propertyBatchQuery.addQueryForBatch(propertyQuery);
                    }
                }
                if (!propertyBatchQuery.isEmpty()) {
                    propertyBatchQuery.execute(connection);
                }

                BatchSqlQuery referenceBatchQuery = new BatchSqlQuery(database);
                for (Entry<String, Set<AuditReferenceValue>> referenceEntry : record.getReferences().entrySet()) {
                    for (AuditReferenceValue referenceValue : referenceEntry.getValue()) {
                        SingleSqlQuery referenceQuery = RAuditReferenceValue.toRepo(id, referenceEntry.getKey(), referenceValue);
                        referenceBatchQuery.addQueryForBatch(referenceQuery);
                    }
                }
                if (!referenceBatchQuery.isEmpty()) {
                    referenceBatchQuery.execute(connection);
                }

                BatchSqlQuery resourceOidBatchQuery = new BatchSqlQuery(database);
                for (String resourceOid : record.getResourceOids()) {
                    SingleSqlQuery resourceOidQuery = RTargetResourceOid.toRepo(id, resourceOid);
                    resourceOidBatchQuery.addQueryForBatch(resourceOidQuery);
                }
                if (!resourceOidBatchQuery.isEmpty()) {
                    resourceOidBatchQuery.execute(connection);
                }
            });

            session.getTransaction().commit();
        } catch (DtoTranslationException ex) {
            baseHelper.handleGeneralCheckedException(ex, session, null);
        } catch (RuntimeException ex) {
            baseHelper.handleGeneralRuntimeException(ex, session, null);
        } finally {
            baseHelper.cleanupSessionAndResult(session, null);
        }
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

    // Hibernate-based
    private void cleanupAuditMaxRecords(CleanupPolicyType policy, OperationResult parentResult) {
        if (policy.getMaxRecords() == null) {
            return;
        }

        final String operation = "deletingMaxRecords";

        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(OP_CLEANUP_AUDIT_MAX_RECORDS, AuditEventRecord.class);
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
        Database database = sqlConfiguration().getDatabaseType();
        try {
            TemporaryTableDialect.getTempTableDialect(database);
        } catch (SystemException e) {
            LOGGER.error("Database type {} doesn't support temporary tables, couldn't cleanup audit logs.",
                    database);
            throw new SystemException(
                    "Database type " + database + " doesn't support temporary tables, couldn't cleanup audit logs.");
        }
    }

    // deletes one batch of records (using recordsSelector to select records according to particular cleanup policy)
    private int batchDeletionAttempt(
            BiFunction<JdbcSession, String, Integer> recordsSelector,
            Holder<Integer> totalCountHolder, long batchStart, OperationResult subResult) {

        try (JdbcSession jdbcSession = baseHelper.newJdbcSession().startTransaction()) {
            try {
                TemporaryTableDialect ttDialect = TemporaryTableDialect
                        .getTempTableDialect(sqlConfiguration().getDatabaseType());

                // create temporary table
                final String tempTable =
                        ttDialect.generateTemporaryTableName(RAuditEventRecord.TABLE_NAME);
                createTemporaryTable(jdbcSession, tempTable);
                LOGGER.trace("Created temporary table '{}'.", tempTable);

                int count = recordsSelector.apply(jdbcSession, tempTable);
                LOGGER.trace("Inserted {} audit record ids ready for deleting.", count);

                // drop records from m_audit_item, m_audit_event, m_audit_delta, and others
                jdbcSession.executeStatement(
                        createDeleteQuery(RAuditItem.TABLE_NAME,
                                tempTable, RAuditItem.COLUMN_RECORD_ID));
                jdbcSession.executeStatement(
                        createDeleteQuery(RObjectDeltaOperation.TABLE_NAME,
                                tempTable, RObjectDeltaOperation.COLUMN_RECORD_ID));
                jdbcSession.executeStatement(
                        createDeleteQuery(RAuditPropertyValue.TABLE_NAME,
                                tempTable, RAuditPropertyValue.COLUMN_RECORD_ID));
                jdbcSession.executeStatement(
                        createDeleteQuery(RAuditReferenceValue.TABLE_NAME,
                                tempTable, RAuditReferenceValue.COLUMN_RECORD_ID));
                jdbcSession.executeStatement(
                        createDeleteQuery(RTargetResourceOid.TABLE_NAME,
                                tempTable, RTargetResourceOid.COLUMN_RECORD_ID));
                jdbcSession.executeStatement(
                        createDeleteQuery(RAuditEventRecord.TABLE_NAME, tempTable, "id"));

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
                jdbcSession.handleGeneralRuntimeException(ex, subResult);
                throw new AssertionError("We shouldn't get here.");
            }
        } finally {
            if (subResult != null && subResult.isUnknown()) {
                subResult.computeStatus();
            }
        }
    }

    private int selectRecordsByMaxAge(
            JdbcSession jdbcSession, String tempTable, Date minValue) {

        QAuditEventRecord aer = QAuditEventRecordMapping.INSTANCE.defaultAlias();
        SQLQuery<Long> populateQuery = jdbcSession.query()
                .select(aer.id)
                .from(aer)
                .where(aer.timestamp.lt(Instant.ofEpochMilli(minValue.getTime())))
                // we limit the query, but we don't care about order, eventually we'll get them all
                .limit(CLEANUP_AUDIT_BATCH_SIZE);

        QAuditTemp tmp = new QAuditTemp("tmp", tempTable);
        SQLInsertClause insert = jdbcSession.insert(tmp).select(populateQuery);
        System.out.println(insert);
        return (int) jdbcSession.insert(tmp).select(populateQuery).execute();
    }

    private int selectRecordsByNumberToKeep(
            JdbcSession jdbcSession, String tempTable, int recordsToKeep) {

        QAuditEventRecord aer = QAuditEventRecordMapping.INSTANCE.defaultAlias();
        long totalAuditRecords = jdbcSession.query().from(aer).fetchCount();

        // we will find the number to delete and limit it to range [0,CLEANUP_AUDIT_BATCH_SIZE]
        long recordsToDelete = Math.max(0,
                Math.min(totalAuditRecords - recordsToKeep, CLEANUP_AUDIT_BATCH_SIZE));
        LOGGER.debug("Total audit records: {}, records to keep: {} => records to delete in this batch: {}",
                totalAuditRecords, recordsToKeep, recordsToDelete);
        if (recordsToDelete == 0) {
            return 0;
        }

        SQLQuery<Long> populateQuery = jdbcSession.query()
                .select(aer.id)
                .from(aer)
                .orderBy(aer.timestamp.asc())
                .limit(recordsToDelete);

        QAuditTemp tmp = new QAuditTemp("tmp", tempTable);
        SQLInsertClause insert = jdbcSession.insert(tmp).select(populateQuery);
        System.out.println(insert);
        return (int) jdbcSession.insert(tmp).select(populateQuery).execute();
    }

    /**
     * This method creates temporary table for cleanup audit method.
     */
    // Hibernate-based
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

    private String createDeleteQuery(String objectTable, String tempTable, String idColumnName) {
        if (sqlConfiguration().isUsingMySqlCompatible()) {
            return createDeleteQueryAsJoin(objectTable, tempTable, idColumnName);
        } else if (sqlConfiguration().isUsingPostgreSQL()) {
            return createDeleteQueryAsJoinPostgreSQL(objectTable, tempTable, idColumnName);
        } else {
            // todo consider using join for other databases as well
            return createDeleteQueryAsSubquery(objectTable, tempTable, idColumnName);
        }
    }

    private String createDeleteQueryAsJoin(
            String objectTable, String tempTable, String idColumnName) {
        return "DELETE FROM main, temp USING " + objectTable + " AS main"
                + " INNER JOIN " + tempTable + " as temp"
                + " WHERE main." + idColumnName + " = temp.id";
    }

    private String createDeleteQueryAsJoinPostgreSQL(
            String objectTable, String tempTable, String idColumnName) {
        return "delete from " + objectTable + " main using " + tempTable
                + " temp where main." + idColumnName + " = temp.id";
    }

    private String createDeleteQueryAsSubquery(
            String objectTable, String tempTable, String idColumnName) {
        return "delete from " + objectTable
                + " where " + idColumnName + " in (select id from " + tempTable
                + ')';
    }

    public long countObjects(String query, Map<String, Object> params) {
        try (JdbcSession jdbcSession = baseHelper.newJdbcSession().startReadOnlyTransaction()) {
            try {
                Database database = jdbcSession.databaseType();

                String basicQuery = query;
                if (StringUtils.isBlank(query)) {
                    basicQuery = "select count(*) from m_audit_event "
                            + (database.equals(Database.ORACLE) ? "" : "as ")
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

    public Map<String, String> getCustomColumn() {
        return customColumn;
    }

    @Override
    public void applyAuditConfiguration(SystemConfigurationAuditType configuration) {
        this.auditConfiguration = CloneUtil.clone(configuration);
    }

    @Override
    public int countObjects(
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult) {

        try {
            // TODO MID-6319 do something with the OperationResult... skipped for now
            return sqlQueryExecutor.count(AuditEventRecordType.class, query, options);
        } catch (QueryException e) {
            throw new SystemException(e);
        }
    }

    @Override
    @NotNull
    public SearchResultList<AuditEventRecordType> searchObjects(
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult)
            throws SchemaException {

        // TODO MID-6319 do something with the OperationResult... skipped for now
        try {
            return sqlQueryExecutor.list(AuditEventRecordType.class, query, options);
        } catch (QueryException e) {
            throw new SystemException(e);
        }
    }
}

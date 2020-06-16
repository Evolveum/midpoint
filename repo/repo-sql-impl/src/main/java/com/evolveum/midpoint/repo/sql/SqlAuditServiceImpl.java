/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.xml.datatype.Duration;

import com.evolveum.midpoint.prism.util.CloneUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditReferenceValue;
import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.CanonicalItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.Database;
import com.evolveum.midpoint.repo.sql.data.BatchSqlQuery;
import com.evolveum.midpoint.repo.sql.data.SelectQueryBuilder;
import com.evolveum.midpoint.repo.sql.data.SingleSqlQuery;
import com.evolveum.midpoint.repo.sql.data.audit.*;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sql.perf.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.GetObjectResult;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sql.util.TemporaryTableDialect;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author lazyman
 */
public class SqlAuditServiceImpl extends SqlBaseService implements AuditService {

    private static final String OP_CLEANUP_AUDIT_MAX_AGE = "cleanupAuditMaxAge";
    private static final String OP_CLEANUP_AUDIT_MAX_RECORDS = "cleanupAuditMaxRecords";
    private static final String OP_LIST_RECORDS = "listRecords";
    private static final String OP_LIST_RECORDS_ATTEMPT = "listRecordsAttempt";
    private static final String OP_LOAD_AUDIT_DELTA = "loadAuditDelta";

    @Autowired
    private BaseHelper baseHelper;

    private static final Trace LOGGER = TraceManager.getTrace(SqlAuditServiceImpl.class);
    private static final Integer CLEANUP_AUDIT_BATCH_SIZE = 500;

    private static final String QUERY_MAX_RESULT = "setMaxResults";
    private static final String QUERY_FIRST_RESULT = "setFirstResult";

    private final Map<String, String> customColumn = new HashMap<>();

    private volatile SystemConfigurationAuditType auditConfiguration;

    public SqlAuditServiceImpl(SqlRepositoryFactory repositoryFactory) {
        super(repositoryFactory);
    }

    @Override
    public void audit(AuditEventRecord record, Task task) {
        Validate.notNull(record, "Audit event record must not be null.");
        Validate.notNull(task, "Task must not be null.");

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
                attemptResult.recordFatalError("Error while trying to list audit records, " + ex.getMessage(), ex);
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

    private void reindexEntryAttempt(AuditEventRecord record) {
        Session session = baseHelper.beginTransaction();
        try {

            RAuditEventRecord reindexed = RAuditEventRecord.toRepo(record, getPrismContext(), null, auditConfiguration);
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

    private void listRecordsIterativeAttempt(String query, Map<String, Object> params,
            AuditResultHandler handler, OperationResult result) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("List records attempt\n  query: {}\n params:\n{}", query,
                    DebugUtil.debugDump(params, 2));
        }

        Session session = baseHelper.beginReadOnlyTransaction();
        try {
            session.doWork(con -> {
                Database database = baseHelper.getConfiguration().getDatabase();
                int count = 0;
                String basicQuery = query;
                if (StringUtils.isBlank(query)) {
                    basicQuery = "select * from m_audit_event "
                            + (database.equals(Database.ORACLE) ? "" : "as ")
                            + "aer where 1=1 order by aer.timestampValue desc";
                }
                String deltaQuery = "select * from m_audit_delta "
                        + (database.equals(Database.ORACLE) ? "" : "as ")
                        + "delta where delta.record_id=?";
                String propertyQuery = "select * from m_audit_prop_value "
                        + (database.equals(Database.ORACLE) ? "" : "as ")
                        + "prop where prop.record_id=?";
                String refQuery = "select * from m_audit_ref_value "
                        + (database.equals(Database.ORACLE) ? "" : "as ")
                        + "ref where ref.record_id=?";
                String resourceQuery = "select * from m_audit_resource "
                        + (database.equals(Database.ORACLE) ? "" : "as ")
                        + "res where res.record_id=?";
                SelectQueryBuilder queryBuilder = new SelectQueryBuilder(database, basicQuery);
                setParametersToQuery(queryBuilder, params);

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("List records attempt\n  processed query: {}", queryBuilder);
                }

                try (PreparedStatement stmt = queryBuilder.build().createPreparedStatement(con)) {
                    ResultSet resultList = stmt.executeQuery();
                    while (resultList.next()) {

                        AuditEventRecord audit = RAuditEventRecord.fromRepo(resultList);
                        if (!customColumn.isEmpty()) {
                            for (Entry<String, String> property : customColumn.entrySet()) {
                                audit.getCustomColumnProperty().put(property.getKey(), resultList.getString(property.getValue()));
                            }
                        }

                        //query for deltas
                        PreparedStatement subStmt = con.prepareStatement(deltaQuery);
                        subStmt.setLong(1, resultList.getLong(RAuditEventRecord.ID_COLUMN_NAME));
                        ResultSet subResultList = subStmt.executeQuery();

                        OperationResult deltaResult = result.createMinorSubresult(OP_LOAD_AUDIT_DELTA);
                        try {
                            while (subResultList.next()) {
                                try {
                                    ObjectDeltaOperation<?> odo = RObjectDeltaOperation.fromRepo(
                                            subResultList, getPrismContext(), getConfiguration().isUsingSQLServer());
                                    audit.addDelta(odo);
                                } catch (DtoTranslationException ex) {
                                    LOGGER.error("Cannot convert stored audit delta. Reason: {}", ex.getMessage(), ex);
                                    deltaResult.recordPartialError("Cannot convert stored audit delta. Reason: " + ex.getMessage(), ex);
                                    //do not throw an error. rather audit record without delta than fatal error.
                                }
                            }
                        } finally {
                            subResultList.close();
                            subStmt.close();
                            deltaResult.computeStatus();
                        }

                        //query for properties
                        subStmt = con.prepareStatement(propertyQuery);
                        subStmt.setLong(1, resultList.getLong(RAuditEventRecord.ID_COLUMN_NAME));
                        subResultList = subStmt.executeQuery();

                        try {
                            while (subResultList.next()) {
                                audit.addPropertyValue(subResultList.getString(RAuditPropertyValue.NAME_COLUMN_NAME),
                                        subResultList.getString(RAuditPropertyValue.VALUE_COLUMN_NAME));
                            }
                        } finally {
                            subResultList.close();
                            subStmt.close();
                        }

                        //query for references
                        subStmt = con.prepareStatement(refQuery);
                        subStmt.setLong(1, resultList.getLong(RAuditEventRecord.ID_COLUMN_NAME));
                        subResultList = subStmt.executeQuery();

                        try {
                            while (subResultList.next()) {
                                audit.addReferenceValue(subResultList.getString(RAuditReferenceValue.NAME_COLUMN_NAME),
                                        RAuditReferenceValue.fromRepo(subResultList));
                            }
                        } finally {
                            subResultList.close();
                            subStmt.close();
                        }

                        //query for target resource oids
                        subStmt = con.prepareStatement(resourceQuery);
                        subStmt.setLong(1, resultList.getLong(RAuditEventRecord.ID_COLUMN_NAME));
                        subResultList = subStmt.executeQuery();

                        try {
                            while (subResultList.next()) {
                                audit.addResourceOid(subResultList.getString(RTargetResourceOid.RESOURCE_OID_COLUMN_NAME));
                            }
                        } finally {
                            subResultList.close();
                            subStmt.close();
                        }

                        try {
                            // TODO what if original name (in audit log) differs from the current one (in repo) ?
                            audit.setInitiator(resolve(session, resultList.getString(RAuditEventRecord.INITIATOR_OID_COLUMN_NAME),
                                    resultList.getString(RAuditEventRecord.INITIATOR_NAME_COLUMN_NAME),
                                    defaultIfNull(RObjectType.values()[resultList.getInt(RAuditEventRecord.INITIATOR_TYPE_COLUMN_NAME)], RObjectType.FOCUS)));
                            audit.setAttorney(resolve(session, resultList.getString(RAuditEventRecord.ATTORNEY_OID_COLUMN_NAME),
                                    resultList.getString(RAuditEventRecord.ATTORNEY_NAME_COLUMN_NAME), RObjectType.FOCUS));
                            audit.setTarget(resolve(session, resultList.getString(RAuditEventRecord.TARGET_OID_COLUMN_NAME),
                                    resultList.getString(RAuditEventRecord.TARGET_NAME_COLUMN_NAME),
                                    RObjectType.values()[resultList.getInt(RAuditEventRecord.TARGET_TYPE_COLUMN_NAME)]), getPrismContext());
                            audit.setTargetOwner(resolve(session, resultList.getString(RAuditEventRecord.TARGET_OWNER_OID_COLUMN_NAME),
                                    resultList.getString(RAuditEventRecord.TARGET_OWNER_NAME_COLUMN_NAME),
                                    RObjectType.values()[resultList.getInt(RAuditEventRecord.TARGET_OWNER_TYPE_COLUMN_NAME)]));
                        } catch (SchemaException ex) {
                            baseHelper.handleGeneralCheckedException(ex, session, null);
                        }
                        count++;
                        if (!handler.handle(audit)) {
                            LOGGER.trace("Skipping handling of objects after {} was handled. ", audit);
                            break;
                        }
                    }
                } finally {
                    result.computeStatus();
                }

//
                LOGGER.trace("List records iterative attempt processed {} records", count);
            });
            session.getTransaction().commit();

        } catch (RuntimeException ex) {
            baseHelper.handleGeneralRuntimeException(ex, session, null);
        } finally {
            baseHelper.cleanupSessionAndResult(session, null);
        }

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

    // using generic parameter to avoid typing warnings
    private <X extends ObjectType> PrismObject<X> resolve(Session session, String oid, String defaultName, RObjectType defaultType) throws SchemaException {
        if (oid == null) {
            return null;
        }
        Query<?> query = session.getNamedQuery("get.object");
        query.setParameter("oid", oid);
        query.setResultTransformer(GetObjectResult.RESULT_STYLE.getResultTransformer());
        GetObjectResult object = (GetObjectResult) query.uniqueResult();

        PrismObject result;
        if (object != null) {
            String serializedForm = RUtil.getSerializedFormFromBytes(object.getFullObject());
            result = getPrismContext().parserFor(serializedForm)
                    .compat().parse();
        } else if (defaultType != null) {
            result = getPrismContext().createObject(defaultType.getJaxbClass());
            result.asObjectable().setName(PolyStringType.fromOrig(defaultName != null ? defaultName : oid));
            result.setOid(oid);
        } else {
            result = null;
        }
        //noinspection unchecked
        return result;
    }

    private void auditAttempt(AuditEventRecord record) {
        Session session = baseHelper.beginTransaction();
        try {
            SingleSqlQuery query = RAuditEventRecord.toRepo(record, customColumn);
            session.doWork(connection -> {
                Database database = getConfiguration().getDatabase();
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
                        CanonicalItemPath canonical = getPrismContext().createCanonicalItemPath(path, objectDelta.getObjectTypeClass());
                        for (int i = 0; i < canonical.size(); i++) {

                            SingleSqlQuery itemQuery = RAuditItem.toRepo(id, canonical.allUpToIncluding(i).asString());
                            itemBatchQuery.addQueryForBatch(itemQuery);
                        }
                    }

                    SingleSqlQuery deltaQuery;
                    try {
                        deltaQuery = RObjectDeltaOperation.toRepo(id, delta, getPrismContext(), auditConfiguration);
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
//                                val.setTransient(isTransient);
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
//                                 val.setTransient(isTransient);
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
        Validate.notNull(policy, "Cleanup policy must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        // TODO review monitoring performance of these cleanup operations
        //  It looks like the attempts (and wasted time) are not counted correctly
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

        // factored out because it produces INFO-level message
        Dialect dialect = Dialect.getDialect(baseHelper.getSessionFactoryBean().getHibernateProperties());
        checkTemporaryTablesSupport(dialect);

        long start = System.currentTimeMillis();
        boolean first = true;
        Holder<Integer> totalCountHolder = new Holder<>(0);
        try {
            while (true) {
                try {
                    LOGGER.info("{} audit cleanup, deleting up to {} (duration '{}'), batch size {}{}.",
                            first ? "Starting" : "Continuing with ", minValue, duration, CLEANUP_AUDIT_BATCH_SIZE,
                            first ? "" : ", up to now deleted " + totalCountHolder.getValue() + " entries");
                    first = false;
                    int count;
                    do {
                        // the following method may restart due to concurrency
                        // (or any other) problem - in any iteration
                        long batchStart = System.currentTimeMillis();
                        LOGGER.debug(
                                "Starting audit cleanup batch, deleting up to {} (duration '{}'), batch size {}, up to now deleted {} entries.",
                                minValue, duration, CLEANUP_AUDIT_BATCH_SIZE, totalCountHolder.getValue());
                        count = batchDeletionAttempt((session, tempTable) -> selectRecordsByMaxAge(session, tempTable, minValue, dialect),
                                totalCountHolder, batchStart, dialect, parentResult);
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
        long opHandle = pm.registerOperationStart(OP_CLEANUP_AUDIT_MAX_RECORDS, AuditEventRecord.class);
        int attempt = 1;

        Integer recordsToKeep = policy.getMaxRecords();

        // factored out because it produces INFO-level message
        Dialect dialect = Dialect.getDialect(baseHelper.getSessionFactoryBean().getHibernateProperties());
        checkTemporaryTablesSupport(dialect);

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
                                "Starting audit cleanup batch, keeping at most {} records, batch size {}, up to now deleted {} entries.",
                                recordsToKeep, CLEANUP_AUDIT_BATCH_SIZE, totalCountHolder.getValue());
                        count = batchDeletionAttempt((session, tempTable) -> selectRecordsByNumberToKeep(session, tempTable, recordsToKeep, dialect),
                                totalCountHolder, batchStart, dialect, parentResult);
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

    private void checkTemporaryTablesSupport(Dialect dialect) {
        TemporaryTableDialect ttDialect = TemporaryTableDialect.getTempTableDialect(dialect);

        if (!ttDialect.supportsTemporaryTables()) {
            LOGGER.error("Dialect {} doesn't support temporary tables, couldn't cleanup audit logs.",
                    dialect);
            throw new SystemException(
                    "Dialect " + dialect + " doesn't support temporary tables, couldn't cleanup audit logs.");
        }
    }

    // deletes one batch of records (using recordsSelector to select records according to particular cleanup policy)
    private int batchDeletionAttempt(BiFunction<Session, String, Integer> recordsSelector, Holder<Integer> totalCountHolder,
            long batchStart, Dialect dialect, OperationResult subResult) {

        Session session = baseHelper.beginTransaction();
        try {
            TemporaryTableDialect ttDialect = TemporaryTableDialect.getTempTableDialect(dialect);

            // create temporary table
            final String tempTable = ttDialect.generateTemporaryTableName(RAuditEventRecord.TABLE_NAME);
            createTemporaryTable(session, dialect, tempTable);
            LOGGER.trace("Created temporary table '{}'.", tempTable);

            int count = recordsSelector.apply(session, tempTable);
            LOGGER.trace("Inserted {} audit record ids ready for deleting.", count);

            // drop records from m_audit_item, m_audit_event, m_audit_delta, and others
            session.createNativeQuery(createDeleteQuery(RAuditItem.TABLE_NAME, tempTable,
                    RAuditItem.COLUMN_RECORD_ID)).executeUpdate();
            session.createNativeQuery(createDeleteQuery(RObjectDeltaOperation.TABLE_NAME, tempTable,
                    RObjectDeltaOperation.COLUMN_RECORD_ID)).executeUpdate();
            session.createNativeQuery(createDeleteQuery(RAuditPropertyValue.TABLE_NAME, tempTable,
                    RAuditPropertyValue.COLUMN_RECORD_ID)).executeUpdate();
            session.createNativeQuery(createDeleteQuery(RAuditReferenceValue.TABLE_NAME, tempTable,
                    RAuditReferenceValue.COLUMN_RECORD_ID)).executeUpdate();
            session.createNativeQuery(createDeleteQuery(RTargetResourceOid.TABLE_NAME, tempTable,
                    RTargetResourceOid.COLUMN_RECORD_ID)).executeUpdate();
            session.createNativeQuery(createDeleteQuery(RAuditEventRecord.TABLE_NAME, tempTable, "id"))
                    .executeUpdate();

            // drop temporary table
            if (ttDialect.dropTemporaryTableAfterUse()) {
                LOGGER.debug("Dropping temporary table.");

                String sb = ttDialect.getDropTemporaryTableString()
                        + ' ' + tempTable;
                session.createNativeQuery(sb).executeUpdate();
            }

            session.getTransaction().commit();
            int totalCount = totalCountHolder.getValue() + count;
            totalCountHolder.setValue(totalCount);
            LOGGER.debug("Audit cleanup batch finishing successfully in {} milliseconds; total count = {}",
                    System.currentTimeMillis() - batchStart, totalCount);
            return count;
        } catch (RuntimeException ex) {
            LOGGER.debug("Audit cleanup batch finishing with exception in {} milliseconds; exception = {}",
                    System.currentTimeMillis() - batchStart, ex.getMessage());
            baseHelper.handleGeneralRuntimeException(ex, session, subResult);
            throw new AssertionError("We shouldn't get here.");
        } finally {
            baseHelper.cleanupSessionAndResult(session, subResult);
        }
    }

    private int selectRecordsByMaxAge(Session session, String tempTable, Date minValue, Dialect dialect) {

        // fill temporary table, we don't need to join task on object on
        // container, oid and id is already in task table
        String selectString = "select a.id as id from " + RAuditEventRecord.TABLE_NAME + " a"
                + " where a." + RAuditEventRecord.COLUMN_TIMESTAMP + " < ###TIME###";

        // batch size
        RowSelection rowSelection = new RowSelection();
        rowSelection.setMaxRows(CLEANUP_AUDIT_BATCH_SIZE);
        LimitHandler limitHandler = dialect.getLimitHandler();
        selectString = limitHandler.processSql(selectString, rowSelection);

        // replace ? -> batch size, $ -> ?
        // Sorry for that .... I just don't know how to write this query in HQL,
        // nor I'm not sure if limiting max size in
        // compound insert into ... select ... query via query.setMaxSize()
        // would work - TODO write more nicely if anybody knows how)
        selectString = selectString.replace("?", String.valueOf(CLEANUP_AUDIT_BATCH_SIZE));
        selectString = selectString.replace("###TIME###", "?");

        String queryString = "insert into " + tempTable + " " + selectString;
        LOGGER.trace("Query string = {}", queryString);
        NativeQuery query = session.createNativeQuery(queryString);
        query.setParameter(1, new Timestamp(minValue.getTime()));

        return query.executeUpdate();
    }

    private int selectRecordsByNumberToKeep(Session session, String tempTable, Integer recordsToKeep, Dialect dialect) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery cq = cb.createQuery(RAuditEventRecord.class);
        cq.select(cb.count(cq.from(RAuditEventRecord.class)));
        Number totalAuditRecords = (Number) session.createQuery(cq).uniqueResult();
        int recordsToDelete = totalAuditRecords.intValue() - recordsToKeep;
        if (recordsToDelete <= 0) {
            recordsToDelete = 0;
        } else if (recordsToDelete > CLEANUP_AUDIT_BATCH_SIZE) {
            recordsToDelete = CLEANUP_AUDIT_BATCH_SIZE;
        }
        LOGGER.debug("Total audit records: {}, records to keep: {} => records to delete in this batch: {}",
                totalAuditRecords, recordsToKeep, recordsToDelete);
        if (recordsToDelete == 0) {
            return 0;
        }

        String selectString = "select a.id as id from " + RAuditEventRecord.TABLE_NAME + " a"
                + " order by a." + RAuditEventRecord.COLUMN_TIMESTAMP + " asc";

        // batch size
        RowSelection rowSelection = new RowSelection();
        rowSelection.setMaxRows(recordsToDelete);
        LimitHandler limitHandler = dialect.getLimitHandler();
        selectString = limitHandler.processSql(selectString, rowSelection);
        selectString = selectString.replace("?", String.valueOf(recordsToDelete));

        String queryString = "insert into " + tempTable + " " + selectString;
        LOGGER.trace("Query string = {}", queryString);
        NativeQuery query = session.createNativeQuery(queryString);
        return query.executeUpdate();
    }

    /**
     * This method creates temporary table for cleanup audit method.
     */
    private void createTemporaryTable(Session session, final Dialect dialect, final String tempTable) {
        session.doWork(connection -> {
            // check if table exists
            if (!getConfiguration().isUsingPostgreSQL()) {
                try {
                    Statement s = connection.createStatement();
                    s.execute("select id from " + tempTable + " where id = 1");

                    s.close();
                    // table already exists
                    return;
                } catch (Exception ex) {
                    // we expect this on the first time
                }
            }

            TemporaryTableDialect ttDialect = TemporaryTableDialect.getTempTableDialect(dialect);

            Statement s = connection.createStatement();
            s.execute(ttDialect.getCreateTemporaryTableString()
                    + ' ' + tempTable + " (id "
                    + dialect.getTypeName(Types.BIGINT)
                    + " not null)"
                    + ttDialect.getCreateTemporaryTablePostfix());
            s.close();
        });
    }

    private String createDeleteQuery(String objectTable, String tempTable, String idColumnName) {
        if (getConfiguration().isUsingMySqlCompatible()) {
            return createDeleteQueryAsJoin(objectTable, tempTable, idColumnName);
        } else if (getConfiguration().isUsingPostgreSQL()) {
            return createDeleteQueryAsJoinPostgreSQL(objectTable, tempTable, idColumnName);
        } else {
            // todo consider using join for other databases as well
            return createDeleteQueryAsSubquery(objectTable, tempTable, idColumnName);
        }
    }

    private String createDeleteQueryAsJoin(String objectTable, String tempTable, String idColumnName) {
        return "DELETE FROM main, temp USING " + objectTable + " AS main INNER JOIN " + tempTable + " as temp "
                + "WHERE main." + idColumnName + " = temp.id";
    }

    private String createDeleteQueryAsJoinPostgreSQL(String objectTable, String tempTable, String idColumnName) {
        return "delete from " + objectTable + " main using " + tempTable + " temp where main." + idColumnName + " = temp.id";
    }

    private String createDeleteQueryAsSubquery(String objectTable, String tempTable, String idColumnName) {
        return "delete from " + objectTable
                + " where " + idColumnName + " in (select id from " + tempTable
                + ')';
    }

    public long countObjects(String query, Map<String, Object> params) {
        long[] count = { 0 };
        Session session = baseHelper.beginTransaction();
        try {
            session.setFlushMode(FlushMode.MANUAL);
            session.doWork(connection -> {
                Database database = getConfiguration().getDatabase();

                String basicQuery = query;
                if (StringUtils.isBlank(query)) {
                    basicQuery = "select count(*) from m_audit_event "
                            + (database.equals(Database.ORACLE) ? "" : "as ")
                            + "aer where 1 = 1";
                }
                SelectQueryBuilder queryBuilder = new SelectQueryBuilder(database, basicQuery);
                setParametersToQuery(queryBuilder, params);

                LOGGER.trace("List records attempt\n  processed query: {}", queryBuilder);

                try (PreparedStatement stmt = queryBuilder.build().createPreparedStatement(connection)) {
                    ResultSet resultList = stmt.executeQuery();
                    if (!resultList.next()) {
                        throw new IllegalArgumentException("Result set don't have value for select: " + query);
                    }
                    if (resultList.getMetaData().getColumnCount() > 1) {
                        throw new IllegalArgumentException("Result have more as one value for select: " + query);
                    }
                    count[0] = resultList.getLong(1);
                }
            });
        } catch (RuntimeException ex) {
            baseHelper.handleGeneralRuntimeException(ex, session, null);
        } finally {
            baseHelper.cleanupSessionAndResult(session, null);
        }
        return count[0];
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
}

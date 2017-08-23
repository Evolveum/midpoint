/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.parser.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventRecord;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventStage;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventType;
import com.evolveum.midpoint.repo.sql.data.audit.RObjectDeltaOperation;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.GetObjectResult;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;

import org.apache.commons.lang.Validate;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.jdbc.Work;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.hibernate.FlushMode;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lazyman
 */
public class SqlAuditServiceImpl extends SqlBaseService implements AuditService {

	@Autowired
	private BaseHelper baseHelper;

    private static final Trace LOGGER = TraceManager.getTrace(SqlAuditServiceImpl.class);
    private static final Integer CLEANUP_AUDIT_BATCH_SIZE = 500;

    public SqlAuditServiceImpl(SqlRepositoryFactory repositoryFactory) {
        super(repositoryFactory);
    }

    @Override
    public void audit(AuditEventRecord record, Task task) {
        Validate.notNull(record, "Audit event record must not be null.");
        Validate.notNull(task, "Task must not be null.");

        final String operation = "audit";
        int attempt = 1;

        while (true) {
            try {
                auditAttempt(record);
                return;
            } catch (RuntimeException ex) {
                attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, null);
            }
        }
    }

    @Override
    public List<AuditEventRecord> listRecords(String query, Map<String, Object> params) {
        final String operation = "listRecords";
        int attempt = 1;

        while (true) {
            try {
                return listRecordsAttempt(query, params);
            } catch (RuntimeException ex) {
                attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, null);
            }
        }
    }

    private List<AuditEventRecord> listRecordsAttempt(String query, Map<String, Object> params) {
        Session session = null;
        List<AuditEventRecord> auditRecords = null;
        try {
            session = baseHelper.beginTransaction();
            session.setFlushMode(FlushMode.MANUAL);
            Query q = session.createQuery(query);
            Set<Entry<String, Object>> paramSet = params.entrySet();
            for (Entry<String, Object> p : paramSet) {
                if (p.getValue() == null) {
                    q.setParameter(p.getKey(), null);
                    continue;
                }
                if (XMLGregorianCalendar.class.isAssignableFrom(p.getValue().getClass())) {
                    q.setParameter(p.getKey(), MiscUtil.asDate((XMLGregorianCalendar) p.getValue()));
                } else if (p.getValue() instanceof AuditEventType) {
                    q.setParameter(p.getKey(), RAuditEventType.toRepo((AuditEventType) p.getValue()));
                } else if (p.getValue() instanceof AuditEventStage) {
                    q.setParameter(p.getKey(), RAuditEventStage.toRepo((AuditEventStage) p.getValue()));
                } else {
                    q.setParameter(p.getKey(), p.getValue());
                }
            }

//            q.setResultTransformer(Transformers.aliasToBean(RAuditEventRecord.class));
            List resultList = q.list();

            auditRecords = new ArrayList<>();

            for (Object o : resultList) {
                if (!(o instanceof RAuditEventRecord)) {
                    throw new DtoTranslationException("Unexpected object in result set. Expected audit record, but got " + o.getClass().getSimpleName());
                }
                RAuditEventRecord raudit = (RAuditEventRecord) o;

                AuditEventRecord audit = RAuditEventRecord.fromRepo(raudit, getPrismContext());

                audit.setInitiator(resolve(session, (raudit.getInitiatorOid())));
                audit.setTarget(resolve(session, (raudit.getTargetOid())));
                audit.setTargetOwner(resolve(session, raudit.getTargetOwnerOid()));

                auditRecords.add(audit);
            }

            session.getTransaction().commit();

        } catch (DtoTranslationException | SchemaException ex) {
			baseHelper.handleGeneralCheckedException(ex, session, null);
        } catch (RuntimeException ex) {
			baseHelper.handleGeneralRuntimeException(ex, session, null);
        } finally {
			baseHelper.cleanupSessionAndResult(session, null);
        }
        return auditRecords;

    }

    private PrismObject resolve(Session session, String oid) throws SchemaException {
        Query query = session.getNamedQuery("get.object");
        query.setParameter("oid", oid);
        query.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);
        GetObjectResult object = null;
        object = (GetObjectResult) query.uniqueResult();

        PrismObject result = null;
        if (object != null) {
            String xml = RUtil.getXmlFromByteArray(object.getFullObject(), getConfiguration().isUseZip());
            result = getPrismContext().parseObject(xml, XNodeProcessorEvaluationMode.COMPAT);
        }

        return result;
    }

    private void auditAttempt(AuditEventRecord record) {
        Session session = null;
        try {
            session = baseHelper.beginTransaction();

            RAuditEventRecord newRecord = RAuditEventRecord.toRepo(record, getPrismContext());
            session.save(newRecord);

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

        final String operation = "deleting";
        int attempt = 1;

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("cleanupAudit");

        if (policy.getMaxAge() == null) {
            return;
        }

        Duration duration = policy.getMaxAge();
        if (duration.getSign() > 0) {
            duration = duration.negate();
        }
        Date minValue = new Date();
        duration.addTo(minValue);

        // factored out because it produces INFO-level message
        Dialect dialect = Dialect.getDialect(baseHelper.getSessionFactoryBean().getHibernateProperties());
        if (!dialect.supportsTemporaryTables()) {
            LOGGER.error("Dialect {} doesn't support temporary tables, couldn't cleanup audit logs.", dialect);
            throw new SystemException("Dialect " + dialect + " doesn't support temporary tables, couldn't cleanup audit logs.");
        }

        long start = System.currentTimeMillis();
        boolean first = true;
        Holder<Integer> totalCountHolder = new Holder<>(0);
        try {
            while (true) {
                try {
                    LOGGER.info("{} audit cleanup, deleting up to {} (duration '{}'), batch size {}{}.",
                            first ? "Starting" : "Restarting",
                            minValue, duration, CLEANUP_AUDIT_BATCH_SIZE,
                            first ? "" : ", up to now deleted " + totalCountHolder.getValue() + " entries");
                    first = false;
                    int count;
                    do {
                        // the following method may restart due to concurrency (or any other) problem - in any iteration
                        count = cleanupAuditAttempt(minValue, duration, totalCountHolder, dialect, parentResult);
                    } while (count > 0);
                    return;
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, parentResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
            LOGGER.info("Audit cleanup finished; deleted {} entries in {} seconds.",
                    totalCountHolder.getValue(), (System.currentTimeMillis() - start)/1000L);
        }
    }

    private int cleanupAuditAttempt(Date minValue, Duration duration, Holder<Integer> totalCountHolder, Dialect dialect, OperationResult subResult) {

        long start = System.currentTimeMillis();
        LOGGER.debug("Starting audit cleanup batch, deleting up to {} (duration '{}'), batch size {}, up to now deleted {} entries.",
                minValue, duration, CLEANUP_AUDIT_BATCH_SIZE, totalCountHolder.getValue());

        Session session = null;
        try {
            session = baseHelper.beginTransaction();

            int count = cleanupAuditAttempt(minValue, session, dialect);

            session.getTransaction().commit();
            int totalCount = totalCountHolder.getValue() + count;
            totalCountHolder.setValue(totalCount);
            LOGGER.debug("Audit cleanup batch finishing successfully in {} milliseconds; total count = {}", System.currentTimeMillis()-start, totalCount);
            return count;
        } catch (RuntimeException ex) {
            LOGGER.debug("Audit cleanup batch finishing with exception in {} milliseconds; exception = {}", System.currentTimeMillis()-start, ex.getMessage());
			baseHelper.handleGeneralRuntimeException(ex, session, subResult);
            throw new AssertionError("We shouldn't get here.");         // just because of the need to return a value
        } finally {
			baseHelper.cleanupSessionAndResult(session, subResult);
        }
    }

    protected int cleanupAuditAttempt(Date minValue, Session session, Dialect dialect) {
        //create temporary table
        final String tempTable = dialect.generateTemporaryTableName(RAuditEventRecord.TABLE_NAME);
        createTemporaryTable(session, dialect, tempTable);
        LOGGER.trace("Created temporary table '{}'.", new Object[]{tempTable});

        //fill temporary table, we don't need to join task on object on container, oid and id is already in task table
        StringBuilder selectSB = new StringBuilder();
        selectSB.append("select a.id as id from ").append(RAuditEventRecord.TABLE_NAME).append(" a");
        selectSB.append(" where a.").append(RAuditEventRecord.COLUMN_TIMESTAMP).append(" < ###TIME###");
        String selectString = selectSB.toString();

        // batch size
        RowSelection rowSelection = new RowSelection();
        rowSelection.setMaxRows(CLEANUP_AUDIT_BATCH_SIZE);
        LimitHandler limitHandler = dialect.buildLimitHandler(selectString, rowSelection);
        selectString = limitHandler.getProcessedSql();

        // replace ? -> batch size, $ -> ?
        // Sorry for that .... I just don't know how to write this query in HQL, nor I'm not sure if limiting max size in
        // compound insert into ... select ... query via query.setMaxSize() would work - TODO write more nicely if anybody knows how)
        selectString = selectString.replace("?", String.valueOf(CLEANUP_AUDIT_BATCH_SIZE));
        selectString = selectString.replace("###TIME###", "?");

        String queryString = "insert into " + tempTable + " " + selectString;
        LOGGER.trace("Query string = {}", queryString);
        SQLQuery query = session.createSQLQuery(queryString);
        query.setParameter(0, new Timestamp(minValue.getTime()));

        int insertCount = query.executeUpdate();
        LOGGER.trace("Inserted {} audit record ids ready for deleting.", new Object[]{insertCount});

        //drop records from m_audit_event, m_audit_delta
        session.createSQLQuery(createDeleteQuery(RObjectDeltaOperation.TABLE_NAME, tempTable,
                RObjectDeltaOperation.COLUMN_RECORD_ID)).executeUpdate();
        session.createSQLQuery(createDeleteQuery(RAuditEventRecord.TABLE_NAME, tempTable, "id")).executeUpdate();

        //drop temporary table
        if (dialect.dropTemporaryTableAfterUse()) {
            LOGGER.debug("Dropping temporary table.");
            StringBuilder sb = new StringBuilder();
            sb.append(dialect.getDropTemporaryTableString());
            sb.append(' ').append(tempTable);

            session.createSQLQuery(sb.toString()).executeUpdate();
        }

        return insertCount;
    }

    /**
     * This method creates temporary table for cleanup audit method.
     *
     * @param session
     * @param dialect
     * @param tempTable
     */
    private void createTemporaryTable(Session session, final Dialect dialect, final String tempTable) {
        session.doWork(new Work() {

            @Override
            public void execute(Connection connection) throws SQLException {
                //check if table exists
                try (Statement s = connection.createStatement()) {
                    s.execute("select id from " + tempTable + " where id = 1");
                    //table already exists
                    return;
                } catch (Exception ex) {
                    //we expect this on the first time
                }

                StringBuilder sb = new StringBuilder();
                sb.append(dialect.getCreateTemporaryTableString());
                sb.append(' ').append(tempTable).append(" (id ");
                sb.append(dialect.getTypeName(Types.BIGINT));
                sb.append(" not null)");
                sb.append(dialect.getCreateTemporaryTablePostfix());

                try (Statement s = connection.createStatement()) {
                    s.execute(sb.toString());
                }
            }
        });
    }

    private String createDeleteQuery(String objectTable, String tempTable, String idColumnName) {
        StringBuilder sb = new StringBuilder();
        sb.append("delete from ").append(objectTable);
        sb.append(" where ").append(idColumnName).append(" in (select id from ").append(tempTable).append(')');

        return sb.toString();
    }
}

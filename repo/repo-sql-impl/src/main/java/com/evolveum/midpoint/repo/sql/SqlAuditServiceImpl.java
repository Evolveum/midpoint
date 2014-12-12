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
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventRecord;
import com.evolveum.midpoint.repo.sql.data.audit.RObjectDeltaOperation;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.GetObjectResult;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.commons.lang.Validate;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.jdbc.Work;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.RootEntityResultTransformer;
import org.hibernate.transform.Transformers;

import javax.xml.datatype.Duration;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
public class SqlAuditServiceImpl extends SqlBaseService implements AuditService {

    private static final Trace LOGGER = TraceManager.getTrace(SqlAuditServiceImpl.class);

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
                attempt = logOperationAttempt(null, operation, attempt, ex, null);
            }
        }
    }
    
    @Override
    public List<AuditEventRecord> listRecords(String query, Map<String, Object> params){
    	final String operation = "listRecords";
        int attempt = 1;

        while (true) {
            try {
                return listRecordsAttempt(query, params);
            } catch (RuntimeException ex) {
                attempt = logOperationAttempt(null, operation, attempt, ex, null);
            }
        }	   
    }
    
    private List<AuditEventRecord> listRecordsAttempt(String query, Map<String, Object> params){
    	Session session = null;
    	List<AuditEventRecord> auditRecords = null;
        try {
            session = beginTransaction();
            Query q = session.createQuery(query);
            for (String paramName : params.keySet()){
            	q.setParameter(paramName, params.get(paramName));
            }
//            q.setResultTransformer(Transformers.aliasToBean(RAuditEventRecord.class));
            List resultList = q.list();
            
            auditRecords = new ArrayList<>();
            
            for (Object o : resultList){
            	if (!(o instanceof RAuditEventRecord)){
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
           
        } catch (DtoTranslationException | SchemaException ex ) {
            handleGeneralCheckedException(ex, session, null);
        } catch (RuntimeException ex) {
            handleGeneralRuntimeException(ex, session, null);
        } finally {
            cleanupSessionAndResult(session, null);
        }
        return auditRecords;
     
    }
    
    private PrismObject resolve(Session session, String oid) throws SchemaException{
    	Query query = session.getNamedQuery("get.object");
    	query.setParameter("oid", oid);
    	query.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);
    	GetObjectResult object = (GetObjectResult) query.uniqueResult();
    	
    	PrismObject result = null;
    	if (object != null){
    		String xml = RUtil.getXmlFromByteArray(object.getFullObject(), getConfiguration().isUseZip());
    		 result = getPrismContext().parseObject(xml);
    	}
    	
    	return result;
    }

    private void auditAttempt(AuditEventRecord record) {
        Session session = null;
        try {
            session = beginTransaction();

            RAuditEventRecord newRecord = RAuditEventRecord.toRepo(record, getPrismContext());
            session.save(newRecord);

            session.getTransaction().commit();
        } catch (DtoTranslationException ex) {
            handleGeneralCheckedException(ex, session, null);
        } catch (RuntimeException ex) {
            handleGeneralRuntimeException(ex, session, null);
        } finally {
            cleanupSessionAndResult(session, null);
        }
    }

    @Override
    public void cleanupAudit(CleanupPolicyType policy, OperationResult parentResult) {
        Validate.notNull(policy, "Cleanup policy must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        final String operation = "deleting";
        int attempt = 1;

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("deleteObject");

        try {
            while (true) {
                try {
                    cleanupAuditAttempt(policy, parentResult);
                    return;
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(null, operation, attempt, ex, parentResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private void cleanupAuditAttempt(CleanupPolicyType policy, OperationResult subResult) {
        if (policy.getMaxAge() == null) {
            return;
        }

        Duration duration = policy.getMaxAge();
        if (duration.getSign() > 0) {
            duration = duration.negate();
        }
        Date minValue = new Date();
        duration.addTo(minValue);
        LOGGER.info("Starting audit cleanup, deleting up to {} (duration '{}').", new Object[]{minValue, duration});

        Session session = null;
        try {
            session = beginTransaction();

            int count = cleanupAuditAttempt(minValue, session);
            LOGGER.info("Cleanup in performed, {} records deleted up to {} (duration '{}').",
                    new Object[]{count, minValue, duration});

            session.getTransaction().commit();
        } catch (RuntimeException ex) {
            handleGeneralRuntimeException(ex, session, subResult);
        } finally {
            cleanupSessionAndResult(session, subResult);
        }
    }

    protected int cleanupAuditAttempt(Date minValue, Session session) {
        final Dialect dialect = Dialect.getDialect(getSessionFactoryBean().getHibernateProperties());
        if (!dialect.supportsTemporaryTables()) {
            LOGGER.error("Dialect {} doesn't support temporary tables, couldn't cleanup audit logs.",
                    new Object[]{dialect});
            throw new SystemException("Dialect " + dialect
                    + " doesn't support temporary tables, couldn't cleanup audit logs.");
        }

        //create temporary table
        final String tempTable = dialect.generateTemporaryTableName(RAuditEventRecord.TABLE_NAME);
        createTemporaryTable(session, dialect, tempTable);
        LOGGER.trace("Created temporary table '{}'.", new Object[]{tempTable});

        //fill temporary table, we don't need to join task on object on container, oid and id is already in task table
        StringBuilder sb = new StringBuilder();
        sb.append("insert into ").append(tempTable).append(' ');
        sb.append("select a.id as id from ").append(RAuditEventRecord.TABLE_NAME).append(" a");
        sb.append(" where a.").append(RAuditEventRecord.COLUMN_TIMESTAMP).append(" < ?");

        SQLQuery query = session.createSQLQuery(sb.toString());
        query.setParameter(0, new Timestamp(minValue.getTime()));
        int insertCount = query.executeUpdate();
        LOGGER.trace("Inserted {} audit record ids ready for deleting.", new Object[]{insertCount});

        //drop records from m_task, m_object, m_container
        session.createSQLQuery(createDeleteQuery(RObjectDeltaOperation.TABLE_NAME, tempTable,
                RObjectDeltaOperation.COLUMN_RECORD_ID)).executeUpdate();
        session.createSQLQuery(createDeleteQuery(RAuditEventRecord.TABLE_NAME, tempTable, "id")).executeUpdate();

        //drop temporary table
        if (dialect.dropTemporaryTableAfterUse()) {
            LOGGER.debug("Dropping temporary table.");
            sb = new StringBuilder();
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
                try {
                    Statement s = connection.createStatement();
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

                Statement s = connection.createStatement();
                s.execute(sb.toString());
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

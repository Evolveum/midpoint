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
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventRecord;
import com.evolveum.midpoint.repo.sql.data.audit.RObjectDeltaOperation;
import com.evolveum.midpoint.repo.sql.data.common.RTask;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CleanupPolicyType;
import org.apache.commons.lang.Validate;
import org.hibernate.PessimisticLockException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.HibernateOptimisticLockingFailureException;

import java.sql.Timestamp;
import java.util.Date;

/**
 * @author lazyman
 */
public class SqlAuditServiceImpl extends SqlBaseService implements AuditService {

    private static final Trace LOGGER = TraceManager.getTrace(SqlAuditServiceImpl.class);
    @Autowired(required = true)
    private SessionFactory sessionFactory;

    public SqlAuditServiceImpl(SqlRepositoryFactory repositoryFactory) {
        super(repositoryFactory);
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
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
        cleanup(RAuditEventRecord.class, policy, parentResult);
    }

    @Override
    protected int cleanupAttempt(Class entity, Date minValue, Session session) {
        if (!RAuditEventRecord.class.equals(entity)) {
            return 0;
        }

        //todo improve delete through temporary table [lazyman]
        Query query = session.createQuery("delete from " + RObjectDeltaOperation.class.getSimpleName()
                + " as o where o.recordId in (select a.id from " + RAuditEventRecord.class.getSimpleName()
                + " as a where a.timestamp < :timestamp)");

        query.setParameter("timestamp", new Timestamp(minValue.getTime()));
        query.executeUpdate();

        query = session.createQuery("delete from " + RAuditEventRecord.class.getSimpleName()
                + " as a where a.timestamp < :timestamp");
        query.setParameter("timestamp", new Timestamp(minValue.getTime()));

        return query.executeUpdate();
    }
}

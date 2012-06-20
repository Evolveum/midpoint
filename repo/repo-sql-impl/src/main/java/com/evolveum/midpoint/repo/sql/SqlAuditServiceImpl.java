/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventRecord;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;
import org.hibernate.PessimisticLockException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.HibernateOptimisticLockingFailureException;

/**
 * @author lazyman
 */
public class SqlAuditServiceImpl extends SqlBaseService implements AuditService {

    private static final Trace LOGGER = TraceManager.getTrace(SqlAuditServiceImpl.class);
    @Autowired(required = true)
    private SessionFactory sessionFactory;
    @Autowired
    private LightweightIdentifierGenerator lightweightIdentifierGenerator;

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
        assertCorrectness(record, task);

        completeRecord(record, task);

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
        } catch (PessimisticLockException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (LockAcquisitionException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (HibernateOptimisticLockingFailureException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (Exception ex) {
            handleGeneralException(ex, session, null);
        } finally {
            cleanupSessionAndResult(session, null);
        }
    }

    private void assertCorrectness(AuditEventRecord record, Task task) {
        if (task == null) {
            LOGGER.warn("Task is null in a call to audit service");
        } else {
            if (task.getOwner() == null) {
                LOGGER.warn("Task '{}' has no owner in a call to audit service", new Object[]{task.getName()});
            }
        }
    }

    /**
     * Complete the record with data that can be computed or discovered from the environment
     */
    private void completeRecord(AuditEventRecord record, Task task) {
        LightweightIdentifier id = null;
        if (record.getEventIdentifier() == null) {
            id = lightweightIdentifierGenerator.generate();
            record.setEventIdentifier(id.toString());
        }
        if (record.getTimestamp() == null) {
            if (id == null) {
                record.setTimestamp(System.currentTimeMillis());
            } else {
                // To be consistent with the ID
                record.setTimestamp(id.getTimestamp());
            }
        }
        if (record.getTaskIdentifier() == null && task != null) {
            record.setTaskIdentifier(task.getTaskIdentifier());
        }
        if (record.getTaskOID() == null && task != null) {
            record.setTaskOID(task.getOid());
        }
        if (record.getTaskOID() == null && task != null) {
            record.setTaskOID(task.getOid());
        }
        if (record.getSessionIdentifier() == null && task != null) {
            // TODO
        }
        if (record.getInitiator() == null && task != null) {
            record.setInitiator(task.getOwner());
        }

        if (record.getHostIdentifier() == null) {
            // TODO
        }
    }
}

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
import com.evolveum.midpoint.repo.sql.data.common.RTask;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CleanupPolicyType;
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
}

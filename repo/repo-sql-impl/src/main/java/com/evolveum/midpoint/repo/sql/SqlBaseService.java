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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.PessimisticLockException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.HibernateOptimisticLockingFailureException;

import java.lang.reflect.Field;

/**
 * @author lazyman
 */
public class SqlBaseService {

    private static final Trace LOGGER = TraceManager.getTrace(SqlBaseService.class);
    // how many times we want to repeat operation after lock aquisition,
    // pessimistic, optimistic exception
    private static final int LOCKING_MAX_ATTEMPTS = 10;
    // time in ms to wait before next operation attempt. it seems that 0
    // works best here (i.e. it is best to repeat operation immediately)
    private static final long LOCKING_TIMEOUT = 0;
    @Autowired(required = true)
    private PrismContext prismContext;
    @Autowired(required = true)
    private SessionFactory sessionFactory;

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        // !!! HACK !!! https://forum.hibernate.org/viewtopic.php?t=978915&highlight=
        // problem with composite keys and object merging
        fixCompositeIdentifierInMetaModel(RAnyContainer.class);

        fixCompositeIdentifierInMetaModel(RObjectReference.class);

        fixCompositeIdentifierInMetaModel(RAssignment.class);
        fixCompositeIdentifierInMetaModel(RExclusion.class);
        for (RContainerType type : ClassMapper.getKnownTypes()) {
            fixCompositeIdentifierInMetaModel(type.getClazz());
        }
        // END HACK

        this.sessionFactory = sessionFactory;
    }

    private void fixCompositeIdentifierInMetaModel(Class clazz) {
        ClassMetadata classMetadata = sessionFactory.getClassMetadata(clazz);
        if (classMetadata instanceof AbstractEntityPersister) {
            AbstractEntityPersister persister = (AbstractEntityPersister) classMetadata;
            EntityMetamodel model = persister.getEntityMetamodel();
            IdentifierProperty identifier = model.getIdentifierProperty();

            try {
                Field field = IdentifierProperty.class.getDeclaredField("hasIdentifierMapper");
                field.setAccessible(true);
                field.set(identifier, true);
                field.setAccessible(false);
            } catch (Exception ex) {
                throw new SystemException("Attempt to fix entity meta model with hack failed, reason: "
                        + ex.getMessage(), ex);
            }
        }
    }

    protected int logOperationAttempt(String oid, String operation, int attempt, RuntimeException ex,
            OperationResult result) {
        if (!(ex instanceof PessimisticLockException) && !(ex instanceof LockAcquisitionException)
                && !(ex instanceof HibernateOptimisticLockingFailureException)) {
            //it's not locking exception (optimistic, pesimistic lock or simple lock acquisition)

            if (ex instanceof GenericJDBCException) {
                //fix for table timeout lock in H2, 50200 is LOCK_TIMEOUT_1 error code
                GenericJDBCException jdbcEx = (GenericJDBCException) ex;
                if (jdbcEx.getErrorCode() != 50200) {
                    throw new SystemException(ex);
                }
            } else {
                throw ex;
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("A locking-related problem occurred when {} object with oid '{}', retrying after "
                    + "{}ms (this was attempt {} of {})\n{}: {}", new Object[]{operation, oid, LOCKING_TIMEOUT,
                    attempt, LOCKING_MAX_ATTEMPTS, ex.getClass().getSimpleName(), ex.getMessage()});
        }

        if (attempt >= LOCKING_MAX_ATTEMPTS) {
            if (ex != null && result != null) {
                result.recordFatalError("A locking-related problem occurred.", ex);
            }
            throw new SystemException(ex.getMessage(), ex);
        }

        if (LOCKING_TIMEOUT > 0) {
            try {
                Thread.sleep(LOCKING_TIMEOUT);
            } catch (InterruptedException ex1) {
                // ignore this
            }
        }
        return ++attempt;
    }

    protected Session beginTransaction() {
        Session session = getSessionFactory().openSession();
        // we're forcing transaction isolation throught MidPointConnectionCustomizer
        // session.doWork(new Work() {
        //
        //      @Override
        //      public void execute(Connection connection) throws SQLException {
        //          connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        //      }
        // });
        session.beginTransaction();

        return session;
    }

    protected void rollbackTransaction(Session session) {
        rollbackTransaction(session, null, null);
    }

    protected void rollbackTransaction(Session session, Exception ex, OperationResult result) {
    	 if (ex != null && result != null) {
             result.recordFatalError(ex.getMessage(), ex);
         }
    	 
    	if (session == null || session.getTransaction() == null || !session.getTransaction().isActive()) {
            return;
        }

        session.getTransaction().rollback();

       
    }

    protected void cleanupSessionAndResult(Session session, OperationResult result) {
        if (session != null && session.isOpen()) {
            session.close();
        }

        if (result != null && result.isUnknown()) {
            result.computeStatus();
        }
    }

    protected void handleGeneralException(Exception ex, Session session, OperationResult result) {
        rollbackTransaction(session, ex, result);
        if (ex instanceof GenericJDBCException) {
            //fix for table timeout lock in H2, this exception will be wrapped as system exception
            //in SqlRepositoryServiceImpl#logOperationAttempt if necessary
            throw (GenericJDBCException) ex;
        }
        if (ex instanceof SystemException) {
            throw (SystemException) ex;
        }
        throw new SystemException(ex.getMessage(), ex);
    }
}

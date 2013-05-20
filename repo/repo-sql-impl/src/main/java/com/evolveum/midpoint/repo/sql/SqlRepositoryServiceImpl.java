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

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.query.QueryDefinitionRegistry;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.QueryInterpreter;
import com.evolveum.midpoint.repo.sql.query.definition.Definition;
import com.evolveum.midpoint.repo.sql.query.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.util.*;
import com.evolveum.midpoint.schema.LabeledString;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.*;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.Dialect;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jdbc.Work;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * @author lazyman
 */
@Repository
public class SqlRepositoryServiceImpl extends SqlBaseService implements RepositoryService {

    private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryServiceImpl.class);
    private static final int MAX_CONSTRAINT_NAME_LENGTH = 40;
    private static final String IMPLEMENTATION_SHORT_NAME = "SQL";
    private static final String IMPLEMENTATION_DESCRIPTION = "Implementation that stores data in generic relational" +
            " (SQL) databases. It is using ORM (hibernate) on top of JDBC to access the database.";
    private static final String DETAILS_TRANSACTION_ISOLATION = "transactionIsolation";
    private static final String DETAILS_CLIENT_INFO = "clientInfo.";
    private static final String DETAILS_DATA_SOURCE = "dataSource";
    private static final String DETAILS_HIBERNATE_DIALECT = "hibernateDialect";
    private static final String DETAILS_HIBERNATE_HBM_2_DDL = "hibernateHbm2ddl";

    public SqlRepositoryServiceImpl(SqlRepositoryFactory repositoryFactory) {
        super(repositoryFactory);
    }

    private <T extends ObjectType> PrismObject<T> getObject(Session session, Class<T> type, String oid, boolean lockForUpdate)
            throws ObjectNotFoundException, SchemaException, DtoTranslationException {

        boolean lockedForUpdateViaHibernate = false;
        boolean lockedForUpdateViaSql = false;

        LockOptions lockOptions = new LockOptions();
        if (lockForUpdate) {
            if (getConfiguration().isLockForUpdateViaHibernate()) {
                lockOptions.setLockMode(LockMode.PESSIMISTIC_WRITE);
                lockedForUpdateViaHibernate = true;
            } else if (getConfiguration().isLockForUpdateViaSql()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Trying to lock object " + oid + " for update (via SQL)");
                }
                long time = System.currentTimeMillis();
                SQLQuery q = session.createSQLQuery("select id from m_container where id = 0 and oid = ? for update");
                q.setString(0, oid);
                Object result = q.uniqueResult();
                if (result == null) {
                    return throwObjectNotFoundException(type, oid);
                }
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Locked via SQL (in " + (System.currentTimeMillis() - time) + " ms)");
                }
                lockedForUpdateViaSql = true;
            }
        }

        if (LOGGER.isTraceEnabled()) {
            if (lockedForUpdateViaHibernate) {
                LOGGER.trace("Getting object " + oid + " with locking for update (via hibernate)");
            } else if (lockedForUpdateViaSql) {
                LOGGER.trace("Getting object " + oid + ", already locked for update (via SQL)");
            } else {
                LOGGER.trace("Getting object " + oid + " without locking for update");
            }
        }

        RObject object = (RObject) session.get(ClassMapper.getHQLTypeClass(type), new RContainerId(oid), lockOptions);
        LOGGER.trace("Got it.");
        if (object == null) {
            throwObjectNotFoundException(type, oid);
        }

        LOGGER.trace("Transforming data to JAXB type.");
        PrismObject<T> objectType = object.toJAXB(getPrismContext()).asPrismObject();
        validateObjectType(objectType, type);

        return objectType;
    }

    private <T extends ObjectType> PrismObject<T> throwObjectNotFoundException(Class<T> type, String oid) throws ObjectNotFoundException {
        throw new ObjectNotFoundException("Object of type '" + type.getSimpleName() + "' with oid '" + oid
                + "' was not found.", null, oid);
    }

    @Override
    public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        final String operation = "getting";
        int attempt = 1;

        OperationResult subResult = result.createMinorSubresult(GET_OBJECT);
        subResult.addParam("type", type.getName());
        subResult.addParam("oid", oid);

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("getObject");

        try {
            while (true) {
                try {
                    return getObjectAttempt(type, oid, subResult);
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(oid, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private <T extends ObjectType> PrismObject<T> getObjectAttempt(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        LOGGER.trace("Getting object '{}' with oid '{}'.", new Object[]{type.getSimpleName(), oid});

        PrismObject<T> objectType = null;

        Session session = null;
        try {
            session = beginReadOnlyTransaction();

            objectType = getObject(session, type, oid, false);

            session.getTransaction().commit();
        } catch (ObjectNotFoundException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (SchemaException ex) {
            rollbackTransaction(session, ex, "Schema error while getting object with oid: "
                    + oid + ". Reason: " + ex.getMessage(), result, true);
            throw ex;
        } catch (DtoTranslationException ex) {
            handleGeneralCheckedException(ex, session, result);
        } catch (RuntimeException ex) {
            handleGeneralRuntimeException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Get object:\n{}", new Object[]{(objectType != null ? objectType.debugDump(3) : null)});
        }

        return objectType;
    }

    private Session beginReadOnlyTransaction() {
        return beginTransaction(getConfiguration().isUseReadOnlyTransactions());
    }

    @Override
    public PrismObject<UserType> listAccountShadowOwner(String accountOid, OperationResult result)
            throws ObjectNotFoundException {
        Validate.notEmpty(accountOid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        final String operation = "listing account shadow owner";
        int attempt = 1;

        OperationResult subResult = result.createSubresult(LIST_ACCOUNT_SHADOW);
        subResult.addParam("accountOid", accountOid);

        while (true) {
            try {
                return listAccountShadowOwnerAttempt(accountOid, subResult);
            } catch (RuntimeException ex) {
                attempt = logOperationAttempt(accountOid, operation, attempt, ex, subResult);
            }
        }
    }

    private PrismObject<UserType> listAccountShadowOwnerAttempt(String accountOid, OperationResult result)
            throws ObjectNotFoundException {
        UserType userType = null;
        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            LOGGER.trace("Selecting account shadow owner for account {}.", new Object[]{accountOid});
            Query query = session.createQuery("select user from " + ClassMapper.getHQLType(UserType.class)
                    + " as user left join user.linkRef as ref where ref.targetOid = :oid");
            query.setString("oid", accountOid);

            List<RUser> users = query.list();
            LOGGER.trace("Found {} users, transforming data to JAXB types.",
                    new Object[]{(users != null ? users.size() : 0)});

            if (users == null || users.isEmpty()) {
                // account shadow owner was not found
                return null;
            }

            if (users.size() > 1) {
                LOGGER.warn("Found {} users for account oid {}, returning first user. [interface change needed]",
                        new Object[]{users.size(), accountOid});
            }

            RUser user = users.get(0);
            userType = user.toJAXB(getPrismContext());

            session.getTransaction().commit();
        } catch (DtoTranslationException ex) {
            handleGeneralCheckedException(ex, session, result);
        } catch (RuntimeException ex) {
            handleGeneralRuntimeException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return userType.asPrismObject();
    }

    private void validateName(PrismObject object) throws SchemaException {
        PrismProperty name = object.findProperty(ObjectType.F_NAME);
        if (name == null || ((PolyString) name.getRealValue()).isEmpty()) {
            throw new SchemaException("Attempt to add object without name.");
        }
    }

    @Override
    public <T extends ObjectType> String addObject(PrismObject<T> object, RepoAddOptions options, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException {
        Validate.notNull(object, "Object must not be null.");
        validateName(object);
        Validate.notNull(result, "Operation result must not be null.");

        if (InternalsConfig.encryptionChecks) {
            CryptoUtil.checkEncrypted(object);
        }

        if (options == null) {
            options = new RepoAddOptions();
        }

        OperationResult subResult = result.createSubresult(ADD_OBJECT);
        subResult.addParam("object", object);
        subResult.addParam("options", options);

        final String operation = "adding";
        int attempt = 1;

        String oid = object.getOid();
        while (true) {
            try {
                return addObjectAttempt(object, options, subResult);
            } catch (RuntimeException ex) {
                attempt = logOperationAttempt(oid, operation, attempt, ex, subResult);
            }
        }
    }

    private <T extends ObjectType> String addObjectAttempt(PrismObject<T> object, RepoAddOptions options,
                                                           OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException {
        LOGGER.trace("Adding object type '{}'", new Object[]{object.getCompileTimeClass().getSimpleName()});

        String oid = null;
        Session session = null;
        // it is needed to keep the original oid for example for import options. if we do not keep it
        // and it was null it can bring some error because the oid is set when the object contains orgRef
        // or it is org. and by the import we do not know it so it will be trying to delete non-existing object
        String originalOid = object.getOid();
        try {
            ObjectType objectType = object.asObjectable();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Object\n{}", new Object[]{getPrismContext().silentMarshalObject(objectType, LOGGER)});
            }

            LOGGER.trace("Translating JAXB to data type.");
            RObject rObject = createDataObjectFromJAXB(objectType);

            session = beginTransaction();
            if (options.isOverwrite()) {
                oid = overwriteAddObjectAttempt(object, objectType, rObject, originalOid, session);
            } else {
                oid = nonOverwriteAddObjectAttempt(object, objectType, rObject, originalOid, session);
            }
            session.getTransaction().commit();

            LOGGER.trace("Saved object '{}' with oid '{}'", new Object[]{
                    object.getCompileTimeClass().getSimpleName(), oid});

            object.setOid(oid);
        } catch (ObjectAlreadyExistsException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (ConstraintViolationException ex) {
            rollbackTransaction(session, ex, result, true);

            LOGGER.debug("Constraint violation occurred (will be rethrown as ObjectAlreadyExistsException).", ex);
            // we don't know if it's only name uniqueness violation, or something else,
            // therefore we're throwing it always as ObjectAlreadyExistsException revert
            // to the original oid and prevent of unexpected behaviour (e.g. by import with overwrite option)
            if (StringUtils.isEmpty(originalOid)) {
                object.setOid(null);
            }
            String constraintName = ex.getConstraintName();
            // Breaker to avoid long unreadable messages
            if (constraintName != null && constraintName.length() > MAX_CONSTRAINT_NAME_LENGTH) {
                constraintName = null;
            }
            throw new ObjectAlreadyExistsException("Conflicting object already exists"
                    + (constraintName == null ? "" : " (violated constraint '" + constraintName + "')"), ex);
        } catch (SchemaException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (DtoTranslationException ex) {
            handleGeneralCheckedException(ex, session, result);
        } catch (RuntimeException ex) {
            handleGeneralRuntimeException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return oid;
    }

    private <T extends ObjectType> String overwriteAddObjectAttempt(PrismObject<T> object, ObjectType objectType,
                                                                    RObject rObject, String originalOid,
                                                                    Session session)
            throws ObjectAlreadyExistsException, SchemaException, DtoTranslationException {

        //check if object already exists, find differences and increment version if necessary
        Collection<? extends ItemDelta> modifications = null;
        if (originalOid != null) {
            try {
                PrismObject<T> oldObject = getObject(session, object.getCompileTimeClass(), originalOid, true);
                ObjectDelta<T> delta = object.diff(oldObject);
                modifications = delta.getModifications();

                //we found existing object which will be overwritten, therefore we increment version
                Long version = RUtil.getLongFromString(oldObject.getVersion());
                version = (version == null) ? 0L : ++version;

                rObject.setVersion(version);
            } catch (ObjectNotFoundException ex) {
                //it's ok that object was not found, therefore we won't be overwriting it
            }
        }

        RObject merged = (RObject) session.merge(rObject);

        //update org. unit hierarchy based on modifications
        if (modifications == null || modifications.isEmpty()) {
            //we're not overwriting object - we fill new hierarchy
            if (objectType instanceof OrgType || !objectType.getParentOrgRef().isEmpty()) {
                long time = System.currentTimeMillis();
                LOGGER.trace("Org. structure closure table update started.");
                objectType.setOid(merged.getOid());
                fillHierarchy(merged, session, true);
                LOGGER.trace("Org. structure closure table update finished ({} ms).",
                        new Object[]{(System.currentTimeMillis() - time)});
            }
        } else {
            //we have to recompute actual hierarchy because we've changed object
            recomputeHierarchy(merged, session, modifications);
        }

        return merged.getOid();
    }

    private <T extends ObjectType> String nonOverwriteAddObjectAttempt(PrismObject<T> object, ObjectType objectType,
                                                                       RObject rObject, String originalOid,
                                                                       Session session)
            throws ObjectAlreadyExistsException, SchemaException {

        // check name uniqueness (by type)
        if (StringUtils.isNotEmpty(originalOid)) {
            LOGGER.trace("Checking oid uniqueness.");
            Criteria criteria = session.createCriteria(ClassMapper.getHQLTypeClass(object.getCompileTimeClass()));
            criteria.add(Restrictions.eq("oid", object.getOid()));
            criteria.setProjection(Projections.rowCount());

            Long count = (Long) criteria.uniqueResult();
            if (count != null && count > 0) {
                throw new ObjectAlreadyExistsException("Object '" + object.getCompileTimeClass().getSimpleName()
                        + "' with oid '" + object.getOid() + "' already exists.");
            }
        }

        LOGGER.trace("Saving object.");
        RContainerId containerId = (RContainerId) session.save(rObject);
        String oid = containerId.getOid();

        if (objectType instanceof OrgType || !objectType.getParentOrgRef().isEmpty()) {
            long time = System.currentTimeMillis();
            LOGGER.trace("Org. structure closure table update started.");
            objectType.setOid(oid);
            fillHierarchy(rObject, session, true);
            LOGGER.trace("Org. structure closure table update finished ({} ms).",
                    new Object[]{(System.currentTimeMillis() - time)});
        }
        
        /*
        if (rObject. instanceof ROrg || !objectType.getParentOrgRef().isEmpty()) {
            long time = System.currentTimeMillis();
            LOGGER.trace("Org. structure closure table update started.");
            objectType.setOid(oid);
            this.fillHierarchyExt(rObject, session);
            LOGGER.trace("Org. structure closure table update finished ({} ms).",
                    new Object[]{(System.currentTimeMillis() - time)});
        }
        */

        return oid;
    }


    private <T extends ObjectType> void fillHierarchy(RObject rOrg, Session session, boolean withIncorrect) throws SchemaException {

        ROrgClosure closure = new ROrgClosure(rOrg, rOrg, 0);
        session.save(closure);

        for (RObjectReference orgRef : rOrg.getParentOrgRef()) {
            fillTransitiveHierarchy(rOrg, orgRef.getTargetOid(), session, withIncorrect);
        }

        if (withIncorrect) {
            Query qIncorrect = session
                    .createQuery("from ROrgIncorrect as o where o.ancestorOid = :oid");
            qIncorrect.setString("oid", rOrg.getOid());

            List<ROrgIncorrect> orgIncorrect = qIncorrect.list();

            for (ROrgIncorrect orgInc : orgIncorrect) {
                Query qOrg = session
                        .createQuery("from ROrg where id = 0 and oid = :oid");
                qOrg.setString("oid", orgInc.getDescendantOid());
                ROrg rOrgI = (ROrg) qOrg.uniqueResult();
                fillTransitiveHierarchy(rOrgI, rOrg.getOid(), session, !withIncorrect);
                session.delete(orgInc);
            }
        }
    }


    private <T extends ObjectType> void fillTransitiveHierarchy(
            RObject descendant, String ancestorOid, Session session,
            boolean withIncorrect) throws SchemaException {

        Criteria cOrgClosure = session.createCriteria(ROrgClosure.class)
                .createCriteria("descendant", "desc")
                .setFetchMode("descendant", FetchMode.JOIN)
                .add(Restrictions.eq("oid", ancestorOid));

        List<ROrgClosure> orgClosure = cOrgClosure.list();

        if (orgClosure.size() > 0) {
            for (ROrgClosure o : orgClosure) {
                LOGGER.trace(
                        "adding {}\t{}\t{}",
                        new Object[]{o.getAncestor().getOid(),
                                descendant.getOid(), o.getDepth() + 1});

                // if not exist pair with same depth, then create else nothing
                // do
                Query qExistClosure = session
                        .createQuery("select count(*) from ROrgClosure as o where "
                                + "o.ancestorId = :ancestorId and o.ancestorOid = :ancestorOid "
                                + "and o.descendantId = :descendantId and o.descendantOid = :descendantOid "
                                + "and o.depth = :depth");
                qExistClosure.setParameter("ancestorId", 0L);
                qExistClosure.setParameter("ancestorOid", o.getAncestor().getOid());
                qExistClosure.setParameter("descendantId", 0L);
                qExistClosure.setParameter("descendantOid", descendant.getOid());
                qExistClosure.setParameter("depth", o.getDepth() + 1);

                boolean existClosure = (Long) qExistClosure.uniqueResult() == 0;

                if (existClosure)
                    session.save(new ROrgClosure(o.getAncestor(), descendant, o
                            .getDepth() + 1));
            }
        } else if (withIncorrect) {
            LOGGER.trace("adding incorrect {}\t{}", new Object[]{ancestorOid,
                    descendant.getOid()});
            session.save(new ROrgIncorrect(ancestorOid, descendant.getOid(),
                    descendant.getId()));
        }
    }

    @Override
    public <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        final String operation = "deleting";
        int attempt = 1;

        OperationResult subResult = result.createSubresult(DELETE_OBJECT);
        subResult.addParam("type", type.getName());
        subResult.addParam("oid", oid);

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("deleteObject");

        try {
            while (true) {
                try {
                    deleteObjectAttempt(type, oid, subResult);
                    return;
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(oid, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private <T extends ObjectType> void deleteObjectAttempt(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException {
        LOGGER.trace("Deleting object type '{}' with oid '{}'", new Object[]{type.getSimpleName(), oid});

        Session session = null;
        try {
            session = beginTransaction();

            Criteria query = session.createCriteria(ClassMapper.getHQLTypeClass(type));
            query.add(Restrictions.eq("oid", oid));
            query.add(Restrictions.eq("id", 0L));
            RObject object = (RObject) query.uniqueResult();
            if (object == null) {
                throw new ObjectNotFoundException("Object of type '" + type.getSimpleName() + "' with oid '" + oid
                        + "' was not found.", null, oid);
            }

            deleteReferences(object, session);

            List<RObject> objectsToRecompute = null;
            if (type.isAssignableFrom(OrgType.class)) {
                objectsToRecompute = deleteTransitiveHierarchy(object, session);
            }

            session.delete(object);

            if (objectsToRecompute != null) {
                recompute(objectsToRecompute, session);
            }

            session.getTransaction().commit();
        } catch (ObjectNotFoundException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (SchemaException ex) {
            handleGeneralCheckedException(ex, session, result);
        } catch (DtoTranslationException ex) {
            handleGeneralCheckedException(ex, session, result);
        } catch (RuntimeException ex) {
            handleGeneralRuntimeException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }
    }


    private void recompute(List<RObject> objectsToRecompute, Session session)
            throws SchemaException, DtoTranslationException {

        LOGGER.trace("Recomputing organization structure closure table after delete.");

        for (RObject object : objectsToRecompute) {
            Criteria query = session.createCriteria(ClassMapper
                    .getHQLTypeClass(object.toJAXB(getPrismContext())
                            .getClass()));
            query.add(Restrictions.eq("oid", object.getOid()));
            query.add(Restrictions.eq("id", 0L));
            RObject obj = (RObject) query.uniqueResult();
            if (obj == null) {
                // object not found..probably it was just deleted.
                continue;
            }
            deleteAncestors(object, session);
            fillHierarchy(object, session, false);
        }
        LOGGER.trace("Closure table for organization structure recomputed.");
    }


    private void deleteAncestors(RObject object, Session session) {
        Criteria criteria = session.createCriteria(ROrgClosure.class);
        criteria.add(Restrictions.eq("descendant", object));
        List<ROrgClosure> objectsToDelete = criteria.list();

        for (ROrgClosure objectToDelete : objectsToDelete) {
            session.delete(objectToDelete);
        }

    }

    private void deleteReferences(RObject object, Session session) {
        Query sqlDelete = session.createQuery("delete from RObjectReference where targetOid = :deleteOid");
        sqlDelete.setParameter("deleteOid", object.getOid());
        sqlDelete.executeUpdate();

        LOGGER.trace("deleting reference: oid:{}", new Object[]{object.getOid()});
    }


    @Override
    public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, OperationResult result) {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.trace("Counting objects of type '{}', query (on trace level).", new Object[]{type});
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Full query\n{}", new Object[]{(query == null ? "undefined" : query.dump())});
        }

        OperationResult subResult = result.createMinorSubresult(COUNT_OBJECTS);
        subResult.addParam("type", type.getName());
        subResult.addParam("query", query);

        final String operation = "counting";
        int attempt = 1;

        while (true) {
            try {
                return countObjectsAttempt(type, query, subResult);
            } catch (RuntimeException ex) {
                attempt = logOperationAttempt(null, operation, attempt, ex, subResult);
            }
        }
    }

    private <T extends ObjectType> int countObjectsAttempt(Class<T> type, ObjectQuery query, OperationResult result) {
        int count = 0;

        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            LOGGER.trace("Updating query criteria.");
            Criteria criteria;
            if (query != null && query.getFilter() != null) {
                QueryInterpreter interpreter = new QueryInterpreter();
                criteria = interpreter.interpret(query, type, getPrismContext(), session);
            } else {
                criteria = session.createCriteria(ClassMapper.getHQLTypeClass(type));
            }
            criteria.setProjection(Projections.rowCount());

            LOGGER.trace("Selecting total count.");
            Long longCount = (Long) criteria.uniqueResult();
            count = longCount.intValue();
        } catch (QueryException ex) {
            handleGeneralCheckedException(ex, session, result);
        } catch (RuntimeException ex) {
            handleGeneralRuntimeException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return count;
    }

    @Override
    public <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
                                                                     OperationResult result) throws SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        logSearchInputParameters(type, query, false);

        OperationResult subResult = result.createSubresult(SEARCH_OBJECTS);
        subResult.addParam("type", type.getName());
        subResult.addParam("query", query);
        // subResult.addParam("paging", paging);

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("searchObjects");

        final String operation = "searching";
        int attempt = 1;
        try {
            while (true) {
                try {
                    return searchObjectsAttempt(type, query, subResult);
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(null, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private <T extends ObjectType> void logSearchInputParameters(Class<T> type, ObjectQuery query, boolean iterative) {
        if (!LOGGER.isTraceEnabled()) {
            return;
        }

        ObjectPaging paging = query != null ? query.getPaging() : null;

        LOGGER.trace("Searching objects of type '{}', query (on trace level), offset {}, count {}, iterative {}.",
                new Object[]{type.getSimpleName(), (paging != null ? paging.getOffset() : "undefined"),
                        (paging != null ? paging.getMaxSize() : "undefined"), iterative});
        LOGGER.trace("Full query\n{}\nFull paging\n{}", new Object[]{
                (query == null ? "undefined" : query.dump()),
                (paging != null ? paging.dump() : "undefined")});
        if (iterative) {
            LOGGER.trace("Iterative search by paging: {}, batch size {}", getConfiguration().isIterativeSearchByPaging(), getConfiguration().getIterativeSearchByPagingBatchSize());
        }
    }

    private <T extends ObjectType> List<PrismObject<T>> searchObjectsAttempt(Class<T> type, ObjectQuery query,
                                                                             OperationResult result) throws SchemaException {

        List<PrismObject<T>> list = new ArrayList<PrismObject<T>>();
        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            Criteria criteria = createCriteria(type, query, session);

            List<RObject> objects = criteria.list();
            LOGGER.trace("Found {} objects, translating to JAXB.",
                    new Object[]{(objects != null ? objects.size() : 0)});

            for (RObject object : objects) {
                ObjectType objectType = object.toJAXB(getPrismContext());
                PrismObject<T> prismObject = objectType.asPrismObject();
                validateObjectType(prismObject, type);
                list.add(prismObject);
            }

            session.getTransaction().commit();
        } catch (DtoTranslationException ex) {
            handleGeneralCheckedException(ex, session, result);
        } catch (QueryException ex) {
            handleGeneralCheckedException(ex, session, result);
        } catch (RuntimeException ex) {
            handleGeneralRuntimeException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return list;
    }

    @Override
    public <T extends ObjectType> void modifyObject(Class<T> type, String oid,
                                                    Collection<? extends ItemDelta> modifications,
                                                    OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        Validate.notNull(modifications, "Modifications must not be null.");
        Validate.notNull(type, "Object class in delta must not be null.");
        Validate.notEmpty(oid, "Oid must not null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        if (InternalsConfig.encryptionChecks) {
            CryptoUtil.checkEncrypted(modifications);
        }

        OperationResult subResult = result.createSubresult(MODIFY_OBJECT);
        subResult.addParam("type", type.getName());
        subResult.addParam("oid", oid);
        subResult.addParam("modifications", modifications);

        if (modifications.isEmpty()) {
            LOGGER.debug("Modification list is empty, nothing was modified.");
            subResult.recordStatus(OperationResultStatus.SUCCESS, "Modification list is empty, nothing was modified.");
            return;
        }

        final String operation = "modifying";
        int attempt = 1;

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("modifyObject");

        try {
            while (true) {
                try {
                    modifyObjectAttempt(type, oid, modifications, subResult);
                    return;
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(oid, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }

    }

    private <T extends ObjectType> void modifyObjectAttempt(Class<T> type, String oid,
                                                            Collection<? extends ItemDelta> modifications,
                                                            OperationResult result) throws ObjectNotFoundException,
            SchemaException, ObjectAlreadyExistsException {
        LOGGER.trace("Modifying object '{}' with oid '{}'.", new Object[]{type.getSimpleName(), oid});
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Modifications: {}", new Object[]{PrettyPrinter.prettyPrint(modifications)});
        }

        Session session = null;
        try {
            session = beginTransaction();

            // get user
            PrismObject<T> prismObject = getObject(session, type, oid, true);
            // apply diff
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT before:\n{}", new Object[]{prismObject.dump()});
            }
            PropertyDelta.applyTo(modifications, prismObject);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT after:\n{}", prismObject.dump());
            }
            // merge and update user
            LOGGER.trace("Translating JAXB to data type.");
            RObject rObject = createDataObjectFromJAXB(prismObject.asObjectable());
            rObject.setVersion(rObject.getVersion() + 1);

            session.merge(rObject);

            recomputeHierarchy(rObject, session, modifications);

            LOGGER.trace("Before commit...");
            session.getTransaction().commit();
            LOGGER.trace("Committed!");
        } catch (ObjectNotFoundException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (ConstraintViolationException ex) {
            rollbackTransaction(session, ex, result, true);

            LOGGER.debug("Constraint violation occurred (will be rethrown as ObjectAlreadyExistsException).", ex);
            // we don't know if it's only name uniqueness violation, or something else,
            // therefore we're throwing it always as ObjectAlreadyExistsException

            //todo improve (we support only 5 DB, so we should probably do some hacking in here)
            throw new ObjectAlreadyExistsException(ex);
        } catch (SchemaException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (DtoTranslationException ex) {
            handleGeneralCheckedException(ex, session, result);
        } catch (RuntimeException ex) {
            handleGeneralRuntimeException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
            LOGGER.trace("Session cleaned up.");
        }

    }


    private <T extends ObjectType> void recomputeHierarchy(
            RObject rObjectToModify, Session session,
            Collection<? extends ItemDelta> modifications)
            throws SchemaException, DtoTranslationException {

        for (ItemDelta delta : modifications) {
            if (!delta.getName().equals(OrgType.F_PARENT_ORG_REF)) continue;

            // if modification is one of the modify or delete, delete old
            // record in org closure table and in the next step fill the
            // closure table with the new records
            if (delta.isReplace() || delta.isDelete()) {
                for (Object orgRefDValue : delta.getValuesToDelete()) {
                    if (!(orgRefDValue instanceof PrismReferenceValue))
                        throw new SchemaException(
                                "Couldn't modify organization structure hierarchy (adding new records). Expected instance of prism reference value but got "
                                        + orgRefDValue);


                    if (rObjectToModify.getClass().isAssignableFrom(ROrg.class)) {
                        List<RObject> objectsToRecompute = deleteTransitiveHierarchy(rObjectToModify, session);
                        refillHierarchy(rObjectToModify, objectsToRecompute, session);
                    } else {
                        deleteHierarchy(rObjectToModify, session);
                        if (rObjectToModify.getParentOrgRef() != null
                                && !rObjectToModify.getParentOrgRef().isEmpty()) {
                            for (RObjectReference orgRef : rObjectToModify.getParentOrgRef()) {
                                fillTransitiveHierarchy(rObjectToModify, orgRef.getTargetOid(), session, true);
                            }
                        }
                    }
                }
            } else {
                // fill closure table with new transitive relations
                for (Object orgRefDValue : delta.getValuesToAdd()) {
                    if (!(orgRefDValue instanceof PrismReferenceValue)) {
                        throw new SchemaException(
                                "Couldn't modify organization structure hierarchy (adding new records). Expected instance of prism reference value but got "
                                        + orgRefDValue);
                    }

                    PrismReferenceValue value = (PrismReferenceValue) orgRefDValue;

                    LOGGER.trace(
                            "filling transitive hierarchy for descendant {}, ref {}",
                            new Object[]{rObjectToModify.getOid(),
                                    value.getOid()});
                    // todo remove
                    fillTransitiveHierarchy(rObjectToModify, value.getOid(), session, true);
                }
            }
        }
    }


    private List<RObject> deleteTransitiveHierarchy(RObject rObjectToModify,
                                                    Session session) throws SchemaException, DtoTranslationException {

        Criteria cDescendant = session.createCriteria(ROrgClosure.class)
                .setProjection(Projections.property("descendant"))
                .add(Restrictions.eq("ancestor", rObjectToModify));

        Criteria cAncestor = session.createCriteria(ROrgClosure.class)
                .setProjection(Projections.property("ancestor"))
                .createCriteria("ancestor", "anc")
                .add(Restrictions.and(Restrictions.eq("this.descendant",
                        rObjectToModify), Restrictions.not(Restrictions.eq(
                        "anc.oid", rObjectToModify.getOid()))));

        Criteria cOrgClosure = session.createCriteria(ROrgClosure.class);

        List<RObject> ocAncestor = cAncestor.list();
        List<RObject> ocDescendant = cDescendant.list();

        if (ocAncestor != null && !ocAncestor.isEmpty()) {
            cOrgClosure.add(Restrictions.in("ancestor", ocAncestor));
        } else {
            LOGGER.trace("No ancestors for object: {}",
                    rObjectToModify.getOid());
        }

        if (ocDescendant != null && !ocDescendant.isEmpty()) {
            cOrgClosure.add(Restrictions.in("descendant", ocDescendant));
        } else {
            LOGGER.trace("No descendants for object: {}",
                    rObjectToModify.getOid());
        }

        List<ROrgClosure> orgClosure = cOrgClosure.list();

        for (ROrgClosure o : orgClosure) {
            LOGGER.trace(
                    "1deleting from hierarchy: A: {} D:{} depth:{}",
                    new Object[]{o.getAncestor().toJAXB(getPrismContext()),
                            o.getDescendant().toJAXB(getPrismContext()),
                            o.getDepth()});
            session.delete(o);
        }
        deleteHierarchy(rObjectToModify, session);
        return ocDescendant;
    }


    private void refillHierarchy(RObject parent, List<RObject> descendants,
                                 Session session) throws SchemaException, DtoTranslationException {
        fillHierarchy(parent, session, false);

        for (RObject descendant : descendants) {
            LOGGER.trace("ObjectToRecompute {}", descendant);
            if (!parent.getOid().equals(descendant.getOid())) {
                fillTransitiveHierarchy(descendant, parent.getOid(),
                        session, false);
            }
        }

    }

    private void deleteHierarchy(RObject objectToDelete, Session session)
            throws DtoTranslationException {

        String sqlDeleteOrgClosure = "delete from ROrgClosure as o where " +
                "(o.descendantId = :modifyId and o.descendantOid = :modifyOid) or " +
                "(o.ancestorId =:modifyId and o.ancestorOid = :modifyOid)";

        session.createQuery(sqlDeleteOrgClosure)
                .setParameter("modifyOid", objectToDelete.getOid())
                .setParameter("modifyId", 0L)
                .executeUpdate();

        String sqlDeleteOrgIncorrect = "delete from ROrgIncorrect as o where "
                + "(o.descendantId = :descendantId and o.descendantOid = :modifyOid) "
                + "or o.ancestorOid = :modifyOid";
        session.createQuery(sqlDeleteOrgIncorrect)
                .setParameter("modifyOid", objectToDelete.getOid())
                .setParameter("descendantId", 0L)
                .executeUpdate();

    }

    // private List<RObject> deleteFromHierarchy(RObject object, Session
    // session) throws SchemaException,
    // DtoTranslationException {
    //
    // LOGGER.trace("Deleting records from organization closure table.");
    //
    // Criteria criteria = session.createCriteria(ROrgClosure.class);
    // List<RObject> descendants =
    // criteria.setProjection(Projections.property("descendant"))
    // .add(Restrictions.eq("ancestor", object)).list();
    //
    // for (RObject desc : descendants) {
    // List<ROrgClosure> orgClosure = session.createCriteria(ROrgClosure.class)
    // .add(Restrictions.eq("descendant", desc)).list();
    // for (ROrgClosure o : orgClosure) {
    // session.delete(o);
    // }
    // // fillHierarchy(desc.toJAXB(getPrismContext()), session);
    // }
    //
    // criteria = session.createCriteria(ROrgClosure.class).add(
    // Restrictions.or(Restrictions.eq("ancestor", object),
    // Restrictions.eq("descendant", object)));
    //
    // List<ROrgClosure> orgClosure = criteria.list();
    // for (ROrgClosure o : orgClosure) {
    // LOGGER.trace("deleting from hierarchy: A: {} D:{} depth:{}",
    // new Object[] { o.getAncestor(), o.getDescendant(), o.getDepth() });
    // session.delete(o);
    // }
    // return descendants;
    // }

    @Override
    public <T extends ShadowType> List<PrismObject<T>> listResourceObjectShadows(String resourceOid,
                                                                                 Class<T> resourceObjectShadowType,
                                                                                 OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
        Validate.notNull(resourceObjectShadowType, "Resource object shadow type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.trace("Listing resource object shadows '{}' for resource '{}'.",
                new Object[]{resourceObjectShadowType.getSimpleName(), resourceOid});
        OperationResult subResult = result.createSubresult(LIST_RESOURCE_OBJECT_SHADOWS);
        subResult.addParam("oid", resourceOid);
        subResult.addParam("resourceObjectShadowType", resourceObjectShadowType);

        final String operation = "listing resource object shadows";
        int attempt = 1;

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("listResourceObjectShadow");

        try {
            while (true) {
                try {
                    return listResourceObjectShadowsAttempt(resourceOid, resourceObjectShadowType, subResult);
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(resourceOid, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private <T extends ShadowType> List<PrismObject<T>> listResourceObjectShadowsAttempt(
            String resourceOid, Class<T> resourceObjectShadowType, OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        List<PrismObject<T>> list = new ArrayList<PrismObject<T>>();
        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            Query query = session.createQuery("select shadow from " + ClassMapper.getHQLType(resourceObjectShadowType)
                    + " as shadow left join shadow.resourceRef as ref where ref.oid = :oid");
            query.setString("oid", resourceOid);

            List<RShadow> shadows = query.list();
            LOGGER.trace("Query returned {} shadows, transforming to JAXB types.",
                    new Object[]{(shadows != null ? shadows.size() : 0)});

            if (shadows != null) {
                for (RShadow shadow : shadows) {
                    ShadowType jaxb = shadow.toJAXB(getPrismContext());
                    PrismObject<T> prismObject = jaxb.asPrismObject();
                    validateObjectType(prismObject, resourceObjectShadowType);

                    list.add(prismObject);
                }
            }
            session.getTransaction().commit();
            LOGGER.trace("Done.");
        } catch (DtoTranslationException ex) {
            handleGeneralCheckedException(ex, session, result);
        } catch (RuntimeException ex) {
            handleGeneralRuntimeException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return list;
    }

    private <T extends ObjectType> void validateObjectType(PrismObject<T> prismObject, Class<T> type)
            throws SchemaException {
        if (prismObject == null || !type.isAssignableFrom(prismObject.getCompileTimeClass())) {
            throw new SchemaException("Expected to find '" + type.getSimpleName() + "' but found '"
                    + prismObject.getCompileTimeClass().getSimpleName() + "' (" + prismObject.toDebugName()
                    + "). Bad OID in a reference?");
        }
        if (InternalsConfig.consistencyChecks) {
            prismObject.checkConsistence();
        }
        if (InternalsConfig.encryptionChecks) {
            CryptoUtil.checkEncrypted(prismObject);
        }
    }

    private <T extends ObjectType> RObject createDataObjectFromJAXB(T object) throws SchemaException {

        RObject rObject;
        Class<? extends RObject> clazz = ClassMapper.getHQLTypeClass(object.getClass());
        try {
            rObject = clazz.newInstance();
            Method method = clazz.getMethod("copyFromJAXB", object.getClass(), clazz, PrismContext.class);
            method.invoke(clazz, object, rObject, getPrismContext());
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (StringUtils.isEmpty(message) && ex.getCause() != null) {
                message = ex.getCause().getMessage();
            }
            throw new SchemaException(message, ex);
        }

        return rObject;
    }

    private <T extends ObjectType> Criteria updatePagingAndSorting(Criteria query, Class<T> type, ObjectPaging paging) {
        if (paging == null) {
            return query;
        }
        if (paging.getOffset() != null) {
            query = query.setFirstResult(paging.getOffset());
        }
        if (paging.getMaxSize() != null) {
            query = query.setMaxResults(paging.getMaxSize());
        }

        if (paging.getDirection() == null && paging.getOrderBy() == null) {
            return query;
        }

        try {
            QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();
            // PropertyPath path = new
            // XPathHolder(paging.getOrderBy()).toPropertyPath();
            if (paging.getOrderBy() == null) {
                LOGGER.warn("Ordering by property path with size not equal 1 is not supported '" + paging.getOrderBy()
                        + "'.");
                return query;
            }
            EntityDefinition definition = registry.findDefinition(type, null, EntityDefinition.class);
            Definition def = definition.findDefinition(paging.getOrderBy(), Definition.class);
            if (def == null) {
                LOGGER.warn("Unknown path '" + paging.getOrderBy() + "', couldn't find definition for it, "
                        + "list will not be ordered by it.");
                return query;
            }

            switch (paging.getDirection()) {
                case ASCENDING:
                    query = query.addOrder(Order.asc(def.getJpaName()));
                    break;
                case DESCENDING:
                    query = query.addOrder(Order.desc(def.getJpaName()));
            }
        } catch (QueryException ex) {
            throw new SystemException(ex.getMessage(), ex);
        }

        return query;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.repo.api.RepositoryService#getRepositoryDiag()
     */
    @Override
    public RepositoryDiag getRepositoryDiag() {
        RepositoryDiag diag = new RepositoryDiag();
        diag.setImplementationShortName(IMPLEMENTATION_SHORT_NAME);
        diag.setImplementationDescription(IMPLEMENTATION_DESCRIPTION);

        SqlRepositoryConfiguration config = getConfiguration();

        //todo improve, find and use real values (which are used by sessionFactory) MID-1219
        diag.setDriverShortName(config.getDriverClassName());
        diag.setRepositoryUrl(config.getJdbcUrl());
        diag.setEmbedded(config.isEmbedded());

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while ((drivers != null && drivers.hasMoreElements())) {
            Driver driver = drivers.nextElement();
            if (!driver.getClass().getName().equals(config.getDriverClassName())) {
                continue;
            }

            diag.setDriverVersion(driver.getMajorVersion() + "." + driver.getMinorVersion());
        }

        List<LabeledString> details = new ArrayList<LabeledString>();
        diag.setAdditionalDetails(details);
        details.add(new LabeledString(DETAILS_DATA_SOURCE, config.getDataSource()));
        details.add(new LabeledString(DETAILS_HIBERNATE_DIALECT, config.getHibernateDialect()));
        details.add(new LabeledString(DETAILS_HIBERNATE_HBM_2_DDL, config.getHibernateHbm2ddl()));

        readDetailsFromConnection(diag, config);

        Collections.sort(details, new Comparator<LabeledString>() {

            @Override
            public int compare(LabeledString o1, LabeledString o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getLabel(), o2.getLabel());
            }
        });

        return diag;
    }

    private void readDetailsFromConnection(RepositoryDiag diag, final SqlRepositoryConfiguration config) {
        final List<LabeledString> details = diag.getAdditionalDetails();

        Session session = getSessionFactory().openSession();
        try {
            session.beginTransaction();
            session.doWork(new Work() {

                @Override
                public void execute(Connection connection) throws SQLException {
                    details.add(new LabeledString(DETAILS_TRANSACTION_ISOLATION,
                            getTransactionIsolation(connection, config)));


                    Properties info = connection.getClientInfo();
                    if (info == null) {
                        return;
                    }

                    for (String name : info.stringPropertyNames()) {
                        details.add(new LabeledString(DETAILS_CLIENT_INFO + name, info.getProperty(name)));
                    }
                }
            });
            session.getTransaction().commit();

            if (!(getSessionFactory() instanceof SessionFactoryImpl)) {
                return;
            }
            SessionFactoryImpl factory = (SessionFactoryImpl) getSessionFactory();
            // we try to override configuration which was read from sql repo configuration with
            // real configuration from session factory
            String dialect = factory.getDialect() != null ? factory.getDialect().getClass().getName() : null;
            details.add(new LabeledString(DETAILS_HIBERNATE_DIALECT, dialect));
        } catch (Exception ex) {
            //nowhere to report error (no operation result available)
            session.getTransaction().rollback();
        } catch (Throwable th) {
            //nowhere to report error (no operation result available)
            session.getTransaction().rollback();
        } finally {
            cleanupSessionAndResult(session, null);
        }
    }

    private String getTransactionIsolation(Connection connection, SqlRepositoryConfiguration config) {
        String value = config.getTransactionIsolation() != null ?
                config.getTransactionIsolation().name() + "(read from repo configuration)" : null;

        try {
            switch (connection.getTransactionIsolation()) {
                case Connection.TRANSACTION_NONE:
                    value = "TRANSACTION_NONE (read from connection)";
                    break;
                case Connection.TRANSACTION_READ_COMMITTED:
                    value = "TRANSACTION_READ_COMMITTED (read from connection)";
                    break;
                case Connection.TRANSACTION_READ_UNCOMMITTED:
                    value = "TRANSACTION_READ_UNCOMMITTED (read from connection)";
                    break;
                case Connection.TRANSACTION_REPEATABLE_READ:
                    value = "TRANSACTION_REPEATABLE_READ (read from connection)";
                    break;
                case Connection.TRANSACTION_SERIALIZABLE:
                    value = "TRANSACTION_SERIALIZABLE (read from connection)";
                    break;
                default:
                    value = "Unknown value in connection.";
            }
        } catch (Exception ex) {
            //nowhere to report error (no operation result available)
        }

        return value;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.repo.api.RepositoryService#repositorySelfTest(com.evolveum.midpoint.schema.result.OperationResult)
     */
    @Override
    public void repositorySelfTest(OperationResult parentResult) {
        // TODO add some SQL-specific self-test methods
        // No self-tests for now
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.repo.api.RepositoryService#getVersion(java.lang.Class, java.lang.String, com.evolveum.midpoint.schema.result.OperationResult)
     */
    @Override
    public <T extends ObjectType> String getVersion(Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(oid, "Object oid must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.debug("Getting version for {} with oid '{}'.", new Object[]{type.getSimpleName(), oid});

        OperationResult subResult = parentResult.createSubresult(GET_VERSION);
        subResult.addParam("type", type.getName());
        subResult.addParam("oid", oid);

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(GET_VERSION);

        final String operation = "getting version";
        int attempt = 1;
        try {
            while (true) {
                try {
                    return getVersionAttempt(type, oid, subResult);
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(null, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private <T extends ObjectType> String getVersionAttempt(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        String version = null;
        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            Query query = session.createQuery("select o.version from " + ClassMapper.getHQLType(type)
                    + " as o where o.id = 0 and o.oid = :oid");
            query.setString("oid", oid);

            Long versionLong = (Long) query.uniqueResult();
            if (versionLong == null) {
                throw new ObjectNotFoundException("Object '" + type.getSimpleName()
                        + "' with oid '" + oid + "' was not found.");
            }
            version = versionLong.toString();
        } catch (RuntimeException ex) {
            handleGeneralRuntimeException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return version;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.repo.api.RepositoryService#searchObjectsIterative(java.lang.Class,
     * com.evolveum.midpoint.prism.query.ObjectQuery, com.evolveum.midpoint.schema.ResultHandler,
     * com.evolveum.midpoint.schema.result.OperationResult)
     */
    @Override
    public <T extends ObjectType> void searchObjectsIterative(Class<T> type, ObjectQuery query,
                                                              ResultHandler<T> handler, OperationResult result)
            throws SchemaException {

        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(handler, "Result handler must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        logSearchInputParameters(type, query, true);

        OperationResult subResult = result.createSubresult(SEARCH_OBJECTS_ITERATIVE);
        subResult.addParam("type", type.getName());
        subResult.addParam("query", query);

        if (getConfiguration().isIterativeSearchByPaging()) {
            searchObjectsIterativeByPaging(type, query, handler, subResult);
            return;
        }

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(SEARCH_OBJECTS_ITERATIVE);

        final String operation = "searching iterative";
        int attempt = 1;
        try {
            while (true) {
                try {
                    searchObjectsIterativeAttempt(type, query, handler, subResult);
                    return;
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(null, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private <T extends ObjectType> Criteria createCriteria(Class<T> type, ObjectQuery query, Session session)
            throws QueryException {

        LOGGER.trace("Updating query criteria.");
        Criteria criteria;
        if (query != null && query.getFilter() != null) {
            QueryInterpreter interpreter = new QueryInterpreter();
            criteria = interpreter.interpret(query, type, getPrismContext(), session);
        } else {
            criteria = session.createCriteria(ClassMapper.getHQLTypeClass(type));
        }

        if (query != null && query.getPaging() != null) {
            criteria = updatePagingAndSorting(criteria, type, query.getPaging());
        }

        return criteria;
    }

    private <T extends ObjectType> void searchObjectsIterativeAttempt(Class<T> type, ObjectQuery query,
                                                                      ResultHandler<T> handler, OperationResult result)
            throws SchemaException {

        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            Criteria criteria = createCriteria(type, query, session);
            ScrollableResults results = criteria.scroll(ScrollMode.FORWARD_ONLY);
            try {
                Iterator<RObject> iterator = new ScrollableResultsIterator(results);
                while (iterator.hasNext()) {
                    RObject object = iterator.next();

                    ObjectType objectType = object.toJAXB(getPrismContext());
                    PrismObject<T> prismObject = objectType.asPrismObject();
                    validateObjectType(prismObject, type);

                    if (!handler.handle(prismObject, result)) {
                        break;
                    }
                }
            } finally {
                if (results != null) {
                    results.close();
                }
            }

            session.getTransaction().commit();
        } catch (DtoTranslationException ex) {
            handleGeneralCheckedException(ex, session, result);
        } catch (QueryException ex) {
            handleGeneralCheckedException(ex, session, result);
        } catch (RuntimeException ex) {
            handleGeneralRuntimeException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }
    }

    private <T extends ObjectType> void searchObjectsIterativeByPaging(Class<T> type, ObjectQuery query,
                                                                       ResultHandler<T> handler, OperationResult result)
            throws SchemaException {

        try {
            ObjectQuery pagedQuery = query != null ? query.clone() : new ObjectQuery();

            int offset;
            int remaining;
            final int batchSize = getConfiguration().getIterativeSearchByPagingBatchSize();

            ObjectPaging paging = pagedQuery.getPaging();

            if (paging == null) {
                paging = ObjectPaging.createPaging(0, 0);        // counts will be filled-in later
                pagedQuery.setPaging(paging);
                offset = 0;
                remaining = countObjects(type, query, result);
            } else {
                offset = paging.getOffset() != null ? paging.getOffset() : 0;
                remaining = paging.getMaxSize() != null ? paging.getMaxSize() : countObjects(type, query, result) - offset;
            }

            while (remaining > 0) {
                paging.setOffset(offset);
                paging.setMaxSize(remaining < batchSize ? remaining : batchSize);

                List<PrismObject<T>> objects = searchObjects(type, pagedQuery, result);

                for (PrismObject<T> object : objects) {
                    if (!handler.handle(object, result)) {
                        break;
                    }
                }

                if (objects.size() == 0) {
                    break;                      // should not occur, but let's check for this to avoid endless loops
                }
                offset += objects.size();
                remaining -= objects.size();
            }
        } finally {
            if (result != null && result.isUnknown()) {
                result.computeStatus();
            }
        }
    }

    @Override
    public void cleanupTasks(CleanupPolicyType policy, OperationResult parentResult) {
        OperationResult subResult = parentResult.createSubresult(CLEANUP_TASKS);
        cleanup(RTask.class, policy, subResult);
    }


    /**
     * This is attempt to do task cleanup by custom native queries. Hibernate tries to delete task
     * through temporary table with columns oid, id. That's not a bad idea until it attempts to call
     * delete from statement with "in" clause with two columns (oid, id). H2 and SQL Server fails
     * with in clause that contains more than one column.
     * <p/>
     * Therefore this method do cleanup by creating temporary table only with oid (id is always 0).
     * Maybe big schema cleanup would help or something like that.
     * <p/>
     * Somebody improve this when there's time for it.
     *
     * @param entity
     * @param minValue
     * @param session
     * @return number of deleted tasks
     */
    @Override
    protected int cleanupAttempt(Class entity, Date minValue, Session session) {
        if (!RTask.class.equals(entity)) {
            return 0;
        }

        MidPointNamingStrategy namingStrategy = new MidPointNamingStrategy();
        final String taskTableName = namingStrategy.classToTableName(RTask.class.getSimpleName());
        final String objectTableName = namingStrategy.classToTableName(RObject.class.getSimpleName());
        final String containerTableName = namingStrategy.classToTableName(RContainer.class.getSimpleName());

        final String completionTimestampColumn = "completionTimestamp";

        final Dialect dialect = Dialect.getDialect(getSessionFactoryBean().getHibernateProperties());
        if (!dialect.supportsTemporaryTables()) {
            LOGGER.error("Dialect {} doesn't support temporary tables, couldn't cleanup tasks.",
                    new Object[]{dialect});
            throw new SystemException("Dialect " + dialect
                    + " doesn't support temporary tables, couldn't cleanup tasks.");
        }

        //create temporary table
        final String tempTable = dialect.generateTemporaryTableName(taskTableName);
        createTemporaryTable(session, dialect, tempTable);

        //fill temporary table
        StringBuilder sb = new StringBuilder();
        sb.append("insert into ").append(tempTable).append(' ');            //todo improve this insert
        sb.append("select t.oid as oid from ").append(taskTableName).append(" t");
        sb.append(" inner join ").append(objectTableName).append(" o on t.id = o.id and t.oid = o.oid");
        sb.append(" inner join ").append(containerTableName).append(" c on t.id = c.id and t.oid = c.oid");
        sb.append(" where t.").append(completionTimestampColumn).append(" < ?");

        SQLQuery query = session.createSQLQuery(sb.toString());
        query.setParameter(0, new Timestamp(minValue.getTime()));
        query.executeUpdate();

        //drop records from m_task, m_object, m_container
        session.createSQLQuery(createDeleteQuery(taskTableName, tempTable)).executeUpdate();
        session.createSQLQuery(createDeleteQuery(objectTableName, tempTable)).executeUpdate();
        int count = session.createSQLQuery(createDeleteQuery(containerTableName, tempTable)).executeUpdate();

        //drop temporary table
        if (dialect.dropTemporaryTableAfterUse()) {
            LOGGER.debug("Dropping temporary table.");
            sb = new StringBuilder();
            sb.append(dialect.getDropTemporaryTableString());
            sb.append(' ').append(tempTable);

            session.createSQLQuery(sb.toString()).executeUpdate();
        }

        return count;
    }

    /**
     * This method creates temporary table for cleanup task method.
     *
     * @param session
     * @param dialect
     * @param tempTable
     */
    private void createTemporaryTable(Session session, final Dialect dialect, final String tempTable) {
        session.doWork(new Work() {

            @Override
            public void execute(Connection connection) throws SQLException {
                StringBuilder sb = new StringBuilder();
                sb.append(dialect.getCreateTemporaryTableString());
                sb.append(' ').append(tempTable).append(" (oid ");
                sb.append(dialect.getTypeName(Types.VARCHAR, 36, 0, 0));
                if (isUsingSQLServer(dialect)) {
                    sb.append(" collate database_default");
                }
                sb.append(" not null)");
                sb.append(dialect.getCreateTemporaryTablePostfix());

                Statement s = connection.createStatement();
                s.execute(sb.toString());
            }
        });
    }

    private String createDeleteQuery(String objectTable, String tempTable) {
        StringBuilder sb = new StringBuilder();
        sb.append("delete from ").append(objectTable);
        sb.append(" where id = 0 and (oid in (select oid from ").append(tempTable).append("))");

        return sb.toString();
    }

    private boolean isUsingSQLServer(Dialect dialect) {
        return UnicodeSQLServer2008Dialect.class.equals(dialect.getClass());
    }
}

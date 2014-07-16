/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.any.RValueType;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query.QueryEngine;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.RQuery;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.*;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.*;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jdbc.Work;
import org.springframework.stereotype.Repository;

import javax.xml.namespace.QName;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

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

    private OrgClosureManager orgClosureManager;

    public SqlRepositoryServiceImpl(SqlRepositoryFactory repositoryFactory) {
        super(repositoryFactory);
    }

    private OrgClosureManager getOrgClosureManager() {
        if (orgClosureManager == null) {
            orgClosureManager = new OrgClosureManager(getConfiguration());
        }
        return orgClosureManager;
    }

    private <T extends ObjectType> PrismObject<T> getObject(Session session, Class<T> type, String oid,
                                                            Collection<SelectorOptions<GetOperationOptions>> options,
                                                            boolean lockForUpdate)
            throws ObjectNotFoundException, SchemaException, DtoTranslationException, QueryException {

        boolean lockedForUpdateViaHibernate = false;
        boolean lockedForUpdateViaSql = false;

        LockOptions lockOptions = new LockOptions();
        //todo fix lock for update!!!!!
        if (lockForUpdate) {
            if (getConfiguration().isLockForUpdateViaHibernate()) {
                lockOptions.setLockMode(LockMode.PESSIMISTIC_WRITE);
                lockedForUpdateViaHibernate = true;
            } else if (getConfiguration().isLockForUpdateViaSql()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Trying to lock object " + oid + " for update (via SQL)");
                }
                long time = System.currentTimeMillis();
                SQLQuery q = session.createSQLQuery("select oid from m_object where oid = ? for update");
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

        GetObjectResult fullObject = null;
        if (!lockForUpdate) {
            Query query = session.getNamedQuery("get.object");
            query.setString("oid", oid);
            query.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);
            query.setLockOptions(lockOptions);

            fullObject = (GetObjectResult) query.uniqueResult();
        } else {
            // we're doing update after this get, therefore we load full object right now
            // (it would be loaded during merge anyway)
            // this just loads object to hibernate session, probably will be removed later. Merge after this get
            // will be faster. Read and use object only from fullObject column.
            // todo remove this later [lazyman]
            Criteria criteria = session.createCriteria(ClassMapper.getHQLTypeClass(type));
            criteria.add(Restrictions.eq("oid", oid));

            criteria.setLockMode(lockOptions.getLockMode());
            RObject obj = (RObject) criteria.uniqueResult();

            if (obj != null) {
                obj.toJAXB(getPrismContext(), null).asPrismObject();
                fullObject = new GetObjectResult(obj.getFullObject(), obj.getStringsCount(), obj.getLongsCount(),
                        obj.getDatesCount(), obj.getReferencesCount(), obj.getPolysCount());
            }
        }

        LOGGER.trace("Got it.");
        if (fullObject == null) {
            throwObjectNotFoundException(type, oid);
        }

        LOGGER.trace("Transforming data to JAXB type.");
        PrismObject<T> prismObject = updateLoadedObject(fullObject, type, options, session);
        validateObjectType(prismObject, type);

        return prismObject;
    }

    private <T extends ObjectType> PrismObject<T> throwObjectNotFoundException(Class<T> type, String oid)
            throws ObjectNotFoundException {
        throw new ObjectNotFoundException("Object of type '" + type.getSimpleName() + "' with oid '" + oid
                + "' was not found.", null, oid);
    }

    @Override
    public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
                                                           Collection<SelectorOptions<GetOperationOptions>> options,
                                                           OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Getting object '{}' with oid '{}'.", new Object[]{type.getSimpleName(), oid});

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
                    return getObjectAttempt(type, oid, options, subResult);
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(oid, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private <T extends ObjectType> PrismObject<T> getObjectAttempt(Class<T> type, String oid,
                                                                   Collection<SelectorOptions<GetOperationOptions>> options,
                                                                   OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        PrismObject<T> objectType = null;

        Session session = null;
        try {
            session = beginReadOnlyTransaction();

            objectType = getObject(session, type, oid, options, false);

            session.getTransaction().commit();
        } catch (ObjectNotFoundException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (SchemaException ex) {
            rollbackTransaction(session, ex, "Schema error while getting object with oid: "
                    + oid + ". Reason: " + ex.getMessage(), result, true);
            throw ex;
        } catch (QueryException | DtoTranslationException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
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
    public <F extends FocusType> PrismObject<F> searchShadowOwner(String shadowOid, OperationResult result)
            throws ObjectNotFoundException {
        Validate.notEmpty(shadowOid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Searching shadow owner for {}", shadowOid);

        final String operation = "searching shadow owner";
        int attempt = 1;

        OperationResult subResult = result.createSubresult(SEARCH_SHADOW_OWNER);
        subResult.addParam("shadowOid", shadowOid);

        while (true) {
            try {
                return searchShadowOwnerAttempt(shadowOid, subResult);
            } catch (RuntimeException ex) {
                attempt = logOperationAttempt(shadowOid, operation, attempt, ex, subResult);
            }
        }
    }

    private <F extends FocusType> PrismObject<F> searchShadowOwnerAttempt(String shadowOid, OperationResult result)
            throws ObjectNotFoundException {
        PrismObject<F> owner = null;
        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            Query query = session.getNamedQuery("searchShadowOwner.getShadow");
            query.setString("oid", shadowOid);
            if (query.uniqueResult() == null) {
                throw new ObjectNotFoundException("Shadow with oid '" + shadowOid + "' doesn't exist.");
            }

            LOGGER.trace("Selecting account shadow owner for account {}.", new Object[]{shadowOid});
            query = session.getNamedQuery("searchShadowOwner.getOwner");
            query.setString("oid", shadowOid);
            query.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);

            List<GetObjectResult> focuses = query.list();
            LOGGER.trace("Found {} focuses, transforming data to JAXB types.",
                    new Object[]{(focuses != null ? focuses.size() : 0)});

            if (focuses == null || focuses.isEmpty()) {
                // account shadow owner was not found
                return null;
            }

            if (focuses.size() > 1) {
                LOGGER.warn("Found {} owners for shadow oid {}, returning first owner.",
                        new Object[]{focuses.size(), shadowOid});
            }

            GetObjectResult focus = focuses.get(0);
            owner = updateLoadedObject(focus, (Class<F>) FocusType.class, null, session);

            session.getTransaction().commit();
        } catch (ObjectNotFoundException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (SchemaException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return owner;
    }

    @Override
    @Deprecated
    public PrismObject<UserType> listAccountShadowOwner(String accountOid, OperationResult result)
            throws ObjectNotFoundException {
        Validate.notEmpty(accountOid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Selecting account shadow owner for account {}.", new Object[]{accountOid});

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
        PrismObject<UserType> userType = null;
        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            Query query = session.getNamedQuery("listAccountShadowOwner.getUser");
            query.setString("oid", accountOid);
            query.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);

            List<GetObjectResult> users = query.list();
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

            GetObjectResult user = users.get(0);
            userType = updateLoadedObject(user, UserType.class, null, session);

            session.getTransaction().commit();
        } catch (SchemaException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return userType;
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

        if (options == null) {
            options = new RepoAddOptions();
        }

        LOGGER.debug("Adding object type '{}', overwrite={}, allowUnencryptedValues={}",
                new Object[]{object.getCompileTimeClass().getSimpleName(), options.isOverwrite(),
                        options.isAllowUnencryptedValues()}
        );

        if (InternalsConfig.encryptionChecks && !RepoAddOptions.isAllowUnencryptedValues(options)) {
            CryptoUtil.checkEncrypted(object);
        }

        if (InternalsConfig.consistencyChecks) {
            object.checkConsistence();
        }

        if (LOGGER.isTraceEnabled()) {
            // Explicitly log name
            PolyStringType namePolyType = object.asObjectable().getName();
            LOGGER.trace("NAME: {} - {}", namePolyType.getOrig(), namePolyType.getNorm());
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
        String oid = null;
        Session session = null;
        // it is needed to keep the original oid for example for import options. if we do not keep it
        // and it was null it can bring some error because the oid is set when the object contains orgRef
        // or it is org. and by the import we do not know it so it will be trying to delete non-existing object
        String originalOid = object.getOid();
        try {
            ObjectType objectType = object.asObjectable();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Object\n{}", new Object[]{object.debugDump()});
            }

            LOGGER.trace("Translating JAXB to data type.");
            RObject rObject = createDataObjectFromJAXB(object, true);

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
        } catch (DtoTranslationException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
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
                PrismObject<T> oldObject = getObject(session, object.getCompileTimeClass(), originalOid, null, true);
                ObjectDelta<T> delta = object.diff(oldObject);
                modifications = delta.getModifications();

                //we found existing object which will be overwritten, therefore we increment version
                Integer version = RUtil.getIntegerFromString(oldObject.getVersion());
                version = (version == null) ? 0 : ++version;

                rObject.setVersion(version);
            } catch (QueryException ex) {
                handleGeneralCheckedException(ex, session, null);
            } catch (ObjectNotFoundException ex) {
                //it's ok that object was not found, therefore we won't be overwriting it
            }
        }

        updateFullObject(rObject, object);
        RObject merged = (RObject) session.merge(rObject);
        //todo finish orgClosureManager
//        orgClosureManager.updateOrgClosure(modifications, session, merged.getOid(), object.getCompileTimeClass(),
//                OrgClosureManager.Operation.ADD);

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

    private <T extends ObjectType> void updateFullObject(RObject object, PrismObject<T> savedObject)
            throws DtoTranslationException, SchemaException {
        LOGGER.debug("Updating full object xml column start.");
        savedObject.setVersion(Integer.toString(object.getVersion()));

        if (UserType.class.equals(savedObject.getCompileTimeClass())) {
            savedObject.removeProperty(UserType.F_JPEG_PHOTO);
        }

        String xml = getPrismContext().serializeObjectToString(savedObject, PrismContext.LANG_XML);
        byte[] fullObject = RUtil.getByteArrayFromXml(xml, getConfiguration().isUseZip());

        if (LOGGER.isTraceEnabled()) LOGGER.trace("Storing full object\n{}", xml);

        object.setFullObject(fullObject);

        LOGGER.debug("Updating full object xml column finish.");
    }

    private <T extends ObjectType> String nonOverwriteAddObjectAttempt(PrismObject<T> object, ObjectType objectType,
                                                                       RObject rObject, String originalOid,
                                                                       Session session)
            throws ObjectAlreadyExistsException, SchemaException, DtoTranslationException {

        // check name uniqueness (by type)
        if (StringUtils.isNotEmpty(originalOid)) {
            LOGGER.trace("Checking oid uniqueness.");
            //todo improve this table name bullshit
            Class hqlType = ClassMapper.getHQLTypeClass(object.getCompileTimeClass());
            SQLQuery query = session.createSQLQuery("select count(*) from " + RUtil.getTableName(hqlType)
                    + " where oid=:oid");
            query.setString("oid", object.getOid());

            Number count = (Number) query.uniqueResult();
            if (count != null && count.longValue() > 0) {
                throw new ObjectAlreadyExistsException("Object '" + object.getCompileTimeClass().getSimpleName()
                        + "' with oid '" + object.getOid() + "' already exists.");
            }
        }

        updateFullObject(rObject, object);

        LOGGER.trace("Saving object (non overwrite).");
        String oid = (String) session.save(rObject);

        //todo finish orgClosureManager
        //Collection<ReferenceDelta> modifications = createAddParentRefDelta(object);
        //orgClosureManager.updateOrgClosure(modifications, session, oid, object.getCompileTimeClass(),
        //        OrgClosureManager.Operation.ADD);


        if (objectType instanceof OrgType || !objectType.getParentOrgRef().isEmpty()) {
            long time = System.currentTimeMillis();
            LOGGER.trace("Org. structure closure table update started.");
            objectType.setOid(oid);
            fillHierarchy(rObject, session, true);
            LOGGER.trace("Org. structure closure table update finished ({} ms).",
                    new Object[]{(System.currentTimeMillis() - time)});
        }

        return oid;
    }

    private boolean existOrgCLosure(Session session, String ancestorOid, String descendantOid, int depth) {
        // if not exist pair with same depth, then create else nothing do
        Query qExistClosure = session.getNamedQuery("existOrgClosure");
        qExistClosure.setParameter("ancestorOid", ancestorOid);
        qExistClosure.setParameter("descendantOid", descendantOid);
        qExistClosure.setParameter("depth", depth);

        return (Long) qExistClosure.uniqueResult() != 0;

    }

    private boolean existIncorrect(Session session, String ancestorOid, String descendantOid) {
        // if not exist pair with same depth, then create else nothing do
        Query qExistIncorrect = session.getNamedQuery("existIncorrect");
        qExistIncorrect.setParameter("ancestorOid", ancestorOid);
        qExistIncorrect.setParameter("descendantOid", descendantOid);

        return (Long) qExistIncorrect.uniqueResult() != 0;
    }

    private <T extends ObjectType> void fillHierarchy(RObject<T> rOrg, Session session, boolean withIncorrect)
            throws SchemaException {

        if (!existOrgCLosure(session, rOrg.getOid(), rOrg.getOid(), 0)) {
            ROrgClosure closure = new ROrgClosure(rOrg, rOrg, 0);
            session.save(closure);
        }

        for (RObjectReference orgRef : rOrg.getParentOrgRef()) {
            fillTransitiveHierarchy(rOrg, orgRef.getTargetOid(), session, withIncorrect);
        }

        if (withIncorrect) {
            Query qIncorrect = session.getNamedQuery("fillHierarchy");
            qIncorrect.setString("oid", rOrg.getOid());

            List<ROrgIncorrect> orgIncorrect = qIncorrect.list();
            for (ROrgIncorrect orgInc : orgIncorrect) {
                Query qObject = session.createQuery("from RObject where oid = :oid");
                qObject.setString("oid", orgInc.getDescendantOid());
                RObject rObjectI = (RObject) qObject.uniqueResult();
                if (rObjectI != null) {
                    fillTransitiveHierarchy(rObjectI, rOrg.getOid(), session, !withIncorrect);
                    session.delete(orgInc);
                }
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
                String anc = "null";
                if (o != null && o.getAncestor() != null) {
                    anc = o.getAncestor().getOid();
                }
                LOGGER.trace(
                        "adding {}\t{}\t{}",
                        new Object[]{anc, descendant == null ? null : descendant.getOid(), o.getDepth() + 1});

                boolean existClosure = existOrgCLosure(session, o.getAncestor().getOid(),
                        descendant.getOid(), o.getDepth() + 1);
                if (!existClosure)
                    session.save(new ROrgClosure(o.getAncestor(), descendant, o.getDepth() + 1));
            }
        } else if (withIncorrect) {
            boolean existIncorrect = existIncorrect(session, ancestorOid, descendant.getOid());
            if (!existIncorrect) {
                LOGGER.trace("adding incorrect {}\t{}", new Object[]{ancestorOid,
                        descendant.getOid()});
                session.save(new ROrgIncorrect(ancestorOid, descendant.getOid()));
            }
        }
    }

    @Override
    public <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Deleting object type '{}' with oid '{}'", new Object[]{type.getSimpleName(), oid});

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

    private <T extends ObjectType> List<ReferenceDelta> createAddParentRefDelta(PrismObject<T> object) {
        PrismReference parentOrgRef = object.findReference(ObjectType.F_PARENT_ORG_REF);
        if (parentOrgRef == null) {
            return new ArrayList<>();
        }

        PrismObjectDefinition def = object.getDefinition();
        ReferenceDelta delta = ReferenceDelta.createModificationAdd(new ItemPath(ObjectType.F_PARENT_ORG_REF),
                def, parentOrgRef.getClonedValues());

        return Arrays.asList(delta);
    }

    private <T extends ObjectType> List<ReferenceDelta> createDeleteParentRefDelta(Class<T> type, String oid,
                                                                                   Session session) {
        Query query = session.createQuery("from RParentOrgRef as r where r.ownerOid = :oid");
        query.setString("oid", oid);

        List<RObjectReference> references = query.list();
        if (references == null || references.isEmpty()) {
            return new ArrayList<>();
        }

        List<PrismReferenceValue> values = new ArrayList<>();
        for (RObjectReference ref : references) {
            ObjectReferenceType refType = new ObjectReferenceType();
            RObjectReference.copyToJAXB(ref, refType, getPrismContext());

            values.add(refType.asReferenceValue());
        }

        PrismObjectDefinition def = getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
        ReferenceDelta delta = ReferenceDelta.createModificationDelete(new ItemPath(ObjectType.F_PARENT_ORG_REF),
                def, values);

        return Arrays.asList(delta);
    }

    private <T extends ObjectType> void deleteObjectAttempt(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException {
        Session session = null;
        try {
            session = beginTransaction();

            Criteria query = session.createCriteria(ClassMapper.getHQLTypeClass(type));
            query.add(Restrictions.eq("oid", oid));
            RObject object = (RObject) query.uniqueResult();
            if (object == null) {
                throw new ObjectNotFoundException("Object of type '" + type.getSimpleName() + "' with oid '" + oid
                        + "' was not found.", null, oid);
            }

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
        } catch (SchemaException | DtoTranslationException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }
    }


    private void recompute(List<RObject> objectsToRecompute, Session session)
            throws SchemaException, DtoTranslationException {

        LOGGER.trace("Recomputing organization structure closure table after delete.");

        for (RObject object : objectsToRecompute) {
            Criteria query = session.createCriteria(ClassMapper
                    .getHQLTypeClass(object.toJAXB(getPrismContext(), null)
                            .getClass()));

            // RObject.toJAXB will be deprecated and this query can't be replaced by:
            // Criteria query = session.createCriteria(object.getClass());
            // Because this will cause deadlock. It's the same query without unnecessary object loading, fuck. [lazyman]

            query.add(Restrictions.eq("oid", object.getOid()));
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

//        Query query = session.createQuery("delete from ROrgClosure as c where c.descendantOid = :dOid");
//        query.setParameter("dOid", object.getOid());
//
//        query.executeUpdate();
    }

    @Override
    public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, OperationResult result) {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Counting objects of type '{}', query (on trace level).", new Object[]{type.getSimpleName()});
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Full query\n{}", new Object[]{(query == null ? "undefined" : query.debugDump())});
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
            Class<? extends RObject> hqlType = ClassMapper.getHQLTypeClass(type);

            session = beginReadOnlyTransaction();
            Number longCount;
            if (query == null || query.getFilter() == null) {
                // this is 5x faster than count with 3 inner joins, it can probably improved also for queries which
                // filters uses only properties from concrete entities like RUser, RRole by improving interpreter [lazyman]
                SQLQuery sqlQuery = session.createSQLQuery("SELECT COUNT(*) FROM " + RUtil.getTableName(hqlType));
                longCount = (Number) sqlQuery.uniqueResult();
            } else {
                QueryEngine engine = new QueryEngine(getConfiguration(), getPrismContext());
                RQuery rQuery = engine.interpret(query, type, null, true, session);

                longCount = (Number) rQuery.uniqueResult();
            }
            LOGGER.trace("Found {} objects.", longCount);
            count = longCount != null ? longCount.intValue() : 0;
        } catch (QueryException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return count;
    }

    @Override
    public <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
                                                                     Collection<SelectorOptions<GetOperationOptions>> options,
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
                    return searchObjectsAttempt(type, query, options, subResult);
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
        ObjectPaging paging = query != null ? query.getPaging() : null;
        LOGGER.debug("Searching objects of type '{}', query (on trace level), offset {}, count {}, iterative {}.",
                new Object[]{type.getSimpleName(), (paging != null ? paging.getOffset() : "undefined"),
                        (paging != null ? paging.getMaxSize() : "undefined"), iterative}
        );

        if (!LOGGER.isTraceEnabled()) {
            return;
        }

        LOGGER.trace("Full query\n{}\nFull paging\n{}", new Object[]{
                (query == null ? "undefined" : query.debugDump()),
                (paging != null ? paging.debugDump() : "undefined")});

        if (iterative) {
            LOGGER.trace("Iterative search by paging: {}, batch size {}",
                    getConfiguration().isIterativeSearchByPaging(),
                    getConfiguration().getIterativeSearchByPagingBatchSize());
        }
    }

    private <T extends ObjectType> List<PrismObject<T>> searchObjectsAttempt(Class<T> type, ObjectQuery query,
                                                                             Collection<SelectorOptions<GetOperationOptions>> options,
                                                                             OperationResult result) throws SchemaException {
        List<PrismObject<T>> list = new ArrayList<>();
        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            QueryEngine engine = new QueryEngine(getConfiguration(), getPrismContext());
            RQuery rQuery = engine.interpret(query, type, options, false, session);

            List<GetObjectResult> objects = rQuery.list();
            LOGGER.trace("Found {} objects, translating to JAXB.", new Object[]{(objects != null ? objects.size() : 0)});

            for (GetObjectResult object : objects) {
                PrismObject<T> prismObject = updateLoadedObject(object, type, options, session);
                list.add(prismObject);
            }

            session.getTransaction().commit();
        } catch (QueryException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return list;
    }

    /**
     * This method provides object parsing from String and validation.
     */
    private <T extends ObjectType> PrismObject<T> updateLoadedObject(GetObjectResult result, Class<T> type,
                                                                     Collection<SelectorOptions<GetOperationOptions>> options,
                                                                     Session session) throws SchemaException {

        String xml = RUtil.getXmlFromByteArray(result.getFullObject(), getConfiguration().isUseZip());
        PrismObject<T> prismObject;
        try {
            prismObject = getPrismContext().parseObject(xml);
        } catch (SchemaException e) {
            LOGGER.debug("Couldn't parse object because of schema exception ({}):\nObject: {}", e, xml);
            throw e;
        }

        if (UserType.class.equals(prismObject.getCompileTimeClass())) {
            if (SelectorOptions.hasToLoadPath(UserType.F_JPEG_PHOTO, options)) {
                //todo improve, use user.hasPhoto flag and take options into account [lazyman]
                //call this only when options contains INCLUDE user/jpegPhoto
                Query query = session.getNamedQuery("get.userPhoto");
                query.setString("oid", prismObject.getOid());
                byte[] photo = (byte[]) query.uniqueResult();
                if (photo != null) {
                    PrismProperty property = prismObject.findOrCreateProperty(UserType.F_JPEG_PHOTO);
                    property.setRealValue(photo);
                }
            }
        } else if (ShadowType.class.equals(prismObject.getCompileTimeClass())) {
            //we store it because provisioning now sends it to repo, but it should be transient
            prismObject.removeContainer(ShadowType.F_ASSOCIATION);

            LOGGER.debug("Loading definitions for shadow attributes.");

            Short[] counts = result.getCountProjection();
            Class[] classes = GetObjectResult.EXT_COUNT_CLASSES;

            for (int i = 0; i < classes.length; i++) {
                if (counts[i] == null || counts[i] == 0) {
                    continue;
                }

                applyShadowAttributeDefinitions(classes[i], prismObject, session);
            }
            LOGGER.debug("Definitions for attributes loaded. Counts: {}", Arrays.toString(counts));
        }

        validateObjectType(prismObject, type);

        return prismObject;
    }

    private void applyShadowAttributeDefinitions(Class<? extends RAnyValue> anyValueType,
                                                 PrismObject object, Session session) throws SchemaException {

        PrismContainer attributes = object.findContainer(ShadowType.F_ATTRIBUTES);

        Query query = session.getNamedQuery("getDefinition." + anyValueType.getSimpleName());
        query.setParameter("oid", object.getOid());
        query.setParameter("ownerType", RObjectExtensionType.ATTRIBUTES);

        List<Object[]> values = query.list();
        if (values == null || values.isEmpty()) {
            return;
        }

        for (Object[] value : values) {
            ItemDefinition def;
            QName name = RUtil.stringToQName((String) value[0]);
            QName type = RUtil.stringToQName((String) value[1]);

            switch ((RValueType) value[2]) {
                case PROPERTY:
                    def = new PrismPropertyDefinition(name, type, object.getPrismContext());
                    break;
                case REFERENCE:
                    def = new PrismReferenceDefinition(name, type, object.getPrismContext());
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported value type " + value[2]);
            }

            Item item = attributes.findItem(def.getName());
            if (item.getDefinition() == null) {
                item.applyDefinition(def, true);
            }
        }
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

        OperationResult subResult = result.createSubresult(MODIFY_OBJECT);
        subResult.addParam("type", type.getName());
        subResult.addParam("oid", oid);
        subResult.addCollectionOfSerializablesAsParam("modifications", modifications);

        if (modifications.isEmpty()) {
            LOGGER.debug("Modification list is empty, nothing was modified.");
            subResult.recordStatus(OperationResultStatus.SUCCESS, "Modification list is empty, nothing was modified.");
            return;
        }

        if (InternalsConfig.encryptionChecks) {
            CryptoUtil.checkEncrypted(modifications);
        }

        if (InternalsConfig.consistencyChecks) {
            for (ItemDelta modification : modifications) {
                modification.checkConsistence();
            }
        }

        if (LOGGER.isTraceEnabled()) {
            for (ItemDelta modification : modifications) {
                if (modification instanceof PropertyDelta<?>) {
                    PropertyDelta<?> propDelta = (PropertyDelta<?>) modification;
                    if (propDelta.getPath().equivalent(new ItemPath(ObjectType.F_NAME))) {
                        Collection<PrismPropertyValue<PolyString>> values = propDelta.getValues(PolyString.class);
                        for (PrismPropertyValue<PolyString> pval : values) {
                            PolyString value = pval.getValue();
                            LOGGER.trace("NAME delta: {} - {}", value.getOrig(), value.getNorm());
                        }
                    }
                }
            }
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
        LOGGER.debug("Modifying object '{}' with oid '{}'.", new Object[]{type.getSimpleName(), oid});
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Modifications:\n{}", new Object[]{DebugUtil.debugDump(modifications)});
        }

        Session session = null;
        try {
            session = beginTransaction();

            // get user
            PrismObject<T> prismObject = getObject(session, type, oid, null, true);
            // apply diff
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT before:\n{}", new Object[]{prismObject.debugDump()});
            }
            ItemDelta.applyTo(modifications, prismObject);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT after:\n{}", prismObject.debugDump());
            }
            // merge and update user
            LOGGER.trace("Translating JAXB to data type.");
            RObject rObject = createDataObjectFromJAXB(prismObject, false);
            rObject.setVersion(rObject.getVersion() + 1);

            updateFullObject(rObject, prismObject);
            session.merge(rObject);

            //todo finish orgClosureManager
            //orgClosureManager.updateOrgClosure(modifications, session, oid, type, OrgClosureManager.Operation.MODIFY);

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
        } catch (QueryException | DtoTranslationException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
            LOGGER.trace("Session cleaned up.");
        }
    }

    private <T extends ObjectType> void recomputeHierarchy(
            RObject<T> rObjectToModify, Session session,
            Collection<? extends ItemDelta> modifications)
            throws SchemaException, DtoTranslationException {

        for (ItemDelta delta : modifications) {
            if (!QNameUtil.match(delta.getElementName(), OrgType.F_PARENT_ORG_REF)) continue;

            // if modification is one of the modify or delete, delete old
            // record in org closure table and in the next step fill the
            // closure table with the new records
            if (delta.isReplace() || delta.isDelete()) {
                for (Object orgRefDValue : delta.getValuesToDelete()) {
                    if (!(orgRefDValue instanceof PrismReferenceValue))
                        throw new SchemaException("Couldn't modify organization structure hierarchy (adding new " +
                                "records). Expected instance of prism reference value but got " + orgRefDValue);

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
            } else if (delta.isAdd()) {
                // fill closure table with new transitive relations
                for (Object orgRefDValue : delta.getValuesToAdd()) {
                    if (!(orgRefDValue instanceof PrismReferenceValue)) {
                        throw new SchemaException(
                                "Couldn't modify organization structure hierarchy (adding new records). Expected " +
                                        "instance of prism reference value but got " + orgRefDValue
                        );
                    }

                    PrismReferenceValue value = (PrismReferenceValue) orgRefDValue;

                    LOGGER.trace("filling transitive hierarchy for descendant {}, ref {}",
                            new Object[]{rObjectToModify.getOid(), value.getOid()});
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
            LOGGER.trace("No ancestors for object: {}", rObjectToModify.getOid());
        }

        if (ocDescendant != null && !ocDescendant.isEmpty()) {
            cOrgClosure.add(Restrictions.in("descendant", ocDescendant));
        } else {
            LOGGER.trace("No descendants for object: {}", rObjectToModify.getOid());
        }

        List<ROrgClosure> orgClosure = cOrgClosure.list();

        for (ROrgClosure o : orgClosure) {
            if (LOGGER.isTraceEnabled()) {
                RObject ancestor = o.getAncestor();
                RObject descendant = o.getDescendant();
                LOGGER.trace("deleting from hierarchy: A:{} D:{} depth:{}",
                        new Object[]{RUtil.getDebugString(ancestor), RUtil.getDebugString(descendant), o.getDepth()});
            }
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

    private void deleteHierarchy(RObject objectToDelete, Session session) {
        session.getNamedQuery("sqlDeleteOrgClosure").setParameter("oid", objectToDelete.getOid()).executeUpdate();
        session.getNamedQuery("sqlDeleteOrgIncorrect").setParameter("oid", objectToDelete.getOid()).executeUpdate();
    }

    @Override
    public <T extends ShadowType> List<PrismObject<T>> listResourceObjectShadows(String resourceOid,
                                                                                 Class<T> resourceObjectShadowType,
                                                                                 OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
        Validate.notNull(resourceObjectShadowType, "Resource object shadow type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Listing resource object shadows '{}' for resource '{}'.",
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

        List<PrismObject<T>> list = new ArrayList<>();
        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            Query query = session.getNamedQuery("listResourceObjectShadows");
            query.setString("oid", resourceOid);
            query.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);

            List<GetObjectResult> shadows = query.list();
            LOGGER.debug("Query returned {} shadows, transforming to JAXB types.",
                    new Object[]{(shadows != null ? shadows.size() : 0)});

            if (shadows != null) {
                for (GetObjectResult shadow : shadows) {
                    PrismObject<T> prismObject = updateLoadedObject(shadow, resourceObjectShadowType, null, session);
                    list.add(prismObject);
                }
            }
            session.getTransaction().commit();
        } catch (SchemaException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
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
        if (InternalsConfig.readEncryptionChecks) {
            CryptoUtil.checkEncrypted(prismObject);
        }
    }

    private <T extends ObjectType> RObject createDataObjectFromJAXB(PrismObject<T> prismObject, boolean add)
            throws SchemaException {

        PrismIdentifierGenerator generator = new PrismIdentifierGenerator();
        generator.generate(prismObject);

        T object = prismObject.asObjectable();

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

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.repo.api.RepositoryService#getRepositoryDiag()
     */
    @Override
    public RepositoryDiag getRepositoryDiag() {
        LOGGER.debug("Getting repository diagnostics.");

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

        List<LabeledString> details = new ArrayList<>();
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
     * @see com.evolveum.midpoint.repo.api.RepositoryService#getVersion(java.lang.Class, java.lang.String,
     * com.evolveum.midpoint.schema.result.OperationResult)
     */
    @Override
    public <T extends ObjectType> String getVersion(Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(oid, "Object oid must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.debug("Getting version for {} with oid '{}'.", new Object[]{type.getSimpleName(), oid});

        OperationResult subResult = parentResult.createMinorSubresult(GET_VERSION);
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
            Query query = session.getNamedQuery("getVersion");
            query.setString("oid", oid);

            Number versionLong = (Number) query.uniqueResult();
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
                                                              ResultHandler<T> handler,
                                                              Collection<SelectorOptions<GetOperationOptions>> options,
                                                              OperationResult result)
            throws SchemaException {

        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(handler, "Result handler must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        logSearchInputParameters(type, query, true);

        OperationResult subResult = result.createSubresult(SEARCH_OBJECTS_ITERATIVE);
        subResult.addParam("type", type.getName());
        subResult.addParam("query", query);

        if (getConfiguration().isIterativeSearchByPaging()) {
            searchObjectsIterativeByPaging(type, query, handler, options, subResult);
            return;
        }

//        turned off until resolved 'unfinished operation' warning
//        SqlPerformanceMonitor pm = getPerformanceMonitor();
//        long opHandle = pm.registerOperationStart(SEARCH_OBJECTS_ITERATIVE);

        final String operation = "searching iterative";
        int attempt = 1;
        try {
            while (true) {
                try {
                    searchObjectsIterativeAttempt(type, query, handler, options, subResult);
                    return;
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(null, operation, attempt, ex, subResult);
//                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
//            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private <T extends ObjectType> void searchObjectsIterativeAttempt(Class<T> type, ObjectQuery query,
                                                                      ResultHandler<T> handler,
                                                                      Collection<SelectorOptions<GetOperationOptions>> options,
                                                                      OperationResult result)
            throws SchemaException {

        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            QueryEngine engine = new QueryEngine(getConfiguration(), getPrismContext());
            RQuery rQuery = engine.interpret(query, type, options, false, session);

            ScrollableResults results = rQuery.scroll(ScrollMode.FORWARD_ONLY);
            try {
                Iterator<GetObjectResult> iterator = new ScrollableResultsIterator(results);
                while (iterator.hasNext()) {
                    GetObjectResult object = iterator.next();

                    PrismObject<T> prismObject = updateLoadedObject(object, type, options, session);
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
        } catch (SchemaException | QueryException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }
    }

    private <T extends ObjectType> void searchObjectsIterativeByPaging(Class<T> type, ObjectQuery query,
                                                                       ResultHandler<T> handler,
                                                                       Collection<SelectorOptions<GetOperationOptions>> options,
                                                                       OperationResult result)
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

                List<PrismObject<T>> objects = searchObjects(type, pagedQuery, options, result);

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
	public boolean isAnySubordinate(String upperOrgOid, Collection<String> lowerObjectOids) throws SchemaException {
		Validate.notNull(upperOrgOid, "upperOrgOid must not be null.");
        Validate.notNull(lowerObjectOids, "lowerObjectOids must not be null.");

        if (LOGGER.isTraceEnabled()) LOGGER.trace("Querying for subordination upper {}, lower {}", new Object[]{upperOrgOid, lowerObjectOids});

        if (lowerObjectOids.isEmpty()) {
        	// trivial case
        	return false;
        }
        
        int attempt = 1;

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("matchObject");
        try {
            while (true) {
                try {
                    return isAnySubordinateAttempt(upperOrgOid, lowerObjectOids);
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(upperOrgOid, "isAnySubordinate", attempt, ex, null);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
	}
    
	private boolean isAnySubordinateAttempt(String upperOrgOid, Collection<String> lowerObjectOids) {
		Session session = null;
        try {
            session = beginTransaction();

            Query query;
            if (lowerObjectOids.size() == 1) {
                query = session.getNamedQuery("isAnySubordinateAttempt.oneLowerOid");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("select count(*) from ROrgClosure o where ");

                sb.append('(');
                Iterator<String> iterator = lowerObjectOids.iterator();
                int paramIndex = 0;
                while (iterator.hasNext()) {
                    iterator.next();
                    sb.append("(o.ancestorOid=:aOid").append(paramIndex);
                    sb.append(" and o.descendantOid=:dOid").append(paramIndex);
                    sb.append(')');
                    paramIndex++;
                    if (iterator.hasNext()) {
                        sb.append(" or ");
                    }
                }
                sb.append(')');

                query = session.createQuery(sb.toString());
            }

            Iterator<String> iterator = lowerObjectOids.iterator();
            int paramIndex = 0;
            while (iterator.hasNext()) {
            	String subOid = iterator.next();
            	query.setString("aOid" + paramIndex, upperOrgOid);
            	query.setString("dOid" + paramIndex, subOid);
            	paramIndex++;
            }
            
            Number number = (Number) query.uniqueResult();
            return (number != null && number.longValue() != 0L) ? true : false;
        } catch (RuntimeException ex) {
            handleGeneralException(ex, session, null);
        } finally {
            cleanupSessionAndResult(session, null);
        }

        throw new SystemException("isAnySubordinateAttempt failed somehow, this really should not happen.");
    }
}

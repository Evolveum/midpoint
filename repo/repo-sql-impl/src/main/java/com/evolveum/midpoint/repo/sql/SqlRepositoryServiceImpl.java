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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.repo.sql.query.*;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.*;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.HibernateOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.w3c.dom.Element;

import java.lang.InstantiationException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
@Repository
public class SqlRepositoryServiceImpl implements RepositoryService {

    private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryServiceImpl.class);
    // how many times we want to repeat operation after lock aquisition,
    // pessimistic, optimistic exception
    private static final int LOCKING_MAX_ATTEMPTS = 5;
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
        fixCompositeIdentifierInMetaModel(RObjectReferenceTaskObject.class);
        fixCompositeIdentifierInMetaModel(RObjectReferenceTaskOwner.class);

        fixCompositeIdentifierInMetaModel(RAssignment.class);
        fixCompositeIdentifierInMetaModel(RExclusion.class);
        for (RContainerType type : ClassMapper.getKnownTypes()) {
            fixCompositeIdentifierInMetaModel(type.getClazz());
        }
        // END HAC

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

    private <T extends ObjectType> PrismObject<T> getObject(Session session, Class<T> type, String oid,
            PropertyReferenceListType resolve) throws ObjectNotFoundException, SchemaException,
            DtoTranslationException {
        Criteria query = session.createCriteria(ClassMapper.getHQLTypeClass(type));
        query.add(Restrictions.eq("oid", oid));
        query.add(Restrictions.eq("id", 0L));
        updateResultFetchInCriteria(query, type, resolve);

        RObject object = (RObject) query.uniqueResult();
        if (object == null) {
            throw new ObjectNotFoundException("Object of type '" + type.getSimpleName() + "' with oid '"
                    + oid + "' was not found.", null, oid);
        }

        LOGGER.debug("Transforming data to JAXB type.");
        PrismObject<T> objectType = object.toJAXB(prismContext).asPrismObject();
        validateObjectType(objectType, type);

        return objectType;
    }

    @Override
    public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid, PropertyReferenceListType resolve,
            OperationResult result) throws ObjectNotFoundException, SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Getting object '{}' with oid '{}'.", new Object[]{type.getSimpleName(), oid});
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resolving\n{}", new Object[]{prismContext.silentMarshalObject(resolve)});
        }

        PrismObject<T> objectType = null;
        OperationResult subResult = result.createSubresult(GET_OBJECT);
        Session session = null;
        try {
            session = beginTransaction();

            objectType = getObject(session, type, oid, resolve);

            session.getTransaction().commit();
        } catch (ObjectNotFoundException ex) {
            throw ex;
        } catch (NonUniqueResultException ex) {
            rollbackTransaction(session);
            throw new SystemException("There are more objects of type '"
                    + type.getSimpleName() + "' with oid '" + oid + "': " + ex.getMessage(), ex);
        } catch (DtoTranslationException ex) {
            rollbackTransaction(session);
            throw new SchemaException(ex.getMessage(), ex);
        } catch (SystemException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (Exception ex) {
            rollbackTransaction(session);
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            cleanupSessionAndResult(session, subResult);
        }

        return objectType;
    }

    private <T extends ObjectType> void updateResultFetchInCriteria(Criteria criteria, Class<T> type,
            PropertyReferenceListType resolve) {

        if (resolve == null || resolve.getProperty().isEmpty()) {
            return;
        }

        //resolving properties
        try {
            QueryRegistry registry = QueryRegistry.getInstance();
            EntityDefinition definition = registry.findDefinition(ObjectTypes.getObjectType(type).getQName());
            for (Element property : resolve.getProperty()) {
                PropertyPath path = new XPathHolder(property).toPropertyPath();
                if (path == null || path.size() != 1) {
                    LOGGER.warn("Resolving property path with size not equal 1 is not supported '"
                            + path + "'.");
                    continue;
                }
                Definition def = definition.findDefinition(path.first().getName());
                if (def == null) {
                    LOGGER.warn("Unknown path '" + path + "', couldn't find definition for it, will not be resolved.");
                    continue;
                }
                criteria.setFetchMode(def.getRealName(), FetchMode.JOIN);
            }
        } catch (QueryException ex) {
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    @Deprecated
    @Override
    public <T extends ObjectType> List<PrismObject<T>> listObjects(Class<T> type, PagingType paging,
            OperationResult result) {
        try {
            return searchObjects(type, null, paging, result);
        } catch (SchemaException ex) {
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    @Override
    public PrismObject<UserType> listAccountShadowOwner(String accountOid, OperationResult result)
            throws ObjectNotFoundException {
        Validate.notEmpty(accountOid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        UserType userType = null;
        OperationResult subResult = result.createSubresult(LIST_ACCOUNT_SHADOW);
        Session session = null;
        try {
            session = beginTransaction();
            LOGGER.debug("Selecting account shadow owner for account {}.", new Object[]{accountOid});
            Query query = session.createQuery("select user from " + ClassMapper.getHQLType(UserType.class)
                    + " as user left join user.accountRefs as ref where ref.targetOid = :oid");
            query.setString("oid", accountOid);

            List<RUser> users = query.list();
            LOGGER.debug("Found {} users, transforming data to JAXB types.",
                    new Object[]{(users != null ? users.size() : 0)});

            if (users == null || users.isEmpty()) {
                throw new ObjectNotFoundException("Account shadow owner for account '"
                        + accountOid + "' was not found.");
            }

            if (users.size() > 1) {
                LOGGER.warn("Found {} users for account oid {}, returning first user. [interface change needed]",
                        new Object[]{users.size(), accountOid});
            }

            RUser user = users.get(0);
            userType = user.toJAXB(prismContext);

            session.getTransaction().commit();
        } catch (ObjectNotFoundException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (SystemException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (Exception ex) {
            rollbackTransaction(session);
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            cleanupSessionAndResult(session, subResult);
        }

        return userType.asPrismObject();
    }

    private void validateName(PrismObject object) throws SchemaException {
        PrismProperty name = object.findProperty(ObjectType.F_NAME);
        if (name == null || StringUtils.isEmpty((String) name.getRealValue())) {
            throw new SchemaException("Attempt to add object without name.");
        }
    }

    @Override
    public <T extends ObjectType> String addObject(PrismObject<T> object, OperationResult result) throws
            ObjectAlreadyExistsException, SchemaException {
        Validate.notNull(object, "Object must not be null.");
        validateName(object);
        Validate.notNull(result, "Operation result must not be null.");

        final String operation = "adding";
        int attempt = 1;

        String oid = object.getOid();
        while (true) {
            try {
                return addObjectAttempt(object, result);
            } catch (PessimisticLockException ex) {
                attempt = logOperationAttempt(oid, operation, attempt, ex);
            } catch (LockAcquisitionException ex) {
                attempt = logOperationAttempt(oid, operation, attempt, ex);
            } catch (HibernateOptimisticLockingFailureException ex) {
                attempt = logOperationAttempt(oid, operation, attempt, ex);
            }
        }
    }

    private <T extends ObjectType> void checkNameUniqueness(Session session, Class<T> type, PrismObject object)
            throws ObjectAlreadyExistsException {
        LOGGER.debug("Checking name uniqueness.");

        String name = null;
        if (object.findProperty(ObjectType.F_NAME) != null) {
            name = (String) object.findProperty(ObjectType.F_NAME).getRealValue();
        }

        if (StringUtils.isEmpty(name)) {
            throw new SystemException("Name in object must not be null or empty.");
        }

        Criteria criteria = session.createCriteria(ClassMapper.getHQLTypeClass(type), "o");
        criteria.add(Restrictions.eq("o.name", name));
        criteria.setProjection(Projections.rowCount());

        Long objectsCount = (Long) criteria.uniqueResult();
        if (objectsCount == null || objectsCount != 0) {
            throw new ObjectAlreadyExistsException("Object with the same name already exists.");
        }
    }

    private <T extends ObjectType> String addObjectAttempt(PrismObject<T> object, OperationResult result) throws
            ObjectAlreadyExistsException, SchemaException {
        LOGGER.debug("Adding object type '{}'", new Object[]{object.getClass().getSimpleName()});

        String oid = null;
        OperationResult subResult = result.createSubresult(ADD_OBJECT);
        Session session = null;
        try {
            ObjectType objectType = object.asObjectable();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Object\n{}", new Object[]{prismContext.silentMarshalObject(objectType)});
            }

            //check name uniqueness (by type)
            session = beginTransaction();
            checkNameUniqueness(session, object.getCompileTimeClass(), object);

            LOGGER.debug("Translating JAXB to data type.");
            RObject rObject = createDataObjectFromJAXB(objectType);

            LOGGER.debug("Saving object.");
            RContainerId containerId = (RContainerId) session.save(rObject);
            oid = containerId.getOid();
            session.getTransaction().commit();

            LOGGER.debug("Saved object '{}' with oid '{}'",
                    new Object[]{object.getCompileTimeClass().getSimpleName(), oid});
        } catch (PessimisticLockException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (LockAcquisitionException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (HibernateOptimisticLockingFailureException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (ObjectAlreadyExistsException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (ConstraintViolationException ex) {
            rollbackTransaction(session);
            throw new ObjectAlreadyExistsException("Object with oid '" + object.getOid() + "' already exists.", ex);
        } catch (SystemException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (Exception ex) {
            rollbackTransaction(session);
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            cleanupSessionAndResult(session, subResult);
        }

        return oid;
    }

    @Override
    public <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult result) throws
            ObjectNotFoundException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        final String operation = "deleting";
        int attempt = 1;

        while (true) {
            try {
                deleteObjectAttempt(type, oid, result);
                return;
            } catch (PessimisticLockException ex) {
                attempt = logOperationAttempt(oid, operation, attempt, ex);
            } catch (LockAcquisitionException ex) {
                attempt = logOperationAttempt(oid, operation, attempt, ex);
            } catch (HibernateOptimisticLockingFailureException ex) {
                attempt = logOperationAttempt(oid, operation, attempt, ex);
            }
        }
    }

    private <T extends ObjectType> void deleteObjectAttempt(Class<T> type, String oid, OperationResult result) throws
            ObjectNotFoundException {
        LOGGER.debug("Deleting object type '{}' with oid '{}'", new Object[]{type.getSimpleName(), oid});

        OperationResult subResult = result.createSubresult(DELETE_OBJECT);
        Session session = null;
        try {
            session = beginTransaction();

            Criteria query = session.createCriteria(ClassMapper.getHQLTypeClass(type));
            query.add(Restrictions.eq("oid", oid));
            query.add(Restrictions.eq("id", 0L));
            RObject object = (RObject) query.uniqueResult();
            if (object == null) {
                throw new ObjectNotFoundException("Object of type '" + type.getSimpleName() + "' with oid '"
                        + oid + "' was not found.", null, oid);
            }
            session.delete(object);
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
        } catch (ObjectNotFoundException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (SystemException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (Exception ex) {
            rollbackTransaction(session);
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            cleanupSessionAndResult(session, subResult);
        }
    }

    @Deprecated
    @Override
    public void claimTask(String oid, OperationResult result) throws ObjectNotFoundException,
            ConcurrencyException, SchemaException {
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        OperationResult subResult = result.createSubresult(CLAIM_TASK);
        updateTaskExclusivity(oid, TaskExclusivityStatusType.CLAIMED, subResult);
    }

    @Deprecated
    @Override
    public void releaseTask(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        OperationResult subResult = result.createSubresult(RELEASE_TASK);
        updateTaskExclusivity(oid, TaskExclusivityStatusType.RELEASED, subResult);
    }

    @Override
    public <T extends ObjectType> int countObjects(Class<T> type, QueryType query, OperationResult result) {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Counting objects of type '{}', query (on trace level).",
                new Object[]{type});
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Full query\n{}", new Object[]{
                    (query == null ? "undefined" : prismContext.silentMarshalObject(query))});
        }

        int count = 0;
        OperationResult subResult = result.createSubresult(COUNT_OBJECTS);
        Session session = null;
        try {
            session = beginTransaction();
            LOGGER.debug("Updating query criteria.");
            Criteria criteria;
            if (query != null && query.getFilter() != null) {
                QueryInterpreter interpreter = new QueryInterpreter(session, type, prismContext);
                criteria = interpreter.interpret(query.getFilter());
            } else {
                criteria = session.createCriteria(ClassMapper.getHQLTypeClass(type));
            }
            criteria.setProjection(Projections.rowCount());

            LOGGER.debug("Selecting total count.");
            Long longCount = (Long) criteria.uniqueResult();
            count = longCount.intValue();
        } catch (SystemException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (Exception ex) {
            rollbackTransaction(session);
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            cleanupSessionAndResult(session, subResult);
        }

        return count;
    }

    @Override
    public <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, QueryType query,
            PagingType paging, OperationResult result) throws SchemaException {

        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Searching objects of type '{}', query (on trace level), offset {}, count {}.", new Object[]{
                type.getSimpleName(), (paging == null ? "undefined" : paging.getOffset()),
                (paging == null ? "undefined" : paging.getMaxSize())});
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Full query\n{}\nFull paging\n{}", new Object[]{
                    (query == null ? "undefined" : prismContext.silentMarshalObject(query)),
                    (paging == null ? "undefined" : prismContext.silentMarshalObject(paging))});
        }

        OperationResult subResult = result.createSubresult(SEARCH_OBJECTS);
        List<PrismObject<T>> list = new ArrayList<PrismObject<T>>();
        Session session = null;
        try {
            session = beginTransaction();
            LOGGER.debug("Updating query criteria.");
            Criteria criteria;
            if (query != null && query.getFilter() != null) {
                QueryInterpreter interpreter = new QueryInterpreter(session, type, prismContext);
                criteria = interpreter.interpret(query.getFilter());
            } else {
                criteria = session.createCriteria(ClassMapper.getHQLTypeClass(type));
            }

            criteria = updatePagingAndSorting(criteria, type, paging);

            List<RObject> objects = criteria.list();
            LOGGER.debug("Found {} objects, translating to JAXB.",
                    new Object[]{(objects != null ? objects.size() : 0)});

            for (RObject object : objects) {
                ObjectType objectType = object.toJAXB(prismContext);
                PrismObject<T> prismObject = objectType.asPrismObject();
                validateObjectType(prismObject, type);
                list.add(prismObject);
            }

            session.getTransaction().commit();
        } catch (SystemException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (Exception ex) {
            rollbackTransaction(session);
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            cleanupSessionAndResult(session, subResult);
        }

        return list;
    }

    @Override
    public <T extends ObjectType> void modifyObject(Class<T> type, String oid,
            Collection<? extends ItemDelta> modifications,
            OperationResult result) throws ObjectNotFoundException, SchemaException {
        Validate.notNull(modifications, "Modifications must not be null.");
        Validate.notNull(type, "Object class in delta must not be null.");
        Validate.notEmpty(oid, "Oid must not null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        final String operation = "modifying";
        int attempt = 1;

        while (true) {
            try {
                modifyObjectAttempt(type, oid, modifications, result);
                return;
            } catch (PessimisticLockException ex) {
                attempt = logOperationAttempt(oid, operation, attempt, ex);
            } catch (LockAcquisitionException ex) {
                attempt = logOperationAttempt(oid, operation, attempt, ex);
            } catch (HibernateOptimisticLockingFailureException ex) {
                attempt = logOperationAttempt(oid, operation, attempt, ex);
            }
        }
    }

    private int logOperationAttempt(String oid, String operation, int attempt, Exception ex) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("A locking-related problem occurred when {} object with oid '{}', retrying after "
                    + "{}ms (this was attempt {} of {})\n{}: {}", new Object[]{operation, oid, LOCKING_TIMEOUT,
                    attempt, LOCKING_MAX_ATTEMPTS, ex.getClass().getSimpleName(), ex.getMessage()});
        }

        if (attempt > LOCKING_MAX_ATTEMPTS) {
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

    private boolean hasNameChanged(Collection<? extends ItemDelta> modifications) {
        for (ItemDelta delta : modifications) {
            PropertyPath parent = delta.getParentPath();
            if ((parent == null || parent.isEmpty()) && ObjectType.F_NAME.equals(delta.getName())) {
                return true;
            }
        }

        return false;
    }

    private <T extends ObjectType> void modifyObjectAttempt(Class<T> type, String oid,
            Collection<? extends ItemDelta> modifications, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        LOGGER.debug("Modifying object '{}' with oid '{}'.", new Object[]{type.getSimpleName(), oid});
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Modifications: {}", new Object[]{DebugUtil.prettyPrint(modifications)});
        }

        OperationResult subResult = result.createSubresult(MODIFY_OBJECT);
        Session session = null;
        try {
            session = beginTransaction();

            //get user
            PrismObject<T> prismObject = getObject(session, type, oid, null);

            //apply diff
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT before:\n{}", new Object[]{prismObject.dump()});
            }
            PropertyDelta.applyTo(modifications, prismObject);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT after:\n{}", prismObject.dump());
            }

            //check name uniqueness if necessary
            if (hasNameChanged(modifications)) {
                checkNameUniqueness(session, type, prismObject);
            }

            //merge and update user
            LOGGER.debug("Translating JAXB to data type.");
            RObject rObject = createDataObjectFromJAXB(prismObject.asObjectable());
            session.merge(rObject);

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
        } catch (ObjectNotFoundException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (SystemException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (Exception ex) {
            rollbackTransaction(session);
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            cleanupSessionAndResult(session, subResult);
        }
    }

    @Override
    public <T extends ResourceObjectShadowType> List<PrismObject<T>> listResourceObjectShadows(String resourceOid,
            Class<T> resourceObjectShadowType, OperationResult result) throws ObjectNotFoundException {
        Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
        Validate.notNull(resourceObjectShadowType, "Resource object shadow type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Listing resource object shadows '{}' for resource '{}'.",
                new Object[]{resourceObjectShadowType.getSimpleName(), resourceOid});
        OperationResult subResult = result.createSubresult(LIST_RESOURCE_OBJECT_SHADOWS);

        List<PrismObject<T>> list = new ArrayList<PrismObject<T>>();
        Session session = null;
        try {
            session = beginTransaction();
            Query query = session.createQuery("select shadow from " + ClassMapper.getHQLType(resourceObjectShadowType)
                    + " as shadow left join shadow.resourceRef as ref where ref.oid = :oid");
            query.setString("oid", resourceOid);

            List<RResourceObjectShadow> shadows = query.list();
            LOGGER.debug("Query returned {} shadows, transforming to JAXB types.",
                    new Object[]{(shadows != null ? shadows.size() : 0)});

            if (shadows != null) {
                for (RResourceObjectShadow shadow : shadows) {
                    ResourceObjectShadowType jaxb = shadow.toJAXB(prismContext);
                    PrismObject<T> prismObject = jaxb.asPrismObject();
                    validateObjectType(prismObject, resourceObjectShadowType);

                    list.add(prismObject);
                }
            }
            session.getTransaction().commit();
            LOGGER.debug("Done.");
        } catch (SystemException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (Exception ex) {
            rollbackTransaction(session);
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            cleanupSessionAndResult(session, subResult);
        }

        return list;
    }

    @Deprecated
    private void updateTaskExclusivity(String oid, TaskExclusivityStatusType newStatus, OperationResult result)
            throws ObjectNotFoundException {

        LOGGER.debug("Updating task '{}' exclusivity to '{}'", new Object[]{oid, newStatus});
        Session session = null;
        try {
            LOGGER.debug("Looking for task.");
            session = beginTransaction();
            Query query = session.createQuery("from " + ClassMapper.getHQLType(TaskType.class)
                    + " as task where task.oid = :oid and task.id = 0");
            query.setString("oid", oid);

            RTask task = (RTask) query.uniqueResult();
            if (task == null) {
                throw new ObjectNotFoundException("Task with oid '" + oid + "' was not found.");
            }
            LOGGER.debug("Task found, updating exclusivity status.");
            task.setExclusivityStatus(newStatus);
            session.save(task);

            session.getTransaction().commit();
            LOGGER.debug("Task status updated.");
        } catch (HibernateOptimisticLockingFailureException ex) {
            rollbackTransaction(session);
            throw new SystemException(ex.getMessage(), ex);
        } catch (ObjectNotFoundException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (SystemException ex) {
            rollbackTransaction(session);
            throw ex;
        } catch (Exception ex) {
            rollbackTransaction(session);
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            cleanupSessionAndResult(session, result);
        }
    }

    private <T extends ObjectType> void validateObjectType(PrismObject<T> prismObject, Class<T> type) {
        if (prismObject == null || !type.isAssignableFrom(prismObject.getCompileTimeClass())) {
            throw new SystemException("Result ('" + prismObject.toDebugName() + "') is not assignable to '"
                    + type.getSimpleName() + "' [really should not happen].");
        }
    }

    private <T extends ObjectType> RObject createDataObjectFromJAXB(T object) throws SchemaException {

        RObject rObject;
        Class<? extends RObject> clazz = ClassMapper.getHQLTypeClass(object.getClass());
        try {
            rObject = clazz.newInstance();
            Method method = clazz.getMethod("copyFromJAXB", object.getClass(), clazz, PrismContext.class);
            method.invoke(clazz, object, rObject, prismContext);
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (StringUtils.isEmpty(message) && ex.getCause() != null) {
                message = ex.getCause().getMessage();
            }
            throw new SchemaException(message, ex);
        }

        return rObject;
    }

    private <T extends ObjectType> Criteria updatePagingAndSorting(Criteria query, Class<T> type, PagingType paging) {
        if (paging == null) {
            return query;
        }
        if (paging.getOffset() != null) {
            query = query.setFirstResult(paging.getOffset());
        }
        if (paging.getMaxSize() != null) {
            query = query.setMaxResults(paging.getMaxSize());
        }

        if (paging.getOrderDirection() == null && paging.getOrderBy() == null) {
            return query;
        }

        try {
            QueryRegistry registry = QueryRegistry.getInstance();
            PropertyPath path = new XPathHolder(paging.getOrderBy()).toPropertyPath();
            if (path == null || path.size() != 1) {
                LOGGER.warn("Ordering by property path with size not equal 1 is not supported '"
                        + path + "'.");
                return query;
            }
            EntityDefinition definition = registry.findDefinition(ObjectTypes.getObjectType(type).getQName());
            Definition def = definition.findDefinition(path.first().getName());
            if (def == null) {
                LOGGER.warn("Unknown path '" + path + "', couldn't find definition for it, "
                        + "list will not be ordered by it.");
                return query;
            }

            switch (paging.getOrderDirection()) {
                case ASCENDING:
                    query = query.addOrder(Order.asc(def.getRealName()));
                    break;
                case DESCENDING:
                    query = query.addOrder(Order.desc(def.getRealName()));
            }
        } catch (QueryException ex) {
            throw new SystemException(ex.getMessage(), ex);
        }

        return query;
    }

    private Session beginTransaction() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();

        return session;
    }

    private void rollbackTransaction(Session session) {
        if (session == null || session.getTransaction() == null || !session.getTransaction().isActive()) {
            return;
        }

        session.getTransaction().rollback();
    }

    private void cleanupSessionAndResult(Session session, OperationResult result) {
        if (session != null && session.isOpen()) {
            session.close();
        }

        result.computeStatus();
    }
}

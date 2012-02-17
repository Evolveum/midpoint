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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.common.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.RResourceObjectShadowType;
import com.evolveum.midpoint.repo.sql.data.common.RTaskType;
import com.evolveum.midpoint.repo.sql.data.common.RUserType;
import com.evolveum.midpoint.repo.sql.query.QueryProcessor;
import com.evolveum.midpoint.schema.ResultArrayList;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.Validate;
import org.hibernate.*;
import org.hibernate.criterion.Projections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.lang.InstantiationException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author lazyman
 */
@Repository
public class SqlRepositoryServiceImpl implements RepositoryService {

    private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryServiceImpl.class);

    @Autowired(required = true)
    private SchemaRegistry schemaRegistry;

    @Autowired(required = true)
    SessionFactory sessionFactory;

    @Override
    public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid, PropertyReferenceListType resolve,
            OperationResult result) throws ObjectNotFoundException, SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        ObjectType objectType = null;
        OperationResult subResult = result.createSubresult(GET_OBJECT);
        Session session = null;
        try {
            LOGGER.debug("Getting object '{}' with oid '{}'.", new Object[]{type.getSimpleName(), oid});
            session = beginTransaction();
            Query query = session.createQuery("from " + ClassMapper.getHQLType(type) + " o where o.oid = :oid");
            query.setString("oid", oid);

            RObjectType object = (RObjectType) query.uniqueResult();
            if (object == null) {
                throw new ObjectNotFoundException("Object of type '" + type.getSimpleName() + "' with oid '"
                        + oid + "' was not found.", null, oid);
            }

            LOGGER.debug("Transforming data to JAXB type.");
            objectType = object.toJAXB();

            if (resolve != null && !resolve.getProperty().isEmpty()) {
                //todo we have to resolve something...
            }
            session.getTransaction().commit();

            validateObjectType(objectType, type);
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

        return objectType.getContainer();
    }

    @Override
    public <T extends ObjectType> ResultList<PrismObject<T>> listObjects(Class<T> type, PagingType paging,
            OperationResult result) {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        ResultList<PrismObject<T>> results = new ResultArrayList<PrismObject<T>>();
        OperationResult subResult = result.createSubresult(LIST_OBJECTS);
        Session session = null;
        try {
            session = beginTransaction();
            LOGGER.debug("Selecting total count.");
            Query query = session.createQuery("select count(o) from " + ClassMapper.getHQLType(type) + " as o");
            Long count = (Long) query.uniqueResult();
            results.setTotalResultCount(count.intValue());  //todo ResultList must have long value

            //todo sort by and asc or desc
            LOGGER.debug("Count is {}, selecting objects.", new Object[]{count});
            query = session.createQuery("from " + ClassMapper.getHQLType(type) + " as o");
            query = updatePaging(query, paging);

            LOGGER.debug("Transforming data to JAXB types.");
            List<? extends RObjectType> objects = query.list();
            if (objects != null) {
                for (RObjectType object : objects) {
                    ObjectType objectType = object.toJAXB();
                    validateObjectType(objectType, type);
                    results.add(objectType.getContainer());
                }
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

        return results;
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
            Query query = session.createQuery("select user from RUserType as user left join user.accountRef " +
                    "as ref where ref.oid = :oid");
            query.setString("oid", accountOid);

            List<RUserType> users = query.list();
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

            RUserType user = users.get(0);
            userType = user.toJAXB();

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

        return userType.getContainer();
    }

    @Override
    public <T extends ObjectType> String addObject(PrismObject<T> object, OperationResult result) throws
            ObjectAlreadyExistsException, SchemaException {

        Validate.notNull(object, "Object must not be null.");
        Validate.notNull(result, "Operation result must not be null.");
        LOGGER.debug("Adding object type '{}'", new Object[]{object.getClass().getSimpleName()});

        String oid = null;
        OperationResult subResult = result.createSubresult(ADD_OBJECT);
        Session session = null;
        try {
            LOGGER.debug("Translating JAXB to data type.");
            RObjectType rObject = createDataObjectFromJAXB(object.getObjectable());

            LOGGER.debug("Saving object.");
            session = beginTransaction();
            oid = (String) session.save(rObject);
            session.getTransaction().commit();

            LOGGER.debug("Saved object '{}' with oid '{}'", new Object[]{object.getClass().getSimpleName(), oid});
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
        LOGGER.debug("Deleting object type '{}' with oid '{}'", new Object[]{type.getSimpleName(), oid});

        OperationResult subResult = result.createSubresult(DELETE_OBJECT);
        Session session = null;
        try {
            session = beginTransaction();
            Query query = session.createQuery("delete from " + ClassMapper.getHQLType(type)
                    + " as user where user.oid = :oid");
            query.setString("oid", oid);

            int count = query.executeUpdate();
            session.getTransaction().commit();

            LOGGER.debug("Deleted was {} object(s).", new Object[]{count});
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
    public void claimTask(String oid, OperationResult result) throws ObjectNotFoundException,
            ConcurrencyException, SchemaException {
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        OperationResult subResult = result.createSubresult(CLAIM_TASK);
        updateTaskExclusivity(oid, TaskExclusivityStatusType.CLAIMED, subResult);
    }

    @Override
    public void releaseTask(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        OperationResult subResult = result.createSubresult(RELEASE_TASK);
        updateTaskExclusivity(oid, TaskExclusivityStatusType.RELEASED, subResult);
    }

    @Override
    public <T extends ObjectType> ResultList<PrismObject<T>> searchObjects(Class<T> type, QueryType query,
            PagingType paging, OperationResult result) throws SchemaException {

        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(query, "Query must not be null.");
        Validate.notNull(query.getFilter(), "Query filter must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        OperationResult subResult = result.createSubresult(SEARCH_OBJECTS);

        ResultList<PrismObject<T>> list = new ResultArrayList<PrismObject<T>>();
        Session session = null;
        try {
            session = beginTransaction();
            Criteria criteria = session.createCriteria(ClassMapper.getHQLTypeClass(type));
            LOGGER.debug("Updating query criteria.");
            criteria = new QueryProcessor().createFilterCriteria(criteria, query.getFilter());
            criteria.setProjection(Projections.rowCount());

            LOGGER.debug("Selecting total count.");
            Long count = (Long) criteria.uniqueResult();
            list.setTotalResultCount(count.intValue());

            LOGGER.debug("Total count is {}, listing object based on paging.", new Object[]{count});
            criteria.setProjection(null);
            criteria = updatePaging(criteria, paging);

            List<RObjectType> objects = criteria.list();
            LOGGER.debug("Found {} objects, translating to JAXB.",
                    new Object[]{(objects != null ? objects.size() : 0)});

            for (RObjectType object : objects) {
                ObjectType objectType = object.toJAXB();
                validateObjectType(objectType, type);
                list.add(objectType.getContainer());
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
    public <T extends ObjectType> void modifyObject(Class<T> type, ObjectDelta<T> delta, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        Validate.notNull(delta, "Object delta must not be null.");
        Validate.notNull(delta.getObjectTypeClass(), "Object class in delta must not be null.");
        Validate.notEmpty(delta.getOid(), "Oid in object delta must not null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Modifying object '{}' with oid '{}'.",
                new Object[]{delta.getObjectTypeClass().getSimpleName(), delta.getOid()});

        OperationResult subResult = result.createSubresult(MODIFY_OBJECT);
        Session session = null;
        try {
            PrismObject<T> prismObject = getObject(delta.getObjectTypeClass(), delta.getOid(), null, subResult);

            PrismSchema schema = schemaRegistry.getObjectSchema();
            PrismObjectDefinition<T> objectDef = schema.findObjectDefinitionByCompileTimeClass(
                    delta.getObjectTypeClass());

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT before:\n{}DELTA:\n{}", new Object[]{prismObject.dump(), delta.dump()});
            }
            delta.applyTo(prismObject);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT after:\n{}", prismObject.dump());
            }

            //todo problem because long id identifiers are lost, or maybe not...
            LOGGER.debug("Translating JAXB to data type.");
            RObjectType rObject = createDataObjectFromJAXB(prismObject.getObjectable());

            session = beginTransaction();
            session.update(rObject);
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
    }

    @Override
    public <T extends ResourceObjectShadowType> ResultList<PrismObject<T>> listResourceObjectShadows(String resourceOid,
            Class<T> resourceObjectShadowType, OperationResult result) throws ObjectNotFoundException {
        Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
        Validate.notNull(resourceObjectShadowType, "Resource object shadow type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Listing resource object shadows '{}' for resource '{}'.",
                new Object[]{resourceObjectShadowType.getSimpleName(), resourceOid});
        OperationResult subResult = result.createSubresult(LIST_RESOURCE_OBJECT_SHADOWS);

        ResultList<PrismObject<T>> list = new ResultArrayList<PrismObject<T>>();
        Session session = null;
        try {
            session = beginTransaction();
            Query query = session.createQuery("select shadow from " + ClassMapper.getHQLType(resourceObjectShadowType)
                    + " as shadow left join shadow.resourceRef as ref where ref.oid = :oid");
            query.setString("oid", resourceOid);

            List<RResourceObjectShadowType> shadows = query.list();
            LOGGER.debug("Query returned {} shadows, transforming to JAXB types.",
                    new Object[]{(shadows != null ? shadows.size() : 0)});

            if (shadows != null) {
                list.setTotalResultCount(shadows.size());
                for (RResourceObjectShadowType shadow : shadows) {
                    ResourceObjectShadowType jaxb = shadow.toJAXB();
                    validateObjectType(jaxb, resourceObjectShadowType);

                    list.add(shadow.toJAXB().getContainer());
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

    private void updateTaskExclusivity(String oid, TaskExclusivityStatusType newStatus, OperationResult result)
            throws ObjectNotFoundException {

        LOGGER.debug("Updating task '{}' exclusivity to '{}'", new Object[]{oid, newStatus});
        Session session = null;
        try {
            LOGGER.debug("Looking for task.");
            session = beginTransaction();
            Query query = session.createQuery("from RTaskType as task where task.oid = :oid");
            query.setString("oid", oid);

            RTaskType task = (RTaskType) query.uniqueResult();
            if (task == null) {
                throw new ObjectNotFoundException("Task with oid '" + oid + "' was not found.");
            }
            LOGGER.debug("Task found, updating exclusivity status.");
            task.setExclusivityStatus(newStatus);
            session.save(task);

            session.getTransaction().commit();
            LOGGER.debug("Task status updated.");
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

    private <T extends ObjectType> void validateObjectType(ObjectType objectType, Class<T> type) {
        if (objectType == null || !(type.isAssignableFrom(objectType.getClass()))) {
            throw new SystemException("Result ('" + objectType + "') is not assignable to '"
                    + type.getSimpleName() + "' [really should not happen].");
        }
    }

    private Criteria updatePaging(Criteria criteria, PagingType paging) {
        if (paging == null) {
            return criteria;
        }

        if (paging.getOffset() != null) {
            criteria = criteria.setFirstResult(paging.getOffset());
        }
        if (paging.getMaxSize() != null) {
            criteria = criteria.setMaxResults(paging.getMaxSize());
        }

        return criteria;
    }

    private <T extends ObjectType> RObjectType createDataObjectFromJAXB(T object) throws InstantiationException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        RObjectType rObject;
        Class<? extends RObjectType> clazz = ClassMapper.getHQLTypeClass(object.getClass());
        rObject = clazz.newInstance();
        Method method = clazz.getMethod("copyFromJAXB", object.getClass(), clazz);
        method.invoke(clazz, object, rObject);

        return rObject;
    }

    private Query updatePaging(Query query, PagingType paging) {
        if (paging == null) {
            return query;
        }
        if (paging.getOffset() != null) {
            query = query.setFirstResult(paging.getOffset());
        }
        if (paging.getMaxSize() != null) {
            query = query.setMaxResults(paging.getMaxSize());
        }

        return query;
    }

    private Session beginTransaction() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();

        return session;
    }

    private void rollbackTransaction(Session session) {
        if (session == null || session.getTransaction() == null) {
            return;
        }
        session.getTransaction().rollback();
    }

    private void cleanupSessionAndResult(Session session, OperationResult result) {
        if (session != null) {
            session.close();
        }

        result.computeStatus();
    }
}

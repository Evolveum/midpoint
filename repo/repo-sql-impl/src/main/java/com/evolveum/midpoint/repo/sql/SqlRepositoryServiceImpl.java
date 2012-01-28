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

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.common.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.RUserType;
import com.evolveum.midpoint.schema.ResultArrayList;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.Validate;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author lazyman
 */
@Repository
public class SqlRepositoryServiceImpl {

    //todo move to interface
    String CLASS_NAME_WITH_DOT = RepositoryService.class.getName() + ".";
    String GET_OBJECT = CLASS_NAME_WITH_DOT + "getObject";
    String LIST_OBJECTS = CLASS_NAME_WITH_DOT + "listObjects";

    private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryServiceImpl.class);

    @Autowired(required = true)
    SessionFactory sessionFactory;

    public <T extends ObjectType> T getObject(Class<T> type, String oid, PropertyReferenceListType resolve,
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
            Query query = session.createQuery("from " + ClassMapper.getHQLType(type) + " o where o.oid = ?");
            query.setString(0, oid);

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
            session.getTransaction().rollback();
            throw new SystemException("There are more objects of type '"
                    + type.getSimpleName() + "' with oid '" + oid + "': " + ex.getMessage(), ex);
        } catch (DtoTranslationException ex) {
            session.getTransaction().rollback();
            throw new SchemaException(ex.getMessage(), ex);
        } catch (SystemException ex) {
            session.getTransaction().rollback();
            throw ex;
        } catch (Exception ex) {
            session.getTransaction().rollback();
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            cleanupSessionAndResult(session, subResult);
        }

        return (T) objectType;
    }

    public <T extends ObjectType> ResultList<T> listObjects(Class<T> type, PagingType paging,
            OperationResult result) {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        ResultList<T> results = new ResultArrayList<T>();
        OperationResult subResult = result.createSubresult(LIST_OBJECTS);
        Session session = null;
        try {
            session = beginTransaction();
            LOGGER.debug("Selecting total count.");
            Query query = session.createQuery("select count(o) from " + ClassMapper.getHQLType(type) + " as o");
            Integer count = (Integer) query.uniqueResult();
            results.setTotalResultCount(count);

            LOGGER.debug("Count is {}, selecting objects.", new Object[]{count});
            query = session.createQuery("from " + ClassMapper.getHQLType(type) + " as o");
            query = updatePaging(query, paging);

            LOGGER.debug("Transforming data to JAXB types.");
            List<? extends RObjectType> objects = query.list();
            if (objects != null) {
                for (RObjectType object : objects) {
                    ObjectType objectType = object.toJAXB();
                    validateObjectType(objectType, type);
                    results.add((T) objectType);
                }
            }
            session.getTransaction().commit();
        } catch (SystemException ex) {
            session.getTransaction().rollback();
            throw ex;
        } catch (Exception ex) {
            session.getTransaction().rollback();
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            cleanupSessionAndResult(session, subResult);
        }

        return results;
    }

    //todo probably remove from interface
    public <T extends ObjectType> PropertyAvailableValuesListType getPropertyAvailableValues(Class<T> type, String oid,
            PropertyReferenceListType properties, OperationResult result) throws ObjectNotFoundException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private <T extends ObjectType> void validateObjectType(ObjectType objectType, Class<T> type) {
        if (objectType == null || !(type.isAssignableFrom(objectType.getClass()))) {
            throw new SystemException("Result ('" + objectType + "') is not assignable to '"
                    + type.getSimpleName() + "' [really should not happen].");
        }
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

    private void cleanupSessionAndResult(Session session, OperationResult result) {
        if (session != null) {
            session.close();
        }

        result.computeStatus();
    }

    public String add(ObjectType object) {
        Session session = sessionFactory.openSession();
        session.beginTransaction();

        RUserType user = new RUserType();
        try {
            RUserType.copyFromJAXB((UserType) object, user);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String oid = (String) session.save(user);

        session.getTransaction().commit();
        session.close();

        return oid;
    }
}

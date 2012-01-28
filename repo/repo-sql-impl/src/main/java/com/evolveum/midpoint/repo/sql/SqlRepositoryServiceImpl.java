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
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.apache.commons.lang.Validate;
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
            session = sessionFactory.openSession();
            session.beginTransaction();

            Query query = session.createQuery("from " + ClassMapper.getHQLType(type) + " o where o.oid = ?");
            query.setString(0, oid);

            List<? extends RObjectType> list = query.list();
            if (list == null || list.isEmpty()) {
                throw new ObjectNotFoundException("Object of type '" + type.getSimpleName() + "' with oid '"
                        + oid + "' was not found.", null, oid);
            }
            if (list.size() > 1) {
                //we probably doesn't need this if clause. If OID is primary key and is UNIQUE
                throw new SystemException("There are '" + list.size() + "' objects of type '"
                        + type.getSimpleName() + "' with oid '" + oid + "'.");
            }

            RObjectType object = list.get(0);
            objectType = object.toJAXB();

            if (resolve != null && !resolve.getProperty().isEmpty()) {
                //todo we have to resolve something...
            }

            if (objectType == null || !(type.isAssignableFrom(objectType.getClass()))) {
                throw new SystemException("Result ('" + objectType + "') is not assignable to '"
                        + type.getSimpleName() + "' [really should not happen].");
            }
        } catch (DtoTranslationException ex) {
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            if (session != null) {
                session.getTransaction().commit();
                session.close();
            }

            subResult.computeStatus();
        }

        return (T) objectType;
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

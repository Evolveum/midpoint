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

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.repo.sql.data.common.RContainer;
import com.evolveum.midpoint.repo.sql.data.common.other.RContainerType;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lazyman
 */
public final class ClassMapper {

    private static final Map<ObjectTypes, RContainerType> types = new HashMap<ObjectTypes, RContainerType>();

    private ClassMapper() {
    }

    static {
        types.put(ObjectTypes.ACCOUNT, RContainerType.ACCOUNT);
        types.put(ObjectTypes.CONNECTOR, RContainerType.CONNECTOR);
        types.put(ObjectTypes.CONNECTOR_HOST, RContainerType.CONNECTOR_HOST);
        types.put(ObjectTypes.GENERIC_OBJECT, RContainerType.GENERIC_OBJECT);
        types.put(ObjectTypes.OBJECT, RContainerType.OBJECT);
        types.put(ObjectTypes.PASSWORD_POLICY, RContainerType.PASSWORD_POLICY);
        types.put(ObjectTypes.RESOURCE, RContainerType.RESOURCE);
        types.put(ObjectTypes.SHADOW, RContainerType.RESOURCE_OBJECT_SHADOW);
        types.put(ObjectTypes.ROLE, RContainerType.ROLE);
        types.put(ObjectTypes.SYSTEM_CONFIGURATION, RContainerType.SYSTEM_CONFIGURATION);
        types.put(ObjectTypes.TASK, RContainerType.TASK);
        types.put(ObjectTypes.USER, RContainerType.USER);
        types.put(ObjectTypes.USER_TEMPLATE, RContainerType.USER_TEMPLATE);
        types.put(ObjectTypes.NODE, RContainerType.NODE);
        types.put(ObjectTypes.ORG, RContainerType.ORG);
        types.put(ObjectTypes.ABSTRACT_ROLE, RContainerType.ABSTRACT_ROLE);

        for (ObjectTypes type : ObjectTypes.values()) {
            if (!types.containsKey(type)) {
                throw new IllegalStateException("Not all object types are mapped by sql repo impl. Found '"
                        + type + "' unmapped.");
            }
        }
    }

    public static Class<? extends RObject> getHQLTypeClass(Class<? extends ObjectType> clazz) {
        Validate.notNull(clazz, "Class must not be null.");

        ObjectTypes type = ObjectTypes.getObjectType(clazz);
        Class<? extends RObject> hqlType = (Class<? extends RObject>) types.get(type).getClazz();
        if (hqlType == null) {
            throw new IllegalStateException("Couldn't find DB type for '" + clazz + "'.");
        }

        return hqlType;
    }

    public static String getHQLType(Class<? extends ObjectType> clazz) {
        Class<? extends RObject> hqlType = getHQLTypeClass(clazz);
        return hqlType.getSimpleName();
    }

    public static RContainerType getHQLTypeForQName(QName qname) {
        if (qname == null) {
            return null;
        }
        for (Map.Entry<ObjectTypes, RContainerType> entry : types.entrySet()) {
            if (entry.getKey().getTypeQName().equals(qname)) {
                return entry.getValue();
            }
        }

        throw new IllegalArgumentException("Couldn't find hql type for qname " + qname);
    }

    public static ObjectTypes getObjectTypeForHQLType(RContainerType type) {
        if (type == null) {
            return null;
        }
        for (Map.Entry<ObjectTypes, RContainerType> entry : types.entrySet()) {
            if (entry.getValue().equals(type)) {
                return entry.getKey();
            }
        }

        throw new IllegalArgumentException("Couldn't find qname for hql type " + type);
    }

    public static ObjectTypes getObjectTypeForHQLType(Class<? extends RContainer> type) {
        if (type == null) {
            return null;
        }
        for (Map.Entry<ObjectTypes, RContainerType> entry : types.entrySet()) {
            if (entry.getValue().getClazz().equals(type)) {
                return entry.getKey();
            }
        }

        throw new IllegalArgumentException("Couldn't find qname for hql type " + type);
    }

    public static QName getQNameForHQLType(RContainerType type) {
        ObjectTypes types = getObjectTypeForHQLType(type);
        return types == null ? null : types.getTypeQName();
    }

    public static Collection<RContainerType> getKnownTypes() {
        return types.values();
    }
}

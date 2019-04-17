/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.Contract;

import javax.xml.namespace.QName;

import java.util.*;

/**
 * @author lazyman
 */
public final class ClassMapper {

    private static final Trace LOGGER = TraceManager.getTrace(ClassMapper.class);

    private static final Map<ObjectTypes, RObjectType> types = new HashMap<>();
    private static final MultiValuedMap<ObjectTypes, RObjectType> descendants = new HashSetValuedHashMap<>();

    private ClassMapper() {
    }

    static {
        types.put(ObjectTypes.CONNECTOR, RObjectType.CONNECTOR);
        types.put(ObjectTypes.CONNECTOR_HOST, RObjectType.CONNECTOR_HOST);
        types.put(ObjectTypes.GENERIC_OBJECT, RObjectType.GENERIC_OBJECT);
        types.put(ObjectTypes.OBJECT, RObjectType.OBJECT);
        types.put(ObjectTypes.PASSWORD_POLICY, RObjectType.VALUE_POLICY);
        types.put(ObjectTypes.RESOURCE, RObjectType.RESOURCE);
        types.put(ObjectTypes.SHADOW, RObjectType.SHADOW);
        types.put(ObjectTypes.ROLE, RObjectType.ROLE);
        types.put(ObjectTypes.SYSTEM_CONFIGURATION, RObjectType.SYSTEM_CONFIGURATION);
        types.put(ObjectTypes.TASK, RObjectType.TASK);
        types.put(ObjectTypes.USER, RObjectType.USER);
        types.put(ObjectTypes.REPORT, RObjectType.REPORT);
        types.put(ObjectTypes.REPORT_OUTPUT, RObjectType.REPORT_OUTPUT);
        types.put(ObjectTypes.OBJECT_TEMPLATE, RObjectType.OBJECT_TEMPLATE);
        types.put(ObjectTypes.NODE, RObjectType.NODE);
        types.put(ObjectTypes.ORG, RObjectType.ORG);
        types.put(ObjectTypes.ABSTRACT_ROLE, RObjectType.ABSTRACT_ROLE);
        types.put(ObjectTypes.FOCUS_TYPE, RObjectType.FOCUS);
        types.put(ObjectTypes.ASSIGNMENT_HOLDER_TYPE, RObjectType.ASSIGNMENT_HOLDER);
        types.put(ObjectTypes.SECURITY_POLICY, RObjectType.SECURITY_POLICY);
        types.put(ObjectTypes.LOOKUP_TABLE, RObjectType.LOOKUP_TABLE);
        types.put(ObjectTypes.ACCESS_CERTIFICATION_DEFINITION, RObjectType.ACCESS_CERTIFICATION_DEFINITION);
        types.put(ObjectTypes.ACCESS_CERTIFICATION_CAMPAIGN, RObjectType.ACCESS_CERTIFICATION_CAMPAIGN);
        types.put(ObjectTypes.SEQUENCE, RObjectType.SEQUENCE);
        types.put(ObjectTypes.SERVICE, RObjectType.SERVICE);
        types.put(ObjectTypes.FORM, RObjectType.FORM);
        types.put(ObjectTypes.CASE, RObjectType.CASE);
        types.put(ObjectTypes.FUNCTION_LIBRARY, RObjectType.FUNCTION_LIBRARY);
        types.put(ObjectTypes.OBJECT_COLLECTION, RObjectType.OBJECT_COLLECTION);
        types.put(ObjectTypes.ARCHETYPE, RObjectType.ARCHETYPE);
        types.put(ObjectTypes.DASHBOARD, RObjectType.DASHBOARD);

        for (ObjectTypes type : ObjectTypes.values()) {
            if (!types.containsKey(type)) {
                String message = "Not all object types are mapped by sql repo impl. Found '" + type + "' unmapped.";
                System.err.println(message);
                LOGGER.error(message);
                throw new IllegalStateException(message);
            }
        }
        try {
            computeDescendants();
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    private static void computeDescendants() {
        descendants.clear();
        for (RObjectType rType : RObjectType.values()) {
            for (RObjectType ancestor : getAncestors(rType)) {
                descendants.put(ObjectTypes.getObjectType(ancestor.getJaxbClass()), rType);
            }
        }
    }

    private static Collection<RObjectType> getAncestors(RObjectType type) {
        Set<RObjectType> rv = new HashSet<>();
        Class<? extends ObjectType> jaxbClass = type.getJaxbClass();
        for (;;) {
            RObjectType rType = RObjectType.getByJaxbTypeIfExists(jaxbClass);
            if (rType != null) {
                // this check is because of auxiliary classes like AbstractAccessCertificationDefinitionType
                // that have no representation in RObjectType
                rv.add(rType);
            }
            Class<?> superclass = jaxbClass.getSuperclass();
            if (superclass == null || !ObjectType.class.isAssignableFrom(superclass)) {
                return rv;
            }
            //noinspection unchecked
            jaxbClass = (Class<? extends ObjectType>) superclass;
        }
    }

    public static Class<? extends RObject> getHQLTypeClass(Class<? extends ObjectType> clazz) {
        Validate.notNull(clazz, "Class must not be null.");

        ObjectTypes type = ObjectTypes.getObjectType(clazz);
        Class<? extends RObject> hqlType = types.get(type).getClazz();
        if (hqlType == null) {
            throw new IllegalStateException("Couldn't find DB type for '" + clazz + "'.");
        }

        return hqlType;
    }

    public static String getHQLType(Class<? extends ObjectType> clazz) {
        Class<? extends RObject> hqlType = getHQLTypeClass(clazz);
        return hqlType.getSimpleName();
    }

	@Contract("!null -> !null; null -> null")
    public static RObjectType getHQLTypeForQName(QName qname) {
    	if (qname == null) {
    		return null;
		}

        for (Map.Entry<ObjectTypes, RObjectType> entry : types.entrySet()) {
            if (QNameUtil.match(entry.getKey().getTypeQName(), qname)) {
                return entry.getValue();
            }
        }

        throw new IllegalArgumentException("Couldn't find hql type for qname " + qname);
    }

	@Contract("!null -> !null; null -> null")
    public static RObjectType getHQLTypeForClass(Class<? extends ObjectType> clazz) {
    	if (clazz == null) {
    		return null;
		} else {
            return getHQLTypeForQName(ObjectTypes.getObjectType(clazz).getTypeQName());
        }
    }

    public static Class<? extends RObject> getHqlClassForHqlName(String hqlName) {
        if (hqlName == null) {
            return null;
        }
        for (RObjectType entry : types.values()) {
            if (entry.getClazz().getSimpleName().equals(hqlName)) {
                return entry.getClazz();
            }
        }
        throw new IllegalArgumentException("Couldn't find hql type for hql name " + hqlName);
    }

    public static ObjectTypes getObjectTypeForHQLType(RObjectType type) {
        if (type == null) {
            return null;
        }
        for (Map.Entry<ObjectTypes, RObjectType> entry : types.entrySet()) {
            if (entry.getValue().equals(type)) {
                return entry.getKey();
            }
        }

        throw new IllegalArgumentException("Couldn't find qname for hql type " + type);
    }

    public static ObjectTypes getObjectTypeForHQLType(Class<? extends RObject> type) {
        if (type == null) {
            return null;
        }
        for (Map.Entry<ObjectTypes, RObjectType> entry : types.entrySet()) {
            if (entry.getValue().getClazz().equals(type)) {
                return entry.getKey();
            }
        }

        throw new IllegalArgumentException("Couldn't find qname for hql type " + type);
    }

    public static QName getQNameForHQLType(RObjectType type) {
        ObjectTypes types = getObjectTypeForHQLType(type);
        return types == null ? null : types.getTypeQName();
    }

    public static Collection<RObjectType> getKnownTypes() {
        return types.values();
    }

    public static Collection<RObjectType> getDescendantsForQName(QName typeName) {
        return descendants.get(ObjectTypes.getObjectTypeFromTypeQName(typeName));
    }
}

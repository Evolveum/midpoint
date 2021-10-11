/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

    private static final Map<ObjectTypes, RObjectType> TYPES = new HashMap<>();
    private static final MultiValuedMap<ObjectTypes, RObjectType> DESCENDANTS = new HashSetValuedHashMap<>();

    private ClassMapper() {
    }

    static {
        TYPES.put(ObjectTypes.CONNECTOR, RObjectType.CONNECTOR);
        TYPES.put(ObjectTypes.CONNECTOR_HOST, RObjectType.CONNECTOR_HOST);
        TYPES.put(ObjectTypes.GENERIC_OBJECT, RObjectType.GENERIC_OBJECT);
        TYPES.put(ObjectTypes.OBJECT, RObjectType.OBJECT);
        TYPES.put(ObjectTypes.PASSWORD_POLICY, RObjectType.VALUE_POLICY);
        TYPES.put(ObjectTypes.RESOURCE, RObjectType.RESOURCE);
        TYPES.put(ObjectTypes.SHADOW, RObjectType.SHADOW);
        TYPES.put(ObjectTypes.ROLE, RObjectType.ROLE);
        TYPES.put(ObjectTypes.SYSTEM_CONFIGURATION, RObjectType.SYSTEM_CONFIGURATION);
        TYPES.put(ObjectTypes.TASK, RObjectType.TASK);
        TYPES.put(ObjectTypes.USER, RObjectType.USER);
        TYPES.put(ObjectTypes.REPORT, RObjectType.REPORT);
        TYPES.put(ObjectTypes.REPORT_OUTPUT, RObjectType.REPORT_OUTPUT);
        TYPES.put(ObjectTypes.OBJECT_TEMPLATE, RObjectType.OBJECT_TEMPLATE);
        TYPES.put(ObjectTypes.NODE, RObjectType.NODE);
        TYPES.put(ObjectTypes.ORG, RObjectType.ORG);
        TYPES.put(ObjectTypes.ABSTRACT_ROLE, RObjectType.ABSTRACT_ROLE);
        TYPES.put(ObjectTypes.FOCUS_TYPE, RObjectType.FOCUS);
        TYPES.put(ObjectTypes.ASSIGNMENT_HOLDER_TYPE, RObjectType.ASSIGNMENT_HOLDER);
        TYPES.put(ObjectTypes.SECURITY_POLICY, RObjectType.SECURITY_POLICY);
        TYPES.put(ObjectTypes.LOOKUP_TABLE, RObjectType.LOOKUP_TABLE);
        TYPES.put(ObjectTypes.ACCESS_CERTIFICATION_DEFINITION, RObjectType.ACCESS_CERTIFICATION_DEFINITION);
        TYPES.put(ObjectTypes.ACCESS_CERTIFICATION_CAMPAIGN, RObjectType.ACCESS_CERTIFICATION_CAMPAIGN);
        TYPES.put(ObjectTypes.SEQUENCE, RObjectType.SEQUENCE);
        TYPES.put(ObjectTypes.SERVICE, RObjectType.SERVICE);
        TYPES.put(ObjectTypes.FORM, RObjectType.FORM);
        TYPES.put(ObjectTypes.CASE, RObjectType.CASE);
        TYPES.put(ObjectTypes.FUNCTION_LIBRARY, RObjectType.FUNCTION_LIBRARY);
        TYPES.put(ObjectTypes.OBJECT_COLLECTION, RObjectType.OBJECT_COLLECTION);
        TYPES.put(ObjectTypes.ARCHETYPE, RObjectType.ARCHETYPE);
        TYPES.put(ObjectTypes.DASHBOARD, RObjectType.DASHBOARD);

        for (ObjectTypes type : ObjectTypes.values()) {
            if (!TYPES.containsKey(type)) {
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
        DESCENDANTS.clear();
        for (RObjectType rType : RObjectType.values()) {
            for (RObjectType ancestor : getAncestors(rType)) {
                DESCENDANTS.put(ObjectTypes.getObjectType(ancestor.getJaxbClass()), rType);
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
        Class<? extends RObject> hqlType = TYPES.get(type).getClazz();
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

        for (Map.Entry<ObjectTypes, RObjectType> entry : TYPES.entrySet()) {
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
        for (RObjectType entry : TYPES.values()) {
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
        for (Map.Entry<ObjectTypes, RObjectType> entry : TYPES.entrySet()) {
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
        for (Map.Entry<ObjectTypes, RObjectType> entry : TYPES.entrySet()) {
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
        return TYPES.values();
    }

    public static Collection<RObjectType> getDescendantsForQName(QName typeName) {
        return DESCENDANTS.get(ObjectTypes.getObjectTypeFromTypeQName(typeName));
    }
}

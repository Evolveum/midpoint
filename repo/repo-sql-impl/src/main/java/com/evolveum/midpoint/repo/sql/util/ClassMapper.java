/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.util;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.MiscUtil;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.jetbrains.annotations.Contract;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public final class ClassMapper {

    private static final Trace LOGGER = TraceManager.getTrace(ClassMapper.class);

    private static final Map<ObjectTypes, RObjectType> TYPES = new LinkedHashMap<>();
    private static final MultiValuedMap<ObjectTypes, RObjectType> DESCENDANTS = new HashSetValuedHashMap<>();

    private ClassMapper() {
    }

    static {
        TYPES.put(ObjectTypes.CONNECTOR, RObjectType.CONNECTOR);
        TYPES.put(ObjectTypes.CONNECTOR_HOST, RObjectType.CONNECTOR_HOST);
        TYPES.put(ObjectTypes.GENERIC_OBJECT, RObjectType.GENERIC_OBJECT);
        TYPES.put(ObjectTypes.TAG, RObjectType.TAG);
        TYPES.put(ObjectTypes.OBJECT, RObjectType.OBJECT);
        // Also matches RObject, but we want it later to avoid unexpected definitions Ent:RObject (jaxb=AssignmentHolderType).
        TYPES.put(ObjectTypes.ASSIGNMENT_HOLDER_TYPE, RObjectType.ASSIGNMENT_HOLDER);
        TYPES.put(ObjectTypes.PASSWORD_POLICY, RObjectType.VALUE_POLICY);
        TYPES.put(ObjectTypes.RESOURCE, RObjectType.RESOURCE);
        TYPES.put(ObjectTypes.SHADOW, RObjectType.SHADOW);
        TYPES.put(ObjectTypes.ROLE, RObjectType.ROLE);
        TYPES.put(ObjectTypes.SYSTEM_CONFIGURATION, RObjectType.SYSTEM_CONFIGURATION);
        TYPES.put(ObjectTypes.TASK, RObjectType.TASK);
        TYPES.put(ObjectTypes.USER, RObjectType.USER);
        TYPES.put(ObjectTypes.REPORT, RObjectType.REPORT);
        TYPES.put(ObjectTypes.REPORT_DATA, RObjectType.REPORT_DATA);
        TYPES.put(ObjectTypes.OBJECT_TEMPLATE, RObjectType.OBJECT_TEMPLATE);
        TYPES.put(ObjectTypes.NODE, RObjectType.NODE);
        TYPES.put(ObjectTypes.ORG, RObjectType.ORG);
        TYPES.put(ObjectTypes.ABSTRACT_ROLE, RObjectType.ABSTRACT_ROLE);
        TYPES.put(ObjectTypes.FOCUS_TYPE, RObjectType.FOCUS);
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
        TYPES.put(ObjectTypes.MESSAGE_TEMPLATE, RObjectType.MESSAGE_TEMPLATE);

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type == ObjectTypes.SIMULATION_RESULT) {
                continue; // FIXME ugly hack
            }
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
            LoggingUtils.logUnexpectedException(LOGGER, t);
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
        for (; ; ) {
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
        Objects.requireNonNull(clazz, "Class must not be null.");

        ObjectTypes type = ObjectTypes.getObjectType(clazz);
        return MiscUtil.requireNonNull(
                        TYPES.get(type),
                        () -> new UnsupportedOperationException(
                                "Generic repository does not support objects of type '" + clazz + "'"))
                .getClazz();
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

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.Contract;

import javax.xml.namespace.QName;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lazyman
 */
public final class ClassMapper {

    private static final Map<ObjectTypes, RObjectType> types = new HashMap<>();

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
        types.put(ObjectTypes.SECURITY_POLICY, RObjectType.SECURITY_POLICY);
        types.put(ObjectTypes.LOOKUP_TABLE, RObjectType.LOOKUP_TABLE);
        types.put(ObjectTypes.ACCESS_CERTIFICATION_DEFINITION, RObjectType.ACCESS_CERTIFICATION_DEFINITION);
        types.put(ObjectTypes.ACCESS_CERTIFICATION_CAMPAIGN, RObjectType.ACCESS_CERTIFICATION_CAMPAIGN);
        types.put(ObjectTypes.SEQUENCE, RObjectType.SEQUENCE);
        types.put(ObjectTypes.SERVICE, RObjectType.SERVICE);
        types.put(ObjectTypes.FORM, RObjectType.FORM);
        types.put(ObjectTypes.CASE, RObjectType.CASE);
        types.put(ObjectTypes.FUNCTION_LIBRARY, RObjectType.FUNCTION_LIBRARY);

        for (ObjectTypes type : ObjectTypes.values()) {
            if (!types.containsKey(type)) {
                String message = "Not all object types are mapped by sql repo impl. Found '" + type + "' unmapped.";
                System.err.println(message);
                throw new IllegalStateException(message);
            }
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
}

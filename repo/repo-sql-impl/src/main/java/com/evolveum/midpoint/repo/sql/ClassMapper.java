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

import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.commons.lang.Validate;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lazyman
 */
public final class ClassMapper {

    private static final Map<ObjectTypes, Class<? extends RObjectType>> types =
            new HashMap<ObjectTypes, Class<? extends RObjectType>>();

    private ClassMapper() {
    }

    static {
        types.put(ObjectTypes.ACCOUNT, RAccountShadowType.class);
        types.put(ObjectTypes.CONNECTOR, RConnectorType.class);
        types.put(ObjectTypes.CONNECTOR_HOST, RConnectorHostType.class);
        types.put(ObjectTypes.GENERIC_OBJECT, RGenericObjectType.class);
        types.put(ObjectTypes.OBJECT, RObjectType.class);
        types.put(ObjectTypes.PASSWORD_POLICY, RPasswordPolicyType.class);
        types.put(ObjectTypes.RESOURCE, RResourceType.class);
        types.put(ObjectTypes.RESOURCE_OBJECT_SHADOW, RResourceObjectShadowType.class);
        types.put(ObjectTypes.ROLE, RRoleType.class);
        types.put(ObjectTypes.SYSTEM_CONFIGURATION, RSystemConfigurationType.class);
        types.put(ObjectTypes.TASK, RTaskType.class);
        types.put(ObjectTypes.USER, RUserType.class);
        types.put(ObjectTypes.USER_TEMPLATE, RUserTemplateType.class);

        for (ObjectTypes type : ObjectTypes.values()) {

        }
    }

    public static String getHQLType(Class<? extends ObjectType> clazz) {
        Validate.notNull(clazz, "Class must not be null.");

        ObjectTypes type = ObjectTypes.getObjectType(clazz);
        Class<? extends RObjectType> hqlType = types.get(type);
        Validate.notNull(hqlType, "HQL data type was not found for '" + clazz.getSimpleName() + "'.");

        return hqlType.getSimpleName();
    }
}

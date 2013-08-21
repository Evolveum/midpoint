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

package com.evolveum.midpoint.repo.sql.data.common.other;

import com.evolveum.midpoint.repo.sql.data.common.*;

/**
 * @author lazyman
 */
public enum RContainerType {

    ASSIGNMENT(RAssignment.class),
    EXCLUSION(RExclusion.class),
    ACCOUNT(RAccountShadow.class),
    CONNECTOR(RConnector.class),
    CONNECTOR_HOST(RConnectorHost.class),
    GENERIC_OBJECT(RGenericObject.class),
    OBJECT(RObject.class),
    VALUE_POLICY(RValuePolicy.class),
    RESOURCE(RResource.class),
    SHADOW(RShadow.class),
    ROLE(RRole.class),
    SYSTEM_CONFIGURATION(RSystemConfiguration.class),
    TASK(RTask.class),
    USER(RUser.class),
    OBJECT_TEMPLATE(RObjectTemplate.class),
    NODE(RNode.class),
    ORG(ROrg.class),
    ABSTRACT_ROLE(RAbstractRole.class),
    AUTHORIZATION(RAuthorization.class),
    FOCUS(RFocus.class),
    TRIGGER(RTrigger.class);

    private Class<? extends RContainer> clazz;

    private RContainerType(Class<? extends RContainer> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends RContainer> getClazz() {
        return clazz;
    }

    public static <T extends RContainer> RContainerType getType(Class<T> clazz) {
        for (RContainerType type : RContainerType.values()) {
            if (type.getClazz().equals(clazz)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Couldn't find type for class '" + clazz + "'.");
    }
}

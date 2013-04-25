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
    PASSWORD_POLICY(RPasswordPolicy.class),
    RESOURCE(RResource.class),
    RESOURCE_OBJECT_SHADOW(RShadow.class),
    ROLE(RRole.class),
    SYSTEM_CONFIGURATION(RSystemConfiguration.class),
    TASK(RTask.class),
    USER(RUser.class),
    USER_TEMPLATE(RUserTemplate.class),
    NODE(RNode.class),
    ORG(ROrg.class),
    ABSTRACT_ROLE(RAbstractRole.class),
    AUTHORIZATION(RAuthorization.class),
    FOCUS(RFocus.class);

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

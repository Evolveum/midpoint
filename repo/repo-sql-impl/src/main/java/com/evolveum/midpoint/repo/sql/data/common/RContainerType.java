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

package com.evolveum.midpoint.repo.sql.data.common;

/**
 * @author lazyman
 */
public enum RContainerType {

    ASSIGNMENT(RAssignmentType.class),

    ACCOUNT(RAccountShadowType.class),
    CONNECTOR(RConnectorType.class),
    CONNECTOR_HOST(RConnectorHostType.class),
    GENERIC_OBJECT(RGenericObjectType.class),
    OBJECT(RObjectType.class),
    PASSWORD_POLICY(RPasswordPolicyType.class),
    RESOURCE(RResourceType.class),
    RESOURCE_OBJECT_SHADOW(RResourceObjectShadowType.class),
    ROLE(RRoleType.class),
    SYSTEM_CONFIGURATION(RSystemConfigurationType.class),
    TASK(RTaskType.class),
    USER(RUserType.class),
    USER_TEMPLATE(RUserTemplateType.class);

    private Class<? extends RContainer> clazz;

    private RContainerType(Class<? extends RContainer> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends RContainer> getClazz() {
        return clazz;
    }
}

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

package com.evolveum.midpoint.web.page.admin.users.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.RoleType;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public enum UserAssignmentDtoType {

    ACCOUNT_CONSTRUCTION(null, null),

    ROLE(RoleType.class, RoleType.COMPLEX_TYPE),

    ORG_UNIT(OrgType.class, OrgType.COMPLEX_TYPE);

    private Class<? extends ObjectType> type;
    private QName qname;

    private UserAssignmentDtoType(Class<? extends ObjectType> type, QName qname) {
        this.type = type;
        this.qname = qname;
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }

    public QName getQname() {
        return qname;
    }

    public static UserAssignmentDtoType getType(Class<? extends ObjectType> type) {
        if (type == null) {
            return ACCOUNT_CONSTRUCTION;
        }

        for (UserAssignmentDtoType e : UserAssignmentDtoType.values()) {
            if (type.equals(e.getType())) {
                return e;
            }
        }

        throw new IllegalArgumentException("Unknown assignment type '" + type.getName() + "'.");
    }
}

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

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;

/**
 * @author lazyman
 */
public class UserAssignmentDto extends Selectable {

    public static enum Type {
        ROLE, OTHER
    }

    private Type type;
    private ObjectReferenceType targetRef;
    private String name;
    private ActivationType activation;
    private UserDtoStatus status;

    public UserAssignmentDto(String name, ObjectReferenceType targetRef, ActivationType activation,
            Type type, UserDtoStatus status) {
        this.activation = activation;
        this.name = name;
        this.targetRef = targetRef;
        this.type = type;
        this.status = status;
    }

    public UserDtoStatus getStatus() {
        return status;
    }

    public ActivationType getActivation() {
        return activation;
    }

    public String getName() {
        return name;
    }

    public ObjectReferenceType getTargetRef() {
        return targetRef;
    }

    public void setStatus(UserDtoStatus status) {
        this.status = status;
    }

    public Type getType() {
        return type;
    }
}

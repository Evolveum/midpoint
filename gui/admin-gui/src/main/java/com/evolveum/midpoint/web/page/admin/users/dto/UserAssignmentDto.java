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

import com.evolveum.midpoint.common.valueconstruction.AsIsValueConstructor;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public class UserAssignmentDto extends Selectable {

    public static enum Type {
        ROLE, OTHER
    }

    private String name;
    private Type type;
    private UserDtoStatus status;
    private AssignmentType assignment;

    public UserAssignmentDto(String name, Type type, UserDtoStatus status, AssignmentType assignment) {
        Validate.notNull(status, "User dto status must not be null.");
        Validate.notNull(type, "Type must not be null.");
        Validate.notNull(assignment, "Assignment must not be null.");

        this.name = name;
        this.type = type;
        this.status = status;
        this.assignment = assignment;
    }

    public UserDtoStatus getStatus() {
        return status;
    }

    public ActivationType getActivation() {
        return assignment.getActivation();
    }

    public String getName() {
        return name;
    }

    public ObjectReferenceType getTargetRef() {
        return assignment.getTargetRef();
    }

    public void setStatus(UserDtoStatus status) {
        this.status = status;
    }

    public Type getType() {
        return type;
    }

    public AssignmentType createAssignment() {
        return assignment;
    }
}

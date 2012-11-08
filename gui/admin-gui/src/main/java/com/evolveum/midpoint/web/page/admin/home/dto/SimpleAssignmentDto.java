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

package com.evolveum.midpoint.web.page.admin.home.dto;

import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;

import java.io.Serializable;

public class SimpleAssignmentDto implements Serializable {
    private String assignmentName;
    private AssignmentEditorDtoType type;
    private ActivationType activation;

    public SimpleAssignmentDto(String assignmentName, AssignmentEditorDtoType type, ActivationType activation) {
        this.assignmentName = assignmentName;
        this.type = type;
        this.activation = activation;
    }

    public String getAssignmentName() {
        return assignmentName;
    }

    public AssignmentEditorDtoType getType() {
        return type;
    }

    public ActivationType getActivation() {
        return activation;
    }
}

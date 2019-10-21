/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.home.dto;

import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

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

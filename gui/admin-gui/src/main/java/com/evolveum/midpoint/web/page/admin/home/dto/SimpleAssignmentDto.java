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

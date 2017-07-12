/*
 * Copyright (c) 2010-2017 Evolveum
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
import org.apache.commons.lang.Validate;

import java.io.Serializable;

/**
 * TODO: unify with AssignmentEditorDto
 * 
 * @author lazyman
 */
public class AssignmentItemDto implements Serializable, Comparable<AssignmentItemDto> {
	private static final long serialVersionUID = 1L;

    public static final String F_TYPE = "type";
    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_RELATION = "relation";

    private AssignmentEditorDtoType type;
    private String name;
    private String description;
    private String relation;

    public AssignmentItemDto(AssignmentEditorDtoType type, String name, String description, String relation) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.relation = relation;
    }

    public AssignmentEditorDtoType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getRelation() {
        if (relation == null) {
            return null;
        }

        return '(' + relation + ')';
    }

    public String getRealRelation() {
        return relation;
    }

    @Override
    public int compareTo(AssignmentItemDto other) {
        Validate.notNull(other, "Can't compare assignment item dto with null.");

        int value = getIndexOfType(getType()) - getIndexOfType(other.getType());
        if (value != 0) {
            return value;
        }

        String name1 = getName() != null ? getName() : "";
        String name2 = other.getName() != null ? other.getName() : "";

        return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
    }

    private int getIndexOfType(AssignmentEditorDtoType type) {
        if (type == null) {
            return 0;
        }

        AssignmentEditorDtoType[] values = AssignmentEditorDtoType.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(type)) {
                return i;
            }
        }

        return 0;
    }
}

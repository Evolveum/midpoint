/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;

import org.apache.commons.lang3.Validate;

import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;

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

    private final AssignmentEditorDtoType type;
    private final String name;
    private final String description;
    private final String relation;

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

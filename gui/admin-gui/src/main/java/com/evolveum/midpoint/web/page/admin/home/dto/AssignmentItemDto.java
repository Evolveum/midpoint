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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.home.dto;

import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class AssignmentItemDto implements Serializable {

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
}

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

import java.io.Serializable;

/**
 * @author lazyman
 */
public class AssignmentItemDto implements Serializable {

    public static enum Type {ROLE, ORG_UNIT, ACCOUNT}

    private Type type;
    private String name;
    private String description;
    private String relation;

    public AssignmentItemDto(Type type, String name, String description, String relation) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.relation = relation;
    }

    public Type getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getRelation() {
        return relation;
    }
}

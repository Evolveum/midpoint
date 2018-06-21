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

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public enum AssignmentEditorDtoType {

    ORG_UNIT(OrgType.class, OrgType.COMPLEX_TYPE, GuiStyleConstants.CLASS_OBJECT_ORG_ICON),

    ROLE(RoleType.class, RoleType.COMPLEX_TYPE, GuiStyleConstants.CLASS_OBJECT_ROLE_ICON),

    SERVICE(ServiceType.class, ServiceType.COMPLEX_TYPE, GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON),

    USER(UserType.class, UserType.COMPLEX_TYPE, GuiStyleConstants.CLASS_OBJECT_USER_ICON),

    CONSTRUCTION(null, ConstructionType.COMPLEX_TYPE, GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON),

    POLICY_RULE(null, PolicyRuleType.COMPLEX_TYPE, GuiStyleConstants.CLASS_FILE_TEXT),

	PERSONA_CONSTRUCTION(null, PersonaConstructionType.COMPLEX_TYPE, GuiStyleConstants.CLASS_OBJECT_USER_ICON);

    private Class<? extends ObjectType> type;
    private QName qname;
    private String iconCssClass;

    private AssignmentEditorDtoType(Class<? extends ObjectType> type, QName qname, String iconCssClass) {
        this.type = type;
        this.qname = qname;
        this.iconCssClass = iconCssClass;
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }

    public QName getQname() {
        return qname;
    }

    public static AssignmentEditorDtoType getType(Class<? extends ObjectType> type) {
        if (type == null) {
            return CONSTRUCTION;
        }

        for (AssignmentEditorDtoType e : AssignmentEditorDtoType.values()) {
            if (type.equals(e.getType())) {
                return e;
            }
        }

        throw new IllegalArgumentException("Unknown assignment type '" + type.getName() + "'.");
    }

    public static AssignmentEditorDtoType getType(QName type) {
        if (type == null) {
            return CONSTRUCTION;
        }

        for (AssignmentEditorDtoType e : AssignmentEditorDtoType.values()) {
            if (type.equals(e.getQname())) {
                return e;
            }
        }

        throw new IllegalArgumentException("Unknown assignment type '" + type + "'.");
    }

	public String getIconCssClass() {
		return iconCssClass;
	}
}

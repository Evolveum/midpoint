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

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public enum ObjectTypeGuiDescriptor {

    CONNECTOR(ObjectTypes.CONNECTOR, "ObjectTypeGuiDescriptor.connector", "silk-link", "silk-link"),

    CONNECTOR_HOST(ObjectTypes.CONNECTOR_HOST, "ObjectTypeGuiDescriptor.connectorHost", "silk-driver_link", "silk-driver_link"),

    GENERIC_OBJECT(ObjectTypes.GENERIC_OBJECT, "ObjectTypeGuiDescriptor.genericObject", "silk-page_white_code", "silk-page_white_code"),

    RESOURCE(ObjectTypes.RESOURCE, "ObjectTypeGuiDescriptor.resource", GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON_COLORED, GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON),

    USER(ObjectTypes.USER, "ObjectTypeGuiDescriptor.user", GuiStyleConstants.CLASS_OBJECT_USER_ICON_COLORED, GuiStyleConstants.CLASS_OBJECT_USER_ICON),

    OBJECT_TEMPLATE(ObjectTypes.OBJECT_TEMPLATE, "ObjectTypeGuiDescriptor.objectTemplate", "silk-layout", "silk-layout"),

    SYSTEM_CONFIGURATION(ObjectTypes.SYSTEM_CONFIGURATION, "ObjectTypeGuiDescriptor.systemConfiguration", "silk-page_white_gear", "silk-page_white_gear"),

    TASK(ObjectTypes.TASK, "ObjectTypeGuiDescriptor.task", GuiStyleConstants.CLASS_OBJECT_TASK_ICON_COLORED, GuiStyleConstants.CLASS_OBJECT_TASK_ICON),

    SHADOW(ObjectTypes.SHADOW, "ObjectTypeGuiDescriptor.shadow", "silk-status_online", "silk-status_online"),

    OBJECT(ObjectTypes.OBJECT, "ObjectTypeGuiDescriptor.object", "silk-page_white", "silk-page_white"),

    ROLE(ObjectTypes.ROLE, "ObjectTypeGuiDescriptor.role", GuiStyleConstants.CLASS_OBJECT_ROLE_ICON_COLORED, GuiStyleConstants.CLASS_OBJECT_ROLE_ICON),

    VALUE_POLICY(ObjectTypes.PASSWORD_POLICY, "ObjectTypeGuiDescriptor.valuePolicy", "silk-lock", "silk-lock"),

    NODE(ObjectTypes.NODE, "ObjectTypeGuiDescriptor.node", "silk-computer", "silk-computer"),

    FORM(ObjectTypes.FORM, "ObjectTypeGuiDescriptor.form", "", ""),

    ORG(ObjectTypes.ORG, "ObjectTypeGuiDescriptor.org", GuiStyleConstants.CLASS_OBJECT_ORG_ICON_COLORED, GuiStyleConstants.CLASS_OBJECT_ORG_ICON),

    ABSTRACT_ROLE(ObjectTypes.ABSTRACT_ROLE, "ObjectTypeGuiDescriptor.abstractRole", "silk-award_star_gold_3", "silk-award_star_gold_3"),

    FOCUS(ObjectTypes.FOCUS_TYPE, "ObjectTypeGuiDescriptor.focus", "", ""),

    REPORT(ObjectTypes.REPORT, "ObjectTypeGuiDescriptor.report", "", ""),

    REPORT_OUTPUT(ObjectTypes.REPORT_OUTPUT, "ObjectTypeGuiDescriptor.reportOutput", "", ""),

    SECURITY_POLICY(ObjectTypes.SECURITY_POLICY, "ObjectTypeGuiDescriptor.securityPolicy", "", ""),

    USER_ORG_MANAGER(SchemaConstants.ORG_MANAGER, "ObjectTypeGuiDescriptor.orgManager", "silk-user_red", "silk-user_red"),

    LOOKUP_TABLE(ObjectTypes.LOOKUP_TABLE, "ObjectTypeGuiDescriptor.lookupTable", "", ""),

    ACCESS_CERTIFICATION_DEFINITION(ObjectTypes.ACCESS_CERTIFICATION_DEFINITION, "ObjectTypeGuiDescriptor.accessCertificationDefinition", "", ""),

    ACCESS_CERTIFICATION_CAMPAIGN(ObjectTypes.ACCESS_CERTIFICATION_CAMPAIGN, "ObjectTypeGuiDescriptor.accessCertificationCampaign", "", ""),

    SEQUENCE(ObjectTypes.SEQUENCE, "ObjectTypeGuiDescriptor.sequence", "", ""),

    SERVICE(ObjectTypes.SERVICE, "ObjectTypeGuiDescriptor.service", GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON_COLORED, GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON),

    CASE(ObjectTypes.CASE, "ObjectTypeGuiDescriptor.case", "", "");             // TODO icons

    public static final String ERROR_ICON = "silk-error";
    public static final String ERROR_LOCALIZATION_KEY = "ObjectTypeGuiDescriptor.unknown";

    private ObjectTypes type;
    private QName relation;
    private String localizationKey;
    private String coloredIcon;
    private String blackIcon;

    ObjectTypeGuiDescriptor(ObjectTypes type, String localizationKey, String coloredIcon, String blackIcon) {
        this.coloredIcon = coloredIcon;
		this.blackIcon = blackIcon;
        this.localizationKey = localizationKey;
        this.type = type;
    }

    ObjectTypeGuiDescriptor(QName relation, String localizationKey, String coloredIcon, String blackIcon) {
        this.coloredIcon = coloredIcon;
		this.blackIcon = blackIcon;
        this.localizationKey = localizationKey;
        this.relation = relation;
    }

	@SuppressWarnings("unused")
    public String getColoredIcon() {
        return coloredIcon;
    }

	public String getBlackIcon() {
		return blackIcon;
	}

	public String getLocalizationKey() {
        return localizationKey;
    }

    public ObjectTypes getType() {
        return type;
    }

    public QName getRelation() {
        return relation;
    }

    public static ObjectTypeGuiDescriptor getDescriptor(Class type) {
        for (ObjectTypeGuiDescriptor descr : ObjectTypeGuiDescriptor.values()) {
            if (descr.getType() != null && descr.getType().getClassDefinition().equals(type)) {
                return descr;
            }
        }

        return null;
    }

    public static ObjectTypeGuiDescriptor getDescriptor(ObjectTypes type) {
		if (type == null) {
			return null;
		}
        for (ObjectTypeGuiDescriptor descr : ObjectTypeGuiDescriptor.values()) {
            if (descr.getType() != null && descr.getType().equals(type)) {
                return descr;
            }
        }

        return null;
    }

    public static ObjectTypeGuiDescriptor getDescriptor(QName relation) {
        for (ObjectTypeGuiDescriptor descr : ObjectTypeGuiDescriptor.values()) {
            if (descr.getRelation() != null && descr.getRelation().equals(relation)) {
                return descr;
            }
        }

        return null;
    }
}

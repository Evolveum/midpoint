/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author lazyman
 * 
 */
public enum ObjectTypes {

	ACCOUNT("schema.objectTypes.account", SchemaConstants.I_ACCOUNT_SHADOW_TYPE, AccountShadowType.class),

	CONNECTOR("schema.objectTypes.connector", SchemaConstants.I_CONNECTOR_TYPE, ConnectorType.class),

	GENERIC_OBJECT("schema.objectTypes.genericObject", SchemaConstants.I_GENERIC_OBJECT_TYPE,
			GenericObjectType.class),

	RESOURCE("schema.objectTypes.resource", SchemaConstants.I_RESOURCE_TYPE, ResourceType.class),

	RESOURCE_STATE("schema.objectTypes.resourceState", SchemaConstants.I_RESOURCE_STATE_TYPE,
			ResourceStateType.class),

	USER("schema.objectTypes.user", SchemaConstants.I_USER_TYPE, UserType.class),

	USER_TEMPLATE("schema.objectTypes.userTemplate", SchemaConstants.I_USER_TEMPLATE_TYPE,
			UserTemplateType.class),
	
	SYSTEM_CONFIGURATION("schema.objectTypes.systemConfiguration", SchemaConstants.I_SYSTEM_CONFIGURATION_TYPE, SystemConfigurationType.class);

	private String localizationKey;
	private QName value;
	private Class<? extends ObjectType> classDefinition;
	private boolean managedByProvisioning;

	private ObjectTypes(String key, QName value, Class<? extends ObjectType> classDefinition) {
		this.localizationKey = key;
		this.value = value;
		this.classDefinition = classDefinition;
	}

	public boolean isManagedByProvisioning() {
		return managedByProvisioning;
	}

	public String getLocalizationKey() {
		return localizationKey;
	}

	public String getValue() {
		return value.getLocalPart();
	}

	public QName getQName() {
		return value;
	}

	public Class<? extends ObjectType> getClassDefinition() {
		return classDefinition;
	}

	public String getObjectTypeUri() {
		return QNameUtil.qNameToUri(getQName());
	}

	public static ObjectTypes getObjectType(String objectType) {
		for (ObjectTypes type : values()) {
			if (type.getValue().equals(objectType)) {
				return type;
			}
		}

		throw new IllegalArgumentException("Unsupported object type " + objectType);
	}
	
	public static ObjectTypes getObjectTypeFromUri(String objectTypeUri) {
		for (ObjectTypes type : values()) {
			if (type.getObjectTypeUri().equals(objectTypeUri)) {
				return type;
			}
		}

		throw new IllegalArgumentException("Unsupported object type uri " + objectTypeUri);
	}

	public static String getObjectTypeUri(String objectType) {
		return getObjectType(objectType).getObjectTypeUri();
	}

	public static Class<? extends ObjectType> getObjectTypeClass(String objectType) {
		for (ObjectTypes type : values()) {
			if (type.getValue().equals(objectType)) {
				return type.getClassDefinition();
			}
		}

		throw new IllegalArgumentException("Unsupported object type " + objectType);
	}
	
	public static ObjectTypes getObjectType(Class<? extends ObjectType> objectType) {
		for (ObjectTypes type : values()) {
			if (type.getClassDefinition().equals(objectType)) {
				return type;
			}
		}

		throw new IllegalArgumentException("Unsupported object type " + objectType);
	}
}

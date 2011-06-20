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
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author lazyman
 * 
 */
public enum ObjectTypes {

	ACCOUNT("schema.objectTypes.account", SchemaConstants.I_ACCOUNT_SHADOW_TYPE),
	
	CONNECTOR("schema.objectTypes.connector", SchemaConstants.I_CONNECTOR_TYPE),

	GENERIC_OBJECT("schema.objectTypes.genericObject", SchemaConstants.I_GENERIC_OBJECT_TYPE),

	RESOURCE("schema.objectTypes.resource", SchemaConstants.I_RESOURCE_TYPE),

	RESOURCE_STATE("schema.objectTypes.resourceState", SchemaConstants.I_RESOURCE_STATE_TYPE),

	USER("schema.objectTypes.user", SchemaConstants.I_USER_TYPE),

	USER_TEMPLATE("schema.objectTypes.userTemplate", SchemaConstants.I_USER_TEMPLATE_TYPE);

	private String localizationKey;
	private QName value;

	private ObjectTypes(String key, QName value) {
		this.localizationKey = key;
		this.value = value;
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

	public String getObjectTypeUri() {
		return QNameUtil.qNameToUri(getQName());
	}

	public static ObjectTypes getObjectType(String objectName) {
		for (ObjectTypes type : values()) {
			if (type.getValue().equals(objectName)) {
				return type;
			}
		}

		throw new IllegalArgumentException("UnsupportedObjectType" + objectName);
	}

	public static String getObjectTypeUri(String objectName) {
		return getObjectType(objectName).getObjectTypeUri();
	}
}

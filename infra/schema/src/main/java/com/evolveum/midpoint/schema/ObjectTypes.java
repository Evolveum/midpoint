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

/**
 * 
 * @author lazyman
 * 
 */
public enum ObjectTypes {

	ACCOUNT("schema.objectTypes.account", "AccountType"),

	GENERIC_OBJECT("schema.objectTypes.genericObject", "GenericObjectType"),

	RESOURCE("schema.objectTypes.resource", "ResourceType"),

	RESOURCE_STATE("schema.objectTypes.resourceState", "ResourceStateType"),

	USER("schema.objectTypes.user", "UserType"),

	USER_TEMPLATE("schema.objectTypes.userTemplate", "UserTemplateType");

	private String localizationKey;
	private String value;

	private ObjectTypes(String key, String value) {
		this.localizationKey = key;
		this.value = value;
	}

	public String getLocalizationKey() {
		return localizationKey;
	}

	public String getValue() {
		return value;
	}
}

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

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author lazyman
 * 
 */
public enum ProvisioningTypes {

	RESOURCE_OBJECT_SHADOW(ResourceObjectShadowType.class, SchemaConstants.I_RESOURCE_OBJECT_SHADOW),

	ACCOUNT_SHADOW(AccountShadowType.class, SchemaConstants.I_ACCOUNT_SHADOW_TYPE),

	RESOURCE(ResourceType.class, SchemaConstants.I_RESOURCE_TYPE),

	CONNECTOR(ConnectorType.class, SchemaConstants.I_CONNECTOR_TYPE);

	private Class<? extends ObjectType> clazz;
	private QName name;

	private ProvisioningTypes(Class<? extends ObjectType> clazz, QName name) {
		this.clazz = clazz;
		this.name = name;
	}

	public static boolean isManagedByProvisioning(ObjectType object) {
		if (object == null) {
			return false;
		}

		for (ProvisioningTypes type : ProvisioningTypes.values()) {
			if (type.clazz.isInstance(object)) {
				return true;
			}
		}

		return false;
	}

	public static boolean isObjectTypeManagedByProvisioning(String objectType) {
		if (StringUtils.isEmpty(objectType)) {
			return false;
		}

		for (ProvisioningTypes type : ProvisioningTypes.values()) {
			if (type.name.getLocalPart().equals(objectType)) {
				return true;
			}
		}

		return false;
	}
}

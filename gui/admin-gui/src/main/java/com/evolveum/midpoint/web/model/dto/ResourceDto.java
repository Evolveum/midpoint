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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.model.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Configuration;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType;

/**
 * 
 * @author semancik
 */
public class ResourceDto extends ExtensibleObjectDto<ResourceType> {

	private static final long serialVersionUID = -2599530038158817244L;

	public ResourceDto() {
	}

	public ResourceDto(ResourceType object) {
		super(object);
	}

	public Element getSchema() {
		return ResourceTypeUtil.getResourceXsdSchema(getXmlObject());
	}

	public List<Object> getConfiguration() {
		return getXmlObject().getConfiguration().getAny();
	}

	public void setConfiguration(Configuration value) {
		// TODO		
	}
	
	public CapabilitiesType getEffectiveCapabilities() {
		return ResourceTypeUtil.getEffectiveCapabilities(getXmlObject());
	}

	public List<AccountTypeDto> getAccountTypes() {
		List<AccountTypeDto> accountTypeList = new ArrayList<AccountTypeDto>();
		if (getXmlObject() == null || getXmlObject().getSchemaHandling() == null) {
			return accountTypeList;
		}

		List<SchemaHandlingType.AccountType> list = getXmlObject().getSchemaHandling().getAccountType();
		for (SchemaHandlingType.AccountType accountType : list) {
			accountTypeList.add(new AccountTypeDto(accountType.getName(), accountType.getObjectClass(),
					accountType.isDefault()));
		}
		return accountTypeList;
	}

	public String getNamespace() {
		return ((ResourceType) getXmlObject()).getNamespace();
	}

	public static class AccountTypeDto {

		private String name;
		private QName objectClass;
		private boolean def;

		private AccountTypeDto(String name, QName objectClass, boolean def) {
			this.name = name;
			this.objectClass = objectClass;
			this.def = def;
		}

		public String getName() {
			return name;
		}

		public QName getObjectClass() {
			return objectClass;
		}

		public boolean isDefault() {
			return def;
		}
	}
}

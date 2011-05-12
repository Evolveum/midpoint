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

package com.evolveum.midpoint.web.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.schema.AccountObjectClassDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceAttributeDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceObjectDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
import com.evolveum.midpoint.provisioning.schema.util.DOMToSchemaParser;
import com.evolveum.midpoint.provisioning.schema.util.SchemaParserException;
import com.evolveum.midpoint.web.model.AccountShadowDto;
import com.evolveum.midpoint.web.model.ResourceDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * 
 * @author Vilo Repan
 */
public class SchemaFormParser {

	private static transient Trace TRACE = TraceManager.getTrace(SchemaFormParser.class);
	private QName defaultAccountType;
	private Map<QName, List<Object>> valueMap = new HashMap<QName, List<Object>>();
	private String displayName;

	public List<ResourceAttributeDefinition> parseSchemaForAccount(AccountShadowDto account)
			throws SchemaParserException {
		if (account == null) {
			throw new IllegalArgumentException("Account shadow can't be null.");
		}

		TRACE.trace("Account contains default object class: " + account.getObjectClass());
		return parseSchemaForAccount(account, account.getObjectClass());
	}

	public List<ResourceAttributeDefinition> parseSchemaForAccount(AccountShadowDto account, QName accountType)
			throws SchemaParserException {
		if (account == null) {
			throw new IllegalArgumentException("Account shadow can't be null.");
		}

		List<Element> attrList = account.getAttributes();
		if (attrList != null) {
			createAttributeValueMap(attrList);
		}

		ResourceDto resource = account.getResource();
		if (resource == null) {
			throw new IllegalArgumentException("Resource dto can't be null.");
		}

		Element xsdSchema = resource.getSchema();
		if (xsdSchema == null) {
			throw new IllegalArgumentException("Resource doesn't contain schema element.");
		}

		if (accountType == null) {
			List<ResourceDto.AccountTypeDto> accountTypes = resource.getAccountTypes();
			for (ResourceDto.AccountTypeDto accountTypeDto : accountTypes) {
				if (accountTypeDto.isDefault()) {
					accountType = accountTypeDto.getObjectClass();
					break;
				}
			}
		}

		DOMToSchemaParser parser = new DOMToSchemaParser();
		ResourceSchema schema = parser.getSchema(resource.getSchema(),
				((ResourceType) resource.getXmlObject()).getSchemaHandling());

		if (accountType == null) {
			List<ResourceObjectDefinition> list = schema.getObjectClassesCopy();
			for (ResourceObjectDefinition object : list) {
				if (!(object instanceof AccountObjectClassDefinition)) {
					continue;
				}

				AccountObjectClassDefinition def = (AccountObjectClassDefinition) object;
				if (def.isDefault()) {
					accountType = def.getQName();
					break;
				}
			}
		}

		if (accountType == null) {
			throw new com.evolveum.midpoint.provisioning.schema.util.SchemaParserException(
					"Account type was not defined.");
		}

		defaultAccountType = accountType;

		ResourceObjectDefinition definition = schema.getObjectDefinition(accountType);
		if (definition == null) {
			throw new com.evolveum.midpoint.provisioning.schema.util.SchemaParserException(
					"Account definition for type '" + accountType + "' was not found.");
		}
		displayName = resource.getName() + ": " + definition.getName();

		List<ResourceAttributeDefinition> attributes = new ArrayList<ResourceAttributeDefinition>(
				definition.getAttributesCopy());
		for (ResourceAttributeDefinition def : attributes) {
			TRACE.trace("Attr. definition: " + def.getQName());
		}
		return attributes;
	}

	public QName getDefaultAccountType() {
		return defaultAccountType;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Map<QName, List<Object>> getAttributeValueMap() {
		if (valueMap == null) {
			valueMap = new HashMap<QName, List<Object>>();
		}

		return valueMap;
	}

	private void createAttributeValueMap(List<Element> attrList) {
		if (attrList == null) {
			return;
		}

		TRACE.trace("Attributes found in account:");
		List<Object> values = null;

		for (Element node : attrList) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				traceAttribute(node.getNamespaceURI(), node.getLocalName(), node.getTextContent());

				QName key = new QName(node.getNamespaceURI(), node.getLocalName());
				values = valueMap.get(key);
				if (values == null) {
					values = new ArrayList<Object>();
					valueMap.put(key, values);
				}
				values.add(node.getTextContent());
			}
		}
	}

	private void traceAttribute(String namespace, String name, String value) {
		if (!TRACE.isTraceEnabled()) {
			return;
		}

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append(namespace);
		builder.append("}");
		builder.append(name);
		builder.append(": ");
		builder.append(value);

		TRACE.trace(builder.toString());
	}
}

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

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Definition;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.web.model.dto.ResourceDto;

/**
 * 
 * @author Vilo Repan
 */
public class SchemaFormParser {

	private static transient Trace LOGGER = TraceManager.getTrace(SchemaFormParser.class);
	private QName defaultAccountType;
	private Map<QName, List<Object>> valueMap = new HashMap<QName, List<Object>>();
	private String displayName;

	public List<ResourceObjectAttributeDefinition> parseSchemaForAccount(AccountShadowDto account)
			throws SchemaException {
		if (account == null) {
			throw new IllegalArgumentException("Account shadow can't be null.");
		}

		LOGGER.trace("Account contains default object class: " + account.getObjectClass());
		return parseSchemaForAccount(account, account.getObjectClass());
	}

	public List<ResourceObjectAttributeDefinition> parseSchemaForAccount(AccountShadowDto account,
			QName accountType) throws SchemaException {
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

		Schema schema = Schema.parse(resource.getXmlObject().getSchema().getAny().get(0));
		if (accountType == null) {
			for (Definition definition : schema.getDefinitions()) {
				if (!(definition instanceof ResourceObjectDefinition)) {
					continue;
				}

				ResourceObjectDefinition def = (ResourceObjectDefinition) definition;
				if (def.isDefaultAccountType()) {
					accountType = def.getName();
					break;
				}
			}
		}		
		schema.updateSchemaAccess(resource.getXmlObject().getSchemaHandling());

		if (accountType == null) {
			throw new SchemaException("Account type was not defined.");
		}

		defaultAccountType = accountType;

		ResourceObjectDefinition definition = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(accountType);
		if (definition == null) {
			throw new SchemaException("Account definition for type '" + accountType
					+ "' was not found.");
		}
		displayName = resource.getName() + ": " + definition.getName().getLocalPart();

		List<ResourceObjectAttributeDefinition> attributes = new ArrayList<ResourceObjectAttributeDefinition>();
		for (PropertyDefinition def : definition.getPropertyDefinitions()) {
			if (!(def instanceof ResourceObjectAttributeDefinition)) {
				continue;
			}
			LOGGER.trace("Attr. definition: " + def.getName());
			attributes.add((ResourceObjectAttributeDefinition) def);
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

		LOGGER.trace("Attributes found in account:");
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
		if (!LOGGER.isTraceEnabled()) {
			return;
		}

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append(namespace);
		builder.append("}");
		builder.append(name);
		builder.append(": ");
		builder.append(value);

		LOGGER.trace(builder.toString());
	}
}

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
package com.evolveum.midpoint.web.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Definition;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.jsf.form.AttributeType;
import com.evolveum.midpoint.web.jsf.form.FormAttribute;
import com.evolveum.midpoint.web.jsf.form.FormAttributeDefinition;
import com.evolveum.midpoint.web.jsf.form.FormAttributeDefinition.Flag;
import com.evolveum.midpoint.web.jsf.form.FormAttributeDefinitionBuilder;
import com.evolveum.midpoint.web.jsf.form.FormObject;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.web.model.dto.ConnectorDto;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Configuration;

/**
 * 
 * @author lazyman
 * 
 */
public class SchemaFormParser2 {

	private static Trace LOGGER = TraceManager.getTrace(SchemaFormParser2.class);

	public List<FormObject> parseSchemaForConnector(ConnectorDto connector, Configuration configuration)
			throws SchemaException {
		Validate.notNull(connector, "Connector must not be null.");
		List<FormObject> objects = new ArrayList<FormObject>();

		// map which contains qname list as "xpath" to configuration value and
		// list of object as config values. They will be inserted into form
		Map<List<QName>, List<Object>> valueMap = createConfigValueMap(configuration);

		Schema schema = Schema.parse(connector.getXmlObject().getSchema().getAny().get(0));
		for (Definition definition : schema.getDefinitions()) {
			if (!(definition instanceof ResourceObjectDefinition)) {
				continue;
			}

			ResourceObjectDefinition object = (ResourceObjectDefinition) definition;
			String name = getName(object);
			FormObject formObject = new FormObject(object.getTypeName(), name);

			for (PropertyDefinition property : object.getPropertyDefinitions()) {
				if (!(property instanceof ResourceObjectAttributeDefinition)) {
					continue;
				}

				FormAttributeDefinition attrDefinition = createFormAttributeDefinition((ResourceObjectAttributeDefinition) property);
				List<Object> values = getAttributeValues(valueMap, object.getTypeName(), property.getName());
				formObject.getAttributes().add(new FormAttribute(attrDefinition, values));
			}
			objects.add(formObject);
		}

		return objects;
	}

	private List<Object> getAttributeValues(Map<List<QName>, List<Object>> valueMap, QName... qnames) {
		List<QName> key = new ArrayList<QName>();		
		for (QName qname : qnames) {
			key.add(qname);
		}

		return valueMap.get(key);
	}

	private FormAttributeDefinition createFormAttributeDefinition(ResourceObjectAttributeDefinition def) {
		FormAttributeDefinitionBuilder builder = new FormAttributeDefinitionBuilder();
		if (def.getAllowedValues() != null) {
			List<Object> availableValues = new ArrayList<Object>();
			availableValues.addAll(Arrays.asList(def.getAllowedValues()));
			builder.setAvailableValues(availableValues);
		}
		builder.setDescription(def.getHelp());

		if (StringUtils.isEmpty(def.getDisplayName())) {
			builder.setDisplayName(def.getName().getLocalPart());
		} else {
			builder.setDisplayName(def.getDisplayName());
		}
		builder.setElementName(def.getName());
		if (def.canRead()) {
			builder.addFlag(Flag.READ);
		}
		if (def.canUpdate()) {
			builder.addFlag(Flag.UPDATE);
		}
		if (def.canCreate()) {
			builder.addFlag(Flag.CREATE);
		}

		builder.setMaxOccurs(def.getMaxOccurs());
		builder.setMinOccurs(def.getMinOccurs());
		builder.setType(AttributeType.getType(def.getTypeName()));
		// builder.setFilledWithExpression(def.isFilledWithExpression());
		// //TODO: where can I get this?????

		return builder.build();
	}

	private Map<List<QName>, List<Object>> createAttributeValueMap(List<?> elements) {
		Map<List<QName>, List<Object>> valueMap = new HashMap<List<QName>, List<Object>>();
		if (elements == null) {
			return valueMap;
		}

		for (Object object : elements) {
			if (!(object instanceof Element)) {
				continue;
			}

			Element element = (Element) object;
			insertConfigValue(valueMap, new ArrayList<QName>(), element);
		}

		return valueMap;
	}

	private Map<List<QName>, List<Object>> createConfigValueMap(Configuration configuration) {
		if (configuration == null) {
			return createAttributeValueMap(null);
		}

		return createAttributeValueMap(configuration.getAny());
	}

	private void insertConfigValue(Map<List<QName>, List<Object>> valueMap, List<QName> parentQNames,
			Element actual) {
		parentQNames.add(createQName(actual));

		if (actual.getFirstChild() instanceof Element) {
			insertConfigValue(valueMap, parentQNames, (Element) actual.getFirstChild());
		} else if (actual.getFirstChild() instanceof Text) {
			String value = actual.getTextContent();
			if (StringUtils.isEmpty(value)) {
				return;
			}

			List<Object> values = valueMap.get(parentQNames);
			if (values == null) {
				values = new ArrayList<Object>();
				valueMap.put(parentQNames, values);
			}
			values.add(value);
		}
	}

	private QName createQName(Element element) {
		return new QName(element.getNamespaceURI(), element.getLocalName());
	}

	private String getName(ResourceObjectDefinition object) {
		String name = object.getTypeName().getLocalPart();

		if (StringUtils.isNotEmpty(object.getDisplayName())) {
			name = object.getDisplayName();
		}

		return name;
	}

	public FormObject parseSchemaForAccount(AccountShadowDto account) throws SchemaException {
		Validate.notNull(account, "Account must not be null.");

		return parseSchemaForAccount(account, account.getObjectClass());
	}

	public FormObject parseSchemaForAccount(AccountShadowDto account, QName accountType)
			throws SchemaException {
		Validate.notNull(account, "Account must not be null.");

		if (accountType == null) {
			accountType = getAccountType(account.getResource());
		}
		if (accountType == null) {
			throw new SchemaException("Account type was not defined.");
		}

		ResourceDto resource = account.getResource();
		Schema schema = ResourceTypeUtil.getResourceSchema(resource.getXmlObject());
		// schema.updateSchemaAccess(resource.getXmlObject().getSchemaHandling());
		ResourceObjectDefinition definition = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(accountType);
		if (definition == null) {
			throw new SchemaException("Account definition for type '" + accountType + "' was not found.");
		}

		FormObject object = new FormObject(definition.getTypeName(), getDisplayName(resource, definition));
		Map<List<QName>, List<Object>> valueMap = createAttributeValueMap(account.getAttributes());
		for (PropertyDefinition property : definition.getPropertyDefinitions()) {
			if (!(property instanceof ResourceObjectAttributeDefinition)) {
				continue;
			}
			LOGGER.trace("Attr. definition: " + property.getName());

			FormAttributeDefinition attrDefinition = createFormAttributeDefinition((ResourceObjectAttributeDefinition) property);
			List<Object> values = getAttributeValues(valueMap, property.getName());
			object.getAttributes().add(new FormAttribute(attrDefinition, values));
		}

		return object;
	}

	private String getDisplayName(ResourceDto resource, ResourceObjectDefinition definition) {
		StringBuilder displayName = new StringBuilder();
		displayName.append(resource.getName());
		displayName.append(": ");
		displayName.append(definition.getTypeName().getLocalPart());

		return displayName.toString();
	}

	private QName getAccountType(ResourceDto resourceDto) throws SchemaException {
		List<ResourceDto.AccountTypeDto> accountTypes = resourceDto.getAccountTypes();
		for (ResourceDto.AccountTypeDto accountTypeDto : accountTypes) {
			if (accountTypeDto.isDefault()) {
				return accountTypeDto.getObjectClass();
			}
		}

		Schema schema = ResourceTypeUtil.getResourceSchema(resourceDto.getXmlObject());
		for (Definition definition : schema.getDefinitions()) {
			if (!(definition instanceof ResourceObjectDefinition)) {
				continue;
			}

			ResourceObjectDefinition def = (ResourceObjectDefinition) definition;
			if (def.isDefaultAccountType()) {
				return def.getTypeName();
			}
		}

		return null;
	}
}

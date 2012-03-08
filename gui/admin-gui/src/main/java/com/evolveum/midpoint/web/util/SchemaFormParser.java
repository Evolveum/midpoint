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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.jsf.form.*;
import com.evolveum.midpoint.web.jsf.form.FormAttributeDefinition.Flag;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.web.model.dto.ConnectorDto;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceConfigurationType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author lazyman
 */
public class SchemaFormParser {

    private static Trace LOGGER = TraceManager.getTrace(SchemaFormParser.class);

    public List<FormObject> parseSchemaForConnector(ConnectorDto connector, ResourceConfigurationType configuration,
            PrismContext prismContext) throws SchemaException {
        Validate.notNull(connector, "Connector must not be null.");
        List<FormObject> objects = new ArrayList<FormObject>();

        // map which contains qname list as "xpath" to configuration value and
        // list of object as config values. They will be inserted into form
        Map<List<QName>, List<Object>> valueMap = createConfigValueMap(configuration);

        PrismSchema schema = PrismSchema.parse(connector.getXmlObject().getSchema().getDefinition().getAny().get(0), prismContext);
        for (Definition definition : schema.getDefinitions()) {
            if (!(definition instanceof ResourceAttributeContainerDefinition)) {
                continue;
            }

            ResourceAttributeContainerDefinition object = (ResourceAttributeContainerDefinition) definition;
            String name = getName(object);
            FormObject formObject = new FormObject(object.getTypeName(), name);

            for (PrismPropertyDefinition property : object.getPropertyDefinitions()) {
                if (!(property instanceof ResourceAttributeDefinition)) {
                    continue;
                }

                FormAttributeDefinition attrDefinition = createFormAttributeDefinition((ResourceAttributeDefinition) property);
                List<Object> values = getAttributeValues(valueMap, object.getTypeName(), property.getName());
                formObject.getAttributes().add(new FormAttribute(attrDefinition, values));
            }
            formObject.sort();
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

    private FormAttributeDefinition createFormAttributeDefinition(ResourceAttributeDefinition def) {
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

    private Map<List<QName>, List<Object>> createConfigValueMap(ResourceConfigurationType configuration) {
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

    private String getName(ResourceAttributeContainerDefinition object) {
        String name = object.getTypeName().getLocalPart();

        if (StringUtils.isNotEmpty(object.getDisplayName())) {
            name = object.getDisplayName();
        }

        return name;
    }

    public FormObject parseSchemaForAccount(AccountShadowDto account, PrismContext prismContext)
            throws SchemaException {
        Validate.notNull(account, "Account must not be null.");

        return parseSchemaForAccount(account, account.getObjectClass(), prismContext);
    }

    public FormObject parseSchemaForAccount(AccountShadowDto account, QName accountType, PrismContext prismContext)
            throws SchemaException {
        Validate.notNull(account, "Account must not be null.");

        if (accountType == null) {
            accountType = getAccountType(account.getResource(), prismContext);
        }
        if (accountType == null) {
            throw new SchemaException("Account type was not defined.");
        }

        ResourceDto resource = account.getResource();
        PrismSchema schema = RefinedResourceSchema.getResourceSchema(resource.getXmlObject(), prismContext);
        // schema.updateSchemaAccess(resource.getXmlObject().getSchemaHandling());
        ResourceAttributeContainerDefinition definition = (ResourceAttributeContainerDefinition) schema
                .findContainerDefinitionByType(accountType);
        if (definition == null) {
            throw new SchemaException("Account definition for type '" + accountType + "' was not found.");
        }

        FormObject object = new FormObject(definition.getTypeName(), getDisplayName(resource, definition));
        Map<List<QName>, List<Object>> valueMap = createAttributeValueMap(account.getAttributes());
        for (PrismPropertyDefinition property : definition.getPropertyDefinitions()) {
            if (!(property instanceof ResourceAttributeDefinition)) {
                continue;
            }
            if (property.isIgnored()) {
                LOGGER.trace("Attr. definition: " + property.getName() + " ignored. Skipping.");
                continue;
            }
            LOGGER.trace("Attr. definition: " + property.getName());

            FormAttributeDefinition attrDefinition = createFormAttributeDefinition((ResourceAttributeDefinition) property);
            List<Object> values = getAttributeValues(valueMap, property.getName());
            object.getAttributes().add(new FormAttribute(attrDefinition, values));
        }
        object.sort();

        return object;
    }

    private String getDisplayName(ResourceDto resource, ResourceAttributeContainerDefinition definition) {
        StringBuilder displayName = new StringBuilder();
        displayName.append(resource.getName());
        displayName.append(": ");
        displayName.append(definition.getTypeName().getLocalPart());

        return displayName.toString();
    }

    private QName getAccountType(ResourceDto resourceDto, PrismContext prismContext) throws SchemaException {
        List<ResourceDto.AccountTypeDto> accountTypes = resourceDto.getAccountTypes();
        for (ResourceDto.AccountTypeDto accountTypeDto : accountTypes) {
            if (accountTypeDto.isDefault()) {
                return accountTypeDto.getObjectClass();
            }
        }

        PrismSchema schema = RefinedResourceSchema.getResourceSchema(resourceDto.getXmlObject(), prismContext);
        for (Definition definition : schema.getDefinitions()) {
            if (!(definition instanceof ResourceAttributeContainerDefinition)) {
                continue;
            }

            ResourceAttributeContainerDefinition def = (ResourceAttributeContainerDefinition) definition;
            if (def.isDefaultAccountType()) {
                return def.getTypeName();
            }
        }

        return null;
    }
}

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

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccessType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AttributeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType.AccountType;

/**
 * Schema as a collection of definitions. This is a midPoint-specific view of
 * schema definition. It is just a collection of definitions grouped under a
 * specific namespace.
 * 
 * The schema and all the public classes in this package define a schema
 * meta-model. It is supposed to be used for run-time schema interpretation. It
 * will not be a convenient tool to work with static data model objects such as
 * user or role. But it is needed for interpreting dynamic schemas for resource
 * objects, extensions and so on.
 * 
 * Schema is immutable.
 * 
 * @author Radovan Semancik
 * 
 */
public class Schema implements Dumpable, Serializable {

	private static final long serialVersionUID = 5068618465625931984L;
	private static final Trace LOGGER = TraceManager.getTrace(Schema.class);
	static final String INDENT = "  ";
	
	private String namespace;
	private Set<Definition> definitions;

	public Schema(String namespace) {
		if (StringUtils.isEmpty(namespace)) {
			throw new IllegalArgumentException("Namespace can't be null or empty.");
		}
		this.namespace = namespace;
		definitions = new HashSet<Definition>();
	}
			
	/**
	 * Returns schema namespace.
	 * 
	 * All schema definitions are placed in the returned namespace.
	 * 
	 * @return schema namespace
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * Returns set of definitions.
	 * 
	 * The set contains all definitions of all types that were parsed. Order of
	 * definitions is insignificant.
	 * 
	 * @return set of definitions
	 */
	public Set<Definition> getDefinitions() {
		if (definitions == null) {
			definitions = new HashSet<Definition>();
		}
		return definitions;
	}

	public static Schema parse(Element schema) throws SchemaException {
		if (schema == null) {
			throw new IllegalArgumentException("Input stream must not be null.");
		}

		DomToSchemaProcessor processor = new DomToSchemaProcessor();
		return processor.parseDom(schema);
	}

	public Document serializeToXsd() throws SchemaException {
		return serializeToXsd(this);
	}

	public static Document serializeToXsd(Schema schema) throws SchemaException {
		if (schema == null) {
			throw new IllegalArgumentException("Schema can't be null.");
		}

		SchemaToDomProcessor processor = new SchemaToDomProcessor();
		return processor.parseSchema(schema);
	}
	
	public PropertyContainer parsePropertyContainer(Element domElement) throws SchemaException {
		// locate appropriate definition based on the element name
		QName domElementName = DOMUtil.getQName(domElement);
		PropertyContainerDefinition propertyContainerDefinition = findItemDefinition(domElementName, PropertyContainerDefinition.class);
		if (propertyContainerDefinition==null) {
			throw new SchemaException("No definition for element "+domElementName);
		}
		return propertyContainerDefinition.parseItem(domElement);
	}

	public void updateSchemaAccess(SchemaHandlingType schemaHandling) {
		if (schemaHandling == null) {
			return;
		}

		List<AccountType> accounts = schemaHandling.getAccountType();
		for (AccountType account : accounts) {
			PropertyContainerDefinition container = findContainerDefinitionByType(account.getObjectClass());
			if (container == null) {
				continue;
			}

			List<AttributeDescriptionType> attributes = account.getAttribute();
			for (AttributeDescriptionType attribute : attributes) {
				List<AccessType> access = attribute.getAccess();
				if (access.isEmpty()) {
					continue;
				}

				PropertyDefinition property = container.findPropertyDefinition(attribute.getRef());
				if (property == null) {
					LOGGER.trace("Property {} was not found, access to attribute won't be updated.",
							new Object[] { attribute.getRef() });
					continue;
				}

				property.setCreate(access.contains(AccessType.create));
				property.setRead(access.contains(AccessType.read));
				property.setUpdate(access.contains(AccessType.update));
			}
		}
	}

	// TODO: Methods for searching the schema, such as findDefinitionByName(),
	// etc.

	/**
	 * Finds a PropertyContainerDefinition by the type name.
	 * 
	 * @param typeName
	 *            property container type name
	 * @return found property container definition
	 * @throws IllegalStateException
	 *             if more than one definition is found
	 */
	public PropertyContainerDefinition findContainerDefinitionByType(QName typeName) {
		if (typeName == null) {
			throw new IllegalArgumentException("typeName must be supplied");
		}
		// TODO: check for multiple definition with the same type
		for (Definition definition : definitions) {
			if (definition instanceof PropertyContainerDefinition
					&& typeName.equals(definition.getTypeName())) {
				return (PropertyContainerDefinition) definition;
			}
		}
		return null;
	}
	
	public ComplexTypeDefinition findComplexTypeDefinition(QName typeName) {
		if (typeName == null) {
			throw new IllegalArgumentException("typeName must be supplied");
		}
		// TODO: check for multiple definition with the same type
		for (Definition definition : definitions) {
			if (definition instanceof ComplexTypeDefinition
					&& typeName.equals(definition.getTypeName())) {
				return (ComplexTypeDefinition) definition;
			}
		}
		return null;
	}

	/**
	 * Finds item definition by name.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public <T extends ItemDefinition> T findItemDefinition(QName definitionName, Class<T> definitionType) {
		if (definitionName == null) {
			throw new IllegalArgumentException("definitionName must be supplied");
		}
		// TODO: check for multiple definition with the same type
		for (Definition definition : definitions) {
			if (definitionType.isAssignableFrom(definition.getClass())) {
				ItemDefinition idef = (ItemDefinition) definition;
				if (definitionName.equals(idef.getName())) {
					return (T) idef;
				}
			}
		}
		return null;
	}
	
	/**
	 * Finds item definition by local name
	 */
	public <T extends ItemDefinition> T findItemDefinition(String localName, Class<T> definitionType) {
		if (localName == null) {
			throw new IllegalArgumentException("localName must be supplied");
		}
		// TODO: check for multiple definition with the same type
		for (Definition definition : definitions) {
			if (definitionType.isAssignableFrom(definition.getClass())) {
				ItemDefinition idef = (ItemDefinition) definition;
				if (localName.equals(idef.getName().getLocalPart())) {
					return (T) idef;
				}
			}
		}
		return null;
	}

	public String dump() {
		StringBuilder sb = new StringBuilder();
		sb.append("Schema ns=");
		sb.append(getNamespace());
		sb.append("\n");
		for (Definition def : getDefinitions()) {
			sb.append(def.dump(1));
		}
		return sb.toString();
	}

	public boolean isEmpty() {
		return definitions.isEmpty();
	}

	/**
	 * Creates a new property container definition and adds it to the schema.
	 * 
	 * @param localTypeName
	 *            type name "relative" to schema namespace
	 * @return new property container definition
	 */
	public PropertyContainerDefinition createPropertyContainerDefinition(String localTypeName) {
		QName typeName = new QName(getNamespace(), localTypeName);
		QName name = new QName(getNamespace(), toElementName(localTypeName));
		ComplexTypeDefinition cTypeDef = new ComplexTypeDefinition(name, typeName,
				getNamespace());
		PropertyContainerDefinition def = new PropertyContainerDefinition(this, name, cTypeDef);
		definitions.add(cTypeDef);
		definitions.add(def);
		return def;
	}

	public ResourceObjectDefinition createResourceObjectDefinition(String localTypeName) {
		QName typeName = new QName(getNamespace(), localTypeName);
		return createResourceObjectDefinition(typeName);
	}

	public ResourceObjectDefinition createResourceObjectDefinition(QName typeName) {
		QName name = new QName(getNamespace(), toElementName(typeName.getLocalPart()));
		ComplexTypeDefinition cTypeDef = new ComplexTypeDefinition(name, typeName,
				getNamespace());
		ResourceObjectDefinition def = new ResourceObjectDefinition(this,name, cTypeDef);
		definitions.add(cTypeDef);
		definitions.add(def);
		return def;
	}

	
	public PropertyDefinition createPropertyDefinition(String localName, QName typeName) {
		QName name = new QName(getNamespace(), localName);
		return createPropertyDefinition(name, typeName);
	}

	public PropertyDefinition createPropertyDefinition(String localName, String localTypeName) {
		QName name = new QName(getNamespace(), localName);
		QName typeName = new QName(getNamespace(), localTypeName);
		return createPropertyDefinition(name, typeName);
	}

	public PropertyDefinition createPropertyDefinition(QName name, QName typeName) {
		PropertyDefinition def = new PropertyDefinition(name, typeName);
		definitions.add(def);
		return def;
	}

	/**
	 * @param localTypeName
	 * @return
	 */
	String toElementName(String localTypeName) {
		String elementName = StringUtils.uncapitalize(localTypeName);
		if (elementName.endsWith("Type")) {
			return elementName.substring(0, elementName.length() - 4);
		}
		return elementName;
	}



}

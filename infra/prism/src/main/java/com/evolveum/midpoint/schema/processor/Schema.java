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

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

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
public class Schema implements Dumpable, DebugDumpable, Serializable {

	private static final long serialVersionUID = 5068618465625931984L;
	private static final QName DEFAULT_XSD_TYPE = DOMUtil.XSD_STRING;

	private static final Trace LOGGER = TraceManager.getTrace(Schema.class);
	
	protected String namespace;
	protected Set<Definition> definitions;

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
	public Collection<Definition> getDefinitions() {
		if (definitions == null) {
			definitions = new HashSet<Definition>();
		}
		return definitions;
	}

	public <T extends Definition> Collection<T> getDefinitions(Class<T> type) {
		Collection<T> defs = new HashSet<T>();
		for (Definition def: getDefinitions()) {
			if (type.isAssignableFrom(def.getClass())) {
				defs.add((T) def);
			}
		}
		return defs;
	}
	
	public static Schema parse(Element element) throws SchemaException {
		return parse(element,null);
	}
	
	public static Schema parse(Element element, EntityResolver resolver) throws SchemaException {
		if (element == null) {
			throw new IllegalArgumentException("Schema DOM element must not be null.");
		}

		DomToSchemaProcessor processor = new DomToSchemaProcessor();
		processor.setEntityResolver(resolver);
		return processor.parseDom(element);
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
		PropertyContainerDefinition propertyContainerDefinition = findItemDefinition(domElementName,
				PropertyContainerDefinition.class);
		if (propertyContainerDefinition == null) {
			throw new SchemaException("No definition for element " + domElementName);
		}
		return propertyContainerDefinition.parseItem(domElement);
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
		return findContainerDefinitionByType(typeName,PropertyContainerDefinition.class);
	}
	
	public ObjectDefinition findObjectDefinitionByType(QName typeName) {
		return findContainerDefinitionByType(typeName,ObjectDefinition.class);
	}

	public <T extends ObjectType> ObjectDefinition<T> findObjectDefinitionByType(QName typeName, Class<T> type) {
		return findContainerDefinitionByType(typeName,ObjectDefinition.class);
	}

	public <T extends ObjectType> ObjectDefinition<T> findObjectDefinition(ObjectTypes objectType, Class<T> type) {
		return findContainerDefinitionByType(objectType.getTypeQName(),ObjectDefinition.class);
	}
	
	public <T extends ObjectType> ObjectDefinition<T> findObjectDefinition(Class<T> type) {
		return findContainerDefinitionByType(ObjectTypes.getObjectType(type).getTypeQName(),ObjectDefinition.class);
	}

	private <T extends PropertyContainerDefinition> T findContainerDefinitionByType(QName typeName, Class<T> type) {
		if (typeName == null) {
			throw new IllegalArgumentException("typeName must be supplied");
		}
		// TODO: check for multiple definition with the same type
		for (Definition definition : definitions) {
			if (type.isAssignableFrom(definition.getClass())
					&& typeName.equals(definition.getTypeName())) {
				return (T) definition;
			}
		}
		return null;
	}
	
	public PropertyContainerDefinition findContainerDefinition(Class<? extends ObjectType> type, PropertyPath path) {
		ObjectTypes objectType = ObjectTypes.getObjectType(type);
		ObjectDefinition objectDefinition = findObjectDefinitionByType(objectType.getTypeQName());
		if (objectDefinition == null) {
			throw new IllegalArgumentException("The definition of object type "+type.getSimpleName()+" not found in the schema");
		}
		return objectDefinition.findItemDefinition(path, PropertyContainerDefinition.class);
	}

	/**
	 * Finds complex type definition by type name.
	 */
	public ComplexTypeDefinition findComplexTypeDefinition(QName typeName) {
		if (typeName == null) {
			throw new IllegalArgumentException("typeName must be supplied");
		}
		// TODO: check for multiple definition with the same type
		for (Definition definition : definitions) {
			if (definition instanceof ComplexTypeDefinition && typeName.equals(definition.getTypeName())) {
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

	/**
	 * Finds item definition by type.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public <T extends ItemDefinition> T findItemDefinitionByType(QName typeName, Class<T> definitionType) {
		if (typeName == null) {
			throw new IllegalArgumentException("typeName must be supplied");
		}
		// TODO: check for multiple definition with the same type
		for (Definition definition : definitions) {
			if (definitionType.isAssignableFrom(definition.getClass())) {
				ItemDefinition idef = (ItemDefinition) definition;
				if (typeName.equals(idef.getTypeName())) {
					return (T) idef;
				}
			}
		}
		return null;
	}

	public boolean isEmpty() {
		return definitions.isEmpty();
	}

	/**
	 * Creates a new property container definition and adds it to the schema.
	 * 
	 * This is a preferred way how to create definition in the schema.
	 * 
	 * @param localTypeName
	 *            type name "relative" to schema namespace
	 * @return new property container definition
	 */
	public PropertyContainerDefinition createPropertyContainerDefinition(String localTypeName) {
		QName typeName = new QName(getNamespace(), localTypeName);
		QName name = new QName(getNamespace(), toElementName(localTypeName));
		ComplexTypeDefinition cTypeDef = new ComplexTypeDefinition(name, typeName, getNamespace());
		PropertyContainerDefinition def = new PropertyContainerDefinition(this, name, cTypeDef);
		definitions.add(cTypeDef);
		definitions.add(def);
		return def;
	}

	/**
	 * Creates a new resource object definition and adds it to the schema.
	 * 
	 * This is a preferred way how to create definition in the schema.
	 * 
	 * @param localTypeName
	 *            type name "relative" to schema namespace
	 * @return new resource object definition
	 */
	public ResourceObjectDefinition createResourceObjectDefinition(String localTypeName) {
		QName typeName = new QName(getNamespace(), localTypeName);
		return createResourceObjectDefinition(typeName);
	}

	/**
	 * Creates a new resource object definition and adds it to the schema.
	 * 
	 * This is a preferred way how to create definition in the schema.
	 * 
	 * @param localTypeName
	 *            type QName
	 * @return new resource object definition
	 */
	public ResourceObjectDefinition createResourceObjectDefinition(QName typeName) {
		QName name = new QName(getNamespace(), toElementName(typeName.getLocalPart()));
		ComplexTypeDefinition cTypeDef = new ComplexTypeDefinition(name, typeName, getNamespace());
		ResourceObjectDefinition def = new ResourceObjectDefinition(this, name, cTypeDef);
		definitions.add(cTypeDef);
		definitions.add(def);
		return def;
	}

	/**
	 * Creates a top-level property definition and adds it to the schema.
	 * 
	 * This is a preferred way how to create definition in the schema.
	 * 
	 * @param localName
	 *            element name "relative" to schema namespace
	 * @param typeName
	 *            XSD type name of the element
	 * @return new property definition
	 */
	public PropertyDefinition createPropertyDefinition(String localName, QName typeName) {
		QName name = new QName(getNamespace(), localName);
		return createPropertyDefinition(name, typeName);
	}

	/**
	 * Creates a top-level property definition and adds it to the schema.
	 * 
	 * This is a preferred way how to create definition in the schema.
	 * 
	 * @param localName
	 *            element name "relative" to schema namespace
	 * @param localTypeName
	 *            XSD type name "relative" to schema namespace
	 * @return new property definition
	 */
	public PropertyDefinition createPropertyDefinition(String localName, String localTypeName) {
		QName name = new QName(getNamespace(), localName);
		QName typeName = new QName(getNamespace(), localTypeName);
		return createPropertyDefinition(name, typeName);
	}

	/**
	 * Creates a top-level property definition and adds it to the schema.
	 * 
	 * This is a preferred way how to create definition in the schema.
	 * 
	 * @param localName
	 *            element name
	 * @param typeName
	 *            XSD type name of the element
	 * @return new property definition
	 */
	public PropertyDefinition createPropertyDefinition(QName name, QName typeName) {
		PropertyDefinition def = new PropertyDefinition(name, name, typeName);
		definitions.add(def);
		return def;
	}

	/**
	 * Internal method to create a "nice" element name from the type name.
	 */
	String toElementName(String localTypeName) {
		String elementName = StringUtils.uncapitalize(localTypeName);
		if (elementName.endsWith("Type")) {
			return elementName.substring(0, elementName.length() - 4);
		}
		return elementName;
	}

	/**
	 * Looks for a default account ObjectClass.
	 */
	public ResourceObjectDefinition findAccountDefinition() {
		return findAccountDefinition(null);
	}
	
	/**
	 * Looks for a specific account ObjectClass.
	 */
	public ResourceObjectDefinition findAccountDefinition(String accountType) {
		for (Definition def: definitions) {
			if (def instanceof ResourceObjectDefinition) {
				ResourceObjectDefinition rod = (ResourceObjectDefinition)def;
				if (rod.isAccountType()) {
					if (accountType == null && rod.isDefaultAccountType()) {
						// Default account requested, default account found.
						return rod;
					}
// TODO
//					if (rod.getAccountType .....)
				}
			}
		}
		return null;
	}

	public ResourceObjectDefinition findResourceObjectDefinitionByType(QName typeName) {
		return findItemDefinitionByType(typeName, ResourceObjectDefinition.class);
	}

	public Collection<? extends ResourceObjectDefinition> getAccountDefinitions() {
		Set<ResourceObjectDefinition> accounts = new HashSet<ResourceObjectDefinition>();
		for (Definition def: definitions) {
			if (def instanceof ResourceObjectDefinition) {
				ResourceObjectDefinition rod = (ResourceObjectDefinition)def;
				if (rod.isAccountType()) {
					accounts.add(rod);
				}
			}
		}
		return accounts;
	}
	
	/**
	 * Try to locate xsi:type definition in the elements and return appropriate ItemDefinition.
	 */
	public static ItemDefinition resolveDynamicItemDefinition(ItemDefinition parentDefinition, List<Object> valueElements) {
		QName typeName = null;
		QName elementName = null;
		for (Object element: valueElements) {
			if (elementName == null) {
				elementName = JAXBUtil.getElementQName(element);
			}
			// TODO: try JAXB types
			if (element instanceof Element) {
				Element domElement = (Element)element;
				if (DOMUtil.hasXsiType(domElement)) {
					typeName = DOMUtil.resolveXsiType(domElement);
					if (typeName != null) {
						break;
					}
				}
			}
		}
		// FIXME: now the definition assumes property, may also be property container?
		if (typeName == null) {
			return null;
		}
		PropertyDefinition propDef = new PropertyDefinition(elementName, typeName);
		// Set it to multi-value to be on the safe side
		propDef.setMaxOccurs(-1);
		// TODO: set "dynamic" flag
		return propDef;
	}
	
	/**
	 * Create default ItemDefinition. Used as a last attempt to provide some useful definition. Kind of a hack.
	 */
	public static ItemDefinition createDefaultItemDefinition(ItemDefinition parentDefinition, List<Object> valueElements) {
		QName elementName = null;
		for (Object element: valueElements) {
			if (elementName == null) {
				elementName = JAXBUtil.getElementQName(element);
				break;
			}
		}
		PropertyDefinition propDef = new PropertyDefinition(elementName, DEFAULT_XSD_TYPE);
		// Set it to multi-value to be on the safe side
		propDef.setMaxOccurs(-1);
		// TODO: set "dynamic" flag
		return propDef;
	}
	
	
	public <T extends ObjectType> MidPointObject<T> parseObjectType(T objectType) throws SchemaException {
		ObjectDefinition<T> objectDefinition = (ObjectDefinition<T>) findObjectDefinition(objectType.getClass());
		if (objectDefinition == null) {
			throw new IllegalArgumentException("No definition for object type "+objectType);
		}
		return objectDefinition.parseObjectType(objectType);
	}
	
	public <T extends ObjectType> MidPointObject<T> parseObject(String stringXml, Class<T> type) throws SchemaException {
		ObjectDefinition<T> objectDefinition = findObjectDefinition(type);
		JAXBElement<T> jaxbElement;
		try {
			jaxbElement = JAXBUtil.unmarshal(type, stringXml);
		} catch (JAXBException e) {
			throw new SchemaException("Error parsing the XML: "+e.getMessage(),e);
		}
        T objectType = jaxbElement.getValue();
        MidPointObject<T> object = objectDefinition.parseObjectType(objectType);
        return object;
	}

	public <T extends ObjectType> MidPointObject<T> parseObject(File xmlFile, Class<T> type) throws SchemaException {
		ObjectDefinition<T> objectDefinition = findObjectDefinition(type);
		JAXBElement<T> jaxbElement;
		try {
			jaxbElement = JAXBUtil.unmarshal(xmlFile, type);
		} catch (JAXBException e) {
			throw new SchemaException("Error parsing the XML: "+e.getMessage(),e);
		}
        T objectType = jaxbElement.getValue();
        MidPointObject<T> object = objectDefinition.parseObjectType(objectType);
        return object;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<indent;i++) {
			sb.append(INDENT_STRING);
		}
		sb.append(toString()).append("\n");
		Iterator<Definition> i = definitions.iterator();
		while (i.hasNext()) {
			Definition def = i.next();
			sb.append(def.debugDump(indent+1));
			if (i.hasNext()) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	@Override
	public String dump() {
		return debugDump(0);
	}

	@Override
	public String toString() {
		return "Schema(ns=" + namespace + ")";
	}
	
}

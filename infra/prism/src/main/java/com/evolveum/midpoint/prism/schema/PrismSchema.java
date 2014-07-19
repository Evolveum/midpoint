/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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
 * @author Radovan Semancik
 * 
 */
public class PrismSchema implements DebugDumpable {

	private static final long serialVersionUID = 5068618465625931984L;

	private static final Trace LOGGER = TraceManager.getTrace(PrismSchema.class);
	
	protected String namespace;
	protected Collection<Definition> definitions;
	protected PrismContext prismContext;

	protected PrismSchema(PrismContext prismContext) {
		this.prismContext = prismContext;
		definitions = new ArrayList<Definition>();
	}
	
	public PrismSchema(String namespace, PrismContext prismContext) {
		if (StringUtils.isEmpty(namespace)) {
			throw new IllegalArgumentException("Namespace can't be null or empty.");
		}
		this.namespace = namespace;
		this.prismContext = prismContext;
		definitions = new ArrayList<Definition>();
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

	public void setNamespace(String namespace) {
		this.namespace = namespace;
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

    public Collection<PrismObjectDefinition> getObjectDefinitions() {
        return getDefinitions(PrismObjectDefinition.class);
    }

    public Collection<ComplexTypeDefinition> getComplexTypeDefinitions() {
        return getDefinitions(ComplexTypeDefinition.class);
    }
	
	public void add(Definition def) {
		definitions.add(def);
	}
	
	public PrismContext getPrismContext() {
		return prismContext;
	}
	
	
	// TODO: cleanup this chaos
	public static PrismSchema parse(Element element, boolean isRuntime, String shortDescription, PrismContext prismContext) throws SchemaException {
		return parse(element, prismContext.getSchemaRegistry(), new PrismSchema(prismContext), isRuntime, shortDescription, prismContext);
	}
	
	public static PrismSchema parse(Element element, EntityResolver resolver, boolean isRuntime, String shortDescription, PrismContext prismContext) throws SchemaException {
		return parse(element, resolver, new PrismSchema(prismContext), isRuntime, shortDescription, prismContext);
	}
	
	protected static PrismSchema parse(Element element, PrismSchema schema, boolean isRuntime, String shortDescription, PrismContext prismContext) throws SchemaException {
		return parse(element, prismContext.getSchemaRegistry(), schema, isRuntime, shortDescription, prismContext);
	}
	
	protected static PrismSchema parse(Element element, EntityResolver resolver, PrismSchema schema, boolean isRuntime, String shortDescription, PrismContext prismContext) throws SchemaException {
		if (element == null) {
			throw new IllegalArgumentException("Schema element must not be null in "+shortDescription);
		}

		DomToSchemaProcessor processor = new DomToSchemaProcessor();
		processor.setEntityResolver(resolver);
		processor.setPrismContext(prismContext);
		processor.setShortDescription(shortDescription);
		processor.setRuntime(isRuntime);
		return processor.parseDom(schema, element);
	}

	
	public Document serializeToXsd() throws SchemaException {
		SchemaToDomProcessor processor = new SchemaToDomProcessor();
		processor.setPrismContext(prismContext);
		return processor.parseSchema(this);
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
	public PrismContainerDefinition findContainerDefinitionByType(QName typeName) {
		return findContainerDefinitionByType(typeName,PrismContainerDefinition.class);
	}
	
	public <X extends Objectable> PrismObjectDefinition<X> findObjectDefinitionByType(QName typeName) {
		return findContainerDefinitionByType(typeName,PrismObjectDefinition.class);
	}
	
	public <X extends Objectable> PrismObjectDefinition<X> findObjectDefinitionByTypeAssumeNs(QName typeName) {
		return findContainerDefinitionByTypeAssumeNs(typeName,PrismObjectDefinition.class);
	}
	
	public <X extends Objectable> PrismObjectDefinition<X> findObjectDefinitionByElementName(QName elementName) {
		return findContainerDefinitionByElementName(elementName, PrismObjectDefinition.class);
	}

	public <T extends Objectable> PrismObjectDefinition<T> findObjectDefinitionByType(QName typeName, Class<T> type) {
		return findContainerDefinitionByType(typeName,PrismObjectDefinition.class);
	}
	
	public <T extends Objectable> PrismObjectDefinition<T> findObjectDefinitionByCompileTimeClass(Class<T> type) {
		for (Definition def: getDefinitions()) {
			if (def instanceof PrismObjectDefinition<?>) {
				PrismObjectDefinition<?> objDef = (PrismObjectDefinition<?>)def;
				if (type.equals(objDef.getCompileTimeClass())) {
					return (PrismObjectDefinition<T>) objDef;
				}
			}
		}
		return null;
	}

	private <T extends PrismContainerDefinition> T findContainerDefinitionByType(QName typeName, Class<T> type) {
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
	
	private <T extends PrismContainerDefinition> T findContainerDefinitionByTypeAssumeNs(QName typeName, Class<T> type) {
		if (typeName == null) {
			throw new IllegalArgumentException("typeName must be supplied");
		}
		// TODO: check for multiple definition with the same type
		for (Definition definition : definitions) {
			if (type.isAssignableFrom(definition.getClass())
					&& QNameUtil.match(typeName,definition.getTypeName())) {
				return (T) definition;
			}
		}
		return null;
	}
	
	public <C extends Containerable> PrismContainerDefinition<C> findContainerDefinitionByElementName(QName elementName) {
		return findContainerDefinitionByElementName(elementName, PrismContainerDefinition.class);
	}

	private <T extends PrismContainerDefinition> T findContainerDefinitionByElementName(QName elementName, Class<T> type) {
		if (elementName == null) {
			throw new IllegalArgumentException("elementName must be supplied");
		}
		// TODO: check for multiple definition with the same type
		for (Definition definition : definitions) {
			if (type.isAssignableFrom(definition.getClass())
					&& elementName.equals(((PrismContainerDefinition)definition).getName())) {
				return (T) definition;
			}
		}
		return null;
	}
	
	public <C extends Containerable> PrismContainerDefinition<C> findContainerDefinitionByCompileTimeClass(Class<C> type) {
		for (Definition def: getDefinitions()) {
			if (def instanceof PrismContainerDefinition<?>) {
				PrismContainerDefinition<C> contDef = (PrismContainerDefinition<C>)def;
				if (type.equals(contDef.getCompileTimeClass())) {
					return contDef;
				}
			}
		}
		return null;
	}

	public PrismPropertyDefinition findPropertyDefinitionByElementName(QName elementName) {
		return findPropertyDefinitionByElementName(elementName, PrismPropertyDefinition.class);
	}

    public PrismReferenceDefinition findReferenceDefinitionByElementName(QName elementName) {
        return findReferenceDefinitionByElementName(elementName, PrismReferenceDefinition.class);
    }

    private <T extends PrismPropertyDefinition> T findPropertyDefinitionByElementName(QName elementName, Class<T> type) {
		if (elementName == null) {
			throw new IllegalArgumentException("elementName must be supplied");
		}
		// TODO: check for multiple definition with the same type
		for (Definition definition : definitions) {
			if (type.isAssignableFrom(definition.getClass())
					&& elementName.equals(((PrismPropertyDefinition)definition).getName())) {
				return (T) definition;
			}
		}
		return null;
	}

    private <T extends PrismReferenceDefinition> T findReferenceDefinitionByElementName(QName elementName, Class<T> type) {
        if (elementName == null) {
            throw new IllegalArgumentException("elementName must be supplied");
        }
        // TODO: check for multiple definition with the same type
        for (Definition definition : definitions) {
            if (type.isAssignableFrom(definition.getClass())
                    && elementName.equals(((PrismReferenceDefinition)definition).getName())) {
                return (T) definition;
            }
        }
        return null;
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
	public PrismContainerDefinition createPropertyContainerDefinition(String localTypeName) {
		QName typeName = new QName(getNamespace(), localTypeName);
		QName name = new QName(getNamespace(), toElementName(localTypeName));
		ComplexTypeDefinition cTypeDef = new ComplexTypeDefinition(typeName, prismContext);
		PrismContainerDefinition def = new PrismContainerDefinition(name, cTypeDef, prismContext);
		definitions.add(cTypeDef);
		definitions.add(def);
		return def;
	}
	
	public PrismContainerDefinition createPropertyContainerDefinition(String localElementName, String localTypeName) {
		QName typeName = new QName(getNamespace(), localTypeName);
		QName name = new QName(getNamespace(), localElementName);
		ComplexTypeDefinition cTypeDef = findComplexTypeDefinition(typeName);
		if (cTypeDef == null) {
			cTypeDef = new ComplexTypeDefinition(typeName, prismContext);
			definitions.add(cTypeDef);
		}
		PrismContainerDefinition def = new PrismContainerDefinition(name, cTypeDef, prismContext);		
		definitions.add(def);
		return def;
	}
	
	public ComplexTypeDefinition createComplexTypeDefinition(QName typeName) {
		ComplexTypeDefinition cTypeDef = new ComplexTypeDefinition(typeName, prismContext);
		definitions.add(cTypeDef);
		return cTypeDef;
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
	public PrismPropertyDefinition createPropertyDefinition(String localName, QName typeName) {
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
	public PrismPropertyDefinition createPropertyDefinition(String localName, String localTypeName) {
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
	public PrismPropertyDefinition createPropertyDefinition(QName name, QName typeName) {
		PrismPropertyDefinition def = new PrismPropertyDefinition(name, typeName, prismContext);
		definitions.add(def);
		return def;
	}

	/**
	 * Internal method to create a "nice" element name from the type name.
	 */
	protected String toElementName(String localTypeName) {
		String elementName = StringUtils.uncapitalize(localTypeName);
		if (elementName.endsWith("Type")) {
			return elementName.substring(0, elementName.length() - 4);
		}
		return elementName;
	}
	
	protected QName toElementQName(QName qname) {
		return new QName(qname.getNamespaceURI(), toElementName(qname.getLocalPart()));
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
	public String toString() {
		return "Schema(ns=" + namespace + ")";
	}
	
}

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

import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.springframework.beans.PropertyAccessException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Definition of a property container.
 * 
 * Property container groups properties into logical blocks. The reason for
 * grouping may be as simple as better understandability of data structure. But
 * the group usually means different meaning, source or structure of the data.
 * For example, the property container is frequently used to hold properties
 * that are dynamic, not fixed by a static schema. Such grouping also naturally
 * translates to XML and helps to "quarantine" such properties to avoid Unique
 * Particle Attribute problems.
 * 
 * Property Container contains a set of (potentially multi-valued) properties.
 * The order of properties is not significant, regardless of the fact that it
 * may be fixed in the XML representation. In the XML representation, each
 * element inside Property Container must be either Property or a Property
 * Container.
 * 
 * This class represents schema definition for property container. See
 * {@link Definition} for more details.
 * 
 * @author Radovan Semancik
 * 
 */
public class PropertyContainerDefinition extends ItemDefinition {

	private static final long serialVersionUID = -5068923696147960699L;
	protected String schemaNamespace;
	protected ComplexTypeDefinition complexTypeDefinition;
	protected Schema schema;

	PropertyContainerDefinition(QName name, ComplexTypeDefinition complexTypeDefinition) {
		super(name, complexTypeDefinition.getDefaultName(), complexTypeDefinition.getTypeName());
		this.complexTypeDefinition = complexTypeDefinition;
	}

	PropertyContainerDefinition(Schema schema, QName name, ComplexTypeDefinition complexTypeDefinition) {
		super(name, complexTypeDefinition.getDefaultName(), complexTypeDefinition.getTypeName());
		this.complexTypeDefinition = complexTypeDefinition;
		if (schema == null) {
			throw new IllegalArgumentException("Schema can't be null.");
		}
		this.schema = schema;
	}

	protected String getSchemaNamespace() {
		return schema.getNamespace();
	}
	
	ComplexTypeDefinition getComplexTypeDefinition() {
		return complexTypeDefinition;
	}
	
	protected <T extends ItemDefinition> T findItemDefinition(QName name, Class<T> clazz) {
		for (ItemDefinition def : getDefinitions()) {
			if (clazz.isAssignableFrom(def.getClass()) && name.equals(def.getName())) {
				return (T) def;
			}
		}
		return null;
	}
	
	public ItemDefinition findItemDefinition(QName name) {
		return findItemDefinition(name,ItemDefinition.class);
	}
	
	/**
	 * Finds a PropertyDefinition by looking at the property name.
	 * 
	 * Returns null if nothing is found.
	 * 
	 * @param name
	 *            property definition name
	 * @return found property definition of null
	 */
	public PropertyDefinition findPropertyDefinition(QName name) {
		return findItemDefinition(name,PropertyDefinition.class);
	}
	
	/**
	 * TODO
	 * Returns set of property definitions.
	 * 
	 * The set contains all property definitions of all types that were parsed.
	 * Order of definitions is insignificant.
	 * 
	 * @return set of definitions
	 */
	public Set<ItemDefinition> getDefinitions() {
		return complexTypeDefinition.getDefinitions();
	}

	/**
	 * Returns set of property definitions.
	 * 
	 * The set contains all property definitions of all types that were parsed.
	 * Order of definitions is insignificant.
	 * 
	 * @return set of definitions
	 */
	public Set<PropertyDefinition> getPropertyDefinitions() {
		Set<PropertyDefinition> props = new HashSet<PropertyDefinition>();
		for (ItemDefinition def: complexTypeDefinition.getDefinitions()) {
			if (def instanceof PropertyDefinition) {
				props.add((PropertyDefinition)def);
			}
		}
		return props;
	}

	public PropertyContainer instantiate() {
		return new PropertyContainer(getNameOrDefaultName(), this);
	}
	
	public PropertyContainer instantiate(QName name) {
		return new PropertyContainer(name, this);
	}
	
	/**
	 * @param domElement
	 * @return
	 * @throws SchemaException 
	 */
	public PropertyContainer parsePropertyContainer(Element domElement) throws SchemaException {
		QName domElementName = DOMUtil.getQName(domElement);
		PropertyContainer container = instantiate(domElementName);
		List<Object> elements = JAXBUtil.listChildElements(domElement);
		Set<Item> newItems = parseItems(elements);
		container.getItems().addAll(newItems);
		return container;
	}

	public PropertyContainer parseItem(Object element) throws SchemaException {
		List<Object> elements = new ArrayList<Object>();
		elements.add(element);
		return parseItem(elements);
	}
	
	@Override
	public PropertyContainer parseItem(List<Object> elements) throws SchemaException {
		if (elements == null || elements.isEmpty()) {
			return null;
		}
		if (elements.size()>1) {
			throw new IllegalArgumentException("Cannot parse container from more than one element");
		}
		return parseItem(elements.get(0),PropertyContainer.class);
	}
	
	/**
	 * @param element
	 * @param class1
	 * @return
	 * @throws SchemaException 
	 */
	protected <T extends PropertyContainer> T parseItem(Object element, Class<T> type) throws SchemaException {
		QName elementQName = JAXBUtil.getElementQName(element);
		T container = (T) this.instantiate(elementQName);
		List<Object> childElements = JAXBUtil.listChildElements(element);
		container.getItems().addAll(parseItems(childElements));
		return container;
	}

	/**
	 * Parses properties from a list of elements.
	 * 
	 * The elements must describe properties as defined by this
	 * PropertyContainerDefinition. Serializes all the elements
	 * from the provided list.
	 * 
	 * min/max constraints are not checked now
	 * TODO: maybe we need to check them
	 * 
	 * @param elements list of elements with serialized properties
	 * @return set of deserialized properties
	 * @throws SchemaProcessorException 
	 */
	public Set<Item> parseItems(List<Object> elements) throws SchemaException {
		return parseItems(elements,null);
	}
		
	/**
	 * Same as parseProperties(List<Element> elements, Class<T> clazz), but
	 * selects only some of the properties to parse. Other properties are
	 * ignored.
	 * 
	 * Useful to parse identifiers out of complete object or similar things.
	 * Used by subclasses.
	 * @throws SchemaProcessorException 
	 */
	protected Set<Item> parseItems(List<Object> elements, Set<? extends ItemDefinition> selection) throws SchemaException {
		
		// TODO: more robustness in handling schema violations (min/max constraints, etc.)
		
		Set<Item> props = new HashSet<Item>();
		
		// Iterate over all the XML elements there. Each element is
		// an attribute.
		for(int i = 0; i < elements.size(); i++) {
			Object propElement = elements.get(i);
			QName elementQName = JAXBUtil.getElementQName(propElement);
			// Collect all elements with the same QName
			List<Object> valueElements = new ArrayList<Object>();
			valueElements.add(propElement);
			while (i + 1 < elements.size()
					   && elementQName.equals(JAXBUtil.getElementQName(elements.get(i + 1)))) {
					i++;
					valueElements.add(elements.get(i));
			}
			
			// If there was a selection filter specified, filter out the
			// properties that are not in the filter.
			
			// Quite an ugly code. TODO: clean it up
			if (selection!=null) {
				boolean selected=false;
				for (ItemDefinition selProdDef : selection) {
					if (selProdDef.getNameOrDefaultName().equals(elementQName)) {
						selected = true;
					}
				}
				if (!selected) {
					continue;
				}
			}
			
			// Find item definition from the schema
			ItemDefinition def = findItemDefinition(elementQName);
			if (def==null) {
				throw new SchemaException("Item "+elementQName+" has no definition",elementQName);
			}
			
			Item item = def.parseItem(valueElements);	
			props.add(item);
		}
		return props;
	}
	
	@Override
	public String dump(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<indent; i++) {
			sb.append(Schema.INDENT);
		}
		sb.append(toString());
		sb.append("\n");
		for (Definition def : getDefinitions()) {
			sb.append(def.dump(indent+1));
		}
		return sb.toString();
	}

	public PropertyDefinition createPropertyDefinition(QName name, QName typeName) {
		PropertyDefinition propDef = new PropertyDefinition(name, typeName);
		getDefinitions().add(propDef);
		return propDef;
	}
	
	// Creates reference to other schema
	// TODO: maybe check if the name is in different namespace
	// TODO: maybe create entirely new concept of property reference?
	public PropertyDefinition createPropertyDefinition(QName name) {
		PropertyDefinition propDef = new PropertyDefinition(name);
		getDefinitions().add(propDef);
		return propDef;
	}

	public PropertyDefinition createPropertyDefinition(String localName, QName typeName) {
		QName name = new QName(getSchemaNamespace(),localName);
		return createPropertyDefinition(name,typeName);
	}

	
	public PropertyDefinition createPropertyDefinition(String localName, String localTypeName) {
		QName name = new QName(getSchemaNamespace(),localName);
		QName typeName = new QName(getSchemaNamespace(),localTypeName);
		return createPropertyDefinition(name,typeName);
	}


	/**
	 * @return
	 */
	public boolean isEmpty() {
		return complexTypeDefinition.isEmpty();
	}

}

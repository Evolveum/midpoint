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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.NotImplementedException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.PropertyModification.ModificationType;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Dumpable;

/**
 * <p>
 * Property container groups properties into logical blocks.The reason for
 * grouping may be as simple as better understandability of data structure. But
 * the group usually means different meaning, source or structure of the data.
 * For example, the property container is frequently used to hold properties
 * that are dynamic, not fixed by a static schema. Such grouping also naturally
 * translates to XML and helps to "quarantine" such properties to avoid Unique
 * Particle Attribute problems.
 * </p><p>
 * Property Container contains a set of (potentially multi-valued) properties or inner property containers.
 * The order of properties is not significant, regardless of the fact that it
 * may be fixed in the XML representation. In the XML representation, each
 * element inside Property Container must be either Property or a Property
 * Container.
 * </p><p>
 * Property Container is mutable.
 * </p>
 * @author Radovan Semancik
 * 
 */
public class PropertyContainer extends Item implements Serializable {
	private static final long serialVersionUID = 5206821250098051028L;
	
	private Set<Item> items = new HashSet<Item>();

	public PropertyContainer() {
		super();
	}

	public PropertyContainer(QName name, PropertyContainerDefinition definition) {
		super(name,definition);
	}

	public PropertyContainer(QName name) {
		super(name);
	}

	/**
	 * Returns a set of items that the property container contains. The items may be properties or inner property containers.
	 * 
	 * The set must not be null. In case there are no properties an empty set is
	 * returned.
	 * 
	 * Returned set is mutable. Live object is returned.
	 * 
	 * @return set of items that the property container contains.
	 */
	public Set<Item> getItems() {
		return items;
	}
	
	/**
	 * Returns a set of properties that the property container contains.
	 * 
	 * The set must not be null. In case there are no properties an empty set is
	 * returned.
	 * 
	 * Returned set is immutable! Any change to it will be ignored.
	 * 
	 * @return set of properties that the property container contains.
	 */
	public Set<Property> getProperties() {
		Set<Property> properties = new HashSet<Property>();
		for (Item item : items) {
			if (item instanceof Property) {
				properties.add((Property)item);
			}
		}
		return properties;
	}
	
	/**
	 * Adds an item to a property container.
	 * @param item item to add.
	 */
	public void add(Item item) {
		items.add(item);
	}

	/**
	 * Adds a collection of items to a property container.
	 * @param itemsToAdd items to add
	 */
	public void addAll(Collection<? extends Item> itemsToAdd) {
		items.addAll(itemsToAdd);
	}

	
	/**
	 * Returns applicable property container definition.
	 * 
	 * May return null if no definition is applicable or the definition is not
	 * know.
	 * 
	 * @return applicable property container definition
	 */
	public PropertyContainerDefinition getDefinition() {
		return (PropertyContainerDefinition) definition;
	}

	/**
	 * Sets applicable property container definition.
	 * 
	 * @param definition
	 *            the definition to set
	 */
	public void setDefinition(PropertyContainerDefinition definition) {
		this.definition = definition;
	}

	/**
	 * Finds a specific property in the container by name.
	 * 
	 * Returns null if nothing is found.
	 * 
	 * @param propertyQName
	 *            property name to find.
	 * @return found property or null
	 */
	public Property findProperty(QName propertyQName) {
		for (Item item : items) {
			if (item instanceof Property && propertyQName.equals(item.getName())) {
				return (Property) item;
			}
		}
		return null;
	}

	/**
	 * Finds a specific property in the container by name.
	 * 
	 * Returns null if nothing is found.
	 * 
	 * @param itemQName
	 *            property name to find.
	 * @return found property or null
	 */
	public Item findItem(QName itemQName) {
		for (Item item : items) {
			if (itemQName.equals(item.getName())) {
				return item;
			}
		}
		return null;
	}

	
	/**
	 * Finds a specific property in the container by definition.
	 * 
	 * Returns null if nothing is found.
	 * 
	 * @param propertyDefinition
	 *            property definition to find.
	 * @return found property or null
	 */
	public Item findItem(ItemDefinition itemDefinition) {
		if (itemDefinition==null) {
			throw new IllegalArgumentException("No item definition");
		}
		return findItem(itemDefinition.getName());
	}
	
	/**
	 * Finds a specific property in the container by definition.
	 * 
	 * Returns null if nothing is found.
	 * 
	 * @param propertyDefinition
	 *            property definition to find.
	 * @return found property or null
	 */
	public Property findProperty(PropertyDefinition propertyDefinition) {
		if (propertyDefinition==null) {
			throw new IllegalArgumentException("No property definition");
		}
		return findProperty(propertyDefinition.getName());
	}

	@Override
	public void serializeToDom(Node parentNode) throws SchemaException {
		if (parentNode==null) {
			throw new IllegalArgumentException("No parent node specified");
		}
		Element containerElement = DOMUtil.getDocument(parentNode).createElementNS(name.getNamespaceURI(), name.getLocalPart());
		for (Item item : items) {
			item.serializeToDom(containerElement);
		}
	}
	
	/**
	 * Serialize properties to DOM or JAXB Elements.
	 * 
	 * The properties are serialized to DOM and returned as a list.
	 * The property container element is not serialized. 
	 * 
	 * @param doc DOM Document
	 * @return list of serialized properties
	 * @throws SchemaProcessorException the schema definition is missing or is inconsistent
	 */
	public List<Object> serializePropertiesToJaxb(Document doc) throws SchemaException {
		List<Object> elements = new ArrayList<Object>();
		// This is not really correct. We should follow the ordering of elements
		// in the schema so we produce valid XML
		// TODO: FIXME
		for (Item item : items) {
			if (item instanceof Property) {
				Property prop = (Property)item;
				if (prop.getDefinition()!=null) {
					elements.addAll(prop.serializeToJaxb(doc));
				} else {
					elements.addAll(prop.serializeToJaxb(doc,getDefinition().findPropertyDefinition(prop.getName())));
				}
			}
		}
		return elements;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getName() + "):"
				+ getItems();
	}

	public String dump() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName()).append("(").append(getName()).append(")\n");
		for (Item item : getItems()) {
			sb.append("  ");
			sb.append(item.dump());
		}
		return sb.toString();
	}

	public boolean isEmpty() {
		return items.isEmpty();
	}

	public void applyModifications(List<PropertyModification> modifications) {
		for (PropertyModification modification : modifications) {
			applyModification(modification);
		}
	}

	public void applyModification(PropertyModification modification) {
		// TODO Auto-generated method stub
		if (modification.getPath() == null || modification.getPath().isEmpty()) {
			// Modification in this container
			
			Property property = findProperty(modification.getPropertyName());
			if (modification.getModificationType() ==  ModificationType.REPLACE) {
				Property newProperty = modification.getProperty();
				items.remove(property);
				items.add(newProperty);
			} else {
				throw new NotImplementedException("Modification type "+modification.getModificationType()+" is not supported yet");
			}
			
		} else {
			throw new NotImplementedException("Modification in subcontainers is not supported yet");
		}
	}
}

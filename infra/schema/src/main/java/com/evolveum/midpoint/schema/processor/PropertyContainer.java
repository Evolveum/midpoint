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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
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
 * Property Container is mutable.
 * 
 * @author Radovan Semancik
 * 
 */
public class PropertyContainer {

	private QName name;
	private Set<Property> properties = new HashSet<Property>();
	private PropertyContainerDefinition definition;

	public PropertyContainer() {
	}

	public PropertyContainer(QName name, PropertyContainerDefinition definition) {
		this.name = name;
		this.definition = definition;
	}

	public PropertyContainer(QName name) {
		this.name = name;
	}

	/**
	 * Returns the name of the property container.
	 * 
	 * The name is a QName. It uniquely defines a property container.
	 * 
	 * The name may be null, but such a property container will not work.
	 * 
	 * The name is the QName of XML element in the XML representation.
	 * 
	 * @return property container name
	 */
	public QName getName() {
		return name;
	}

	/**
	 * Sets the name of the property container.
	 * 
	 * The name is a QName. It uniquely defines a property container.
	 * 
	 * The name may be null, but such a property container will not work.
	 * 
	 * The name is the QName of XML element in the XML representation.
	 * 
	 * @param name
	 *            the name to set
	 */
	public void setName(QName name) {
		this.name = name;
	}

	/**
	 * Returns a set of properties that the property container contains.
	 * 
	 * The set must not be null. In case there are no properties an empty set is
	 * returned.
	 * 
	 * Returned set is mutable. Life instance of the set is returned, therefore
	 * changing the set means changing the contents of property container.
	 * 
	 * @return set of properties that the property container contains.
	 */
	public Set<Property> getProperties() {
		return properties;
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
		return definition;
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
	 * Returns a display name for the property container type.
	 * 
	 * Returns null if the display name cannot be determined.
	 * 
	 * The display name is fetched from the definition. If no definition
	 * (schema) is available, the display name will not be returned.
	 * 
	 * @return display name for the property container type
	 */
	public String getDisplayName() {
		return getDefinition() == null ? null : getDefinition()
				.getDisplayName();
	}

	/**
	 * Returns help message defined for the property container type.
	 * 
	 * Returns null if the help message cannot be determined.
	 * 
	 * The help message is fetched from the definition. If no definition
	 * (schema) is available, the help message will not be returned.
	 * 
	 * @return help message for the property container type
	 */
	public String getHelp() {
		return getDefinition() == null ? null : getDefinition().getHelp();
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
		for (Property property : properties) {
			if (propertyQName.equals(property.getName())) {
				return property;
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
	public Property findProperty(PropertyDefinition propertyDefinition) {
		throw new IllegalStateException("not implemented yet.");
	}

	/**
	 * Serialize properties to DOM.
	 * 
	 * The properties are serialized to DOM and returned as a list.
	 * The property container element is not serialized. 
	 * 
	 * @param doc DOM Document
	 * @return list of serialized properties
	 * @throws SchemaProcessorException the schema definition is missing or is inconsistent
	 */
	public List<Element> serializePropertiesToDom(Document doc) throws SchemaProcessorException {
		if (getDefinition() == null) {
			throw new IllegalStateException(
					"Object definition missing, unable to serialize");
		}
		return getDefinition().serializePropertiesToDom(getProperties(), doc);
	}

	/**
	 * Serialize entire property container to DOM.
	 * 
	 * Entire property container is returned wrapped in a
	 * single DOM element. The properties are serialized
	 * as sub-elements.
	 * 
	 * List of DOM elements is returned. The property container
	 * must have a schema definition (see getDefinition())
	 * 
	 * @param doc DOM Document
	 * @return serialized property container as DOM element
	 * @throws SchemaProcessorException the schema definition is missing or is inconsistent
	 */
	public Element serializeToDom(Document doc) throws SchemaProcessorException {
		List<Element> elements = serializePropertiesToDom(doc);
		Element container = doc.createElementNS(getName().getNamespaceURI(),
				getName().getLocalPart());
		for (Element el : elements) {
			container.appendChild(el);
		}
		return container;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getName() + "):"
				+ getProperties();
	}
}

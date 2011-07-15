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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.XsdTypeConverter;

/**
 * Property is a specific characteristic of an object. It may be considered
 * object "attribute" or "field". For example User has fullName property that
 * contains string value of user's full name.
 * 
 * Properties may be single-valued or multi-valued
 * 
 * Properties may contain primitive types or complex types (defined by XSD
 * schema)
 * 
 * Property values are unordered, implementation may change the order of values
 * 
 * Duplicate values of properties should be silently removed by implementations,
 * but clients must be able tolerate presence of duplicate values.
 * 
 * Operations that modify the objects work with the granularity of properties.
 * They add/remove/replace the values of properties, but do not "see" inside the
 * property.
 * 
 * Property is mutable.
 * 
 * @author Radovan Semancik
 * 
 */
public class Property {

	private QName name;
	private PropertyDefinition definition;
	private Set<Object> values;

	public Property() {
		super();
	}

	public Property(QName name) {
		super();
		this.name = name;
		this.definition = null;
		this.values = new HashSet<Object>();
	}

	public Property(QName name, PropertyDefinition definition) {
		super();
		this.name = name;
		this.definition = definition;
		this.values = new HashSet<Object>();
	}

	public Property(QName name, PropertyDefinition definition, Set<Object> values) {
		super();
		this.name = name;
		this.definition = definition;
		this.values = values;
	}

	/**
	 * Returns applicable property definition.
	 * 
	 * May return null if no definition is applicable or the definition is not
	 * know.
	 * 
	 * @return applicable property definition
	 */
	public PropertyDefinition getDefinition() {
		return definition;
	}

	/**
	 * Returns the name of the property.
	 * 
	 * The name is a QName. It uniquely defines a property.
	 * 
	 * The name may be null, but such a property will not work.
	 * 
	 * The name is the QName of XML element in the XML representation.
	 * 
	 * @return property name
	 */
	public QName getName() {
		return name;
	}

	/**
	 * Sets the name of the property.
	 * 
	 * The name is a QName. It uniquely defines a property.
	 * 
	 * The name may be null, but such a property will not work.
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
	 * Sets applicable property definition.
	 * 
	 * @param definition
	 *            the definition to set
	 */
	public void setDefinition(PropertyDefinition definition) {
		this.definition = definition;
	}

	/**
	 * Returns property values.
	 * 
	 * The values are returned as set. The order of values is not significant.
	 * 
	 * @return property values
	 */
	public Set<Object> getValues() {
		return values;
	}

	/**
	 * Returns property values.
	 * 
	 * The values are returned as set. The order of values is not significant.
	 * 
	 * The values are cast to the "T" java type.
	 * 
	 * @param <T>
	 *            Target class for property values
	 * @param T
	 *            Target class for property values
	 * @return property values
	 * @throws ClassCastException
	 *             if the values cannot be cast to "T"
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> Set<T> getValues(Class<T> T) {
		// TODO: Is this OK?
		return (Set) values;
	}

	/**
	 * Returns value of a single-valued property.
	 * 
	 * The value is cast to the "T" java type.
	 * 
	 * @param <T>
	 *            Target class for property values
	 * @param T
	 *            Target class for property values
	 * @return value of a single-valued property
	 * @throws ClassCastException
	 * @throws IllegalStateException
	 *             more than one value is present
	 */
	@SuppressWarnings("unchecked")
	public <T> T getValue(Class<T> T) {
		// TODO: check schema definition if avalilable
		if (values.size() > 1) {
			throw new IllegalStateException("Attempt to get single value from property " + name
					+ " with multiple values");
		}
		Object o = values.iterator().next();
		if (o == null) {
			return null;
		}
		return (T) o;
	}
	
	/**
	 * Means as a short-hand for setting just a value for single-valued
	 * attributes.
	 * Will remove all existing values.
	 * TODO
	 */
	public void setValue(Object value) {
		values.clear();
		values.add(value);
	}

	/**
	 * Returns a display name for the property type.
	 * 
	 * Returns null if the display name cannot be determined.
	 * 
	 * The display name is fetched from the definition. If no definition
	 * (schema) is available, the display name will not be returned.
	 * 
	 * @return display name for the property type
	 */
	public String getDisplayName() {
		return getDefinition() == null ? null : getDefinition().getDisplayName();
	}

	/**
	 * Returns help message defined for the property type.
	 * 
	 * Returns null if the help message cannot be determined.
	 * 
	 * The help message is fetched from the definition. If no definition
	 * (schema) is available, the help message will not be returned.
	 * 
	 * @return help message for the property type
	 */
	public String getHelp() {
		return getDefinition() == null ? null : getDefinition().getHelp();
	}
	
	/**
	 * Serializes property to DOM element(s).
	 * 
	 * The property name will be used as an element QName.
	 * The values will be in the element content. Single-value
	 * properties will produce one element (on none), multi-valued
	 * properies may produce several elements. All of the elements will
	 * have the same QName.
	 * 
	 * The property must have a definition (getDefinition() must not
	 * return null).
	 * 
	 * @param doc DOM Document
	 * @return property serialized to DOM
	 * @throws SchemaProcessorException No definition or inconsistent definition 
	 */
	public List<Element> serializeToDom(Document doc) throws SchemaProcessorException {
		return serializeToDom(doc,null);
	}
	
	/**
	 * Same as serializeToDom(Document doc) but allows external definition.
	 * 
	 * Package-private. Useful for some internal calls inside schema processor.
	 */
	List<Element> serializeToDom(Document doc,PropertyDefinition propDef) throws SchemaProcessorException {
		List<Element> elements = new ArrayList<Element>();
		if (propDef==null) {
			propDef = getDefinition();
		}
		if (propDef==null) {
			throw new SchemaProcessorException("Definition of property "+this+" not found");
		}
		Set<Object> values = getValues();
		for (Object val : values) {
			Element element = doc.createElementNS(getName().getNamespaceURI(), getName().getLocalPart());
			XsdTypeConverter.toXsdElement(val,propDef.getTypeName(),element);
			elements.add(element);
		}			
		return elements;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+getName()+"):"+getValues();
	}

	public String dump() {
		return toString();
	}

}

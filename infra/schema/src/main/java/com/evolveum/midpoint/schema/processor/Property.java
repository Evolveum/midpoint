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
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.util.JAXBUtil;


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
public class Property extends Item {

	private Set<Object> values;

	public Property() {
		super();
	}

	public Property(QName name) {
		super(name);
		this.values = new HashSet<Object>();
	}

	public Property(QName name, PropertyDefinition definition) {
		super(name,definition);
		this.values = new HashSet<Object>();
	}

	public Property(QName name, PropertyDefinition definition, Set<Object> values) {
		super(name,definition);
		if (values == null) {
			this.values = new HashSet<Object>();
		} else {
			this.values = values;
		}
	}

	public Property(QName name, PropertyDefinition definition, Set<Object> values, Object element) {
		super(name,definition,element);
		if (values == null) {
			this.values = new HashSet<Object>();
		} else {
			this.values = values;
		}
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
		return (PropertyDefinition) definition;
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
		if (values.isEmpty()) {
			return null;
		}
		Object o = values.iterator().next();
		if (o == null) {
			return null;
		}
		return (T) o;
	}
	
	public Object getValue() {
		if (values.size() > 1) {
			throw new IllegalStateException("Attempt to get single value from property " + name
					+ " with multiple values");
		}
		if (values.isEmpty()) {
			return null;
		}
		Object o = values.iterator().next();
		return o;
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
	
	public PropertyModification createModification(PropertyModification.ModificationType modificationType, Set<Object> modifyValues) {

		return new PropertyModification(this,modificationType, modifyValues);
	}

	public PropertyModification createModification(PropertyModification.ModificationType modificationType, Object modifyValue) {
		Set<Object> modifyValues = new HashSet<Object>();
		modifyValues.add(modifyValue);
		return new PropertyModification(this,modificationType,modifyValues);
	}

	@Override
	public void serializeToDom(Node parentNode) throws SchemaException {
		serializeToDom(parentNode,null,null,false);
	}
				
	public void serializeToDom(Node parentNode, PropertyDefinition propDef, Set<Object> alternateValues, boolean recordType) throws SchemaException {
	
		if (propDef==null) {
			propDef = getDefinition();
		}
		
		Set<Object> serializeValues = getValues();
		if (alternateValues!=null) {
			serializeValues = alternateValues;
		}
		
		for (Object val : serializeValues) {
			// If we have a definition then try to use it. The conversion may be more realiable
			// Otherwise the conversion will be governed by Java type
			QName xsdType = null;
			if (propDef!=null) {
				xsdType = propDef.getTypeName();
			}
				try {
					XsdTypeConverter.appendBelowNode(val,xsdType,getName(),parentNode,recordType);
				} catch (JAXBException e) {
					throw new SystemException("Unexpected JAXB problem while converting "+propDef.getTypeName()+" : "+e.getMessage(),e);
				}
		}			
	}
	
	/**
	 * Serializes property to DOM or JAXB element(s).
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
	 * @return property serialized to DOM Element or JAXBElement
	 * @throws SchemaProcessorException No definition or inconsistent definition 
	 */
	public List<Object> serializeToJaxb(Document doc) throws SchemaException {
		return serializeToJaxb(doc,null,null,false);
	}
	
	/**
	 * Same as serializeToDom(Document doc) but allows external definition.
	 * 
	 * Package-private. Useful for some internal calls inside schema processor.
	 */
	List<Object> serializeToJaxb(Document doc,PropertyDefinition propDef) throws SchemaException {
		// No need to record types, we have schema definition here
		return serializeToJaxb(doc,propDef,null,false);
	}
	
	/**
	 * Same as serializeToDom(Document doc) but allows external definition.
	 * 
	 * Allows alternate values.
	 * Allows option to record type in the serialized output (using xsi:type)
	 * 
	 * Package-private. Useful for some internal calls inside schema processor.
	 */
	List<Object> serializeToJaxb(Document doc,PropertyDefinition propDef, Set<Object> alternateValues, boolean recordType) throws SchemaException {
		
		
		// Try to locate definition
		List<Object> elements = new ArrayList<Object>();
		
		//check if the property has value..if not, return empty elemnts list..
		
		if (propDef==null) {
			propDef = getDefinition();
		}
		
		Set<Object> serializeValues = getValues();
		if (alternateValues!=null) {
			serializeValues = alternateValues;
		}
		
		
		
		for (Object val : serializeValues) {
			// If we have a definition then try to use it. The conversion may be more realiable
			// Otherwise the conversion will be governed by Java type
			QName xsdType = null;
			if (propDef!=null) {
				xsdType = propDef.getTypeName();
			}
			
				try {
					elements.add(XsdTypeConverter.toXsdElement(val,xsdType,getName(),doc,recordType));
				} catch (JAXBException e) {
					throw new SystemException("Unexpected JAXB problem while converting "+propDef.getTypeName()+" : "+e.getMessage(),e);
				}
			
		}			
		return elements;
	}
	
	public void applyValueToElement() throws SchemaException {
		if (element == null) {
			throw new IllegalStateException("Cannot apply value to element as the element is null (property "+getName()+")");
		}
		if (element instanceof Element) {
			// TODO
			Element domElement = (Element) element;
			Node parentNode = domElement.getParentNode();
			if (!(parentNode instanceof Element)) {
				// This is unlikely for JAXB elements, the will be JAXBElement instead. But this may happen for
				// "primitive" types. It may need some solution later.
				throw new IllegalStateException("Cannot apply value changes to top-level DOM elements (property "+getName()+")");
			}
			Element parentElement = (Element) parentNode;
			Object newElement = null;
			try {
				newElement = XsdTypeConverter.toXsdElement(getValue(),getDefinition().getTypeName(),getName(),domElement.getOwnerDocument(),false);
			} catch (JAXBException e) {
				throw new SchemaException("Cannot convert value of property "+getName()+": "+e.getMessage(),e);
			}
			Element newDomElement = null;
			try {
				newDomElement = JAXBUtil.toDomElement(newElement,parentElement.getOwnerDocument());
			} catch (JAXBException e) {
				throw new SchemaException("Cannot convert value of property "+getName()+": "+e.getMessage(),e);
			}
			parentElement.replaceChild(newDomElement, domElement);
			element = newDomElement;
		} else if (element instanceof JAXBElement) {
			((JAXBElement)element).setValue(getValue());
		} else {
			throw new IllegalStateException("Unknown element type "+element+" ("+element.getClass().getName()+"), property "+getName());
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+getName()+"):"+getValues();
	}
	
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indent; i++) {
			sb.append(INDENT_STRING);
		}
		sb.append(getDebugDumpClassName()).append(": ").append(getName()).append(" = ");
		if (getValues() == null) {
			sb.append("null");
		} else {
			sb.append("[ ");
			for (Object value : getValues()) {
				sb.append(value);
				sb.append(", ");
			}
			sb.append(" ]");
		}
		return sb.toString();
	}

	/**
	 * Return a human readable name of this class suitable for logs.
	 */
	protected String getDebugDumpClassName() {
		return "Property";
	}


}

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

import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.util.JAXBUtil;

/**
 * Property Definition.
 * 
 * Property is a basic unit of information in midPoint. This class provides
 * definition of property type, multiplicity and so on.
 * 
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
 * This class represents schema definition for property. See {@link Definition}
 * for more details.
 * 
 * @author Radovan Semancik
 * 
 */
public class PropertyDefinition extends ItemDefinition {

	private static final long serialVersionUID = 7259761997904371009L;
	private QName valueType;
	private int minOccurs = 1;
	private int maxOccurs = 1;
	private Object[] allowedValues;
	private boolean create = true;
	private boolean read = true;
	private boolean update = true;

	PropertyDefinition(QName name, QName defaultName, QName typeName) {
		super(name, defaultName, typeName);
	}

	public PropertyDefinition(QName name, QName typeName) {
		super(name, null, typeName);
	}

	// This creates reference to other schema
	public PropertyDefinition(QName name) {
		super(name, null, null);
	}
	
	/**
	 * Returns allowed values for this property.
	 * 
	 * @return Object array. May be null.
	 */
	public Object[] getAllowedValues() {
		return allowedValues;
	}

	/**
	 * TODO:
	 * 
	 * @return
	 */
	public boolean canRead() {
		return read;
	}

	/**
	 * TODO:
	 * 
	 * @return
	 */
	public boolean canUpdate() {
		return update;
	}
	
	/**
	 * 
	 */
	public void setReadOnly() {
		create = false;
		read = true;
		update = false;
	}

	/**
	 * Returns QName of the property value type.
	 * 
	 * The returned type is either XSD simple type or complex type. It may not
	 * be defined in the same schema (especially if it is standard XSD simple
	 * type).
	 * 
	 * @return QName of the property value type
	 */
	public QName getValueType() {
		return valueType;
	}

	void setValueType(QName valueType) {
		this.valueType = valueType;
	}

	/**
	 * Return the number of minimal value occurrences.
	 * 
	 * @return the minOccurs
	 */
	public int getMinOccurs() {
		return minOccurs;
	}

	public void setMinOccurs(int minOccurs) {
		this.minOccurs = minOccurs;
	}

	/**
	 * Return the number of maximal value occurrences.
	 * 
	 * Any negative number means "unbounded".
	 * 
	 * @return the maxOccurs
	 */
	public int getMaxOccurs() {
		return maxOccurs;
	}

	public void setMaxOccurs(int maxOccurs) {
		this.maxOccurs = maxOccurs;
	}

	/**
	 * Returns true if property is single-valued.
	 * 
	 * @return true if property is single-valued.
	 */
	public boolean isSingleValue() {
		return getMaxOccurs() >= 0 && getMaxOccurs() <= 1;
	}

	/**
	 * Returns true if property is multi-valued.
	 * 
	 * @return true if property is multi-valued.
	 */
	public boolean isMultiValue() {
		return getMaxOccurs() < 0 || getMaxOccurs() > 1;
	}

	/**
	 * Returns true if property is mandatory.
	 * 
	 * @return true if property is mandatory.
	 */
	public boolean isMandatory() {
		return getMinOccurs() > 0;
	}

	/**
	 * Returns true if property is optional.
	 * 
	 * @return true if property is optional.
	 */
	public boolean isOptional() {
		return getMinOccurs() == 0;
	}

	@Override
	public Property instantiate() {
		return instantiate(getNameOrDefaultName());
	}
	
	@Override
	public Property instantiate(QName name) {
		return new Property(name, this);
	}

	@Override
	public Property instantiate(QName name, Object element) {
		return new Property(name, this, null, element);
	}

	// TODO: factory methods for DOM and JAXB elements

	public void setRead(boolean read) {
		this.read = read;
	}

	public void setUpdate(boolean update) {
		this.update = update;
	}

	public void setCreate(boolean create) {
		this.create = create;
	}

	public boolean canCreate() {
		return create;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.schema.processor.Definition#parseItem(java.util.List)
	 */
	@Override
	public Property parseItem(List<Object> elements) throws SchemaException {
		if (elements == null || elements.isEmpty()) {
			return null;
		}
		QName propName = JAXBUtil.getElementQName(elements.get(0));
		Property prop = null;
		if (elements.size()==1) {
			prop = this.instantiate(propName, elements.get(0));
		} else {
			// In-place modification not supported for multi-valued properties
			prop = this.instantiate(propName, null);
		}

		if (!isMultiValue() && elements.size()>1) {
			throw new SchemaException("Attempt to store multiple values in single-valued property "+propName);
		}

		for (Object element : elements) {		
			Object value;
			try {
				value = XsdTypeConverter.toJavaValue(element, getTypeName());
			} catch (JAXBException e) {
				throw new SchemaException("Schema error in property "+propName+" : "+e.getMessage(),e);
			}
			prop.getValues().add(value);
		}
		return prop;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName()).append(":").append(getName()).append(" (").append(getTypeName()).append(")");
		if (isMultiValue()) {
			sb.append(" multi");
		}
		if (isOptional()) {
			sb.append(" opt");
		}
		return sb.toString();
	}
	
	
}

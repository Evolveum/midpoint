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

import javax.xml.namespace.QName;

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
public class PropertyDefinition extends Definition {

	private QName valueType;
	private int minOccurs = 1;
	private int maxOccurs = 1;
	private Object[] allowedValues;
	private boolean readable;
	private boolean updateable;

	PropertyDefinition(QName name, QName defaultName, QName typeName) {
		super(name, defaultName, typeName);
	}
	

	public PropertyDefinition(QName name, QName typeName) {
		super(name, null, typeName);
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
	 * @return
	 */
	public boolean isReadable() {
		return readable;
	}
	
	/**
	 * TODO:
	 * @return
	 */
	public boolean isUpdateable() {
		return updateable;
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
		return getMaxOccurs() <= 1;
	}

	/**
	 * Returns true if property is multi-valued.
	 * 
	 * @return true if property is multi-valued.
	 */
	public boolean isMultiValue() {
		return getMaxOccurs() > 1;
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

	public Property instantiate() {
		return new Property(getNameOrDefaultName(), this);
	}

	// TODO: factory methods for DOM and JAXB elements

}

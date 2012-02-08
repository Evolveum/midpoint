/**
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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.prism;

import javax.xml.namespace.QName;

/**
 * @author Radovan Semancik
 *
 */
public class TypedValue {
	private Object value;
	private QName xsdType;
	private QName elementName;
	
	/**
	 * 
	 */
	public TypedValue() {
		super();
	}
	/**
	 * @param value
	 * @param xsdType
	 */
	public TypedValue(Object value, QName xsdType) {
		super();
		this.value = value;
		this.xsdType = xsdType;
	}
	/**
	 * @param javaValue
	 * @param xsiType
	 * @param qName
	 */
	public TypedValue(Object value, QName xsdType, QName elementName) {
		super();
		this.value = value;
		this.xsdType = xsdType;
		this.elementName = elementName;
	}
	/**
	 * @return the value
	 */
	public Object getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(Object value) {
		this.value = value;
	}
	/**
	 * @return the xsdType
	 */
	public QName getXsdType() {
		return xsdType;
	}
	/**
	 * @param xsdType the xsdType to set
	 */
	public void setXsdType(QName xsdType) {
		this.xsdType = xsdType;
	}
	/**
	 * @return the elementName
	 */
	public QName getElementName() {
		return elementName;
	}
	/**
	 * @param elementName the elementName to set
	 */
	public void setElementName(QName elementName) {
		this.elementName = elementName;
	}
	
}

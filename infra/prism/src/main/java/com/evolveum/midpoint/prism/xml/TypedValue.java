/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.prism.xml;

import javax.xml.namespace.QName;

/**
 * @author Radovan Semancik
 */
public class TypedValue {
	private Object value;
	private QName xsdType;
	private QName elementName;

	public TypedValue() {
		super();
	}

	/**
	 * @param value the value
	 * @param xsdType the xsdType
	 */
	public TypedValue(Object value, QName xsdType) {
		super();
		this.value = value;
		this.xsdType = xsdType;
	}

	/**
	 * @param value the value
	 * @param xsdType the xsdType
	 * @param elementName the elementName
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

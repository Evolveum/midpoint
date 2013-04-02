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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.icf.dummy.resource;

/**
 * @author Radovan Semancik
 *
 */
public class DummyAttributeDefinition {
	
	private String attributeName;
	private Class<?> attributeType;
	private boolean isRequired;
	private boolean isMulti;
	private boolean isReturnedByDefault = true;
	
	public DummyAttributeDefinition(String attributeName, Class<?> attributeType) {
		super();
		this.attributeName = attributeName;
		this.attributeType = attributeType;
		isRequired = false;
		isMulti = false;
	}

	public DummyAttributeDefinition(String attributeName, Class<?> attributeType, boolean isRequired,
			boolean isMulti) {
		super();
		this.attributeName = attributeName;
		this.attributeType = attributeType;
		this.isRequired = isRequired;
		this.isMulti = isMulti;
	}
	
	public String getAttributeName() {
		return attributeName;
	}
	
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
	
	public Class<?> getAttributeType() {
		return attributeType;
	}
	
	public void setAttributeType(Class<?> attributeType) {
		this.attributeType = attributeType;
	}
	
	public boolean isRequired() {
		return isRequired;
	}
	
	public void setRequired(boolean isRequired) {
		this.isRequired = isRequired;
	}
	
	public boolean isMulti() {
		return isMulti;
	}
	
	public void setMulti(boolean isMulti) {
		this.isMulti = isMulti;
	}

	public boolean isReturnedByDefault() {
		return isReturnedByDefault;
	}

	public void setReturnedByDefault(boolean isReturnedByDefault) {
		this.isReturnedByDefault = isReturnedByDefault;
	}

}

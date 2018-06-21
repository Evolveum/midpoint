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

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

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Radovan Semancik
 *
 */
public class DummyObjectClass {

	private Collection<DummyAttributeDefinition> attributeDefinitions;

	public DummyObjectClass() {
		attributeDefinitions = new ArrayList<>();
	}

	public Collection<DummyAttributeDefinition> getAttributeDefinitions() {
		return attributeDefinitions;
	}

	public DummyAttributeDefinition getAttributeDefinition(String attrName) {
		for (DummyAttributeDefinition attrDef: attributeDefinitions) {
			if (attrName.equals(attrDef.getAttributeName())) {
				return attrDef;
			}
		}
		return null;
	}

	public void add(DummyAttributeDefinition attrDef) {
		attributeDefinitions.add(attrDef);
	}

	public void clear() {
		attributeDefinitions.clear();
	}

	public void addAttributeDefinition(String attributeName) {
		addAttributeDefinition(attributeName,String.class,false,false);
	}

	public void addAttributeDefinition(String attributeName, Class<?> attributeType) {
		addAttributeDefinition(attributeName,attributeType,false,false);
	}

	public void addAttributeDefinition(String attributeName, Class<?> attributeType, boolean isOptional) {
		addAttributeDefinition(attributeName,attributeType,isOptional,false);
	}

	public void addAttributeDefinition(String attributeName, Class<?> attributeType, boolean isRequired,
			boolean isMulti) {
		DummyAttributeDefinition attrDef = new DummyAttributeDefinition(attributeName,attributeType,isRequired,isMulti);
		add(attrDef);
	}

}

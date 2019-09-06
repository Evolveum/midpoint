/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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

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

import java.util.HashSet;
import java.util.Set;

/**
 * @author Radovan Semancik
 *
 */
public class DummyObjectClass {
	
	private Set<DummyAttributeDefinition> attributeDefinitions;

	public DummyObjectClass() {
		attributeDefinitions = new HashSet<DummyAttributeDefinition>();
	}

	public Set<DummyAttributeDefinition> getAttributeDefinitions() {
		return attributeDefinitions;
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

	public void addAttributeDefinition(String attributeName, Class<?> attributeType, boolean isOptional,
			boolean isMulti) {
		DummyAttributeDefinition attrDef = new DummyAttributeDefinition(attributeName,attributeType,isOptional,isMulti);
		add(attrDef);
	}
	
}

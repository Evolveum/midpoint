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

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

/**
 * Definition of a property container.
 * 
 * Property container groups properties into logical blocks. The reason for
 * grouping may be as simple as better understandability of data structure. But
 * the group usually means different meaning, source or structure of the data.
 * For example, the property container is frequently used to hold properties
 * that are dynamic, not fixed by a static schema. Such grouping also naturally
 * translates to XML and helps to "quarantine" such properties to avoid Unique
 * Particle Attribute problems.
 * 
 * Property Container contains a set of (potentially multi-valued) properties.
 * The order of properties is not significant, regardless of the fact that it
 * may be fixed in the XML representation. In the XML representation, each
 * element inside Property Container must be either Property or a Property
 * Container.
 * 
 * This class represents schema definition for property container. See
 * {@link Definition} for more details.
 * 
 * @author Radovan Semancik
 * 
 */
public class PropertyContainerDefinition extends Definition {

	private Set<PropertyDefinition> propertyDefinitions;

	PropertyContainerDefinition(QName name, QName defaultName, QName typeName) {
		super(name, defaultName, typeName);
	}

	/**
	 * Finds a PropertyDefinition by looking at the property name.
	 * 
	 * Returns null if nothing is found.
	 * 
	 * @param name
	 *            property definition name
	 * @return found property definition of null
	 */
	public PropertyDefinition findPropertyDefinition(QName name) {
		for (PropertyDefinition def : propertyDefinitions) {
			if (name.equals(def.getName())) {
				return def;
			}
		}
		return null;
	}

	/**
	 * Returns set of property definitions.
	 * 
	 * The set contains all property definitions of all types that were parsed.
	 * Order of definitions is insignificant.
	 * 
	 * @return set of definitions
	 */
	public Set<PropertyDefinition> getDefinitions() {
		if (propertyDefinitions == null) {
			propertyDefinitions = new HashSet<PropertyDefinition>();
		}
		return propertyDefinitions;
	}
	
	void setPropertyDefinitions(Set<PropertyDefinition> propertyDefinitions) {
		this.propertyDefinitions = propertyDefinitions;
	}
	
	public PropertyContainer instantiate() {
		return new PropertyContainer(getNameOrDefaultName(), this);
	}
	
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<indent; i++) {
			sb.append(Schema.INDENT);
		}
		sb.append(toString());
		sb.append("\n");
		for (PropertyDefinition def : getDefinitions()) {
			sb.append(def.debugDump(indent+1));
		}
		return sb.toString();
	}

}

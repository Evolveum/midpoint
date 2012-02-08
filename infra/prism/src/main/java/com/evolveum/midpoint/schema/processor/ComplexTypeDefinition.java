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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

/**
 * TODO
 * 
 * @author Radovan Semancik
 * 
 */
public class ComplexTypeDefinition extends Definition {
	private static final long serialVersionUID = 2655797837209175037L;
	private Set<ItemDefinition> itemDefinitions;
	private QName extensionForType;

	ComplexTypeDefinition(QName defaultName, QName typeName, PrismContext prismContext) {
		super(defaultName, typeName, prismContext);
		itemDefinitions = new HashSet<ItemDefinition>();
	}

	protected String getSchemaNamespace() {
		return getTypeName().getNamespaceURI();
	}
		
	/**
	 * Returns set of property definitions.
	 * 
	 * The set contains all property definitions of all types that were parsed.
	 * Order of definitions is insignificant.
	 * 
	 * @return set of definitions
	 */
	public Set<ItemDefinition> getDefinitions() {
		if (itemDefinitions == null) {
			itemDefinitions = new HashSet<ItemDefinition>();
		}
		return itemDefinitions;
	}
	
	public QName getExtensionForType() {
		return extensionForType;
	}

	public void setExtensionForType(QName extensionForType) {
		this.extensionForType = extensionForType;
	}
		
	public PropertyDefinition createPropertyDefinifion(QName name, QName typeName) {
		PropertyDefinition propDef = new PropertyDefinition(name, name, typeName, prismContext);
		itemDefinitions.add(propDef);
		return propDef;
	}
	
	// Creates reference to other schema
	// TODO: maybe check if the name is in different namespace
	// TODO: maybe create entirely new concept of property reference?
	public PropertyDefinition createPropertyDefinifion(QName name) {
		PropertyDefinition propDef = new PropertyDefinition(name, name, null, prismContext);
		itemDefinitions.add(propDef);
		return propDef;
	}

	public PropertyDefinition createPropertyDefinition(String localName, QName typeName) {
		QName name = new QName(getSchemaNamespace(),localName);
		return createPropertyDefinifion(name,typeName);
	}

	
	public PropertyDefinition createPropertyDefinifion(String localName, String localTypeName) {
		QName name = new QName(getSchemaNamespace(),localName);
		QName typeName = new QName(getSchemaNamespace(),localTypeName);
		return createPropertyDefinifion(name,typeName);
	}

	@Override
	void revive(PrismContext prismContext) {
		if (this.prismContext != null) {
			return;
		}
		this.prismContext = prismContext;
		for (ItemDefinition def: itemDefinitions) {
			def.revive(prismContext);
		}
	}

	public boolean isEmpty() {
		return itemDefinitions.isEmpty();
	}
	
	/**
	 * Shallow clone.
	 */
	public ComplexTypeDefinition clone() {
		ComplexTypeDefinition clone = new ComplexTypeDefinition(this.defaultName, this.typeName, prismContext);
		copyDefinitionData(clone);
		return clone;
	}
	
	protected void copyDefinitionData(ComplexTypeDefinition clone) {
		super.copyDefinitionData(clone);
		clone.itemDefinitions.addAll(this.itemDefinitions);
	}

	public void replaceDefinition(QName propertyName, ItemDefinition newDefinition) {
		for (ItemDefinition itemDef: itemDefinitions) {
			if (itemDef.getName().equals(propertyName)) {
				if (!itemDef.getClass().isAssignableFrom(newDefinition.getClass())) {
					throw new IllegalArgumentException("The provided definition of class "+newDefinition.getClass().getName()+" does not match existing definition of class "+itemDef.getClass().getName());
				}
				itemDefinitions.remove(itemDef);
				itemDefinitions.add(newDefinition);
				return;
			}
		}
		throw new IllegalArgumentException("The definition with name "+propertyName+" was not found in complex type "+getTypeName());
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<indent; i++) {
			sb.append(DebugDumpable.INDENT_STRING);
		}
		sb.append(toString());
		if (extensionForType != null) {
			sb.append(" ext:");
			sb.append(DebugUtil.prettyPrint(extensionForType));
		}
		sb.append("\n");
		for (ItemDefinition def : getDefinitions()) {
			sb.append(def.debugDump(indent+1));
		}
		return sb.toString();
	}


}

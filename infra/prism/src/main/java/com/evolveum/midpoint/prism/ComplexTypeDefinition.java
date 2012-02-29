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

package com.evolveum.midpoint.prism;

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
	private QName superType;
	private boolean containerMarker;
	private boolean objectMarker;
	private boolean xsdAnyMarker;
	private QName extensionForType;

	public ComplexTypeDefinition(QName defaultName, QName typeName, PrismContext prismContext) {
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
	
	/**
	 * Flag indicating whether this type was marked as "container"
	 * in the original schema. Does not provide any information to
	 * schema processing logic, just conveys the marker from oginal
	 * schema so we can serialized and deserialize the schema without
	 * loss of information.
	 */
	public boolean isContainerMarker() {
		return containerMarker;
	}

	public void setContainerMarker(boolean containerMarker) {
		this.containerMarker = containerMarker;
	}

	/**
	 * Flag indicating whether this type was marked as "object"
	 * in the original schema. Does not provide any information to
	 * schema processing logic, just conveys the marker from original
	 * schema so we can serialized and deserialize the schema without
	 * loss of information.
	 */
	public boolean isObjectMarker() {
		return objectMarker;
	}
	
	public boolean isXsdAnyMarker() {
		return xsdAnyMarker;
	}

	public void setXsdAnyMarker(boolean xsdAnyMarker) {
		this.xsdAnyMarker = xsdAnyMarker;
	}

	public QName getSuperType() {
		return superType;
	}

	public void setSuperType(QName superType) {
		this.superType = superType;
	}

	public void setObjectMarker(boolean objectMarker) {
		this.objectMarker = objectMarker;
	}

	public void add(ItemDefinition definition) {
		itemDefinitions.add(definition);
	}
		
	public PrismPropertyDefinition createPropertyDefinifion(QName name, QName typeName) {
		PrismPropertyDefinition propDef = new PrismPropertyDefinition(name, name, typeName, prismContext);
		itemDefinitions.add(propDef);
		return propDef;
	}
	
	// Creates reference to other schema
	// TODO: maybe check if the name is in different namespace
	// TODO: maybe create entirely new concept of property reference?
	public PrismPropertyDefinition createPropertyDefinifion(QName name) {
		PrismPropertyDefinition propDef = new PrismPropertyDefinition(name, name, null, prismContext);
		itemDefinitions.add(propDef);
		return propDef;
	}

	public PrismPropertyDefinition createPropertyDefinition(String localName, QName typeName) {
		QName name = new QName(getSchemaNamespace(),localName);
		return createPropertyDefinifion(name,typeName);
	}

	
	public PrismPropertyDefinition createPropertyDefinifion(String localName, String localTypeName) {
		QName name = new QName(getSchemaNamespace(),localName);
		QName typeName = new QName(getSchemaNamespace(),localTypeName);
		return createPropertyDefinifion(name,typeName);
	}
	
	/**
     * Finds a PropertyDefinition by looking at the property name.
     * <p/>
     * Returns null if nothing is found.
     *
     * @param name property definition name
     * @return found property definition or null
     */
    public PrismPropertyDefinition findPropertyDefinition(QName name) {
        return findItemDefinition(name, PrismPropertyDefinition.class);
    }
	
	public <T extends ItemDefinition> T findItemDefinition(QName name, Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("type not specified while searching for " + name + " in " + this);
        }
        if (name == null) {
            throw new IllegalArgumentException("name not specified while searching in " + this);
        }

        for (ItemDefinition def : getDefinitions()) {
            if (isItemValid(def, name, clazz)) {
                return (T) def;
            }
        }
        return null;
    }
	
	private <T extends ItemDefinition> boolean isItemValid(ItemDefinition def, QName name, Class<T> clazz) {
        return clazz.isAssignableFrom(def.getClass()) && name.equals(def.getName());
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
			sb.append(",ext:");
			sb.append(DebugUtil.prettyPrint(extensionForType));
		}
		if (ignored) {
			sb.append(",ignored");
		}
		if (containerMarker) {
			sb.append(",Mc");
		}
		if (objectMarker) {
			sb.append(",Mo");
		}
		if (xsdAnyMarker) {
			sb.append(",Ma");
		}
		sb.append("\n");
		for (ItemDefinition def : getDefinitions()) {
			sb.append(def.debugDump(indent+1));
		}
		return sb.toString();
	}

	/**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "CTD";
    }
}

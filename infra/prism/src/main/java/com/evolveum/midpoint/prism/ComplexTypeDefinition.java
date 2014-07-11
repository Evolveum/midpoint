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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.PrettyPrinter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
	private List<ItemDefinition> itemDefinitions;
	private QName superType;
	private boolean containerMarker;
	private boolean objectMarker;
	private boolean xsdAnyMarker;
	private QName extensionForType;
	private Class<?> compileTimeClass;

	public ComplexTypeDefinition(QName typeName, PrismContext prismContext) {
		super(typeName, prismContext);
		itemDefinitions = new ArrayList<ItemDefinition>();
	}
	
	public ComplexTypeDefinition(QName typeName, PrismContext prismContext, Class<?> compileTimeClass) {
		super(typeName, prismContext);
		itemDefinitions = new ArrayList<ItemDefinition>();
		this.compileTimeClass = compileTimeClass;
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
	public List<? extends ItemDefinition> getDefinitions() {
		if (itemDefinitions == null) {
			itemDefinitions = new ArrayList<ItemDefinition>();
		}
		return itemDefinitions;
	}
	
	public void addDefinition(ItemDefinition itemDef) {
		itemDefinitions.add(itemDef);
	}
	
	public Class<?> getCompileTimeClass() {
		return compileTimeClass;
	}

	public void setCompileTimeClass(Class<?> compileTimeClass) {
		this.compileTimeClass = compileTimeClass;
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
		PrismPropertyDefinition propDef = new PrismPropertyDefinition(name, typeName, prismContext);
		itemDefinitions.add(propDef);
		return propDef;
	}
	
	// Creates reference to other schema
	// TODO: maybe check if the name is in different namespace
	// TODO: maybe create entirely new concept of property reference?
	public PrismPropertyDefinition createPropertyDefinifion(QName name) {
		PrismPropertyDefinition propDef = new PrismPropertyDefinition(name, null, prismContext);
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
    public <C extends Containerable> PrismPropertyDefinition<C> findPropertyDefinition(QName name) {
        return findItemDefinition(name, PrismPropertyDefinition.class);
    }
    
    public <C extends Containerable> PrismPropertyDefinition<C> findPropertyDefinition(ItemPath path) {
        return findItemDefinition(path, PrismPropertyDefinition.class);
    }
	
    public <C extends Containerable> PrismContainerDefinition<C> findContainerDefinition(QName name) {
    	return findItemDefinition(name, PrismContainerDefinition.class);
    }
    
    public <C extends Containerable> PrismContainerDefinition<C> findContainerDefinition(ItemPath path) {
    	return findItemDefinition(path, PrismContainerDefinition.class);
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

	public <T extends ItemDefinition> T findItemDefinition(ItemPath path, Class<T> clazz) {
    	while (!path.isEmpty() && !(path.first() instanceof NameItemPathSegment)) {
    		path = path.rest();
    	}
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Cannot resolve empty path on complex type definition "+this);
        }
        QName firstName = ((NameItemPathSegment)path.first()).getName();
        for (ItemDefinition def : getDefinitions()) {
            if (firstName.equals(def.getName())) {
                return def.findItemDefinition(path.rest(), clazz);
            }
        }
        return null;
    }
	
	private <T extends ItemDefinition> boolean isItemValid(ItemDefinition def, QName name, Class<T> clazz) {
		if (def == null) {
    		return false;
    	}
    	return def.isValidFor(name, clazz);
	}
	
	/**
	 * Merge provided definition into this definition.
	 */
	public void merge(ComplexTypeDefinition otherComplexTypeDef) {
		for (ItemDefinition otherItemDef: otherComplexTypeDef.getDefinitions()) {
			add(otherItemDef.clone());
		}
	}

	@Override
	public void revive(PrismContext prismContext) {
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
		ComplexTypeDefinition clone = new ComplexTypeDefinition(this.typeName, prismContext);
		copyDefinitionData(clone);
		return clone;
	}
	
	protected void copyDefinitionData(ComplexTypeDefinition clone) {
		super.copyDefinitionData(clone);
        clone.superType = this.superType;
        clone.containerMarker = this.containerMarker;
        clone.objectMarker = this.objectMarker;
        clone.xsdAnyMarker = this.xsdAnyMarker;
        clone.extensionForType = this.extensionForType;
        clone.compileTimeClass = this.compileTimeClass;
        clone.itemDefinitions.addAll(this.itemDefinitions);
	}

	public void replaceDefinition(QName propertyName, ItemDefinition newDefinition) {
		for (int i=0; i<itemDefinitions.size(); i++) {
			ItemDefinition itemDef = itemDefinitions.get(i);
			if (itemDef.getName().equals(propertyName)) {
				if (!itemDef.getClass().isAssignableFrom(newDefinition.getClass())) {
					throw new IllegalArgumentException("The provided definition of class "+newDefinition.getClass().getName()+" does not match existing definition of class "+itemDef.getClass().getName());
				}
				if (!itemDef.getName().equals(newDefinition.getName())) {
					newDefinition = newDefinition.clone();
					newDefinition.setName(propertyName);
				}
				// Make sure this is set, not add. set will keep correct ordering
				itemDefinitions.set(i, newDefinition);
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
			sb.append(PrettyPrinter.prettyPrint(extensionForType));
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
		extendDumpHeader(sb);
		for (ItemDefinition def : getDefinitions()) {
			sb.append("\n");
			sb.append(def.debugDump(indent+1));
			extendDumpDefinition(sb, def);
		}
		return sb.toString();
	}

	protected void extendDumpHeader(StringBuilder sb) {
		// Do nothing
	}

	protected void extendDumpDefinition(StringBuilder sb, ItemDefinition def) {
		// Do nothing		
	}

	/**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "CTD";
    }

    @Override
    public String getDocClassName() {
        return "complex type";
    }

}

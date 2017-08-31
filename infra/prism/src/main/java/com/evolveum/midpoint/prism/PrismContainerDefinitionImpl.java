/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.prism.path.ParentPathSegment;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Definition of a property container.
 * <p/>
 * Property container groups properties into logical blocks. The reason for
 * grouping may be as simple as better understandability of data structure. But
 * the group usually means different meaning, source or structure of the data.
 * For example, the property container is frequently used to hold properties
 * that are dynamic, not fixed by a static schema. Such grouping also naturally
 * translates to XML and helps to "quarantine" such properties to avoid Unique
 * Particle Attribute problems.
 * <p/>
 * Property Container contains a set of (potentially multi-valued) properties.
 * The order of properties is not significant, regardless of the fact that it
 * may be fixed in the XML representation. In the XML representation, each
 * element inside Property Container must be either Property or a Property
 * Container.
 * <p/>
 * This class represents schema definition for property container. See
 * {@link Definition} for more details.
 *
 * @author Radovan Semancik
 */
public class PrismContainerDefinitionImpl<C extends Containerable> extends ItemDefinitionImpl<PrismContainer<C>>
		implements PrismContainerDefinition<C> {

    private static final long serialVersionUID = -5068923696147960699L;

	// There are situations where CTD is (maybe) null but class is defined.
	// TODO clean up this.
    protected ComplexTypeDefinition complexTypeDefinition;
    protected Class<C> compileTimeClass;

    /**
     * The constructors should be used only occasionally (if used at all).
     * Use the factory methods in the ResourceObjectDefintion instead.
     */
    public PrismContainerDefinitionImpl(@NotNull QName name, ComplexTypeDefinition complexTypeDefinition, @NotNull PrismContext prismContext) {
        this(name, complexTypeDefinition, prismContext, null);
    }

    public PrismContainerDefinitionImpl(@NotNull QName name, ComplexTypeDefinition complexTypeDefinition, @NotNull PrismContext prismContext,
    		Class<C> compileTimeClass) {
        super(name, determineTypeName(complexTypeDefinition), prismContext);
        this.complexTypeDefinition = complexTypeDefinition;
        if (complexTypeDefinition == null) {
            isRuntimeSchema = true;
            super.setDynamic(true);
        } else {
            isRuntimeSchema = complexTypeDefinition.isXsdAnyMarker();
            super.setDynamic(isRuntimeSchema);
        }
		this.compileTimeClass = compileTimeClass;
    }

    private static QName determineTypeName(ComplexTypeDefinition complexTypeDefinition) {
        if (complexTypeDefinition == null) {
            // Property container without type: xsd:any
            // FIXME: this is kind of hack, but it works now
            return DOMUtil.XSD_ANY;
        }
        return complexTypeDefinition.getTypeName();
    }

    @Override
	public Class<C> getCompileTimeClass() {
		if (compileTimeClass != null) {
			return compileTimeClass;
		}
		if (complexTypeDefinition == null) {
			return null;
		}
		return (Class<C>) complexTypeDefinition.getCompileTimeClass();	
	}

	public void setCompileTimeClass(Class<C> compileTimeClass) {
		this.compileTimeClass = compileTimeClass;
	}
    
    protected String getSchemaNamespace() {
        return getName().getNamespaceURI();
    }

    @Override
	public ComplexTypeDefinition getComplexTypeDefinition() {
        return complexTypeDefinition;
    }

    public void setComplexTypeDefinition(ComplexTypeDefinition complexTypeDefinition) {
        this.complexTypeDefinition = complexTypeDefinition;
    }

    @Override
    public boolean isAbstract() {
        if (super.isAbstract()) {
            return true;
        }
		return complexTypeDefinition != null && complexTypeDefinition.isAbstract();
	}
    
    @Override
	public void revive(PrismContext prismContext) {
		if (this.prismContext != null) {
			return;
		}
		this.prismContext = prismContext;
		if (complexTypeDefinition != null) {
			complexTypeDefinition.revive(prismContext);
		}
	}

    @Override
	public <D extends ItemDefinition> D findItemDefinition(@NotNull QName name, @NotNull Class<D> clazz, boolean caseInsensitive) {
        if (complexTypeDefinition != null) {
            return complexTypeDefinition.findItemDefinition(name, clazz, caseInsensitive);
        } else {
        	return null;	// xsd:any and similar dynamic definitions
        }
    }

    @Override
	public String getDefaultNamespace() {
		return complexTypeDefinition != null ? complexTypeDefinition.getDefaultNamespace() : null;
	}

	@Override
	public List<String> getIgnoredNamespaces() {
		return complexTypeDefinition != null ? complexTypeDefinition.getIgnoredNamespaces() : null;
	}

    public <ID extends ItemDefinition> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        for (;;) {
            if (path.isEmpty()) {
                if (clazz.isAssignableFrom(PrismContainerDefinition.class)) {
                    return (ID) this;
                } else {
                    return null;
                }
            }
            ItemPathSegment first = path.first();
            if (first instanceof NameItemPathSegment) {
                QName firstName = ((NameItemPathSegment)first).getName();
                return findNamedItemDefinition(firstName, path.rest(), clazz);
            } else if (first instanceof IdItemPathSegment) {
                path = path.rest();
            } else if (first instanceof ParentPathSegment) {
				ItemPath rest = path.rest();
                ComplexTypeDefinition parent = getSchemaRegistry().determineParentDefinition(getComplexTypeDefinition(), rest);
				if (rest.isEmpty()) {
					// requires that the parent is defined as an item (container, object)
					return (ID) getSchemaRegistry().findItemDefinitionByType(parent.getTypeName());
				} else {
					return parent.findItemDefinition(rest, clazz);
				}
            } else if (first instanceof ObjectReferencePathSegment) {
                throw new IllegalStateException("Couldn't use '@' path segment in this context. PCD=" + getTypeName() + ", path=" + path);
            } else {
                throw new IllegalStateException("Unexpected path segment: " + first + " in " + path);
            }
        }
    }

    @Override
	public <ID extends ItemDefinition> ID findNamedItemDefinition(@NotNull QName firstName, @NotNull ItemPath rest, @NotNull Class<ID> clazz) {

		// we need to be compatible with older versions..soo if the path does
		// not contains qnames with namespaces defined (but the prefix was
		// specified) match definition according to the local name
        if (StringUtils.isEmpty(firstName.getNamespaceURI())) {
        	for (ItemDefinition def : getDefinitions()){
        		if (QNameUtil.match(firstName, def.getName())){
        			return (ID) def.findItemDefinition(rest, clazz);
        		}
        	}
        }
        
        for (ItemDefinition def : getDefinitions()) {
            if (firstName.equals(def.getName())) {
                return (ID) def.findItemDefinition(rest, clazz);
            }
        }

//        if (isRuntimeSchema()) {
//            return findRuntimeItemDefinition(firstName, rest, clazz);
//        }

        return null;
    }

//    @Override
//	public <T> PrismPropertyDefinition<T> findPropertyDefinition(ItemPath path) {
//        while (!path.isEmpty() && !(path.first() instanceof NameItemPathSegment)) {
//    		path = path.rest();
//    	}
//        if (path.isEmpty()) {
//            throw new IllegalArgumentException("Property path is empty while searching for property definition in " + this);
//        }
//        QName firstName = ((NameItemPathSegment)path.first()).getName();
//        if (path.size() == 1) {
//            return findPropertyDefinition(firstName);
//        }
//        PrismContainerDefinition pcd = findContainerDefinition(firstName);
//        if (pcd == null) {
//            throw new IllegalArgumentException("There is no " + firstName + " subcontainer in " + this);
//        }
//        return pcd.findPropertyDefinition(path.rest());
//    }

    /**
     * Returns set of property definitions.
     * <p/>
     * WARNING: This may return definitions from the associated complex type.
     * Therefore changing the returned set may influence also the complex type definition.
     * <p/>
     * The set contains all property definitions of all types that were parsed.
     * Order of definitions is insignificant.
     *
     * @return set of definitions
     */
    @Override
	public List<? extends ItemDefinition> getDefinitions() {
        if (complexTypeDefinition == null) {
            // e.g. for xsd:any containers
            // FIXME
            return new ArrayList<>();
        }
        return complexTypeDefinition.getDefinitions();
    }

    /**
     * Returns set of property definitions.
     * <p/>
     * The set contains all property definitions of all types that were parsed.
     * Order of definitions is insignificant.
     * <p/>
     * The returned set is immutable! All changes may be lost.
     *
     * @return set of definitions
     */
    @Override
	public List<PrismPropertyDefinition> getPropertyDefinitions() {
    	List<PrismPropertyDefinition> props = new ArrayList<PrismPropertyDefinition>();
        for (ItemDefinition def : complexTypeDefinition.getDefinitions()) {
            if (def instanceof PrismPropertyDefinition) {
                props.add((PrismPropertyDefinition) def);
            }
        }
        return props;
    }

    @NotNull
	@Override
    public PrismContainer<C> instantiate() throws SchemaException {
        return instantiate(getName());
    }

    @NotNull
    @Override
    public PrismContainer<C> instantiate(QName elementName) throws SchemaException {
    	if (isAbstract()) {
			throw new SchemaException("Cannot instantiate abstract definition "+this);
		}
        elementName = addNamespaceIfApplicable(elementName);
        return new PrismContainer<>(elementName, this, prismContext);
    }

    @Override
	public ContainerDelta<C> createEmptyDelta(ItemPath path) {
		return new ContainerDelta(path, this, prismContext);
	}

	/**
     * Shallow clone
     */
    @NotNull
	@Override
    public PrismContainerDefinitionImpl<C> clone() {
        PrismContainerDefinitionImpl<C> clone = new PrismContainerDefinitionImpl<C>(name, complexTypeDefinition, prismContext, compileTimeClass);
        copyDefinitionData(clone);
        return clone;
    }

    protected void copyDefinitionData(PrismContainerDefinitionImpl<C> clone) {
        super.copyDefinitionData(clone);
        clone.complexTypeDefinition = this.complexTypeDefinition;
        clone.compileTimeClass = this.compileTimeClass;
    }
    
    @Override
	public ItemDefinition deepClone(Map<QName,ComplexTypeDefinition> ctdMap, Map<QName,ComplexTypeDefinition> onThisPath) {
		PrismContainerDefinitionImpl<C> clone = clone();
		ComplexTypeDefinition ctd = getComplexTypeDefinition();
		if (ctd != null) {
			ctd = ctd.deepClone(ctdMap, onThisPath);
			clone.setComplexTypeDefinition(ctd);
		}
		return clone;
	}

	@Override
	public PrismContainerDefinition<C> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition) {
    	PrismContainerDefinitionImpl<C> clone = clone();
    	clone.replaceDefinition(itemName, newDefinition);
        return clone;
    }

	@Override
	public void replaceDefinition(QName itemName, ItemDefinition newDefinition) {
    	ComplexTypeDefinition originalComplexTypeDefinition = getComplexTypeDefinition();
        ComplexTypeDefinition cloneComplexTypeDefinition = originalComplexTypeDefinition.clone();
        setComplexTypeDefinition(cloneComplexTypeDefinition);
		((ComplexTypeDefinitionImpl) cloneComplexTypeDefinition).replaceDefinition(itemName, newDefinition);
    }

    /**
     * Creates new instance of property definition and adds it to the container.
     * <p/>
     * This is the preferred method of creating a new definition.
     *
     * @param name     name of the property (element name)
     * @param typeName XSD type of the property
     * @return created property definition
     */
    public PrismPropertyDefinitionImpl createPropertyDefinition(QName name, QName typeName) {
        PrismPropertyDefinitionImpl propDef = new PrismPropertyDefinitionImpl(name, typeName, prismContext);
        addDefinition(propDef);
        return propDef;
    }
    
    private void addDefinition(ItemDefinition itemDef) {
		if (complexTypeDefinition == null) {
			throw new UnsupportedOperationException("Cannot add an item definition because there's no complex type definition");
		} else if (!(complexTypeDefinition instanceof ComplexTypeDefinitionImpl)) {
			throw new UnsupportedOperationException("Cannot add an item definition into complex type definition of type " + complexTypeDefinition.getClass().getName());
		} else {
			((ComplexTypeDefinitionImpl) complexTypeDefinition).add(itemDef);
		}
    }

    /**
     * Creates new instance of property definition and adds it to the container.
     * <p/>
     * This is the preferred method of creating a new definition.
     *
     * @param name      name of the property (element name)
     * @param typeName  XSD type of the property
     * @param minOccurs minimal number of occurrences
     * @param maxOccurs maximal number of occurrences (-1 means unbounded)
     * @return created property definition
     */
    public PrismPropertyDefinition createPropertyDefinition(QName name, QName typeName,
            int minOccurs, int maxOccurs) {
        PrismPropertyDefinitionImpl propDef = new PrismPropertyDefinitionImpl(name, typeName, prismContext);
        propDef.setMinOccurs(minOccurs);
        propDef.setMaxOccurs(maxOccurs);
        addDefinition(propDef);
        return propDef;
    }

    // Creates reference to other schema
    // TODO: maybe check if the name is in different namespace
    // TODO: maybe create entirely new concept of property reference?
    public PrismPropertyDefinition createPropertyDefinition(QName name) {
        PrismPropertyDefinition propDef = new PrismPropertyDefinitionImpl(name, null, prismContext);
        addDefinition(propDef);
        return propDef;
    }

    /**
     * Creates new instance of property definition and adds it to the container.
     * <p/>
     * This is the preferred method of creating a new definition.
     *
     * @param localName name of the property (element name) relative to the schema namespace
     * @param typeName  XSD type of the property
     * @return created property definition
     */
    public PrismPropertyDefinition createPropertyDefinition(String localName, QName typeName) {
        QName name = new QName(getSchemaNamespace(), localName);
        return createPropertyDefinition(name, typeName);
    }

    /**
     * Creates new instance of property definition and adds it to the container.
     * <p/>
     * This is the preferred method of creating a new definition.
     *
     * @param localName     name of the property (element name) relative to the schema namespace
     * @param localTypeName XSD type of the property
     * @return created property definition
     */
    public PrismPropertyDefinition createPropertyDefinition(String localName, String localTypeName) {
        QName name = new QName(getSchemaNamespace(), localName);
        QName typeName = new QName(getSchemaNamespace(), localTypeName);
        return createPropertyDefinition(name, typeName);
    }

    /**
     * Creates new instance of property definition and adds it to the container.
     * <p/>
     * This is the preferred method of creating a new definition.
     *
     * @param localName     name of the property (element name) relative to the schema namespace
     * @param localTypeName XSD type of the property
     * @param minOccurs     minimal number of occurrences
     * @param maxOccurs     maximal number of occurrences (-1 means unbounded)
     * @return created property definition
     */
    public PrismPropertyDefinition createPropertyDefinition(String localName, String localTypeName,
            int minOccurs, int maxOccurs) {
        QName name = new QName(getSchemaNamespace(), localName);
        QName typeName = new QName(getSchemaNamespace(), localTypeName);
        PrismPropertyDefinitionImpl propertyDefinition = createPropertyDefinition(name, typeName);
        propertyDefinition.setMinOccurs(minOccurs);
        propertyDefinition.setMaxOccurs(maxOccurs);
        return propertyDefinition;
    }
    
    public PrismContainerDefinition createContainerDefinition(QName name, QName typeName) {
    	return createContainerDefinition(name, typeName, 1, 1);
    }
    
    public PrismContainerDefinition createContainerDefinition(QName name, QName typeName,
            int minOccurs, int maxOccurs) {
    	PrismSchema typeSchema = prismContext.getSchemaRegistry().findSchemaByNamespace(typeName.getNamespaceURI());
    	if (typeSchema == null) {
    		throw new IllegalArgumentException("Schema for namespace "+typeName.getNamespaceURI()+" is not known in the prism context");
    	}
    	ComplexTypeDefinition typeDefinition = typeSchema.findComplexTypeDefinition(typeName);
    	if (typeDefinition == null) {
    		throw new IllegalArgumentException("Type "+typeName+" is not known in the schema");
    	}
    	return createContainerDefinition(name, typeDefinition, minOccurs, maxOccurs);
    }
    
    public PrismContainerDefinition<C> createContainerDefinition(QName name, ComplexTypeDefinition complexTypeDefinition,
            int minOccurs, int maxOccurs) {
    	PrismContainerDefinitionImpl<C> def = new PrismContainerDefinitionImpl<C>(name, complexTypeDefinition, prismContext);
        def.setMinOccurs(minOccurs);
        def.setMaxOccurs(maxOccurs);
        addDefinition(def);
        return def;
    }
    
	@Override
	public PrismContainerValue<C> createValue() {
		return new PrismContainerValue<>(prismContext);
	}

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(toString());
        if (isRuntimeSchema()) {
            sb.append(" dynamic");
        }
        for (Definition def : getDefinitions()) {
        	sb.append("\n");
        	if (def == this) {
        		// Not perfect loop protection, but works for now
                DebugUtil.indentDebugDump(sb, indent);
                sb.append("<itself>");
        	} else {
        		sb.append(def.debugDump(indent + 1));
        	}
        }
        return sb.toString();
    }


    @Override
	public boolean isEmpty() {
        return complexTypeDefinition.isEmpty();
    }
    
    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "PCD";
    }

    @Override
    public String getDocClassName() {
        return "container";
    }

}

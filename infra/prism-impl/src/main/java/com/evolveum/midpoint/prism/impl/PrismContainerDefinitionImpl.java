/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.impl.delta.ContainerDeltaImpl;
import com.evolveum.midpoint.prism.path.*;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.DefinitionUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

/**
 * Definition of a property container.
 * <p>
 * Property container groups properties into logical blocks. The reason for
 * grouping may be as simple as better understandability of data structure. But
 * the group usually means different meaning, source or structure of the data.
 * For example, the property container is frequently used to hold properties
 * that are dynamic, not fixed by a static schema. Such grouping also naturally
 * translates to XML and helps to "quarantine" such properties to avoid Unique
 * Particle Attribute problems.
 * <p>
 * Property Container contains a set of (potentially multi-valued) properties.
 * The order of properties is not significant, regardless of the fact that it
 * may be fixed in the XML representation. In the XML representation, each
 * element inside Property Container must be either Property or a Property
 * Container.
 * <p>
 * This class represents schema definition for property container. See
 * {@link Definition} for more details.
 *
 * @author Radovan Semancik
 */
public class PrismContainerDefinitionImpl<C extends Containerable> extends ItemDefinitionImpl<PrismContainer<C>>
        implements MutablePrismContainerDefinition<C> {

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
            //super.setDynamic(true);             // todo is this really ok?
        } else {
            isRuntimeSchema = complexTypeDefinition.isXsdAnyMarker();
            //super.setDynamic(isRuntimeSchema);  // todo is this really ok?
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

    @Override
    public void setCompileTimeClass(Class<C> compileTimeClass) {
        this.compileTimeClass = compileTimeClass;
    }

    @Override
    public Class<C> getTypeClass() {
        return compileTimeClass;
    }

    protected String getSchemaNamespace() {
        return getItemName().getNamespaceURI();
    }

    @Override
    public ComplexTypeDefinition getComplexTypeDefinition() {
        return complexTypeDefinition;
    }

    @Override
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
    public <D extends ItemDefinition> D findLocalItemDefinition(@NotNull QName name, @NotNull Class<D> clazz, boolean caseInsensitive) {
        if (complexTypeDefinition != null) {
            return complexTypeDefinition.findLocalItemDefinition(name, clazz, caseInsensitive);
        } else {
            return null;    // xsd:any and similar dynamic definitions
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

    @Override
    public <ID extends ItemDefinition> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        for (;;) {
            if (path.isEmpty()) {
                if (clazz.isAssignableFrom(PrismContainerDefinition.class)) {
                    return (ID) this;
                } else {
                    return null;
                }
            }
            Object first = path.first();
            if (ItemPath.isName(first)) {
                return findNamedItemDefinition(ItemPath.toName(first), path.rest(), clazz);
            } else if (ItemPath.isId(first)) {
                path = path.rest();
            } else if (ItemPath.isParent(first)) {
                ItemPath rest = path.rest();
                ComplexTypeDefinition parent = getSchemaRegistry().determineParentDefinition(getComplexTypeDefinition(), rest);
                if (rest.isEmpty()) {
                    // requires that the parent is defined as an item (container, object)
                    return (ID) getSchemaRegistry().findItemDefinitionByType(parent.getTypeName());
                } else {
                    return parent.findItemDefinition(rest, clazz);
                }
            } else if (ItemPath.isObjectReference(first)) {
                throw new IllegalStateException("Couldn't use '@' path segment in this context. PCD=" + getTypeName() + ", path=" + path);
            } else {
                throw new IllegalStateException("Unexpected path segment: " + first + " in " + path);
            }
        }
    }

    @Override
    public <ID extends ItemDefinition> ID findNamedItemDefinition(@NotNull QName firstName, @NotNull ItemPath rest, @NotNull Class<ID> clazz) {

        for (ItemDefinition def : getDefinitions()) {
            if (QNameUtil.match(firstName, def.getItemName())) {
                return (ID) def.findItemDefinition(rest, clazz);
            }
        }

        if (complexTypeDefinition != null && complexTypeDefinition.isXsdAnyMarker()) {
            SchemaRegistry schemaRegistry = getSchemaRegistry();
            if (schemaRegistry != null) {
                ItemDefinition def = schemaRegistry.findItemDefinitionByElementName(firstName);
                if (def != null) {
                    return (ID) def.findItemDefinition(rest, clazz);
                }
            }
        }

        return null;
    }

    /**
     * Returns set of property definitions.
     * <p>
     * WARNING: This may return definitions from the associated complex type.
     * Therefore changing the returned set may influence also the complex type definition.
     * <p>
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
     * <p>
     * The set contains all property definitions of all types that were parsed.
     * Order of definitions is insignificant.
     * <p>
     * The returned set is immutable! All changes may be lost.
     *
     * @return set of definitions
     */
    @Override
    public List<PrismPropertyDefinition> getPropertyDefinitions() {
        List<PrismPropertyDefinition> props = new ArrayList<>();
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
        return instantiate(getItemName());
    }

    @NotNull
    @Override
    public PrismContainer<C> instantiate(QName elementName) throws SchemaException {
        if (isAbstract()) {
            throw new SchemaException("Cannot instantiate abstract definition "+this);
        }
        elementName = DefinitionUtil.addNamespaceIfApplicable(elementName, this.itemName);
        return new PrismContainerImpl<>(elementName, this, prismContext);
    }

    @Override
    public ContainerDelta<C> createEmptyDelta(ItemPath path) {
        return new ContainerDeltaImpl<>(path, this, prismContext);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        if (complexTypeDefinition != null) {
            complexTypeDefinition.accept(visitor);
        }
    }

    /**
     * Shallow clone
     */
    @NotNull
    @Override
    public PrismContainerDefinitionImpl<C> clone() {
        PrismContainerDefinitionImpl<C> clone = new PrismContainerDefinitionImpl<>(itemName, complexTypeDefinition, prismContext, compileTimeClass);
        copyDefinitionData(clone);
        return clone;
    }

    protected void copyDefinitionData(PrismContainerDefinitionImpl<C> clone) {
        super.copyDefinitionData(clone);
        clone.complexTypeDefinition = this.complexTypeDefinition;
        clone.compileTimeClass = this.compileTimeClass;
    }

    @Override
    public ItemDefinition deepClone(Map<QName,ComplexTypeDefinition> ctdMap, Map<QName,ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
        PrismContainerDefinitionImpl<C> clone = clone();
        ComplexTypeDefinition ctd = getComplexTypeDefinition();
        if (ctd != null) {
            ctd = ctd.deepClone(ctdMap, onThisPath, postCloneAction);
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
     * <p>
     * This is the preferred method of creating a new definition.
     *
     * @param name     name of the property (element name)
     * @param typeName XSD type of the property
     * @return created property definition
     */
    @Override
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
     * <p>
     * This is the preferred method of creating a new definition.
     *
     * @param name      name of the property (element name)
     * @param typeName  XSD type of the property
     * @param minOccurs minimal number of occurrences
     * @param maxOccurs maximal number of occurrences (-1 means unbounded)
     * @return created property definition
     */
    @Override
    public MutablePrismPropertyDefinition<?> createPropertyDefinition(QName name, QName typeName,
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
     * <p>
     * This is the preferred method of creating a new definition.
     *
     * @param localName name of the property (element name) relative to the schema namespace
     * @param typeName  XSD type of the property
     * @return created property definition
     */
    @Override
    public MutablePrismPropertyDefinition<?> createPropertyDefinition(String localName, QName typeName) {
        QName name = new QName(getSchemaNamespace(), localName);
        return createPropertyDefinition(name, typeName);
    }

    /**
     * Creates new instance of property definition and adds it to the container.
     * <p>
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
     * <p>
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

    @Override
    public MutablePrismContainerDefinition<?> createContainerDefinition(QName name, QName typeName,
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

    @Override
    public MutablePrismContainerDefinition<?> createContainerDefinition(QName name, ComplexTypeDefinition complexTypeDefinition,
            int minOccurs, int maxOccurs) {
        PrismContainerDefinitionImpl<C> def = new PrismContainerDefinitionImpl<>(name, complexTypeDefinition, prismContext);
        def.setMinOccurs(minOccurs);
        def.setMaxOccurs(maxOccurs);
        addDefinition(def);
        return def;
    }

    @Override
    public boolean canBeDefinitionOf(PrismValue pvalue) {
        if (pvalue == null) {
            return false;
        }
        if (!(pvalue instanceof PrismContainerValue<?>)) {
            return false;
        }
        Itemable parent = pvalue.getParent();
        if (parent != null) {
            if (!(parent instanceof PrismContainer<?>)) {
                return false;
            }
            return canBeDefinitionOf((PrismContainer)parent);
        } else {
            // TODO: maybe look at the subitems?
            return true;
        }
    }

    @Override
    public boolean canRepresent(@NotNull QName typeName) {
        return QNameUtil.match(this.typeName, typeName) || complexTypeDefinition.canRepresent(typeName);
    }

    @Override
    public PrismContainerValue<C> createValue() {
        return new PrismContainerValueImpl<>(prismContext);
    }

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, new IdentityHashMap<>());
    }

    @Override
    public String debugDump(int indent, IdentityHashMap<Definition, Object> seen) {
        StringBuilder sb = DebugUtil.createIndentedStringBuilder(indent);
        sb.append(toString());
        extendDumpHeader(sb);
        if (isRuntimeSchema()) {
            sb.append(" dynamic");
        }
        if (seen.containsKey(this) || complexTypeDefinition != null && seen.containsKey(complexTypeDefinition)) {
            sb.append(" (already shown)");
        } else {
            seen.put(this, null);
            if (complexTypeDefinition != null) {
                seen.put(complexTypeDefinition, null);
            }
            for (Definition def : getDefinitions()) {
                sb.append("\n");
                sb.append(def.debugDump(indent + 1, seen));
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
    public String getDebugDumpClassName() {
        return "PCD";
    }

    @Override
    public String getDocClassName() {
        return "container";
    }

    @Override
    public MutablePrismContainerDefinition<C> toMutable() {
        return this;
    }
}

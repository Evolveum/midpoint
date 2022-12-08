/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismContainerImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.Checks;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

import org.jetbrains.annotations.NotNull;

/**
 * TODO review docs
 *
 * Resource Object.
 *
 * Resource Object understands resource-specific annotations, such as
 * identifiers, native object class, etc.
 *
 * Object class can be determined by using the definition (inherited from
 * PropertyContainer)
 *
 * @author Radovan Semancik
 */
@SuppressWarnings("rawtypes")
public final class ResourceAttributeContainerImpl extends PrismContainerImpl<ShadowAttributesType> implements ResourceAttributeContainer {
    private static final long serialVersionUID = 8878851067509560312L;

    /**
     * The constructors should be used only occasionally (if used at all).
     * Use the factory methods in the ResourceObjectDefinition instead.
     */
    ResourceAttributeContainerImpl(QName name, ResourceAttributeContainerDefinition definition) {
        super(name, definition, PrismContext.get());
    }

    @Override
    public ResourceAttributeContainerDefinition getDefinition() {
        PrismContainerDefinition prismContainerDefinition = super.getDefinition();
        if (prismContainerDefinition == null) {
            return null;
        }
        if (prismContainerDefinition instanceof ResourceAttributeContainerDefinition) {
            return (ResourceAttributeContainerDefinition) prismContainerDefinition;
        } else {
            throw new IllegalStateException("definition should be " + ResourceAttributeContainerDefinition.class + " but it is " + prismContainerDefinition.getClass() + " instead; definition = " + prismContainerDefinition.debugDump(0));
        }
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public @NotNull Collection<ResourceAttribute<?>> getAttributes() {
        // TODO: Iterate over the list to assert correct types
        return (Collection) getValue().getProperties();
    }

    @Override
    public void add(ResourceAttribute<?> attribute) throws SchemaException {
        super.add(attribute);
    }

    @Override
    public PrismProperty<?> getPrimaryIdentifier() {
        Collection<ResourceAttribute<?>> attrDefs = getPrimaryIdentifiers();
        if (attrDefs.size() > 1){
            throw new IllegalStateException("Resource object has more than one identifier.");
        }

        for (PrismProperty<?> p : attrDefs){
            return p;
        }

        return null;
    }

    @Override
    public Collection<ResourceAttribute<?>> getPrimaryIdentifiers() {
        return extractAttributesByDefinitions(getDefinition().getPrimaryIdentifiers());
    }

    @Override
    public <T> PrismProperty<T> getSecondaryIdentifier() {
        Collection<ResourceAttribute<?>> secondaryIdentifiers = getSecondaryIdentifiers();
        if (secondaryIdentifiers.size() > 1){
            throw new IllegalStateException("Resource object has more than one identifier.");
        }
        for (PrismProperty<?> p : secondaryIdentifiers){
            return (PrismProperty<T>) p;
        }
        return null;
    }

    @Override
    public Collection<ResourceAttribute<?>> getSecondaryIdentifiers() {
        return extractAttributesByDefinitions(getDefinition().getSecondaryIdentifiers());
    }

    @Override
    public Collection<ResourceAttribute<?>> getAllIdentifiers() {
        return extractAttributesByDefinitions(getDefinition().getAllIdentifiers());
    }

    @Override
    public @NotNull Collection<ResourceAttribute<?>> extractAttributesByDefinitions(
            Collection<? extends ResourceAttributeDefinition> definitions) {
        Collection<ResourceAttribute<?>> attributes = new ArrayList<>(definitions.size());
        for (ResourceAttributeDefinition attrDef : definitions) {
            for (ResourceAttribute<?> property : getAttributes()){
                if (attrDef.getItemName().equals(property.getElementName())){
                    property.setDefinition(attrDef);
                    attributes.add(property);
                }
            }
        }
        return attributes;
    }

    @Override
    public ResourceAttribute<String> getDescriptionAttribute() {
        if (getDefinition() == null) {
            return null;
        }
        return findAttribute(getDefinition().getDescriptionAttribute());
    }

    @Override
    public ResourceAttribute<String> getNamingAttribute() {
        ResourceAttributeContainerDefinition containerDef = getDefinition();
        if (containerDef == null) {
            return null;
        }
        ResourceAttributeDefinition<?> namingAttrDef = containerDef.getNamingAttribute();
        if (namingAttrDef == null) {
            return null;
        }
        return findAttribute(namingAttrDef);
    }

    @Override
    public ResourceAttribute getDisplayNameAttribute() {
        if (getDefinition() == null) {
            return null;
        }
        return findAttribute(getDefinition().getDisplayNameAttribute());
    }

    @Override
    public String getNativeObjectClass() {
        return getDefinition() == null ? null : getDefinition().getNativeObjectClass();
    }

    @Override
    public boolean isDefaultInAKind() {
        ResourceAttributeContainerDefinition definition = getDefinition();
        return definition != null && definition.isDefaultAccountDefinition();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> ResourceAttribute<X> findAttribute(QName attributeQName) {
        return (ResourceAttribute<X>) super.findProperty(ItemName.fromQName(attributeQName));
    }

    @Override
    public <X> ResourceAttribute<X> findAttribute(ResourceAttributeDefinition attributeDefinition) {
        return (ResourceAttribute<X>) getValue().findProperty(attributeDefinition);
    }

    @Override
    public <X> ResourceAttribute<X> findOrCreateAttribute(ResourceAttributeDefinition attributeDefinition) throws SchemaException {
        return (ResourceAttribute<X>) getValue().findOrCreateProperty(attributeDefinition);
    }

    @Override
    public <X> ResourceAttribute<X> findOrCreateAttribute(QName attributeName) throws SchemaException {
        return (ResourceAttribute<X>) getValue().findOrCreateProperty(ItemName.fromQName(attributeName));
    }

    @Override
    public <T> boolean contains(ResourceAttribute<T> attr) {
        return getValue().contains(attr);
    }

    @Override
    public ResourceAttributeContainer clone() {
        return cloneComplex(CloneStrategy.LITERAL);
    }

    @Override
    public ResourceAttributeContainerImpl cloneComplex(CloneStrategy strategy) {
        ResourceAttributeContainerImpl clone = new ResourceAttributeContainerImpl(getElementName(), getDefinition());
        copyValues(strategy, clone);
        return clone;
    }

    protected void copyValues(CloneStrategy strategy, ResourceAttributeContainerImpl clone) {
        super.copyValues(strategy, clone);
        // Nothing to copy
    }


    @Override
    public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw,
            ConsistencyCheckScope scope) {
        super.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
        List<PrismContainerValue<ShadowAttributesType>> values = getValues();
        if (values == null) {
            throw new IllegalStateException("Null values in ResourceAttributeContainer");
        }
        if (values.isEmpty()) {
            return;
        }
        if (values.size() > 1) {
            throw new IllegalStateException(values.size()+" values in ResourceAttributeContainer, expected just one");
        }
        PrismContainerValue value = values.get(0);
        Collection<Item<?,?>> items = value.getItems();
        for (Item item: items) {
            if (!(item instanceof ResourceAttribute)) {
                throw new IllegalStateException("Found illegal item in ResourceAttributeContainer: "+item+" ("+item.getClass()+")");
            }
        }
    }

    @Override
    public void applyDefinition(PrismContainerDefinition<ShadowAttributesType> definition, boolean force)
            throws SchemaException {
        if (definition != null) {
            Checks.checkSchema(definition instanceof ResourceAttributeContainerDefinition, "Definition should be %s not %s" ,
                    ResourceAttributeContainerDefinition.class.getSimpleName(), definition.getClass().getName());
        }
        super.applyDefinition(definition, force);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "RAC";
    }
}

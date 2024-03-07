/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismContainerImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

import com.google.common.base.Preconditions;
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
public final class ResourceAttributeContainerImpl
        extends PrismContainerImpl<ShadowAttributesType> implements ResourceAttributeContainer {
    @Serial private static final long serialVersionUID = 8878851067509560312L;

    /**
     * The constructors should be used only occasionally (if used at all).
     * Use the factory methods in the {@link ResourceObjectDefinition} instead.
     */
    ResourceAttributeContainerImpl(QName name, ResourceAttributeContainerDefinition definition) {
        super(name, definition);
    }

    @Override
    public ResourceAttributeContainerDefinition getDefinition() {
        PrismContainerDefinition prismContainerDefinition = super.getDefinition();
        if (prismContainerDefinition == null) {
            return null;
        }
        if (prismContainerDefinition instanceof ResourceAttributeContainerDefinition resourceAttributeContainerDefinition) {
            return resourceAttributeContainerDefinition;
        } else {
            throw new IllegalStateException(
                    "Definition should be %s but it is %s instead; definition = %s".formatted(
                            ResourceAttributeContainerDefinition.class,
                            prismContainerDefinition.getClass(),
                            prismContainerDefinition.debugDump(0)));
        }
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public @NotNull Collection<ResourceAttribute<?>> getAttributes() {
        // TODO: Iterate over the list to assert correct types
        return (Collection) getValue().getItems();
    }

    @Override
    public void add(ResourceAttribute<?> attribute) throws SchemaException {
        super.add(attribute);
    }

    @Override
    public ResourceAttribute<?> getPrimaryIdentifier() {
        return MiscUtil.extractSingleton(
                getPrimaryIdentifiers(),
                () -> new IllegalStateException("Resource object has no identifier"));
    }

    @Override
    public @NotNull Collection<ResourceAttribute<?>> getPrimaryIdentifiers() {
        return extractAttributesByDefinitions(getDefinitionRequired().getPrimaryIdentifiers());
    }

    @Override
    public @NotNull Collection<ResourceAttribute<?>> getSecondaryIdentifiers() {
        return extractAttributesByDefinitions(getDefinitionRequired().getSecondaryIdentifiers());
    }

    @Override
    public @NotNull Collection<ResourceAttribute<?>> getAllIdentifiers() {
        return extractAttributesByDefinitions(getDefinitionRequired().getAllIdentifiers());
    }

    private @NotNull Collection<ResourceAttribute<?>> extractAttributesByDefinitions(
            Collection<? extends ResourceAttributeDefinition> definitions) {
        Collection<ResourceAttribute<?>> attributes = new ArrayList<>(definitions.size());
        for (ResourceAttributeDefinition attrDef : definitions) {
            for (ResourceAttribute<?> property : getAttributes()) {
                if (attrDef.getItemName().equals(property.getElementName())) {
                    //noinspection unchecked
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
        //noinspection unchecked
        return (ResourceAttribute<X>) getValue().findProperty(attributeDefinition);
    }

    @Override
    public <X> ResourceAttribute<X> findOrCreateAttribute(ResourceAttributeDefinition attributeDefinition) throws SchemaException {
        //noinspection unchecked
        return (ResourceAttribute<X>) getValue().findOrCreateProperty(attributeDefinition);
    }

    @Override
    public <X> ResourceAttribute<X> findOrCreateAttribute(QName attributeName) throws SchemaException {
        //noinspection unchecked
        return (ResourceAttribute<X>) getValue().findOrCreateProperty(ItemName.fromQName(attributeName));
    }

    @Override
    public <T> boolean contains(ResourceAttribute<T> attr) {
        return getValue().contains(attr);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
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

    private void copyValues(CloneStrategy strategy, ResourceAttributeContainerImpl clone) {
        super.copyValues(strategy, clone);
        // Nothing to copy
    }

    @Override
    public void checkConsistenceInternal(
            Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope) {
        super.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
        List<PrismContainerValue<ShadowAttributesType>> values = getValues();
        if (values.isEmpty()) {
            return;
        }
        if (values.size() > 1) {
            throw new IllegalStateException(values.size()+" values in ResourceAttributeContainer, expected just one");
        }
        PrismContainerValue<ShadowAttributesType> value = values.get(0);
        Collection<Item<?,?>> items = value.getItems();
        for (Item item : items) {
            if (!(item instanceof ResourceAttribute)) {
                throw new IllegalStateException("Found illegal item in ResourceAttributeContainer: "+item+" ("+item.getClass()+")");
            }
        }
    }

    @Override
    protected void checkDefinition(@NotNull PrismContainerDefinition<ShadowAttributesType> def) {
        super.checkDefinition(def);
        Preconditions.checkArgument(
                def instanceof ResourceAttributeContainerDefinition,
                "Definition should be %s not %s" ,
                ResourceAttributeContainerDefinition.class.getSimpleName(), def.getClass().getName());
    }

    @Override
    protected String getDebugDumpClassName() {
        return "RAC";
    }
}

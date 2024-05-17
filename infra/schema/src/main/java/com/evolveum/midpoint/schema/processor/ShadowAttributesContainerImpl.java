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
public final class ShadowAttributesContainerImpl
        extends PrismContainerImpl<ShadowAttributesType> implements ShadowAttributesContainer {
    @Serial private static final long serialVersionUID = 8878851067509560312L;

    /**
     * The constructors should be used only occasionally (if used at all).
     * Use the factory methods in the {@link ResourceObjectDefinition} instead.
     */
    ShadowAttributesContainerImpl(QName name, ResourceAttributeContainerDefinition definition) {
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
    public @NotNull Collection<ShadowSimpleAttribute<?>> getAttributes() {
        // TODO: Iterate over the list to assert correct types
        return (Collection) getValue().getItems();
    }

    @Override
    public void add(ShadowSimpleAttribute<?> attribute) throws SchemaException {
        super.add(attribute);
    }

    @Override
    public ShadowSimpleAttribute<?> getPrimaryIdentifier() {
        return MiscUtil.extractSingleton(
                getPrimaryIdentifiers(),
                () -> new IllegalStateException("Resource object has no identifier"));
    }

    @Override
    public @NotNull Collection<ShadowSimpleAttribute<?>> getPrimaryIdentifiers() {
        return extractAttributesByDefinitions(getDefinitionRequired().getPrimaryIdentifiers());
    }

    @Override
    public @NotNull Collection<ShadowSimpleAttribute<?>> getSecondaryIdentifiers() {
        return extractAttributesByDefinitions(getDefinitionRequired().getSecondaryIdentifiers());
    }

    @Override
    public @NotNull Collection<ShadowSimpleAttribute<?>> getAllIdentifiers() {
        return extractAttributesByDefinitions(getDefinitionRequired().getAllIdentifiers());
    }

    private @NotNull Collection<ShadowSimpleAttribute<?>> extractAttributesByDefinitions(
            Collection<? extends ShadowSimpleAttributeDefinition> definitions) {
        Collection<ShadowSimpleAttribute<?>> attributes = new ArrayList<>(definitions.size());
        for (ShadowSimpleAttributeDefinition attrDef : definitions) {
            for (ShadowSimpleAttribute<?> property : getAttributes()) {
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
    public ShadowSimpleAttribute<String> getNamingAttribute() {
        ResourceAttributeContainerDefinition containerDef = getDefinition();
        if (containerDef == null) {
            return null;
        }
        ShadowSimpleAttributeDefinition<?> namingAttrDef = containerDef.getResourceObjectDefinition().getNamingAttribute();
        if (namingAttrDef == null) {
            return null;
        }
        return findAttribute(namingAttrDef);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> ShadowSimpleAttribute<X> findAttribute(QName attributeQName) {
        return (ShadowSimpleAttribute<X>) super.findProperty(ItemName.fromQName(attributeQName));
    }

    @Override
    public <X> ShadowSimpleAttribute<X> findAttribute(ShadowSimpleAttributeDefinition attributeDefinition) {
        //noinspection unchecked
        return (ShadowSimpleAttribute<X>) getValue().findProperty(attributeDefinition);
    }

    @Override
    public <X> ShadowSimpleAttribute<X> findOrCreateAttribute(ShadowSimpleAttributeDefinition attributeDefinition) throws SchemaException {
        //noinspection unchecked
        return (ShadowSimpleAttribute<X>) getValue().findOrCreateProperty(attributeDefinition);
    }

    @Override
    public <X> ShadowSimpleAttribute<X> findOrCreateAttribute(QName attributeName) throws SchemaException {
        //noinspection unchecked
        return (ShadowSimpleAttribute<X>) getValue().findOrCreateProperty(ItemName.fromQName(attributeName));
    }

    @Override
    public <T> boolean contains(ShadowSimpleAttribute<T> attr) {
        return getValue().contains(attr);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ShadowAttributesContainer clone() {
        return cloneComplex(CloneStrategy.LITERAL);
    }

    @Override
    public ShadowAttributesContainerImpl cloneComplex(CloneStrategy strategy) {
        ShadowAttributesContainerImpl clone = new ShadowAttributesContainerImpl(getElementName(), getDefinition());
        copyValues(strategy, clone);
        return clone;
    }

    private void copyValues(CloneStrategy strategy, ShadowAttributesContainerImpl clone) {
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
            if (!(item instanceof ShadowSimpleAttribute)) {
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

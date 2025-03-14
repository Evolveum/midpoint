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
import java.util.stream.Collectors;

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
    ShadowAttributesContainerImpl(QName name, ShadowAttributesContainerDefinition definition) {
        super(name, definition);
    }

    @Override
    public ShadowAttributesContainerDefinition getDefinition() {
        PrismContainerDefinition prismContainerDefinition = super.getDefinition();
        if (prismContainerDefinition == null) {
            return null;
        } else if (prismContainerDefinition instanceof ShadowAttributesContainerDefinition shadowAttributesContainerDefinition) {
            return shadowAttributesContainerDefinition;
        } else {
            throw new IllegalStateException(
                    "Definition should be %s but it is %s instead; definition = %s".formatted(
                            ShadowAttributesContainerDefinition.class,
                            prismContainerDefinition.getClass(),
                            prismContainerDefinition.debugDump(0)));
        }
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public @NotNull Collection<ShadowAttribute<?, ?, ?, ?>> getAttributes() {
        // TODO: Iterate over the list to assert correct types
        return (Collection) getValue().getItems();
    }

    @Override
    public @NotNull Collection<ShadowSimpleAttribute<?>> getSimpleAttributes() {
        return getValue().getItems().stream()
                .filter(item -> item instanceof ShadowSimpleAttribute)
                .map(item -> (ShadowSimpleAttribute<?>) item)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public @NotNull Collection<ShadowReferenceAttribute> getReferenceAttributes() {
        return getValue().getItems().stream()
                .filter(item -> item instanceof ShadowReferenceAttribute)
                .map(item -> (ShadowReferenceAttribute) item)
                .toList();
    }

    @Override
    public void add(ShadowAttribute<?, ?, ?, ?> attribute) throws SchemaException {
        super.add((Item<?, ?>) attribute);
    }

    @Override
    public ShadowSimpleAttribute<?> getPrimaryIdentifier() {
        return MiscUtil.extractSingleton(
                getPrimaryIdentifiers(),
                () -> new IllegalStateException("Resource object has no identifier"));
    }

    @Override
    public @NotNull Collection<ShadowSimpleAttribute<?>> getPrimaryIdentifiers() {
        return extractSimpleAttributesByDefinitions(getDefinitionRequired().getPrimaryIdentifiers());
    }

    @Override
    public @NotNull Collection<ShadowSimpleAttribute<?>> getSecondaryIdentifiers() {
        return extractSimpleAttributesByDefinitions(getDefinitionRequired().getSecondaryIdentifiers());
    }

    @Override
    public @NotNull Collection<ShadowSimpleAttribute<?>> getAllIdentifiers() {
        return extractSimpleAttributesByDefinitions(getDefinitionRequired().getAllIdentifiers());
    }

    private @NotNull Collection<ShadowSimpleAttribute<?>> extractSimpleAttributesByDefinitions(
            Collection<? extends ShadowSimpleAttributeDefinition> definitions) {
        Collection<ShadowSimpleAttribute<?>> attributes = new ArrayList<>(definitions.size());
        for (var simpleAttribute : getSimpleAttributes()) {
            for (var attrDef : definitions) {
                if (attrDef.getItemName().equals(simpleAttribute.getElementName())) {
                    attributes.add(simpleAttribute);
                }
            }
        }
        return attributes;
    }

    @Override
    public ShadowSimpleAttribute<String> getNamingAttribute() {
        ShadowAttributesContainerDefinition containerDef = getDefinition();
        if (containerDef == null) {
            return null;
        }
        ShadowSimpleAttributeDefinition<?> namingAttrDef = containerDef.getResourceObjectDefinition().getNamingAttribute();
        if (namingAttrDef == null) {
            return null;
        }
        return findSimpleAttribute(namingAttrDef);
    }

    @Override
    public ShadowAttribute<?, ?, ?, ?> findAttribute(QName attrName) {
        return (ShadowAttribute<?, ?, ?, ?>) super.findItem(ItemName.fromQName(attrName));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> ShadowSimpleAttribute<X> findSimpleAttribute(QName attributeQName) {
        return (ShadowSimpleAttribute<X>) super.findProperty(ItemName.fromQName(attributeQName));
    }

    @Override
    public ShadowReferenceAttribute findReferenceAttribute(QName attributeQName) {
        return (ShadowReferenceAttribute) super.findReference(ItemName.fromQName(attributeQName));
    }

    @Override
    public <X> ShadowSimpleAttribute<X> findSimpleAttribute(ShadowSimpleAttributeDefinition attributeDefinition) {
        //noinspection unchecked
        return (ShadowSimpleAttribute<X>) getValue().findProperty(attributeDefinition);
    }

    @Override
    public <X> ShadowSimpleAttribute<X> findOrCreateSimpleAttribute(ShadowSimpleAttributeDefinition attributeDefinition)
            throws SchemaException {
        //noinspection unchecked
        return (ShadowSimpleAttribute<X>) getValue().findOrCreateProperty(attributeDefinition);
    }

    @Override
    public <X> ShadowSimpleAttribute<X> findOrCreateSimpleAttribute(QName attributeName) throws SchemaException {
        //noinspection unchecked
        return (ShadowSimpleAttribute<X>) getValue().findOrCreateProperty(ItemName.fromQName(attributeName));
    }

    @Override
    public ShadowReferenceAttribute findOrCreateReferenceAttribute(QName attributeName) throws SchemaException {
        return (ShadowReferenceAttribute) getValue().findOrCreateReference(ItemName.fromQName(attributeName));
    }

    @Override
    public <T> boolean contains(ShadowSimpleAttribute<T> attr) {
        return getValue().contains(attr);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ShadowAttributesContainer clone() {
        return cloneComplex(CloneStrategy.LITERAL_MUTABLE);
    }

    @Override
    public void remove(ShadowAttribute<?, ?, ?, ?> item) {
        super.remove((Item<?, ?>) item);
    }

    @Override
    public void removeAttribute(@NotNull ItemName name) {
        if (hasAnyValue()) {
            getValue().removeItem(name);
        }
    }

    @Override
    public @NotNull ShadowAttributesContainerImpl cloneComplex(@NotNull CloneStrategy strategy) {
        if (isImmutable() && !strategy.mutableCopy()) {
            return this; // FIXME here should come a flyweight
        }

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
        for (var item : values.get(0).getItems()) {
            if (!(item instanceof ShadowAttribute<?, ?, ?, ?>)) {
                throw new IllegalStateException(
                        "Found illegal item in %s: %s (%s)".formatted(
                                getClass().getSimpleName(), item, item.getClass()));
            }
        }
    }

    @Override
    protected void checkDefinition(@NotNull PrismContainerDefinition<ShadowAttributesType> def) {
        super.checkDefinition(def);
        Preconditions.checkArgument(
                def instanceof ShadowAttributesContainerDefinition,
                "Definition should be %s not %s" ,
                ShadowAttributesContainerDefinition.class.getSimpleName(), def.getClass().getName());
    }

    @Override
    public void performFreeze() {
        getValue(); // let's create an empty value if there's none
        super.performFreeze();
    }

    @Override
    protected String getDebugDumpClassName() {
        return "SAC";
    }
}

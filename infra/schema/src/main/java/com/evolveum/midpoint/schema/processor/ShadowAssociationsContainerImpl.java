/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismContainerImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationsType;

public final class ShadowAssociationsContainerImpl
        extends PrismContainerImpl<ShadowAssociationsType> implements ShadowAssociationsContainer {

    /**
     * The constructors should be used only occasionally (if used at all).
     * Use the factory methods in the {@link ResourceObjectDefinition} instead.
     */
    ShadowAssociationsContainerImpl(QName name, ShadowAssociationsContainerDefinition definition) {
        super(name, definition);
    }

    @Override
    public ShadowAssociationsContainerDefinition getDefinition() {
        PrismContainerDefinition<?> actualDefinition = super.getDefinition();
        if (actualDefinition == null) {
            return null;
        }
        if (actualDefinition instanceof ShadowAssociationsContainerDefinition shadowAssociationsContainerDefinition) {
            return shadowAssociationsContainerDefinition;
        } else {
            throw new IllegalStateException(
                    "Definition should be %s but it is %s instead; definition = %s".formatted(
                            ShadowAssociationsContainerDefinition.class,
                            actualDefinition.getClass(),
                            actualDefinition.debugDump(0)));
        }
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public @NotNull Collection<ShadowAssociation> getAssociations() {
        // TODO: Iterate over the list to assert correct types
        return List.copyOf((Collection) getValue().getItems());
    }

    @Override
    public void add(Item<?, ?> item) throws SchemaException {
        if (item instanceof ShadowAssociation association) {
            add(association);
        } else {
            throw new IllegalArgumentException("Couldn't add item: " + item);
        }
    }

    @Override
    public void add(ShadowAssociation association) throws SchemaException {
        super.add(association);
    }

    @Override
    public ShadowAssociation findAssociation(QName assocName) {
        return (ShadowAssociation) super.<ShadowAssociationValueType>findContainer(assocName);
    }

    @Override
    public ShadowAssociation findOrCreateAssociation(QName assocName) throws SchemaException {
        return (ShadowAssociation) getValue().<ShadowAssociationValueType>findOrCreateContainer(assocName);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ShadowAssociationsContainerImpl clone() {
        return cloneComplex(CloneStrategy.LITERAL_MUTABLE);
    }

    @Override
    public @NotNull ShadowAssociationsContainerImpl cloneComplex(@NotNull CloneStrategy strategy) {
        if (isImmutable() && !strategy.mutableCopy()) {
            return this; // FIXME here should come a flyweight
        }

        ShadowAssociationsContainerImpl clone = new ShadowAssociationsContainerImpl(getElementName(), getDefinition());
        copyValues(strategy, clone);
        return clone;
    }

    private void copyValues(CloneStrategy strategy, ShadowAssociationsContainerImpl clone) {
        super.copyValues(strategy, clone);
        // Nothing to copy
    }

    @Override
    public void checkConsistenceInternal(
            Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope) {
        super.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
        var values = getValues();
        if (values.isEmpty()) {
            return;
        }
        if (values.size() > 1) {
            throw new IllegalStateException(values.size()+" values in ResourceAssociationContainer, expected just one");
        }
        var value = values.get(0);
        Collection<Item<?,?>> items = value.getItems();
        for (Item<?, ?> item : items) {
            if (!(item instanceof ShadowAssociation)) {
                throw new IllegalStateException(
                        "Found illegal item in ResourceAssociationContainer: %s (%s)".formatted(
                                item, item.getClass()));
            }
        }
    }

    @Override
    public void checkDefinition(@NotNull PrismContainerDefinition<ShadowAssociationsType> def) {
        super.checkDefinition(def);
        Preconditions.checkArgument(
                def instanceof ShadowAssociationsContainerDefinition,
                "Definition should be %s not %s" ,
                ShadowAssociationsContainerDefinition.class.getSimpleName(), def.getClass().getName());
    }

    @Override
    protected String getDebugDumpClassName() {
        return "RAsC";
    }
}

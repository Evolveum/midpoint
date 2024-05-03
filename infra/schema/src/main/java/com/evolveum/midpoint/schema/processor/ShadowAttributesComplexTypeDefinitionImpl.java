/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * Implementation of a CTD for a {@link ShadowAttributesContainer}.
 *
 * It is simply a wrapper around {@link ResourceObjectDefinition} that hides all item definitions
 * except for attribute definitions.
 */
class ShadowAttributesComplexTypeDefinitionImpl
        extends AbstractShadowItemsContainerTypeDefinitionImpl
        implements ShadowAttributesComplexTypeDefinition {

    private ShadowAttributesComplexTypeDefinitionImpl(@NotNull ResourceObjectDefinition objectDefinition) {
        super(objectDefinition);
    }

    public static ShadowAttributesComplexTypeDefinitionImpl of(@NotNull ResourceObjectDefinition resourceObjectDefinition) {
        return new ShadowAttributesComplexTypeDefinitionImpl(resourceObjectDefinition);
    }

    @Override
    public @NotNull List<? extends ShadowSimpleAttributeDefinition<?>> getDefinitions() {
        return objectDefinition.getAttributeDefinitions();
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        var def = objectDefinition.findItemDefinition(path, clazz);
        if (def instanceof ShadowSimpleAttributeDefinition<?>) {
            return def;
        } else {
            return null;
        }
    }

    @Override
    public @NotNull ShadowAttributesComplexTypeDefinitionImpl clone() {
        return of(
                objectDefinition.clone());
    }

    @Override
    public @NotNull List<? extends ShadowSimpleAttributeDefinition<?>> getAttributeDefinitions() {
        return objectDefinition.getAttributeDefinitions();
    }

    @Override
    public @NotNull Collection<? extends ShadowSimpleAttributeDefinition<?>> getPrimaryIdentifiers() {
        return objectDefinition.getPrimaryIdentifiers();
    }

    @Override
    public @NotNull Collection<QName> getPrimaryIdentifiersNames() {
        return objectDefinition.getPrimaryIdentifiersNames();
    }

    @Override
    public @NotNull Collection<? extends ShadowSimpleAttributeDefinition<?>> getSecondaryIdentifiers() {
        return objectDefinition.getSecondaryIdentifiers();
    }

    @Override
    public @NotNull Collection<QName> getSecondaryIdentifiersNames() {
        return objectDefinition.getSecondaryIdentifiersNames();
    }

    @Override
    public String toString() {
        return "RACTD (" + getAttributeDefinitions().size() + " attributes) in " + objectDefinition;
    }
}

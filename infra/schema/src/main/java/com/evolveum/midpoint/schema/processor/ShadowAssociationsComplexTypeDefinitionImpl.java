/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.List;

import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.Nullable;

/**
 * Implementation of a CTD for a {@link ShadowAssociationsContainer}.
 *
 * It is simply a wrapper around {@link ResourceObjectDefinition} that hides all item definitions
 * except for associations definitions.
 */
class ShadowAssociationsComplexTypeDefinitionImpl
        extends AbstractShadowItemsContainerTypeDefinitionImpl
        implements ShadowAssociationsComplexTypeDefinition {

    private ShadowAssociationsComplexTypeDefinitionImpl(@NotNull ResourceObjectDefinition objectDefinition) {
        super(objectDefinition);
    }

    public static ShadowAssociationsComplexTypeDefinitionImpl of(@NotNull ResourceObjectDefinition resourceObjectDefinition) {
        return new ShadowAssociationsComplexTypeDefinitionImpl(resourceObjectDefinition);
    }

    @Override
    public @NotNull List<? extends ShadowReferenceAttributeDefinition> getDefinitions() {
        return objectDefinition.getReferenceAttributeDefinitions();
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        var def = objectDefinition.findItemDefinition(path, clazz);
        if (def instanceof ShadowReferenceAttributeDefinition) {
            return def;
        } else {
            return null;
        }
    }

    @Override
    public @Nullable SchemaContextDefinition getSchemaContextDefinition() {
        return objectDefinition.getSchemaContextDefinition();
    }

    @Override
    public @NotNull ShadowAssociationsComplexTypeDefinitionImpl clone() {
        return of(
                objectDefinition.clone());
    }

    public @NotNull List<? extends ShadowReferenceAttributeDefinition> getReferenceAttributeDefinitions() {
        return objectDefinition.getReferenceAttributeDefinitions();
    }

    @Override
    public String toString() {
        return "SAssocCTD (" + getReferenceAttributeDefinitions().size() + " associations) in " + objectDefinition;
    }
}

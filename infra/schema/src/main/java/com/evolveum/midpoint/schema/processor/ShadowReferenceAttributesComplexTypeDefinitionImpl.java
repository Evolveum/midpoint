/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Implementation of a CTD for a {@link ShadowAttributesContainer} providing reference attributes only.
 */
class ShadowReferenceAttributesComplexTypeDefinitionImpl
        extends ShadowAttributesComplexTypeDefinitionImpl {

    private ShadowReferenceAttributesComplexTypeDefinitionImpl(@NotNull ResourceObjectDefinition objectDefinition) {
        super(objectDefinition);
    }

    public static ShadowReferenceAttributesComplexTypeDefinitionImpl of(@NotNull ResourceObjectDefinition resourceObjectDefinition) {
        return new ShadowReferenceAttributesComplexTypeDefinitionImpl(resourceObjectDefinition);
    }

    @Override
    public @NotNull List<? extends ItemDefinition<?>> getDefinitions() {
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

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public @NotNull ShadowReferenceAttributesComplexTypeDefinitionImpl clone() {
        return of(
                objectDefinition.clone());
    }

    @Override
    public @NotNull List<? extends ShadowAttributeDefinition<?, ?, ?, ?>> getAttributeDefinitions() {
        return objectDefinition.getReferenceAttributeDefinitions();
    }

    @Override
    public String toString() {
        return "SRACTDImpl(" + getReferenceAttributeDefinitions().size() + " reference attributes) in " + objectDefinition;
    }
}

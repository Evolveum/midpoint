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
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;

/**
 * Implementation of a CTD for a {@link ShadowAttributesContainer} providing simple attributes only.
 */
class ShadowSimpleAttributesComplexTypeDefinitionImpl
        extends ShadowAttributesComplexTypeDefinitionImpl {

    private ShadowSimpleAttributesComplexTypeDefinitionImpl(@NotNull ResourceObjectDefinition objectDefinition) {
        super(objectDefinition);
    }

    public static ShadowSimpleAttributesComplexTypeDefinitionImpl of(@NotNull ResourceObjectDefinition resourceObjectDefinition) {
        return new ShadowSimpleAttributesComplexTypeDefinitionImpl(resourceObjectDefinition);
    }

    @Override
    public @NotNull List<? extends ItemDefinition<?>> getDefinitions() {
        return objectDefinition.getSimpleAttributeDefinitions();
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

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public @NotNull ShadowSimpleAttributesComplexTypeDefinitionImpl clone() {
        return of(
                objectDefinition.clone());
    }

    @Override
    public @NotNull List<? extends ShadowAttributeDefinition<?, ?, ?, ?>> getAttributeDefinitions() {
        return objectDefinition.getSimpleAttributeDefinitions();
    }

    @Override
    public String toString() {
        return "SSACTDImpl(" + getSimpleAttributeDefinitions().size() + " simple attributes) in " + objectDefinition;
    }
}

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

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.util.MiscUtil.castOrNull;

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
    public @NotNull List<? extends ShadowAssociationDefinition> getDefinitions() {
        return objectDefinition.getAssociationDefinitions();
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        if (path.size() == 1) {
            return findLocalItemDefinition(path.firstToName(), clazz, false);
        } else {
            return null;
        }
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findLocalItemDefinition(@NotNull QName name, @NotNull Class<ID> clazz, boolean caseInsensitive) {
        if (caseInsensitive) {
            return findLocalItemDefinitionByIteration(name, clazz, true);
        } else {
            return castOrNull(objectDefinition.findAssociationDefinition(name), clazz);
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

    @Override
    public String toString() {
        return "SAssocCTD (" + getDefinitions().size() + " associations) in " + objectDefinition;
    }
}

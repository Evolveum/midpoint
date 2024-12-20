/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents ad-hoc combination of definitions of structural and auxiliary object classes.
 *
 * @author semancik
 */
public interface CompositeObjectDefinition extends ResourceObjectDefinition, LayeredDefinition {

    /**
     * Returns the structural definition that represents the "base" of this composite definition.
     */
    @NotNull ResourceObjectDefinition getStructuralDefinition();

    /**
     * Returns auxiliary definitions. They enrich the structural definition e.g. by adding attribute
     * definitions. TODO specify better
     */
    @NotNull Collection<? extends ResourceObjectDefinition> getAuxiliaryDefinitions();

    /**
     * Returns the names of auxiliary object classes that are "statically" defined for the structural object type.
     * (The restriction to the structural definition is intentional.)
     *
     * @see ResourceObjectDefinition#getConfiguredAuxiliaryObjectClassNames()
     */
    @Override
    default @NotNull Collection<QName> getConfiguredAuxiliaryObjectClassNames() {
        return getStructuralDefinition()
                .getConfiguredAuxiliaryObjectClassNames();
    }

    /**
     * Returns immutable definition. Assumes component definitions are immutable.
     *
     * FIXME sometimes, the `structuralDefinition` is itself a composite definition. We should avoid wrapping it
     *  into another composite definitions. Please fix this some day. See MID-9156.
     */
    static @NotNull CompositeObjectDefinitionImpl of(
            @NotNull ResourceObjectDefinition structuralDefinition,
            @Nullable Collection<? extends ResourceObjectDefinition> auxiliaryDefinitions) {
        return CompositeObjectDefinitionImpl.immutable(structuralDefinition, auxiliaryDefinitions);
    }

    static @NotNull CompositeObjectDefinitionImpl mutableOf(
            @NotNull ResourceObjectDefinition structuralDefinition,
            @Nullable Collection<? extends ResourceObjectDefinition> auxiliaryDefinitions) {
        return CompositeObjectDefinitionImpl.mutable(structuralDefinition, auxiliaryDefinitions);
    }
}

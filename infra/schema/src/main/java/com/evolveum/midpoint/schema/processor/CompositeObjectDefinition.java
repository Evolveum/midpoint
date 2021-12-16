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

/**
 * Used to represent combined definition of structural and auxiliary object classes.
 *
 * @author semancik
 *
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
    @NotNull Collection<ResourceObjectDefinition> getAuxiliaryDefinitions();

    /**
     * The restriction to the structural definition is intentional.
     *
     * @see ResourceObjectDefinition#getConfiguredAuxiliaryObjectClassNames()
     */
    @Override
    default Collection<QName> getConfiguredAuxiliaryObjectClassNames() {
        return getStructuralDefinition()
                .getConfiguredAuxiliaryObjectClassNames();
    }
}

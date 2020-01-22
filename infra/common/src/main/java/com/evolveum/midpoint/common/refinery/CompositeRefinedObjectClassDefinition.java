/*
 * Copyright (c) 2015-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.refinery;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Used to represent combined definition of structural and auxiliary object classes.
 *
 * @author semancik
 *
 */
public interface CompositeRefinedObjectClassDefinition extends RefinedObjectClassDefinition {

    RefinedObjectClassDefinition getStructuralObjectClassDefinition();

    @NotNull
    Collection<RefinedObjectClassDefinition> getAuxiliaryObjectClassDefinitions();

}

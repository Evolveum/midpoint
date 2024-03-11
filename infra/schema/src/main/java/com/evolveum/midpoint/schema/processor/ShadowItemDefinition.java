/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Access to both {@link ResourceAttributeDefinition} and {@link ShadowAssociationDefinition}.
 * For the time being, it does not extend {@link ItemDefinition} because of typing complications.
 */
public interface ShadowItemDefinition<I extends ShadowItem> {

    @NotNull I instantiate() throws SchemaException;

}

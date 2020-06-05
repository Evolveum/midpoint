/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * Provides value policy when needed (e.g. in generate expression evaluator).
 * Accepts setting of path and definition for the item for which the value policy will be obtained.
 */
public interface ConfigurableValuePolicyResolver extends ValuePolicyResolver {

    /**
     * Sets the definition of the item for which value policy will be provided.
     */
    default void setOutputDefinition(ItemDefinition outputDefinition) { }

    /**
     * Sets the path of the item for which value policy will be provided.
     * (Actually this seems to be quite unused.)
     */
    @SuppressWarnings("unused")
    default void setOutputPath(ItemPath outputPath) { }
}

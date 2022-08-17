/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import com.evolveum.midpoint.prism.ItemDefinition;

/**
 * Provides value policy when needed (e.g. in generate expression evaluator).
 * Accepts setting of path and definition for the item for which the value policy will be obtained.
 */
public interface ConfigurableValuePolicySupplier extends ValuePolicySupplier {

    /**
     * Sets the definition of the item for which value policy will be provided.
     */
    default void setOutputDefinition(ItemDefinition<?> outputDefinition) { }
}

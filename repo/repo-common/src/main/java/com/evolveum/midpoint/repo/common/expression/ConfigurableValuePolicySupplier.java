/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

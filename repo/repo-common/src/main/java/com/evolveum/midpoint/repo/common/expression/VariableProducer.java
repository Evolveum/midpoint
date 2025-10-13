/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.expression;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Produces extra variables from values of existing sources.
 *
 * TODO consider better name or generalization of this interface
 */
@FunctionalInterface
public interface VariableProducer {

    /**
     * Processes a source value, putting extra variables to `variables` map (if applicable).
     */
    void processSourceValue(@NotNull Source<?, ?> source, @Nullable PrismValue value, @NotNull VariablesMap variables);
}

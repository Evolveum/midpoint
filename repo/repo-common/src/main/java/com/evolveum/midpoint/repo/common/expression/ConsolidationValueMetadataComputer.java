/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.expression;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

/**
 * Computes value metadata during consolidation.
 *
 */
@Experimental
public interface ConsolidationValueMetadataComputer {

    @NotNull ValueMetadataType compute(@NotNull List<ValueMetadataType> nonNegativeValues,
            @NotNull List<ValueMetadataType> existingValues,
            @NotNull OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException;

    static ConsolidationValueMetadataComputer named(Supplier<String> nameSupplier, ConsolidationValueMetadataComputer computer) {
        return new ConsolidationValueMetadataComputer() {
            @Override
            public @NotNull ValueMetadataType compute(@NotNull List<ValueMetadataType> nonNegativeValues,
                    @NotNull List<ValueMetadataType> existingValues,
                    @NotNull OperationResult result)
                    throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
                    ConfigurationException, ExpressionEvaluationException {
                return computer.compute(nonNegativeValues, existingValues, result);
            }

            @Override
            public String toString() {
                return nameSupplier.get();
            }
        };
    }
}

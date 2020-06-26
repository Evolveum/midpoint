/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.ValueMetadata;
import com.evolveum.midpoint.repo.common.expression.ValueMetadataComputer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;

/**
 * TODO
 */
class SimpleValueMetadataComputer implements ValueMetadataComputer {
    @NotNull final MappingImpl<?, ?> dataMapping;
    @NotNull final MetadataMappingEvaluator metadataMappingEvaluator;

    SimpleValueMetadataComputer(MappingImpl<?, ?> dataMapping) {
        assert !dataMapping.getMetadataMappings().isEmpty(); // for now

        this.dataMapping = dataMapping;
        this.metadataMappingEvaluator = dataMapping.metadataMappingEvaluator;
        if (metadataMappingEvaluator == null) {
            throw new IllegalStateException("Metadata mapping evaluation requested but"
                    + " metadataMappingEvaluator is not present; in " + dataMapping.getMappingContextDescription());
        }
    }

    @Override
    public ValueMetadata compute(@NotNull List<PrismValue> valuesTuple, @NotNull OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ValueMetadataComputation computation = new ValueMetadataComputation(valuesTuple, this, result);
        return computation.execute();
    }

    public String getContextDescription() {
        return "metadata evaluation in " + dataMapping.getMappingContextDescription();
    }
}

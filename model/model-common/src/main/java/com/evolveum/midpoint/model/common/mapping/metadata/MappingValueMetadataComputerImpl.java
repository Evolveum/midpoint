/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata;

import java.util.List;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.ValueMetadata;
import com.evolveum.midpoint.repo.common.expression.ValueMetadataComputer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;

/**
 * Value metadata computer implementation that works in the context of a data mapping.
 * It obtains metadata mappings from the current mapping as well as from current object template.
 */
public class MappingValueMetadataComputerImpl implements ValueMetadataComputer {

    @NotNull final ValueMetadataProcessingSpec processingSpec;
    @NotNull final MappingImpl<?, ?> dataMapping;
    @NotNull final ModelCommonBeans beans;

    public MappingValueMetadataComputerImpl(@NotNull ValueMetadataProcessingSpec processingSpec, @NotNull MappingImpl<?, ?> dataMapping) {
        this.processingSpec = processingSpec;
        this.dataMapping = dataMapping;
        this.beans = dataMapping.getBeans();
        if (beans.metadataMappingEvaluator == null) {
            throw new IllegalStateException("Metadata mapping evaluation requested but"
                    + " metadataMappingEvaluator is not present; in " + dataMapping.getMappingContextDescription());
        }
    }

    @Override
    public ValueMetadata compute(@NotNull List<PrismValue> inputValues, @NotNull OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ValueMetadataComputation computation = ValueMetadataComputation.forMapping(inputValues, this, result);
        return computation.execute();
    }

    public String getContextDescription() {
        return "metadata evaluation in " + dataMapping.getMappingContextDescription();
    }
}

/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.common.mapping.metadata.builtin.BuiltinMetadataMapping;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingSpecificationType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 *
 */
public class TransformationalMetadataComputation extends ValueMetadataComputation {

    private static final Trace LOGGER = TraceManager.getTrace(TransformationalMetadataComputation.class);

    /**
     * Input for the computation. Currently a simple list of values,
     * - either different in case of expression evaluation (note: they can belong to the same or different items)
     * - or the same in case of value consolidation.
     */
    @NotNull private final List<PrismValue> inputValues;

    private TransformationalMetadataComputation(
            @NotNull List<PrismValue> inputValues,
            @NotNull ValueMetadataProcessingSpec processingSpec,
            @Nullable MappingSpecificationType mappingSpecification,
            @NotNull ModelCommonBeans beans,
            MappingEvaluationEnvironment env) {
        super(processingSpec, mappingSpecification, beans, env);
        this.inputValues = inputValues;
    }

    @Override
    void logStart() {
        LOGGER.trace("Starting metadata computation for input values: {}", inputValues);
    }

    @Override
    void applyBuiltinMapping(BuiltinMetadataMapping mapping) throws SchemaException {
        mapping.applyForTransformation(this);
    }

    public static TransformationalMetadataComputation forMapping(List<PrismValue> inputValues, ValueMetadataProcessingSpec processingSpec,
            MappingImpl<?, ?> mapping) {
        MappingEvaluationEnvironment env = new MappingEvaluationEnvironment(
                "metadata evaluation in " + mapping.getMappingContextDescription(),
                mapping.getNow(), mapping.getTask());
        return new TransformationalMetadataComputation(inputValues, processingSpec, mapping.getMappingSpecification(), mapping.getBeans(), env);
    }

    @Override
    Collection<?> getSourceValues(ItemPath sourcePath) {
        Collection<PrismValue> values = new HashSet<>();
        for (PrismValue dataValue : inputValues) {
            if (dataValue != null) {
                // TEMPORARY!!!
                for (PrismContainerValue<Containerable> metadataValue : dataValue.getValueMetadata().getValues()) {
                    Item<PrismValue, ItemDefinition> sourceItem = metadataValue.findItem(sourcePath);
                    if (sourceItem != null) {
                        values.addAll(CloneUtil.cloneCollectionMembers(sourceItem.getValues()));
                    }
                }
            }
        }
        return values;
    }

    public List<PrismValue> getInputValues() {
        return Collections.unmodifiableList(inputValues);
    }
}

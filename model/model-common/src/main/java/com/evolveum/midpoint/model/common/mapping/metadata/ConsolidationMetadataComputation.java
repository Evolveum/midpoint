/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.common.mapping.metadata.builtin.BuiltinMetadataMapping;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 *
 */
public class ConsolidationMetadataComputation extends ValueMetadataComputation {

    private static final Trace LOGGER = TraceManager.getTrace(ConsolidationMetadataComputation.class);

    @NotNull private final List<ValueMetadataType> nonNegativeValues;
    @NotNull private final List<ValueMetadataType> existingValues;

    private ConsolidationMetadataComputation(
            @NotNull List<ValueMetadataType> nonNegativeValues,
            @NotNull List<ValueMetadataType> existingValues,
            @NotNull ItemValueMetadataProcessingSpec processingSpec,
            @Nullable MappingSpecificationType mappingSpecification,
            @NotNull ModelCommonBeans beans,
            MappingEvaluationEnvironment env) throws SchemaException {
        super(processingSpec, mappingSpecification, beans, env);
        this.nonNegativeValues = nonNegativeValues;
        this.existingValues = existingValues;
    }

    @Override
    void logStart() {
        LOGGER.trace("Starting consolidation metadata computation for input:\nNon-negative values:\n{}\nExisting value(s):\n{}",
                DebugUtil.debugDumpLazily(nonNegativeValues, 1),
                DebugUtil.debugDumpLazily(existingValues, 1));
    }

    @Override
    void applyBuiltinMapping(BuiltinMetadataMapping mapping) throws SchemaException {
        mapping.applyForConsolidation(this);
    }

    public static ConsolidationMetadataComputation forConsolidation(@NotNull List<ValueMetadataType> nonNegativeValues,
            @NotNull List<ValueMetadataType> existingValues, ItemValueMetadataProcessingSpec processingSpec,
            ModelCommonBeans beans, MappingEvaluationEnvironment env) throws SchemaException {
        return new ConsolidationMetadataComputation(nonNegativeValues, existingValues, processingSpec, null, beans, env.createChild("metadata evaluation in"));
    }

    // TEMPORARY!!! (ignoring existing item for now)
    @Override
    Collection<?> getSourceValues(ItemPath sourcePath) {
        Collection<PrismValue> values = new HashSet<>();
        for (ValueMetadataType yield : nonNegativeValues) {
            Item<?, ?> sourceItem = yield.asPrismContainerValue().findItem(sourcePath);
            if (sourceItem != null) {
                values.addAll(CloneUtil.cloneCollectionMembers(sourceItem.getValues()));
            }
        }
        return values;
    }

    public List<ValueMetadataType> getNonNegativeValues() {
        return nonNegativeValues;
    }

    public List<ValueMetadataType> getExistingValues() {
        return existingValues;
    }
}

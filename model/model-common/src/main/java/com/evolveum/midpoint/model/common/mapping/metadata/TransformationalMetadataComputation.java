/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata;

import java.util.*;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.mapping.AbstractMappingImpl;
import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.common.mapping.metadata.builtin.BuiltinMetadataMapping;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

/**
 *
 */
public class TransformationalMetadataComputation extends ValueMetadataComputation {

    private static final Trace LOGGER = TraceManager.getTrace(TransformationalMetadataComputation.class);

    /**
     * Input for the computation. Currently a simple list of source values.
     */
    @NotNull private final List<PrismValue> inputValues;

    /**
     * Mapping in context of which this transformation is being executed.
     */
    @NotNull private final AbstractMappingImpl<?, ?, ?> mapping;

    private TransformationalMetadataComputation(
            @NotNull List<PrismValue> inputValues,
            @NotNull ItemValueMetadataProcessingSpec processingSpec,
            @NotNull AbstractMappingImpl<?, ?, ?> mapping,
            @NotNull ModelCommonBeans beans,
            MappingEvaluationEnvironment env) {
        super(processingSpec, mapping.getMappingSpecification(), beans, env);
        this.inputValues = inputValues;
        this.mapping = mapping;
    }

    @Override
    void logStart() {
        LOGGER.trace("Starting metadata computation for input values: {}", inputValues);
    }

    @Override
    void applyBuiltinMapping(BuiltinMetadataMapping mapping) throws SchemaException {
        mapping.applyForTransformation(this);
    }

    public static TransformationalMetadataComputation forMapping(List<PrismValue> inputValues, ItemValueMetadataProcessingSpec processingSpec,
            MappingImpl<?, ?> mapping) {
        MappingEvaluationEnvironment env = new MappingEvaluationEnvironment(
                "metadata evaluation in " + mapping.getMappingContextDescription(),
                mapping.getNow(), mapping.getTask());
        return new TransformationalMetadataComputation(inputValues, processingSpec, mapping, mapping.getBeans(), env);
    }

    @Override
    Collection<?> getSourceValues(ItemPath sourcePath) {
        Collection<PrismValue> values = new HashSet<>();
        for (PrismValue dataValue : inputValues) {
            if (dataValue != null) {
                // TEMPORARY!!!
                for (PrismContainerValue<Containerable> metadataValue : dataValue.getValueMetadata().getValues()) {
                    Item<?, ?> sourceItem = metadataValue.findItem(sourcePath);
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

    public List<QName> getSourceNames() {
        return mapping.getSourceNames();
    }

    @Override
    void createCustomMappingVariables(MetadataMappingBuilder<?, ?> builder, MetadataMappingType metadataMappingBean) {
        super.createCustomMappingVariables(builder, metadataMappingBean);
        builder.addVariableDefinition(ExpressionConstants.VAR_METADATA_COMPUTATION_INPUT,
                createMetadataComputationInput(builder, metadataMappingBean),
                MetadataComputationInput.class);
    }

    // Temporary implementation
    private MetadataComputationInput createMetadataComputationInput(MetadataMappingBuilder<?, ?> builder,
            MetadataMappingType metadataMappingBean) {
        MetadataComputationInput metadataComputationInput = new MetadataComputationInput();
        for (PrismValue inputDataValue : inputValues) {
            Map<String, Collection<?>> metadataSourceMap = new HashMap<>();
            if (inputDataValue != null) {
                for (VariableBindingDefinitionType sourceDef : metadataMappingBean.getSource()) {
                    ItemPath metadataSourcePath = getSourcePath(sourceDef);
                    QName metadataSourceName = getSourceName(sourceDef, metadataSourcePath);
                    List<PrismValue> metadataSourceValues = new ArrayList<>();
                    for (PrismContainerValue<Containerable> metadataValue : inputDataValue.getValueMetadata().getValues()) {
                        Item<?, ?> sourceItem = metadataValue.findItem(metadataSourcePath);
                        if (sourceItem != null) {
                            //noinspection unchecked
                            metadataSourceValues.addAll((Collection<? extends PrismValue>) sourceItem.clone().getRealValues());
                        }
                    }
                    metadataSourceMap.put(metadataSourceName.getLocalPart(), metadataSourceValues);
                }
                metadataComputationInput.add(inputDataValue, metadataSourceMap);
            }
        }
        return metadataComputationInput;
    }
}

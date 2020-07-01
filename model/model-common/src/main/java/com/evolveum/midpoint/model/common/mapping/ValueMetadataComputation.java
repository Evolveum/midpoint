/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.model.common.mapping.builtin.BuiltinMetadataMapping;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.metadata.MidpointValueMetadataFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.Objects;

/**
 * Computation of value metadata for given combination of input values.
 *
 * Preliminary implementation.
 * For example, it does no real consolidation: it simply adds all values into respective items.
 */
class ValueMetadataComputation {

    @NotNull private final List<PrismValue> valuesTuple;
    @NotNull private final MappingValueMetadataComputerImpl computer;
    @NotNull private final OperationResult result;
    @NotNull private final PrismContext prismContext;
    @NotNull private final PrismContainerValue<ValueMetadataType> outputMetadata;
    @NotNull private final PrismContainerDefinition<ValueMetadataType> metadataDefinition;

    ValueMetadataComputation(@NotNull List<PrismValue> valuesTuple, MappingValueMetadataComputerImpl computer, @NotNull OperationResult result) {
        this.valuesTuple = valuesTuple;
        this.computer = computer;
        this.result = result;
        this.prismContext = computer.metadataMappingEvaluator.prismContext;
        //noinspection unchecked
        this.outputMetadata = new ValueMetadataType(prismContext).asPrismContainerValue();
        this.metadataDefinition = Objects.requireNonNull(
                prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(ValueMetadataType.class),
                "No definition of value metadata container");
    }

    public ValueMetadata execute() throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        processCustomMappings();
        processBuiltinMappings();
        return MidpointValueMetadataFactory.createFrom(outputMetadata);
    }

    private void processCustomMappings()
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        for (MetadataMappingType mappingBean : computer.processingSpec.getMappings()) {
            MetadataMappingImpl<?, ?> mapping = createMapping(mappingBean);
            mapping.evaluate(computer.dataMapping.getTask(), result);
            appendValues(mapping.getOutputPath(), mapping.getOutputTriple());
        }
    }

    private void appendValues(ItemPath outputPath, PrismValueDeltaSetTriple<?> outputTriple) throws SchemaException {
        ItemDelta<?, ?> itemDelta = prismContext.deltaFor(ValueMetadataType.class)
                .item(outputPath)
                .add(outputTriple.getNonNegativeValues())
                .asItemDelta();
        itemDelta.applyTo(outputMetadata);
    }

    private MetadataMappingImpl<?, ?> createMapping(MetadataMappingType mappingBean) throws SchemaException {
        MetadataMappingBuilder<?, ?> builder = computer.metadataMappingEvaluator.mappingFactory.createMappingBuilder(mappingBean,
                "metadata mapping in " + computer.dataMapping.getMappingContextDescription());
        createSources(builder, mappingBean);
        builder.targetContext(metadataDefinition)
                .now(computer.dataMapping.now)
                .conditionMaskOld(false); // We are not interested in old values (deltas are irrelevant in metadata mappings).
        return builder.build();
    }

    // TODO unify with parsing data mapping sources (MappingParser class)
    private void createSources(MetadataMappingBuilder<?, ?> builder, MetadataMappingType mappingBean) throws SchemaException {
        for (VariableBindingDefinitionType sourceDef : mappingBean.getSource()) {
            ItemPath sourcePath = getSourcePath(sourceDef);
            QName sourceName = getSourceName(sourceDef, sourcePath);
            ItemDefinition sourceDefinition = getAdaptedSourceDefinition(sourcePath);
            Item sourceItem = sourceDefinition.instantiate();
            //noinspection unchecked
            sourceItem.addAll(getSourceValues(sourcePath));
            //noinspection unchecked
            Source<?, ?> source = new Source<>(sourceItem, null, null, sourceName, sourceDefinition);
            source.recompute();
            builder.additionalSource(source);
        }
    }

    @NotNull
    private MutableItemDefinition getAdaptedSourceDefinition(ItemPath sourcePath) {
        ItemDefinition sourceDefinition =
                Objects.requireNonNull(metadataDefinition.findItemDefinition(sourcePath),
                        () -> "No definition for '" + sourcePath + "' in " + getContextDescription());
        MutableItemDefinition sourceDefinitionMultivalued =
                sourceDefinition.clone().toMutable();
        sourceDefinitionMultivalued.setMaxOccurs(-1);
        return sourceDefinitionMultivalued;
    }

    private Collection<?> getSourceValues(ItemPath sourcePath) {
        Collection<PrismValue> values = new HashSet<>();
        for (PrismValue dataValue : valuesTuple) {
            if (dataValue != null) {
                Item<PrismValue, ItemDefinition> sourceItem = dataValue.getValueMetadata().findItem(sourcePath);
                if (sourceItem != null) {
                    values.addAll(CloneUtil.cloneCollectionMembers(sourceItem.getValues()));
                }
            }
        }
        return values;
    }

    private QName getSourceName(VariableBindingDefinitionType sourceDef, ItemPath sourcePath) {
        return sourceDef.getName() != null ? sourceDef.getName() : ItemPath.toName(sourcePath.last());
    }

    private ItemPath getSourcePath(VariableBindingDefinitionType sourceDef) {
        return Objects.requireNonNull(sourceDef.getPath(), () -> "No source path in " + getContextDescription())
                .getItemPath();
    }

    private String getContextDescription() {
        return computer.getContextDescription(); // TODO indication of value tuple being processed
    }

    private void processBuiltinMappings() throws SchemaException {
        for (BuiltinMetadataMapping mapping : computer.metadataMappingEvaluator.getBuiltinMappings()) {
            if (isApplicable(mapping)) {
                mapping.apply(valuesTuple, outputMetadata, computer.dataMapping, result);
            }
        }
    }

    private boolean isApplicable(BuiltinMetadataMapping mapping) throws SchemaException {
        return computer.processingSpec.isFullProcessing(mapping.getTargetPath());
    }
}

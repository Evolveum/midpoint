/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.mapping.metadata.builtin.BuiltinMetadataMapping;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.metadata.MidpointValueMetadataFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.Objects;

/**
 * Computation of value metadata.
 *
 * It is used currently in two contexts:
 * 1. During expression evaluation where zero, one, or more input values are combined to form zero, one, or multiple output vales.
 * 2. During consolidation where a set of the same values (possibly with different metadata) are combined into single value
 *    with given metadata (that have to be derived from the constituents).
 *
 * Preliminary implementation. For example,
 * - it does no real consolidation: it simply adds all values into respective items;
 * - it works with simplified computation model: its input is simply a list of input values (regardless of their
 *   parent item).
 */
class ValueMetadataComputation {

    /**
     * Input for the computation. Currently a simple list of values,
     * - either different in case of expression evaluation (note: they can belong to the same or different items)
     * - or the same in case of value consolidation.
     */
    @NotNull private final List<PrismValue> inputValues;

    /**
     * Metadata processing specification: how should we compute the resulting metadata?
     */
    @NotNull private final ValueMetadataProcessingSpec processingSpec;

    /**
     * Instant of the evaluation.
     */
    private final XMLGregorianCalendar now;

    /**
     * Current context description.
     */
    private final String contextDescription;

    /**
     * Task under which the computation executes.
     */
    private final Task task;

    /**
     * The operation result.
     */
    @NotNull private final OperationResult result;

    /**
     * Necessary beans.
     */
    @NotNull private final ModelCommonBeans beans;

    /**
     * Definition of ValueMetadataType container.
     */
    @NotNull private final PrismContainerDefinition<ValueMetadataType> metadataDefinition;

    /**
     * Result of the computation: the metadata.
     */
    @NotNull private final PrismContainerValue<ValueMetadataType> outputMetadata;

    private ValueMetadataComputation(@NotNull List<PrismValue> inputValues, @NotNull ValueMetadataProcessingSpec processingSpec,
            @NotNull ModelCommonBeans beans, XMLGregorianCalendar now, String contextDescription, Task task,
            @NotNull OperationResult result) {
        this.inputValues = inputValues;
        this.processingSpec = processingSpec;
        this.beans = beans;
        this.now = now;
        this.contextDescription = contextDescription;
        this.task = task;
        this.result = result;
        this.metadataDefinition = Objects.requireNonNull(
                beans.prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(ValueMetadataType.class),
                "No definition of value metadata container");
        //noinspection unchecked
        this.outputMetadata = new ValueMetadataType(beans.prismContext).asPrismContainerValue();
    }

    static ValueMetadataComputation forMapping(List<PrismValue> inputValues, MappingValueMetadataComputerImpl computer,
            OperationResult result) {
        return new ValueMetadataComputation(inputValues, computer.processingSpec, computer.beans, computer.dataMapping.getNow(),
                computer.getContextDescription(), computer.dataMapping.getTask(), result);
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
        for (MetadataMappingType mappingBean : processingSpec.getMappings()) {
            MetadataMappingImpl<?, ?> mapping = createMapping(mappingBean);
            mapping.evaluate(task, result);
            appendValues(mapping.getOutputPath(), mapping.getOutputTriple());
        }
    }

    private void appendValues(ItemPath outputPath, PrismValueDeltaSetTriple<?> outputTriple) throws SchemaException {
        ItemDelta<?, ?> itemDelta = beans.prismContext.deltaFor(ValueMetadataType.class)
                .item(outputPath)
                .add(outputTriple.getNonNegativeValues())
                .asItemDelta();
        itemDelta.applyTo(outputMetadata);
    }

    private MetadataMappingImpl<?, ?> createMapping(MetadataMappingType mappingBean) throws SchemaException {
        MetadataMappingBuilder<?, ?> builder = beans.metadataMappingEvaluator.mappingFactory
                .createMappingBuilder(mappingBean, contextDescription);
        createSources(builder, mappingBean);
        builder.targetContext(metadataDefinition)
                .now(now)
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
                        () -> "No definition for '" + sourcePath + "' in " + contextDescription);
        MutableItemDefinition sourceDefinitionMultivalued =
                sourceDefinition.clone().toMutable();
        sourceDefinitionMultivalued.setMaxOccurs(-1);
        return sourceDefinitionMultivalued;
    }

    private Collection<?> getSourceValues(ItemPath sourcePath) {
        Collection<PrismValue> values = new HashSet<>();
        for (PrismValue dataValue : inputValues) {
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
        return Objects.requireNonNull(sourceDef.getPath(), () -> "No source path in " + contextDescription)
                .getItemPath();
    }

    private void processBuiltinMappings() throws SchemaException {
        for (BuiltinMetadataMapping mapping : beans.metadataMappingEvaluator.getBuiltinMappings()) {
            if (isApplicable(mapping)) {
                mapping.apply(inputValues, outputMetadata, contextDescription, now, task, result);
            }
        }
    }

    private boolean isApplicable(BuiltinMetadataMapping mapping) throws SchemaException {
        return processingSpec.isFullProcessing(mapping.getTargetPath());
    }
}

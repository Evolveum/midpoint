/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.metadata.MidpointValueMetadataFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Computation of value metadata for given combination of input values.
 *
 * Preliminary implementation.
 * For example, it does no real consolidation: it simply adds all values into respective items.
 */
class ValueMetadataComputation {

    @NotNull private final List<PrismValue> valuesTuple;
    @NotNull private final SimpleValueMetadataComputer computer;
    @NotNull private final OperationResult result;
    @NotNull private final PrismContext prismContext;
    @NotNull private final PrismContainerValue<ValueMetadataType> outputMetadata;
    @NotNull private final PrismContainerDefinition<ValueMetadataType> metadataDefinition;

    ValueMetadataComputation(@NotNull List<PrismValue> valuesTuple, SimpleValueMetadataComputer computer, @NotNull OperationResult result) {
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
        List<MetadataMappingType> mappingBeans = computer.dataMapping.getMetadataHandling().getMapping();
        for (MetadataMappingType mappingBean : mappingBeans) {
            processMapping(mappingBean);
        }
        return MidpointValueMetadataFactory.createFrom(outputMetadata);
    }

    private void processMapping(MetadataMappingType mappingBean) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        MetadataMappingImpl<?, ?> mapping = createMetadataMapping(mappingBean);
        mapping.evaluate(computer.dataMapping.getTask(), result);
        appendValues(mapping.getOutputPath(), mapping.getOutputTriple());
    }

    private void appendValues(ItemPath outputPath, PrismValueDeltaSetTriple<?> outputTriple) throws SchemaException {
        ItemDelta<?, ?> itemDelta = prismContext.deltaFor(ValueMetadataType.class)
                .item(outputPath)
                .add(outputTriple.getNonNegativeValues())
                .asItemDelta();
        itemDelta.applyTo(outputMetadata);
    }

    private MetadataMappingImpl<?, ?> createMetadataMapping(MetadataMappingType mappingBean) throws SchemaException {
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

    private Collection<?> getSourceValues(ItemPath sourcePath) throws SchemaException {
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
}

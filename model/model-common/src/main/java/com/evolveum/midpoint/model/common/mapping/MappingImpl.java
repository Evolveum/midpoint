/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.common.mapping.metadata.TransformationalMetadataComputation;
import com.evolveum.midpoint.model.common.mapping.metadata.ItemValueMetadataProcessingSpec;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.TransformationValueMetadataComputer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataMappingScopeType.TRANSFORMATION;

/**
 * (Traditional) data mapping.
 */
public class MappingImpl<V extends PrismValue, D extends ItemDefinition<?>> extends AbstractMappingImpl<V, D, MappingType> {

    MappingImpl(MappingBuilder<V, D> builder) {
        super(builder);
    }

    private MappingImpl(MappingImpl<V, D> prototype) {
        super(prototype);
    }

    protected TransformationValueMetadataComputer createValueMetadataComputer(OperationResult result) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        ItemValueMetadataProcessingSpec processingSpec = createProcessingSpec(result);
        LOGGER.trace("Value metadata processing spec:\n{}", processingSpec.debugDumpLazily(1));
        if (processingSpec.isEmpty()) {
            return null;
        } else {
            return new TransformationValueMetadataComputer() {
                @Override
                public @NotNull ValueMetadataType compute(@NotNull List<PrismValue> inputValues,
                        @NotNull OperationResult computationOpResult)
                        throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
                        ConfigurationException, ExpressionEvaluationException {
                    return TransformationalMetadataComputation
                            .forMapping(inputValues, processingSpec, MappingImpl.this)
                            .execute(computationOpResult);
                }

                @Override
                public boolean supportsProvenance() throws SchemaException, ConfigurationException {
                    return processingSpec.isFullProcessing(ValueMetadataType.F_PROVENANCE);
                }

                @Override
                public String toString() {
                    return "Computer for " + getContextDescription();
                }
            };
        }
    }

    @NotNull
    private ItemValueMetadataProcessingSpec createProcessingSpec(OperationResult result) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        var processingSpec = ItemValueMetadataProcessingSpec.forScope(TRANSFORMATION, canUseDefaultsForMetadataProcessing());
        if (ignoreValueMetadata) {
            return processingSpec; // empty
        }
        processingSpec.addPathsToIgnore(mappingBean.getIgnoreMetadataProcessing());
        // TODO What about persona mappings? outbound mappings? We should not use object template for that.
        ItemPath outputPath = parser.getOutputPath();
        D outputDefinition = parser.getOutputDefinition();
        if (outputPath != null) { // can it ever be null?
            processingSpec.populateFromCurrentFocusTemplate(
                    outputPath, outputDefinition, ModelCommonBeans.get().objectResolver,
                    getMappingContextDescription(), task, result);
        }
        processingSpec.addMetadataMappings(
                mappingBean.getMetadataMapping(),
                // [EP:M:MM] DONE (mappingCI is assumed to be OK)
                mappingConfigItem.originProviderFor(MappingType.F_METADATA_MAPPING));
        return processingSpec;
    }

    private boolean canUseDefaultsForMetadataProcessing() {
        return true;
    }

    @Override
    public MappingImpl<V, D> clone() {
        return new MappingImpl<>(this);
    }

    @Override
    protected boolean determinePushChangesRequested() {
        var lensContext = ModelExpressionThreadLocalHolder.getLensContext();
        ModelExecuteOptions options = lensContext != null ? lensContext.getOptions() : null;
        return ModelExecuteOptions.isPushChanges(options);
    }
}

/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.common.mapping.metadata.ConsolidationMetadataComputation;
import com.evolveum.midpoint.model.common.mapping.metadata.ItemValueMetadataProcessingSpec;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ConsolidationValueMetadataComputer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataMappingScopeType.CONSOLIDATION;

/**
 * Metadata-related utility methods.
 */
@Experimental
public class LensMetadataUtil {

    private static final Trace LOGGER = TraceManager.getTrace(LensMetadataUtil.class);

    public static ConsolidationValueMetadataComputer createValueMetadataConsolidationComputer(
            @NotNull ItemPath itemPath,
            LensContext<?> lensContext,
            ModelBeans beans,
            MappingEvaluationEnvironment env,
            OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ItemValueMetadataProcessingSpec processingSpec = createProcessingSpec(itemPath, lensContext, beans, env, result);
        LOGGER.trace("Value metadata processing spec:\n{}", processingSpec.debugDumpLazily(1));
        if (processingSpec.isEmpty()) {
            return null;
        } else {
            return ConsolidationValueMetadataComputer.named(() -> "Computer for consolidation of " + itemPath + " in " + env.contextDescription,
                    (nonNegativeValues, existingValues, computationOpResult) ->
                            ConsolidationMetadataComputation
                                    .forConsolidation(nonNegativeValues, existingValues, processingSpec, beans.commonBeans, env)
                                    .execute(computationOpResult));
        }
    }

    @NotNull
    private static ItemValueMetadataProcessingSpec createProcessingSpec(
            @NotNull ItemPath itemPath, LensContext<?> lensContext, ModelBeans beans,
            MappingEvaluationEnvironment env, OperationResult result) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        ItemValueMetadataProcessingSpec processingSpec = ItemValueMetadataProcessingSpec.forScope(CONSOLIDATION);
        // TODO What about persona mappings? outbound mappings? We should not use object template for that.
        processingSpec.populateFromCurrentFocusTemplate(lensContext, itemPath, beans.modelObjectResolver,
                env.contextDescription, env.task, result);
        return processingSpec;
    }

}

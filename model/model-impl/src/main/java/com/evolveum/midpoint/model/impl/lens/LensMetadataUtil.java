/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.common.mapping.metadata.ConsolidationMetadataComputation;
import com.evolveum.midpoint.model.common.mapping.metadata.ValueMetadataProcessingSpec;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ConsolidationValueMetadataComputer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataMappingScopeType.CONSOLIDATION;

/**
 * Metadata-related utility methods.
 */
@Experimental
public class LensMetadataUtil {

    public static ConsolidationValueMetadataComputer createValueMetadataConsolidationComputer(ItemPath itemPath, LensContext<?> lensContext, ModelBeans beans,
            MappingEvaluationEnvironment env, OperationResult result) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        ValueMetadataProcessingSpec processingSpec = createProcessingSpec(itemPath, lensContext, beans, env, result);
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

    private static ValueMetadataProcessingSpec createProcessingSpec(ItemPath itemPath, LensContext<?> lensContext, ModelBeans beans,
            MappingEvaluationEnvironment env, OperationResult result) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        ValueMetadataProcessingSpec processingSpec = ValueMetadataProcessingSpec.forScope(CONSOLIDATION);
        // TODO What about persona mappings? outbound mappings? We should not use object template for that.
        processingSpec.populateFromCurrentFocusTemplate(lensContext, itemPath, beans.modelObjectResolver,
                env.contextDescription, env.task, result);
        return processingSpec;
    }

}

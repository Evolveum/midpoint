/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.operations.*;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

/**
 *
 */
@Experimental
public class OpNodeFactory {

    public static OpNode createOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent, TraceInfo traceInfo) {
        if (info.getType() != null) {
            switch (info.getType()) {
                case CLOCKWORK_RUN:
                    return new ClockworkRunOpNode(prismContext, result, info, parent, traceInfo);
                case CLOCKWORK_CLICK:
                    return new ClockworkClickOpNode(prismContext, result, info, parent, traceInfo);
                case RESOURCE_OBJECT_CONSTRUCTION_EVALUATION:
                    return new ResourceObjectConstructionEvaluationOpNode(prismContext, result, info, parent, traceInfo);
                case MAPPING_EVALUATION:
                    return new MappingEvaluationOpNode(prismContext, result, info, parent, traceInfo);
                case MAPPING_TIME_VALIDITY_EVALUATION:
                    return new MappingTimeValidityEvaluationOpNode(prismContext, result, info, parent, traceInfo);

                case FOCUS_CHANGE_EXECUTION:
                    return new FocusChangeExecutionOpNode(prismContext, result, info, parent, traceInfo);
                case PROJECTION_CHANGE_EXECUTION:
                    return new ProjectionChangeExecutionOpNode(prismContext, result, info, parent, traceInfo);
                case CHANGE_EXECUTION_DELTA:
                    return new ChangeExecutionDeltaOpNode(prismContext, result, info, parent, traceInfo);
                case UPDATE_SHADOW_SITUATION:
                    return new UpdateShadowSituationOpNode(prismContext, result, info, parent, traceInfo);
                case LINK_UNLINK_SHADOW:
                    return new LinkUnlinkShadowOpNode(prismContext, result, info, parent, traceInfo);

                case TRANSFORMATION_EXPRESSION_EVALUATION:
                    return new TransformationExpressionEvaluationOpNode(prismContext, result, info, parent, traceInfo);
                case VALUE_TUPLE_TRANSFORMATION:
                    return new ValueTupleTransformationOpNode(prismContext, result, info, parent, traceInfo);
                case ITEM_CONSOLIDATION:
                    return new ItemConsolidationOpNode(prismContext, result, info, parent, traceInfo);
                case PROJECTOR_PROJECTION:
                    return new ProjectorProjectionOpNode(prismContext, result, info, parent, traceInfo);
                case PROJECTION_ACTIVATION:
                    return new ProjectionActivationOpNode(prismContext, result, info, parent, traceInfo);
                case PROJECTOR_FOCUS_POLICY_RULES:
                    return new FocusPolicyRulesOpNode(prismContext, result, info, parent, traceInfo);
                case PROJECTOR_INBOUND:
                case PROJECTOR_ASSIGNMENTS:
                case PROJECTOR_TEMPLATE_BEFORE_ASSIGNMENTS:
                case PROJECTOR_TEMPLATE_AFTER_ASSIGNMENTS:
                case PROJECTOR_COMPONENT_OTHER:
                    return new ProjectorComponentOpNode(prismContext, result, info, parent, traceInfo);
                case ASSIGNMENT_EVALUATION:
                    return new AssignmentEvaluationOpNode(prismContext, result, info, parent, traceInfo);
                case ASSIGNMENT_SEGMENT_EVALUATION:
                    return new AssignmentSegmentEvaluationOpNode(prismContext, result, info, parent, traceInfo);
                case POLICY_RULE_EVALUATION:
                    return new PolicyRuleEvaluationOpNode(prismContext, result, info, parent, traceInfo);
                case POLICY_CONSTRAINT_EVALUATION:
                    return new PolicyConstraintEvaluationOpNode(prismContext, result, info, parent, traceInfo);
                case FOCUS_REPOSITORY_LOAD:
                    return new FocusRepositoryLoadOpNode(prismContext, result, info, parent, traceInfo);
                case FULL_PROJECTION_LOAD:
                    return new FullProjectionLoadOpNode(prismContext, result, info, parent, traceInfo);
                case REPOSITORY_CACHE:
                    return new RepositoryCacheOpNode(prismContext, result, info, parent, traceInfo);
            }
        }
        return new OpNode(prismContext, result, info, parent, traceInfo);
    }
}

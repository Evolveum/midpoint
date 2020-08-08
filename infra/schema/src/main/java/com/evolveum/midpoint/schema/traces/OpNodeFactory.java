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
                case MAPPING_EVALUATION:
                    return new MappingEvaluationOpNode(prismContext, result, info, parent, traceInfo);
                case FOCUS_CHANGE_EXECUTION:
                    return new FocusChangeExecutionOpNode(prismContext, result, info, parent, traceInfo);
                case TRANSFORMATION_EXPRESSION_EVALUATION:
                    return new TransformationExpressionEvaluationOpNode(prismContext, result, info, parent, traceInfo);
                case VALUE_TUPLE_TRANSFORMATION:
                    return new ValueTupleTransformationOpNode(prismContext, result, info, parent, traceInfo);
                case ITEM_CONSOLIDATION:
                    return new ItemConsolidationOpNode(prismContext, result, info, parent, traceInfo);
            }
        }
        return new OpNode(prismContext, result, info, parent, traceInfo);
    }
}

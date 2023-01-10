/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTreeDeltasType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectionObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

public class VisualizationUtil {

    public static Visualization visualizeObjectTreeDeltas(ObjectTreeDeltasType deltas, String displayNameKey,
            PrismContext prismContext, ModelInteractionService modelInteractionService,
            ObjectReferenceType objectRef, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
        List<Visualization> visualizations = new ArrayList<>();
        if (deltas != null) {
            if (deltas.getFocusPrimaryDelta() != null) {
                ObjectDelta<? extends ObjectType> delta = DeltaConvertor.createObjectDelta(deltas.getFocusPrimaryDelta(), prismContext);
                visualizations.add(modelInteractionService.visualizeDelta(delta, false, objectRef, task, result));
            }
            for (ProjectionObjectDeltaType projectionObjectDelta : deltas.getProjectionPrimaryDelta()) {
                ObjectDelta<? extends ObjectType> delta = DeltaConvertor.createObjectDelta(projectionObjectDelta.getPrimaryDelta(), prismContext);
                visualizations.add(modelInteractionService.visualizeDelta(delta, task, result));
            }
        }
        return new WrapperVisualization(visualizations, displayNameKey);
    }

    public static Visualization visualizeObjectDeltaType(ObjectDeltaType objectDeltaType, String displayNameKey,
            PrismContext prismContext, ModelInteractionService modelInteractionService,
            ObjectReferenceType objectRef, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
        List<Visualization> visualizations = new ArrayList<>();
        if (objectDeltaType != null) {
            ObjectDelta<? extends ObjectType> delta = DeltaConvertor.createObjectDelta(objectDeltaType, prismContext);
            visualizations.add(modelInteractionService.visualizeDelta(delta, false, objectRef, task, result));
        }
        return new WrapperVisualization(visualizations, displayNameKey);
    }
}

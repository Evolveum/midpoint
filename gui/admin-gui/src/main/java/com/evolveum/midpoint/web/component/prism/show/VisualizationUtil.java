/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.gui.api.factory.wrapper.PrismContainerWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ValueMetadataWrapperImpl;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.ValueMetadata;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTreeDeltasType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectionObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import java.util.ArrayList;
import java.util.List;

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
        return new WrapperVisualization(new SingleLocalizableMessage(displayNameKey), visualizations);
    }

    public static Visualization visualizeObjectDeltaType(ObjectDeltaType objectDeltaType, String displayNameKey,
                                                         PrismContext prismContext, ModelInteractionService modelInteractionService,
                                                         ObjectReferenceType objectRef, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
        List<Visualization> visualizations = new ArrayList<>();
        if (objectDeltaType != null) {
            ObjectDelta<? extends ObjectType> delta = DeltaConvertor.createObjectDelta(objectDeltaType, prismContext);
            visualizations.add(modelInteractionService.visualizeDelta(delta, false, objectRef, task, result));
        }
        return new WrapperVisualization(new SingleLocalizableMessage(displayNameKey), visualizations);
    }

    public static String createChangeTypeCssClassForOutlineCard(ChangeType change) {
        if (change == null) {
            return "card-outline-left-secondary";
        }

        switch (change) {
            case ADD:
                return "card-outline-left-success";
            case MODIFY:
                return "card-outline-left-info";
            case DELETE:
                return "card-outline-left-danger";
        }

        return "card-outline-left-secondary";
    }

    public static ValueMetadataWrapperImpl createValueMetadataWrapper(PrismValue value, PageBase page) {
        if (value == null) {
            return null;
        }

        ValueMetadata valueMetadata = value.getValueMetadata();
        if (valueMetadata == null || valueMetadata.isEmpty()) {
            return null;
        }

        try {
            Task task = page.createSimpleTask("load wrapper");

            PrismContainerWrapperFactory<?> factory = page.findContainerWrapperFactory(valueMetadata.getDefinition());

            WrapperContext ctx = new WrapperContext(task, task.getResult());
            ctx.setMetadata(true);
            ctx.setReadOnly(true);
            ctx.setCreateOperational(true);
            PrismContainerWrapper cw = factory.createWrapper(null, valueMetadata, ItemStatus.NOT_CHANGED, ctx);

            return new ValueMetadataWrapperImpl(cw);
        } catch (Exception ex) {
            // todo handle
            ex.printStackTrace();
        }

        return null;
    }
}

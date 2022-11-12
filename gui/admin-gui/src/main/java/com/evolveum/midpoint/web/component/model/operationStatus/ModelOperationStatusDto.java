/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.model.operationStatus;

import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.WrapperScene;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ModelOperationStatusDto implements Serializable {

    public static final String F_STATE = "state";
    public static final String F_FOCUS_TYPE = "focusType";
    public static final String F_FOCUS_NAME = "focusName";
    public static final String F_PRIMARY_DELTA = "primaryDelta";

    private final ModelState state;

    private String focusType;
    private String focusName;
    private SceneDto primarySceneDto;

    public ModelOperationStatusDto(ModelContext<?> modelContext, ModelInteractionService modelInteractionService, Task opTask, OperationResult result) {

        state = modelContext.getState();

        if (modelContext.getFocusContext() != null) {

            // focusType & focusName
            PrismObject object = modelContext.getFocusContext().getObjectNew();
            if (object == null) {
                object = modelContext.getFocusContext().getObjectOld();
            }
            if (object != null) {
                focusType = object.getElementName() != null ? object.getElementName().toString() : null;
                focusName = object.asObjectable().getName() != null ? object.asObjectable().getName().getOrig() : null;
            }

            // primaryDelta
            final List<ObjectDelta<? extends ObjectType>> primaryDeltas = new ArrayList<>();
//            final List<ObjectDelta<? extends ObjectType>> secondaryDeltas = new ArrayList<>();
            final List<? extends Visualization> primaryScenes;
//            final List<? extends Scene> secondaryScenes;
            try {
                addIgnoreNull(primaryDeltas, modelContext.getFocusContext().getPrimaryDelta());
//                addIgnoreNull(secondaryDeltas, modelContext.getFocusContext().getSecondaryDelta());
                for (ModelProjectionContext projCtx : modelContext.getProjectionContexts()) {
                    addIgnoreNull(primaryDeltas, projCtx.getPrimaryDelta());
//                    addIgnoreNull(secondaryDeltas, projCtx.getExecutableDelta());
                }
                primaryScenes = modelInteractionService.visualizeDeltas(primaryDeltas, opTask, result);
//                secondaryScenes = modelInteractionService.visualizeDeltas(secondaryDeltas, opTask, result);
            } catch (SchemaException | ExpressionEvaluationException e) {
                throw new SystemException(e);        // TODO
            }
            final WrapperScene primaryWrapperScene = new WrapperScene(primaryScenes, primaryDeltas.size() != 1 ? "PagePreviewChanges.primaryChangesMore" : "PagePreviewChanges.primaryChangesOne", primaryDeltas.size());
            primarySceneDto = new SceneDto(primaryWrapperScene);
        }
    }

    public ModelState getState() {
        return state;
    }

    public String getFocusType() {
        return focusType;
    }

    public String getFocusName() {
        return focusName;
    }

    public SceneDto getPrimaryDelta() {
        return primarySceneDto;
    }
}

/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
public class AssignmentDescriptionHandler implements VisualizationDescriptionHandler {

    @Autowired
    private Resolver resolver;

    @Override
    public boolean match(VisualizationImpl visualization, VisualizationImpl parentVisualization) {
        PrismContainerValue<?> value = visualization.getSourceValue();
        if (value == null) {
            return false;
        }

        return AssignmentHolderType.F_ASSIGNMENT.equivalent(value.getPath().namedSegmentsOnly());
    }

    @Override
    public void apply(VisualizationImpl visualization, VisualizationImpl parentVisualization, Task task, OperationResult result) {
        PrismContainerValue<?> value = visualization.getSourceValue();

        AssignmentType a = (AssignmentType) value.asContainerable();
        if (a.getConstruction() != null) {
            handleConstruction(visualization, a.getConstruction(), task, result);
        } else if (a.getTargetRef() != null) {
            handleTargetRef(visualization, a.getTargetRef(), task, result);
        }
    }

    private void handleConstruction(VisualizationImpl visualization, ConstructionType construction, Task task, OperationResult result) {
        ChangeType change = visualization.getChangeType();

        ShadowKindType kind = construction.getKind() != null ? construction.getKind() : ShadowKindType.UNKNOWN;
        String intent = construction.getIntent() != null ? "(" + construction.getIntent() + ")" : "";

        ObjectReferenceType resourceRef = construction.getResourceRef();
        Object resourceName = null;
        if (resourceRef != null) {
            resourceName = resolver.resolveReferenceName(resourceRef, false, task, result);
        }

        if (resourceName == null) {
            resourceName = new SingleLocalizableMessage("ShadowDescriptionHandler.unknownResource",
                    new Object[] { resourceRef != null ? resourceRef.getOid() : null });
        }

        visualization.getName().setOverview(
                new SingleLocalizableMessage("AssignmentDescriptionHandler.assignment.construction", new Object[] {
                        new SingleLocalizableMessage("ShadowKindType." + kind),
                        intent,
                        resourceName,
                        createAssignedMessage(change)
                })
        );
    }

    private LocalizableMessage createAssignedMessage(ChangeType change) {
        if (change == null) {
            return new SingleLocalizableMessage("");
        }

        String key = null;
        switch (change){
            case ADD -> key = "AssignmentDescriptionHandler.assigned";
            case MODIFY -> key = "AssignmentDescriptionHandler.modified";
            case DELETE -> key = "AssignmentDescriptionHandler.unassigned";
        }
        return new SingleLocalizableMessage(key);
    }

    private void handleTargetRef(VisualizationImpl visualization, ObjectReferenceType targetRef, Task task, OperationResult result) {
        QName type = targetRef.getType() != null ? targetRef.getType() : ObjectType.COMPLEX_TYPE;
        ObjectTypes ot = ObjectTypes.getObjectTypeFromTypeQName(type);

        String targetName = resolver.resolveReferenceDisplayName(targetRef, true, task, result);

        ChangeType change = visualization.getChangeType();

        visualization.getName().setOverview(
                new SingleLocalizableMessage("AssignmentDescriptionHandler.assignment", new Object[] {
                        new SingleLocalizableMessage("ObjectTypes." + ot.name()),
                        targetName,
                        createAssignedMessage(change)
                }, (String) null));
    }
}

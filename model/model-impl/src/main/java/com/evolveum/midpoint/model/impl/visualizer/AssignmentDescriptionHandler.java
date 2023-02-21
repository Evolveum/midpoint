/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import static com.evolveum.midpoint.prism.delta.ChangeType.ADD;

import javax.xml.namespace.QName;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
public class AssignmentDescriptionHandler implements VisualizationDescriptionHandler {

    @Override
    public boolean match(VisualizationImpl visualization) {
        PrismContainerValue value = visualization.getSourceValue();
        if (value == null || value.getPath() == null) {
            return false;
        }

        return AssignmentHolderType.F_ASSIGNMENT.equivalent(value.getPath().namedSegmentsOnly());
    }

    @Override
    public void apply(VisualizationImpl visualization, Task task, OperationResult result) {
        PrismContainerValue value = visualization.getSourceValue();
        ChangeType changeType = visualization.getChangeType();

        AssignmentType a = (AssignmentType) value.asContainerable();
        ObjectReferenceType targetRef = a.getTargetRef();
        if (targetRef == null) {
            return;
        }

        QName type = targetRef.getType() != null ? targetRef.getType() : ObjectType.COMPLEX_TYPE;
        ObjectTypes ot = ObjectTypes.getObjectTypeFromTypeQName(type);

        visualization.getName().setSimpleDescription(
                new SingleLocalizableMessage("AssignmentDescriptionHandler.assignment", new Object[] {
                        new SingleLocalizableMessage("ObjectTypes." + ot.name()),
                        targetRef.getTargetName() != null ? targetRef.getTargetName() : targetRef.getOid(),
                        changeType == ADD ? "assigned" : "unassigned"
                }, (String) null));
    }
}

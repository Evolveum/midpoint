/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import javax.xml.namespace.QName;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
public class ActivationDescriptionHandler implements VisualizationDescriptionHandler {

    @Override
    public boolean match(VisualizationImpl visualization) {
        PrismContainerValue<?> value = visualization.getSourceValue();
        if (value == null) {
            return false;
        }

        if (ActivationType.class.equals(value.getCompileTimeClass())) {
            // if there's password
            return value.findProperty(ActivationType.F_EFFECTIVE_STATUS) != null;
        }

        // we're modifying/deleting password
        return ActivationType.F_EFFECTIVE_STATUS.equivalent(value.getPath());
    }

    @Override
    public void apply(VisualizationImpl visualization, Task task, OperationResult result) {
        PrismContainerValue<?> value = visualization.getSourceValue();
        PrismProperty<ActivationStatusType> effectiveStatus = value.findProperty(ActivationType.F_EFFECTIVE_STATUS);
        ActivationStatusType status = effectiveStatus.getRealValue();

        if (status == null) {
            return;
        }

        QName type = ObjectTypes.OBJECT.getTypeQName();
        PrismContainerValue root = value.getRootValue();
        if (root != null) {
            type = root.getTypeName();
        }

        ObjectTypes ot = ObjectTypes.getObjectTypeFromTypeQName(type);

        visualization.getName().setOverview(
                new SingleLocalizableMessage("ActivationDescriptionHandler.effectiveStatus", new Object[] {
                        new SingleLocalizableMessage("ObjectTypes." + ot.name()),
                        new SingleLocalizableMessage("ActivationDescriptionHandler.ActivationStatusType." + status.name())
                })
        );
    }
}

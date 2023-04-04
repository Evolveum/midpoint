/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

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

        String typeKey = null;
        PrismContainerValue root = value.getRootValue();
        if (root != null) {
            Class clazz = root.getCompileTimeClass();
            if (clazz != null && ObjectType.class.isAssignableFrom(clazz)) {
                typeKey = "ObjectTypes." + ObjectTypes.getObjectType(clazz).name();
            } else {
                typeKey = root.getDefinition() != null ? root.getDefinition().getDisplayName() : null;
            }
        }

        if (typeKey == null) {
            typeKey = "ObjectTypes.OBJECT";
        }

        visualization.getName().setOverview(
                new SingleLocalizableMessage("ActivationDescriptionHandler.effectiveStatus", new Object[] {
                        new SingleLocalizableMessage(typeKey),
                        new SingleLocalizableMessage("ActivationDescriptionHandler.ActivationStatusType." + status.name())
                })
        );
    }
}

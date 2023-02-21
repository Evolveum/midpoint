/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.util.ReferenceResolver;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
public class ShadowDescriptionHandler implements VisualizationDescriptionHandler {

    @Autowired
    private ModelService modelService;

    @Override
    public boolean match(VisualizationImpl visualization) {
        PrismContainerValue value = visualization.getSourceValue();
        if (value == null || !(value.asContainerable() instanceof ShadowType)) {
            return false;
        }

        return true;
    }

    @Override
    public void apply(VisualizationImpl visualization, Task task, OperationResult result) {
        PrismContainerValue value = visualization.getSourceValue();
        ShadowType shadow = (ShadowType) value.asContainerable();

        ShadowKindType kind = shadow.getKind() != null ? shadow.getKind() : ShadowKindType.UNKNOWN;
        String name = shadow.getName() != null ? shadow.getName().getOrig() : "";
        ChangeType change = visualization.getChangeType();

        String resourceName;
        ObjectReferenceType resourceRef = shadow.getResourceRef();
        try {
            PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, resourceRef.getOid(), GetOperationOptions.createRawCollection(), task, result);
            resourceName = resource.getName().getOrig();
        } catch (Exception ex) {
            // todo fix
            ex.printStackTrace();
            resourceName = resourceRef.getOid();
        }

        visualization.getName().setSimpleDescription(
                new SingleLocalizableMessage("ShadowDescriptionHandler.shadow", new Object[] {
                        new SingleLocalizableMessage("ShadowKindType." + kind.name()),
                        name,
                        shadow.getIntent(),
                        new SingleLocalizableMessage("ShadowDescriptionHandler.changeType." + change.name()),
                        resourceName
                })
        );
    }
}

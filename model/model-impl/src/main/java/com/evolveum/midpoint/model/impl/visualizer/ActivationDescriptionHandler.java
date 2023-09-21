/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.util.exception.SchemaException;

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
    public boolean match(VisualizationImpl visualization, VisualizationImpl parentVisualization) {

        PrismContainerValue<?> value = visualization.getSourceValue();
        if (value == null) {
            return false;
        }

        if (parentVisualization != null && parentVisualization.getSourceDelta() != null) {
            ItemDelta<PrismValue, ItemDefinition<?>> deltaItem = parentVisualization.getSourceDelta().findItemDelta(ItemPath.create(
                    visualization.getSourceRelPath(), ActivationType.F_EFFECTIVE_STATUS));
            try {
                if(deltaItem != null && deltaItem.getItemNew() != null && deltaItem.getItemNew().getRealValue() != null) {
                    return true;
                }
            } catch (SchemaException e) {
                //ignore it and try sourceValue
            }
        }

        if (ActivationType.class.equals(value.getCompileTimeClass())) {
            // if there's password
            return value.findProperty(ActivationType.F_EFFECTIVE_STATUS) != null;
        }

        // we're modifying/deleting password
        return ActivationType.F_EFFECTIVE_STATUS.equivalent(value.getPath());
    }

    @Override
    public void apply(VisualizationImpl visualization, VisualizationImpl parentVisualization, Task task, OperationResult result) {
        ActivationStatusType status = null;
        if (parentVisualization != null && parentVisualization.getSourceDelta() != null) {
            ItemDelta<PrismValue, ItemDefinition<?>> deltaItem = parentVisualization.getSourceDelta().findItemDelta(ItemPath.create(
                    visualization.getSourceRelPath(), ActivationType.F_EFFECTIVE_STATUS));
            try {
                if(deltaItem != null && deltaItem.getItemNew() != null) {
                    status = (ActivationStatusType) deltaItem.getItemNew().getRealValue();
                }
            } catch (SchemaException e) {
                //ignore it and try sourceValue
            }
        }

        PrismContainerValue<?> value = visualization.getSourceValue();

        if (status == null) {
            PrismProperty<ActivationStatusType> effectiveStatus = value.findProperty(ActivationType.F_EFFECTIVE_STATUS);
            status = effectiveStatus.getRealValue();
        }

        if (status == null) {
            return;
        }

        String typeKey = null;
        PrismContainerValue root = value.getRootValue();
        Class clazz = root.getCompileTimeClass();
        if (clazz != null && ObjectType.class.isAssignableFrom(clazz)) {
            typeKey = "ObjectTypes." + ObjectTypes.getObjectType(clazz).name();
        } else {
            typeKey = root.getDefinition() != null ? root.getDefinition().getDisplayName() : null;
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

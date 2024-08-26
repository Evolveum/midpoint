/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationItemImpl;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationItemValueImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowAssociationsUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
public class AssociationDescriptionHandler extends ShadowDescriptionHandler {

    @Autowired
    private Resolver resolver;

    @Override
    public boolean match(VisualizationImpl visualization, VisualizationImpl parentVisualization) {
        PrismContainerValue<?> value = visualization.getSourceValue();
        if (value == null) {
            return false;
        }

        return visualization.getSourceValue().getPath().size() == 2
                && visualization.getSourceValue().getPath().namedSegmentsOnly().startsWith(ShadowType.F_ASSOCIATIONS);
    }

    @Override
    public void apply(VisualizationImpl visualization, VisualizationImpl parentVisualization, Task task, OperationResult result) {
        Visualization shadowVisualization = visualization.getOwner().getOwner();    // todo not very nice -> search via path or something
        ShadowType subject = (ShadowType) shadowVisualization.getSourceValue().asContainerable();

        ShadowAssociationValueType associationValue = (ShadowAssociationValueType) visualization.getSourceValue().asContainerable();

        String subjectName = getShadowName(subject);

        String association = visualization.getSourceDefinition().getItemName().getLocalPart();  // todo how to get association name or displayName

        String objectName = "ShadowDescriptionHandler.noName";

        ObjectReferenceType shadowRef = ShadowAssociationsUtil.getSingleObjectRefRelaxed(associationValue); //value.asObjectReferenceType();
        if (shadowRef != null) {
            PrismObject<ShadowType> object = (PrismObject<ShadowType>) resolver.resolveObject(shadowRef, task, result);
            ShadowType objectShadow = object.asObjectable();
            objectName = getShadowName(objectShadow);

            Optional<? extends VisualizationItemImpl> shadowRefDelta = visualization.getItems().stream().filter(visualizationItem ->
                            visualizationItem.getSourceDefinition().getItemName().equivalent(ShadowAssociationValueType.F_OBJECTS))
                    .findFirst();
            if (shadowRefDelta.isPresent() && !shadowRefDelta.get().getNewValues().isEmpty()) {
                VisualizationItemValueImpl newItemValue = new VisualizationItemValueImpl(
                        shadowRefDelta.get().getNewValues().get(0).getText());
                newItemValue.setSourceValue(shadowRef.asReferenceValue());
                shadowRefDelta.get().setNewValues(List.of(newItemValue));

            }
        }

        ChangeType change = visualization.getChangeType();

        visualization.getName().setOverview(
                new SingleLocalizableMessage("ShadowDescriptionHandler.association", new Object[] {
                        new SingleLocalizableMessage(association),
                        new SingleLocalizableMessage(subjectName),
                        new SingleLocalizableMessage(objectName),
                        new SingleLocalizableMessage("ShadowDescriptionHandler.changeType." + change.name())
                })
        );
    }
}

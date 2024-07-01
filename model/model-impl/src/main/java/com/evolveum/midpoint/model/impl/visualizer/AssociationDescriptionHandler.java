/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationItemImpl;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationItemValueImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.xml.bind.JAXBElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SingleLocalizableMessage;

import java.util.List;
import java.util.Optional;

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
        ShadowType subject = (ShadowType) parentVisualization.getSourceValue().asContainerable();
        String subjectName = getShadowName(subject);

        String association = visualization.getSourceDefinition().getItemName().getLocalPart();

        ShadowReferenceAttributeValue value = (ShadowReferenceAttributeValue) visualization.getSourceValue();



        String objectName = "ShadowDescriptionHandler.noName";

        // FIXME reenable and adapt this code
//        ObjectReferenceType shadowRef = value.asObjectReferenceType();
//        if (shadowRef != null || shadowRef.getObject() != null) {
//            PrismObject<ShadowType> shadow = shadowRef.getObject();
//            List<Object> associations = shadow.asObjectable().getAssociations().getAny();
//            if (!associations.isEmpty() && associations.get(0) instanceof JAXBElement<?> shadowValueJaxb) {
//                ShadowAssociationValueType shadowValue = (ShadowAssociationValueType) shadowValueJaxb.getValue();
//                ObjectReferenceType objectShadowRef = shadowValue.getShadowRef();
//
//                ShadowType objectShadow = objectShadowRef.getObject() != null ?
//                        (ShadowType) objectShadowRef.getObject().asObjectable() :
//                        (ShadowType) resolver.resolveObject(objectShadowRef, task, result);
//                objectName = getShadowName(objectShadow);
//
//                Optional<? extends VisualizationItemImpl> shadowRefDelta = visualization.getItems().stream().filter(visualizationItem ->
//                                visualizationItem.getSourceDefinition().getItemName().equivalent(ShadowAssociationValueType.F_SHADOW_REF))
//                        .findFirst();
//                if (shadowRefDelta.isPresent() && !shadowRefDelta.get().getNewValues().isEmpty()) {
//                    VisualizationItemValueImpl newItemValue = new VisualizationItemValueImpl(
//                            shadowRefDelta.get().getNewValues().get(0).getText());
//                    newItemValue.setSourceValue(objectShadowRef.asReferenceValue());
//                    shadowRefDelta.get().setNewValues(List.of(newItemValue));
//
//                }
//            }
//        }

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

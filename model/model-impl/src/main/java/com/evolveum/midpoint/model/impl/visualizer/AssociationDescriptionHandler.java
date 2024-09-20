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
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
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

    private Visualization getRootVisualization(Visualization visualization) {
        Visualization parent = visualization.getOwner();
        if (parent == null) {
            return visualization;
        }
        return getRootVisualization(parent);
    }

    @Override
    public void apply(VisualizationImpl visualization, VisualizationImpl parentVisualization, Task task, OperationResult result) {
        Visualization shadowVisualization = getRootVisualization(visualization);
        PrismContainerValue<?> pcv = shadowVisualization.getSourceValue();
        if (pcv == null || !(pcv.asContainerable() instanceof ShadowType subject)) {
            return;
        }

        ShadowAssociationValue associationValue = (ShadowAssociationValue) visualization.getSourceValue();

        String subjectName = getShadowName(subject);

        ShadowAssociationDefinition def = associationValue.getDefinitionRequired();
        ShadowReferenceAttributeDefinition refDef = def.getReferenceAttributeDefinition();
        String association = refDef.getDisplayName();
        if (association == null) {
            association = refDef.getItemName().getLocalPart();
        }

        String objectName = "ShadowDescriptionHandler.noName";

        ObjectReferenceType shadowRef = associationValue.getSingleObjectRefRelaxed();
        if (shadowRef != null) {
            PrismObject<ShadowType> object = shadowRef.getObject() != null ?
                    shadowRef.getObject() : (PrismObject<ShadowType>) resolver.resolveObject(shadowRef, task, result);

            if (object != null) {
                ShadowType objectShadow = object.asObjectable();
                objectName = getShadowName(objectShadow);
            }

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

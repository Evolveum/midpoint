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

import com.evolveum.midpoint.model.api.visualizer.LocalizationCustomizationContext;
import com.evolveum.midpoint.model.api.visualizer.localization.LocalizationPart;
import com.evolveum.midpoint.model.api.visualizer.localization.WrapableLocalization;
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

    private static final SingleLocalizableMessage ON = new SingleLocalizableMessage(
            "AssignmentDescriptionHandler.assignment.construction.on", null, "on");
    private static final SingleLocalizableMessage CONSTRUCTION_OF = new SingleLocalizableMessage(
            "AssignmentDescriptionHandler.assignment.construction.of", null, "Construction of");

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

        final LocalizableMessage shadowKind = new SingleLocalizableMessage("ShadowKindType." + kind);
        final LocalizableMessage localizableResourceName = resourceName instanceof LocalizableMessage
                ? (LocalizableMessage) resourceName
                : new SingleLocalizableMessage("", null, (String)resourceName);
        final LocalizableMessage action = createAssignedMessage(change);
        final SingleLocalizableMessage localizableIntent = new SingleLocalizableMessage("", null, intent);
        final WrapableLocalization<String, LocalizationCustomizationContext> wrapableOverview = WrapableLocalization.of(
                LocalizationPart.forHelpingWords(CONSTRUCTION_OF),
                LocalizationPart.forObject(shadowKind,
                        LocalizationCustomizationContext.builder().objectType(ObjectTypes.SHADOW).build()),
                LocalizationPart.forAdditionalInfo(localizableIntent, null),
                LocalizationPart.forHelpingWords(ON),
                LocalizationPart.forObjectName(localizableResourceName,
                        LocalizationCustomizationContext.builder().objectType(ObjectTypes.RESOURCE).build()),
                LocalizationPart.forAction(action, null));

        visualization.getName().setCustomizableOverview(wrapableOverview);
        visualization.getName().setOverview(
                new SingleLocalizableMessage("AssignmentDescriptionHandler.assignment.construction", new Object[] {
                        shadowKind,
                        intent,
                        resourceName, action
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

        final LocalizableMessage objectType = new SingleLocalizableMessage("ObjectTypes." + ot.name());
        final LocalizableMessage objectName = new SingleLocalizableMessage("", null, targetName);
        final LocalizableMessage action = createAssignedMessage(change);
        final LocalizationCustomizationContext
                customizationContext = LocalizationCustomizationContext.builder().objectType(ot).build();
        final WrapableLocalization<String, LocalizationCustomizationContext> wrapableOverview = WrapableLocalization.of(
                LocalizationPart.forObject(objectType, customizationContext),
                LocalizationPart.forObjectName(objectName, customizationContext),
                LocalizationPart.forAction(action, null));

        visualization.getName().setCustomizableOverview(wrapableOverview);
        visualization.getName().setOverview(
                new SingleLocalizableMessage("AssignmentDescriptionHandler.assignment", new Object[] {
                        objectType, targetName, action
                }, (String) null));
    }
}

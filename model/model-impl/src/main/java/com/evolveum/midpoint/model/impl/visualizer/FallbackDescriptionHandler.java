/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import com.evolveum.midpoint.model.api.visualizer.LocalizationCustomizationContext;
import com.evolveum.midpoint.model.api.visualizer.localization.LocalizationPart;
import com.evolveum.midpoint.model.api.visualizer.localization.WrapableLocalization;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class FallbackDescriptionHandler implements VisualizationDescriptionHandler {

    private static final LocalizableMessage WAS = new SingleLocalizableMessage(
            "FallbackDescriptionHandler.was", null, "was");

    @Override
    public boolean match(VisualizationImpl visualization, @Nullable VisualizationImpl parentVisualization) {
        //should match all visualizations in case no other specific handler is found
        return true;
    }

    @Override
    public void apply(VisualizationImpl visualization, VisualizationImpl parentVisualization, Task task, OperationResult result) {
        PrismContainerValue<?> value = visualization.getSourceValue();

        if (value == null) {
            return;
        }

        ChangeType change = visualization.getChangeType();

        String displayName = value.getDefinition().getDisplayName();
        String displayNameKey = displayName != null ? displayName : value.getCompileTimeClass().getSimpleName();
        final LocalizableMessage localizableContainerName = new SingleLocalizableMessage(displayNameKey, null, displayNameKey);

        final LocalizableMessage localizableChange = new SingleLocalizableMessage(
                "FallbackDescriptionHandler.changeType." + change.name());

        final WrapableLocalization<String, LocalizationCustomizationContext> customizableOverview =
                WrapableLocalization.of(
                        LocalizationPart.forObjectName(localizableContainerName, LocalizationCustomizationContext.empty()),
                        LocalizationPart.forHelpingWords(WAS),
                        LocalizationPart.forAction(localizableChange, LocalizationCustomizationContext.empty())
                );

        visualization.getName().setCustomizableOverview(customizableOverview);
        visualization.getName().setOverview(
                new SingleLocalizableMessage("FallbackDescriptionHandler.message",
                        new Object[] {
                                localizableContainerName, localizableChange
                        })
        );
    }

}

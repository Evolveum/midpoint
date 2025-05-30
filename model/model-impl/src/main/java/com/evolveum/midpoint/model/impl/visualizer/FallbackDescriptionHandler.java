/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import com.evolveum.midpoint.model.api.visualizer.LocalizationCustomizationContext;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.api.visualizer.localization.LocalizationPart;
import com.evolveum.midpoint.model.api.visualizer.localization.WrapableLocalization;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;

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

        String displayName = getTypeDisplayName(value);
        final LocalizableMessage localizableContainerName = new SingleLocalizableMessage(displayName, null, displayName);

        final LocalizableMessage localizableChangePerformed = new SingleLocalizableMessage(
                "FallbackDescriptionHandler.changeType.performed." + change.name());
        final LocalizableMessage localizableChangeInPresentTense = new SingleLocalizableMessage(
                "FallbackDescriptionHandler.changeType.present.tense." + change.name());

        if (visualization.getOwner() != null) {
            final WrapableLocalization<String, LocalizationCustomizationContext> customizableOverview =
                    WrapableLocalization.of(
                            LocalizationPart.forObjectName(localizableContainerName, LocalizationCustomizationContext.empty()),
                            LocalizationPart.forHelpingWords(WAS),
                            LocalizationPart.forAction(localizableChangePerformed, LocalizationCustomizationContext.empty())
                    );
            visualization.getName().setCustomizableOverview(customizableOverview);
            visualization.getName().setOverview(
                    new SingleLocalizableMessage("FallbackDescriptionHandler.performed.message",
                            new Object[] {
                                    localizableContainerName, localizableChangePerformed
                            })
            );
        } else {
            LocalizableMessage visualizationName = getVisualizationName(visualization);

            final WrapableLocalization<String, LocalizationCustomizationContext> customizableOverview =
                    WrapableLocalization.of(
                            LocalizationPart.forAction(localizableChangeInPresentTense, LocalizationCustomizationContext.empty()),
                            LocalizationPart.forObject(localizableContainerName, LocalizationCustomizationContext.empty()),
                            LocalizationPart.forObjectName(visualizationName, LocalizationCustomizationContext.empty())
                    );
            visualization.getName().setCustomizableOverview(customizableOverview);
            visualization.getName().setOverview(
                    new SingleLocalizableMessage("FallbackDescriptionHandler.present.tense.message",
                            new Object[] {
                                    localizableChangeInPresentTense, localizableContainerName, visualizationName
                            })
            );
        }
    }

    private String getTypeDisplayName(PrismContainerValue<?> value) {
        ComplexTypeDefinition def = value.getComplexTypeDefinition();
        if (def != null) {
            String displayName = def.getDisplayName();
            if (displayName != null) {
                return displayName;
            }
        }
        PrismContainerDefinition<?> pcd = value.getDefinition();
        if (pcd != null) {
            String displayName = pcd.getDisplayName();
            if (displayName != null) {
                return displayName;
            }
        }
        return value.getCompileTimeClass().getSimpleName();
    }

    private LocalizableMessage getVisualizationName(Visualization visualization) {
        if (visualization.getName() == null) {
            return null;
        }
        LocalizableMessage displayName = visualization.getName().getDisplayName();
        if (displayName != null) {
            return displayName;
        }
        return visualization.getName().getSimpleName();
    }



}

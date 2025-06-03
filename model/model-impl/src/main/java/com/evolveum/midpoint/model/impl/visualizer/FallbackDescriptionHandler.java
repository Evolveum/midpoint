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
import com.evolveum.midpoint.util.LocalizableMessageList;
import com.evolveum.midpoint.util.SingleLocalizableMessage;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class FallbackDescriptionHandler implements VisualizationDescriptionHandler {

    private static final LocalizableMessage WAS = new SingleLocalizableMessage(
            "FallbackDescriptionHandler.was", null, "was");
    private static final LocalizableMessage CLOSE_PARENTHESIS = new SingleLocalizableMessage(
            ")", null, ")");
    private static final LocalizableMessage NAME_AND_DISPLAY_NAME_SEPARATOR = new SingleLocalizableMessage(
            " (", null, " (");

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
        final LocalizableMessage visualizationDisplayName = getVisualizationDisplayName(visualization);


        LocalizationPart[] localizationParts;   // we want to show "Display name (name)" for object headers and "Container name"
                                                // as a header name for other child containers. Therefore, there is the
                                                // following condition
        if (visualization.getOwner() != null) {
            localizationParts = new LocalizationPart[] {
                    LocalizationPart.forObjectName(localizableContainerName, LocalizationCustomizationContext.empty()),
                    LocalizationPart.forHelpingWords(WAS),
                    LocalizationPart.forAction(localizableChangePerformed, LocalizationCustomizationContext.empty())
            };
        } else {
            localizationParts = new LocalizationPart[] {
                    LocalizationPart.forObject(localizableContainerName, LocalizationCustomizationContext.empty()),
                    LocalizationPart.forObjectName(visualizationDisplayName, LocalizationCustomizationContext.empty()),
                    LocalizationPart.forHelpingWords(WAS),
                    LocalizationPart.forAction(localizableChangePerformed, LocalizationCustomizationContext.empty())
            };
        }
            final WrapableLocalization<String, LocalizationCustomizationContext> customizableOverview =
                    WrapableLocalization.of(localizationParts);
            visualization.getName().setCustomizableOverview(customizableOverview);
            visualization.getName().setOverview(
                    new SingleLocalizableMessage("FallbackDescriptionHandler.performed.message",
                            new Object[] {
                                    localizableContainerName, localizableChangePerformed
                            })
            );
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

    private LocalizableMessage getVisualizationDisplayName(Visualization visualization) {
        if (visualization.getName() == null) {
            return null;
        }
        LocalizableMessage displayName = visualization.getName().getDisplayName();
        LocalizableMessage simpleName = visualization.getName().getSimpleName();
        if (displayName == null) {
            return simpleName;
        }
        if (sameNames(displayName, simpleName)) {
            return displayName;
        }
        // this is some kind of hack to get the display name of the object to the state "Display name (name)"
        // e.g. "Jack Jackson (jjackson)". The separator between display name and name contains " (".
        //  The postfix for the messages list is ")" just to close the opened parenthesis.
        return new LocalizableMessageList(Arrays.asList(displayName, simpleName),
                NAME_AND_DISPLAY_NAME_SEPARATOR, null, CLOSE_PARENTHESIS);
    }

    private boolean sameNames(LocalizableMessage name1, LocalizableMessage name2) {
        if (name1 == null && name2 == null) {
            return true;
        }
        if (name1 == null || name2 == null) {
            return false;
        }

        return StringUtils.equals(name1.getFallbackMessage(), name2.getFallbackMessage());
    }
}

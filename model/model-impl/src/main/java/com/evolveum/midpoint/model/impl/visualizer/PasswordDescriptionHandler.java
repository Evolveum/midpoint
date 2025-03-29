/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.visualizer.LocalizationCustomizationContext;
import com.evolveum.midpoint.model.api.visualizer.localization.LocalizationPart;
import com.evolveum.midpoint.model.api.visualizer.localization.WrapableLocalization;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationDeltaItemImpl;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationItemImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
public class PasswordDescriptionHandler implements VisualizationDescriptionHandler {

    private static final LocalizableMessage PASSWORD = new SingleLocalizableMessage(
            "PasswordDescriptionHandler.password.password", null, "Password");
    private static final ItemPath PATH_PASSWORD = ItemPath.create(FocusType.F_CREDENTIALS, CredentialsType.F_PASSWORD);

    @Override
    public boolean match(VisualizationImpl visualization, VisualizationImpl parentVisualization) {
        PrismContainerValue<?> value = visualization.getSourceValue();
        if (value == null) {
            return false;
        }

        if (CredentialsType.class.equals(value.getCompileTimeClass())) {
            // if there's password
            return value.findContainer(CredentialsType.F_PASSWORD) != null;
        }

        // we're modifying/deleting password
        return PATH_PASSWORD.equivalent(value.getPath());
    }

    @Override
    public void apply(VisualizationImpl visualization, VisualizationImpl parentVisualization, Task task, OperationResult result) {
        ChangeType change = visualization.getChangeType();

        VisualizationItemImpl item = visualization.getItems().stream()
                .filter(i -> {
                    ItemPath path = i.getSourceRelPath();
                    return path != null && PasswordType.F_VALUE.equivalent(path.namedSegmentsOnly());
                }).findFirst().orElse(null);

        if (item instanceof VisualizationDeltaItemImpl) {
            VisualizationDeltaItemImpl deltaItem = (VisualizationDeltaItemImpl) item;
            if (!deltaItem.getDeletedValues().isEmpty()) {
                change = deltaItem.getAddedValues().isEmpty() ? ChangeType.DELETE : ChangeType.MODIFY;
            } else {
                change = ChangeType.ADD;
            }
        }

        final SingleLocalizableMessage localizableChange = new SingleLocalizableMessage(
                "PasswordDescriptionHandler.changeType." + change.name());
        final WrapableLocalization<String, LocalizationCustomizationContext> customizableOverview =
                WrapableLocalization.of(
                        LocalizationPart.forHelpingWords(PASSWORD),
                        LocalizationPart.forAction(localizableChange, LocalizationCustomizationContext.empty()));

        visualization.getName().setCustomizableOverview(customizableOverview);
        visualization.getName().setOverview(
                new SingleLocalizableMessage("PasswordDescriptionHandler.password", new Object[] {localizableChange})
        );
    }
}

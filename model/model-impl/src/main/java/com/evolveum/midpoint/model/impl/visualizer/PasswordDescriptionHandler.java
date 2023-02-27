/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationDeltaItemImpl;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationItemImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
public class PasswordDescriptionHandler implements VisualizationDescriptionHandler {

    @Override
    public boolean match(VisualizationImpl visualization) {
        PrismContainerValue<?> value = visualization.getSourceValue();
        if (value == null) {
            return false;
        }

        if (value.asContainerable() instanceof CredentialsType) {
            // if there's password
            return value.findContainer(CredentialsType.F_PASSWORD) != null;
        }

        // we're modifying/deleting password
        return ItemPath.create(FocusType.F_CREDENTIALS, CredentialsType.F_PASSWORD).equivalent(value.getPath());
    }

    @Override
    public void apply(VisualizationImpl visualization, Task task, OperationResult result) {
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

        visualization.getName().setOverview(
                new SingleLocalizableMessage("PasswordDescriptionHandler.password", new Object[] {
                        new SingleLocalizableMessage("PasswordDescriptionHandler.changeType." + change.name())
                })
        );
    }
}

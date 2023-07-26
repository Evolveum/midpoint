/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityControlFlowDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityErrorHandlingStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@SuppressWarnings("unused")
public class LiveSyncErrorHandlingProcessor implements UpgradeObjectProcessor<TaskType> {

    @Override
    public UpgradePhase getPhase() {
        return UpgradePhase.BEFORE;
    }

    @Override
    public UpgradePriority getPriority() {
        return UpgradePriority.NECESSARY;
    }

    @Override
    public UpgradeType getType() {
        return UpgradeType.PREVIEW;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        return matchObjectTypeAndPathTemplate(
                object, path,
                TaskType.class,
                ItemPath.create(
                        TaskType.F_EXTENSION,
                        new QName(SchemaConstants.NS_MODEL_EXTENSION, "liveSyncErrorHandlingStrategy")
                )
        );
    }

    @Override
    public boolean process(PrismObject<TaskType> object, ItemPath path) {
        ActivityErrorHandlingStrategyType errorHandling = object.findItem(path)
                .getRealValue(ActivityErrorHandlingStrategyType.class).clone();

        TaskType task = object.asObjectable();
        ActivityDefinitionType activity = task.getActivity();
        if (activity == null) {
            activity = new ActivityDefinitionType();
            task.setActivity(activity);
        }

        ActivityControlFlowDefinitionType controlFlow = activity.getControlFlow();
        if (controlFlow == null) {
            controlFlow = new ActivityControlFlowDefinitionType();
            activity.setControlFlow(controlFlow);
        }

        controlFlow.setErrorHandling(errorHandling);

        PrismContainer extension = object.findContainer(TaskType.F_EXTENSION);
        extension.removeItem(path.lastName(), PrismContainer.class);

        if (extension.isEmpty()) {
            object.remove(extension);
        }

        return true;
    }
}

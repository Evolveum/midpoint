/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
public class ClassLoggerDescriptionHandler implements VisualizationDescriptionHandler {

    private static final ItemPath PATH_CLASS_LOGGER = ItemPath.create(SystemConfigurationType.F_LOGGING, LoggingConfigurationType.F_CLASS_LOGGER);

    @Override
    public boolean match(VisualizationImpl visualization) {
        PrismContainerValue<?> value = visualization.getSourceValue();
        if (value == null) {
            return false;
        }

        return PATH_CLASS_LOGGER.equivalent(value.getPath().namedSegmentsOnly());
    }

    @Override
    public void apply(VisualizationImpl visualization, Task task, OperationResult result) {
        PrismContainerValue<?> value = visualization.getSourceValue();
        ChangeType changeType = visualization.getChangeType();

        ClassLoggerConfigurationType logger = (ClassLoggerConfigurationType) value.asContainerable();

        visualization.getName().setOverview(
                new SingleLocalizableMessage("ClassLoggerDescriptionHandler.classLogger", new Object[] {
                        new SingleLocalizableMessage("ClassLoggerDescriptionHandler.changeType." + changeType.name()),
                        logger.getLevel(),
                        logger.getPackage()
                }, (String) null));
    }
}

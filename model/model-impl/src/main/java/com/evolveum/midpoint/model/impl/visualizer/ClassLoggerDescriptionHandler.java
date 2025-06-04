/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.visualizer.LocalizationCustomizationContext;
import com.evolveum.midpoint.model.api.visualizer.localization.LocalizationPart;
import com.evolveum.midpoint.model.api.visualizer.localization.WrapableLocalization;
import com.evolveum.midpoint.model.impl.visualizer.output.VisualizationImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class ClassLoggerDescriptionHandler implements VisualizationDescriptionHandler {


    private static final LocalizableMessage LEVEL = new SingleLocalizableMessage(
            "ClassLoggerDescriptionHandler.classLogger.level", null, "level");
    private static final LocalizableMessage FOR_LOGGER = new SingleLocalizableMessage(
            "ClassLoggerDescriptionHandler.classLogger.forLogger", null, "for logger");
    private static final ItemPath PATH_CLASS_LOGGER = ItemPath.create(SystemConfigurationType.F_LOGGING, LoggingConfigurationType.F_CLASS_LOGGER);

    @Override
    public boolean match(VisualizationImpl visualization, VisualizationImpl parentVisualization) {
        PrismContainerValue<?> value = visualization.getSourceValue();
        if (value == null) {
            return false;
        }

        return PATH_CLASS_LOGGER.equivalent(value.getPath().namedSegmentsOnly());
    }

    @Override
    public void apply(VisualizationImpl visualization, VisualizationImpl parentVisualization, Task task, OperationResult result) {
        PrismContainerValue<?> value = visualization.getSourceValue();
        ChangeType changeType = visualization.getChangeType();

        ClassLoggerConfigurationType logger = (ClassLoggerConfigurationType) value.asContainerable();

        final SingleLocalizableMessage localizableChange = new SingleLocalizableMessage(
                "ClassLoggerDescriptionHandler.changeType." + changeType.name());
        final WrapableLocalization<String, LocalizationCustomizationContext> customizableOverview =
                WrapableLocalization.of(
                        LocalizationPart.forAction(localizableChange, LocalizationCustomizationContext.empty()),
                        LocalizationPart.forHelpingWords(LEVEL),
                        LocalizationPart.forObjectName(
                                new SingleLocalizableMessage("", null, logger.getLevel().value()),
                                LocalizationCustomizationContext.empty()),
                        LocalizationPart.forHelpingWords(FOR_LOGGER),
                        LocalizationPart.forObjectName(new SingleLocalizableMessage("", null, logger.getPackage()),
                                LocalizationCustomizationContext.empty()));

        visualization.getName().setCustomizableOverview(customizableOverview);
        visualization.getName().setOverview(
                new SingleLocalizableMessage("ClassLoggerDescriptionHandler.classLogger", new Object[] {
                        localizableChange,
                        logger.getLevel(),
                        logger.getPackage()
                }, (String) null));
    }
}

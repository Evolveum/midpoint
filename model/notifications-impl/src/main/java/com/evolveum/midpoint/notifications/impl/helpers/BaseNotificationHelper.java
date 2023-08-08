/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.helpers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BaseEventHandlerType;

@Component
public abstract class BaseNotificationHelper {

    protected void logStart(
            @NotNull Trace LOGGER,
            @NotNull ConfigurationItem<? extends BaseEventHandlerType> eventHandlerConfig,
            @NotNull EventProcessingContext<?> ctx) {
        logStart(LOGGER, eventHandlerConfig, ctx, null);
    }

    protected void logStart(
            @NotNull Trace LOGGER,
            @NotNull ConfigurationItem<? extends BaseEventHandlerType> eventHandlerConfig,
            @NotNull EventProcessingContext<?> ctx,
            @Nullable Object additionalData) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Starting processing event {} with handler {} (name: {}{})",
                    ctx.event(),
                    eventHandlerConfig.getClass(),
                    eventHandlerConfig.value().getName(),
                    (additionalData != null ?
                            (", parameters: " + additionalData) :
                            (", configuration: " + eventHandlerConfig)));
        }
    }

    public void logEnd(
            @NotNull Trace LOGGER,
            @NotNull ConfigurationItem<? extends BaseEventHandlerType> eventHandlerConfig,
            @NotNull EventProcessingContext<?> ctx,
            boolean result) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Finishing processing event {}, result = {}", ctx.event(), result);
        }
    }
}

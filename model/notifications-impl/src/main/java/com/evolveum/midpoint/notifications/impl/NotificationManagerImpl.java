/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.schema.config.EventHandlerConfigItem;

import com.evolveum.midpoint.schema.config.OriginProvider;

import com.evolveum.midpoint.schema.expression.ExpressionProfile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.transport.impl.TransportUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

@Component
public class NotificationManagerImpl implements NotificationManager {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationManager.class);
    private static final String OP_PROCESS_EVENT = NotificationManager.class.getName() + ".processEvent";

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

    @Autowired private EventHandlerRegistry eventHandlerRegistry;

    private boolean disabled = false; // for testing purposes (in order for model-intest to run more quickly)

    @Override
    public void processEvent(
            @NotNull Event event,
            @Nullable EventHandlerConfigItem customHandler,
            @Nullable ExpressionProfile customHandlerExpressionProfile,
            @NotNull Task task,
            @NotNull OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_PROCESS_EVENT)
                .addArbitraryObjectAsParam("event", event)
                .build();
        try {
            LOGGER.trace("NotificationManager processing event:\n{}", event.debugDumpLazily(1));

            if (customHandler != null) {
                argCheck(customHandlerExpressionProfile != null, "customHandlerExpressionProfile is null");
                var ctx = new EventProcessingContext<>(event, customHandlerExpressionProfile, task);
                processEvent(customHandler, ctx, result);
            }

            SystemConfigurationType systemConfiguration = getSystemConfiguration(task, result);
            if (systemConfiguration == null) {
                LOGGER.trace("No system configuration in repository, are we doing initial import?");
            } else if (systemConfiguration.getNotificationConfiguration() == null) {
                LOGGER.trace("No notification configuration in repository, finished event processing.");
            } else {
                NotificationConfigurationType notificationConfiguration = systemConfiguration.getNotificationConfiguration();
                for (EventHandlerType eventHandlerBean : notificationConfiguration.getHandler()) {
                    // Default expression profile for embedded handlers is always "full".
                    // We don't use archetype manager to avoid wasting cpu cycles
                    // TODO review in the future
                    ExpressionProfile profile = ExpressionProfile.full();
                    var ctx = new EventProcessingContext<>(event, profile, task);
                    processEvent(
                            EventHandlerConfigItem.of(
                                    eventHandlerBean,
                                    OriginProvider.embedded()),
                            ctx, result);
                }
                LOGGER.trace("NotificationManager successfully processed event {} ({} top level handler(s))", event,
                        notificationConfiguration.getHandler().size());
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private SystemConfigurationType getSystemConfiguration(Task task, OperationResult result) {
        boolean errorIfNotFound = !SchemaConstants.CHANNEL_INIT_URI.equals(task.getChannel());
        return TransportUtil.getSystemConfiguration(cacheRepositoryService, errorIfNotFound, result);
    }

    private void processEvent(
            @NotNull EventHandlerConfigItem eventHandlerConfig,
            @NotNull EventProcessingContext<?> ctx,
            @NotNull OperationResult result) {
        try {
            eventHandlerRegistry.forwardToHandler(eventHandlerConfig, ctx, result);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Event couldn't be processed: {}", t, ctx);
        }
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}

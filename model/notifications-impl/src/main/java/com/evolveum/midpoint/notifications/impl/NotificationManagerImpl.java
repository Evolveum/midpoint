/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.notifications.api.NotificationFunctions;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.BaseEvent;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */

@Component
public class NotificationManagerImpl implements NotificationManager {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationManager.class);
    private static final String OP_PROCESS_EVENT = NotificationManager.class + ".processEvent";

    @Autowired
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @Autowired private PrismContext prismContext;
    @Autowired private NotificationFunctions notificationFunctions;
    @Autowired private TransportRegistry transportRegistry;
    @Autowired private EventHandlerRegistry eventHandlerRegistry;

    private boolean disabled = false; // for testing purposes (in order for model-intest to run more quickly)

    @Override
    public void registerTransport(String name, Transport transport) {
        transportRegistry.registerTransport(name, transport);
    }

    @Override
    public void processEvent(@NotNull Event event, Task task, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_PROCESS_EVENT)
                .addArbitraryObjectAsParam("event", event)
                .build();
        try {
            if (event instanceof BaseEvent) {
                ((BaseEvent) event).setNotificationFunctions(notificationFunctions);
                ((BaseEvent) event).setPrismContext(prismContext);
            }

            LOGGER.trace("NotificationManager processing event:\n{}", event.debugDumpLazily(1));

            if (event.getAdHocHandler() != null) {
                processEvent(event, event.getAdHocHandler(), task, result);
            }

            SystemConfigurationType systemConfigurationType = getSystemConfiguration(task, result);
            if (systemConfigurationType == null) {
                LOGGER.trace("No system configuration in repository, are we doing initial import?");
            } else if (systemConfigurationType.getNotificationConfiguration() == null) {
                LOGGER.trace("No notification configuration in repository, finished event processing.");
            } else {
                NotificationConfigurationType notificationConfiguration = systemConfigurationType.getNotificationConfiguration();
                for (EventHandlerType eventHandlerBean : notificationConfiguration.getHandler()) {
                    processEvent(event, eventHandlerBean, task, result);
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
        boolean errorIfNotFound = !SchemaConstants.CHANNEL_GUI_INIT_URI.equals(task.getChannel());
        return NotificationFunctionsImpl
                .getSystemConfiguration(cacheRepositoryService, errorIfNotFound, result);
    }

    @Override
    public boolean processEvent(@NotNull Event event, EventHandlerType eventHandlerBean, Task task, OperationResult result) {
        try {
            return eventHandlerRegistry.forwardToHandler(event, eventHandlerBean, task, result);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Event couldn't be processed; event = {}", t, event);
            return true; // continue if you can
        }
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}

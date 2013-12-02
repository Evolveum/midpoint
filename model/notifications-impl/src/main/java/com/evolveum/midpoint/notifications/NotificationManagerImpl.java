/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications;

import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.notifications.transports.Transport;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NotificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.HashMap;

/**
 * @author mederly
 */

@Component
public class NotificationManagerImpl implements NotificationManager {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationManager.class);

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    private boolean disabled = false;               // for testing purposes (in order for model-intest to run more quickly)

    private HashMap<Class<? extends EventHandlerType>,EventHandler> handlers = new HashMap<Class<? extends EventHandlerType>,EventHandler>();
    private HashMap<String,Transport> transports = new HashMap<String,Transport>();

    public void registerEventHandler(Class<? extends EventHandlerType> clazz, EventHandler handler) {
        LOGGER.trace("Registering event handler " + handler + " for " + clazz);
        handlers.put(clazz, handler);
    }

    private EventHandler getEventHandler(EventHandlerType eventHandlerType) {
        EventHandler handler = handlers.get(eventHandlerType.getClass());
        if (handler == null) {
            throw new IllegalStateException("Unknown handler for " + eventHandlerType);
        } else {
            return handler;
        }
    }

    public void registerTransport(String name, Transport transport) {
        LOGGER.trace("Registering notification transport " + transport + " under name " + name);
        transports.put(name, transport);
    }

    // accepts name:subname (e.g. dummy:accounts) - a primitive form of passing parameters (will be enhanced/replaced in the future)
    public Transport getTransport(String name) {
        String key = name.split(":")[0];
        Transport transport = transports.get(key);
        if (transport == null) {
            throw new IllegalStateException("Unknown transport named " + key);
        } else {
            return transport;
        }
    }

    // event may be null
    public void processEvent(Event event) {
        processEvent(event, new OperationResult("dummy"));
    }

    // event may be null
    public void processEvent(Event event, OperationResult result) {
        if (event == null) {
            return;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("NotificationManager processing event " + event);
        }

        SystemConfigurationType systemConfigurationType = NotificationsUtil.getSystemConfiguration(cacheRepositoryService, result).asObjectable();
        if (systemConfigurationType.getNotificationConfiguration() == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("No notification configuration in repository, exiting the change listener.");
            }
            return;
        }

        NotificationConfigurationType notificationConfigurationType = systemConfigurationType.getNotificationConfiguration();

        for (JAXBElement<? extends EventHandlerType> eventHandlerType : notificationConfigurationType.getHandler()) {
            processEvent(event, eventHandlerType.getValue(), result);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("NotificationManager successfully processed event " + event + " (" +notificationConfigurationType.getHandler().size() + " top level handler(s))");
        }
    }

    public boolean processEvent(Event event, EventHandlerType eventHandlerType, OperationResult result) {
        try {
            return getEventHandler(eventHandlerType).processEvent(event, eventHandlerType, this, result);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Event couldn't be processed; event = {}", e, event);
            return true;        // continue if you can
        }
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}

/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.notifications;

import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.notifications.handlers.EventHandler;
import com.evolveum.midpoint.notifications.notifiers.*;
import com.evolveum.midpoint.notifications.transports.DummyTransport;
import com.evolveum.midpoint.notifications.transports.Transport;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.HashMap;

/**
 * @author mederly
 */

@Component
public class NotificationManager {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationManager.class);

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

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

    public void processEvent(Event event) {
        processEvent(event, new OperationResult("dummy"));
    }

    public void processEvent(Event event, OperationResult result) {
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
        return getEventHandler(eventHandlerType).processEvent(event, eventHandlerType, this, result);
    }

}

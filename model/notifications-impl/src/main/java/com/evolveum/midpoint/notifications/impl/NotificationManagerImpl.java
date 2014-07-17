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

package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.notifications.api.EventHandler;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.WorkflowEventCreator;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.impl.events.workflow.DefaultWorkflowEventCreator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessInstanceState;

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

    @Autowired
    private DefaultWorkflowEventCreator defaultWorkflowEventCreator;

    private boolean disabled = false;               // for testing purposes (in order for model-intest to run more quickly)

    private HashMap<Class<? extends EventHandlerType>,EventHandler> handlers = new HashMap<Class<? extends EventHandlerType>,EventHandler>();
    private HashMap<String,Transport> transports = new HashMap<String,Transport>();
    private HashMap<Class<? extends ProcessInstanceState>,WorkflowEventCreator> workflowEventCreators = new HashMap<>();        // key = class of type ProcessInstanceState

    public void registerEventHandler(Class<? extends EventHandlerType> clazz, EventHandler handler) {
        LOGGER.trace("Registering event handler " + handler + " for " + clazz);
        handlers.put(clazz, handler);
    }

    public EventHandler getEventHandler(EventHandlerType eventHandlerType) {
        EventHandler handler = handlers.get(eventHandlerType.getClass());
        if (handler == null) {
            throw new IllegalStateException("Unknown handler for " + eventHandlerType);
        } else {
            return handler;
        }
    }

    @Override
    public void registerTransport(String name, Transport transport) {
        LOGGER.trace("Registering notification transport " + transport + " under name " + name);
        transports.put(name, transport);
    }

    // accepts name:subname (e.g. dummy:accounts) - a primitive form of passing parameters (will be enhanced/replaced in the future)
    @Override
    public Transport getTransport(String name) {
        String key = name.split(":")[0];
        Transport transport = transports.get(key);
        if (transport == null) {
            throw new IllegalStateException("Unknown transport named " + key);
        } else {
            return transport;
        }
    }

    @Override
    public void registerWorkflowEventCreator(Class<? extends ProcessInstanceState> clazz, WorkflowEventCreator workflowEventCreator) {
        // TODO think again about this mechanism
        if (workflowEventCreators.containsKey(clazz)) {
            LOGGER.warn("Multiple registrations of workflow event creators for class {}", clazz.getName());
        }
        workflowEventCreators.put(clazz, workflowEventCreator);
    }

    @Override
    public WorkflowEventCreator getWorkflowEventCreator(PrismObject<? extends ProcessInstanceState> instanceState) {
        WorkflowEventCreator workflowEventCreator = workflowEventCreators.get(instanceState.asObjectable().getClass());
        if (workflowEventCreator == null) {
            return defaultWorkflowEventCreator;
        } else {
            return workflowEventCreator;
        }
    }

    // event may be null
    public void processEvent(Event event) {
        processEvent(event, null, new OperationResult("dummy"));
    }

    // event may be null
    public void processEvent(Event event, Task task, OperationResult result) {
        if (event == null) {
            return;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("NotificationManager processing event " + event);
        }

        SystemConfigurationType systemConfigurationType = NotificationsUtil.getSystemConfiguration(cacheRepositoryService, result);
        if (systemConfigurationType == null) {      // something really wrong happened (or we are doing initial import of objects)
            return;
        }
        if (systemConfigurationType.getNotificationConfiguration() == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("No notification configuration in repository, exiting the change listener.");
            }
            return;
        }

        NotificationConfigurationType notificationConfigurationType = systemConfigurationType.getNotificationConfiguration();

        for (EventHandlerType eventHandlerType : notificationConfigurationType.getHandler()) {
            processEvent(event, eventHandlerType, task, result);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("NotificationManager successfully processed event " + event + " (" +notificationConfigurationType.getHandler().size() + " top level handler(s))");
        }
    }

    public boolean processEvent(Event event, EventHandlerType eventHandlerType, Task task, OperationResult result) {
        try {
            return getEventHandler(eventHandlerType).processEvent(event, eventHandlerType, this, task, result);
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

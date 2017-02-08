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
import com.evolveum.midpoint.notifications.api.NotificationFunctions;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.BaseEvent;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * @author mederly
 */

@Component
public class NotificationManagerImpl implements NotificationManager {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationManager.class);
	private static final String OPERATION_PROCESS_EVENT = NotificationManager.class + ".processEvent";

	@Autowired
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

	@Autowired
	private NotificationFunctions notificationFunctions;

	@Autowired
	private TaskManager taskManager;

    private boolean disabled = false;               // for testing purposes (in order for model-intest to run more quickly)

    private HashMap<Class<? extends EventHandlerType>,EventHandler> handlers = new HashMap<>();
    private HashMap<String,Transport> transports = new HashMap<String,Transport>();

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
        LOGGER.trace("Registering notification transport {} under name {}", transport, name);
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

    public void processEvent(@Nullable Event event) {
		Task task = taskManager.createTaskInstance(OPERATION_PROCESS_EVENT);
        processEvent(event, task, task.getResult());
    }

    public void processEvent(@Nullable Event event, Task task, OperationResult result) {
        if (event == null) {
            return;
        }

		if (event instanceof BaseEvent) {
			((BaseEvent) event).setNotificationFunctions(notificationFunctions);
		}

		LOGGER.trace("NotificationManager processing event {}", event);

        if (event.getAdHocHandler() != null) {
            processEvent(event, event.getAdHocHandler(), task, result);
        }

        SystemConfigurationType systemConfigurationType = NotificationFunctionsImpl
				.getSystemConfiguration(cacheRepositoryService, result);
        if (systemConfigurationType == null) {      // something really wrong happened (or we are doing initial import of objects)
            return;
        }
        
//        boolean specificSecurityPoliciesDefined = false;
//        if (systemConfigurationType.getGlobalSecurityPolicyRef() != null) {
//        
//        	SecurityPolicyType securityPolicyType = NotificationFuctionsImpl.getSecurityPolicyConfiguration(systemConfigurationType.getGlobalSecurityPolicyRef(), cacheRepositoryService, result);
//        	if (securityPolicyType != null && securityPolicyType.getAuthentication() != null) {
//        		
//        		for (MailAuthenticationPolicyType mailPolicy : securityPolicyType.getAuthentication().getMailAuthentication()) {
//        			NotificationConfigurationType notificationConfigurationType = mailPolicy.getNotificationConfiguration();
//        			if (notificationConfigurationType != null) {
//        				specificSecurityPoliciesDefined = true;
//        				processNotifications(notificationConfigurationType, event, task, result);
//        			}
//        		}
//        		
//        		for (SmsAuthenticationPolicyType mailPolicy : securityPolicyType.getAuthentication().getSmsAuthentication()) {
//        			NotificationConfigurationType notificationConfigurationType = mailPolicy.getNotificationConfiguration();
//        			if (notificationConfigurationType != null) {
//        				specificSecurityPoliciesDefined = true;
//        				processNotifications(notificationConfigurationType, event, task, result);
//        			}
//        		}
//        		
//        		return;
//        	}
//        }
//        
//        if (specificSecurityPoliciesDefined) {
//        	LOGGER.trace("Specific policy for notifier set in security configuration, skupping notifiers defined in system configuration.");
//            return;
//        }
        
        if (systemConfigurationType.getNotificationConfiguration() == null) {
			LOGGER.trace("No notification configuration in repository, finished event processing.");
            return;
        }
        
        NotificationConfigurationType notificationConfigurationType = systemConfigurationType.getNotificationConfiguration(); 
        processNotifications(notificationConfigurationType, event, task, result);

		LOGGER.trace("NotificationManager successfully processed event {} ({} top level handler(s))", event, notificationConfigurationType.getHandler().size());
    }
    
    private void processNotifications(NotificationConfigurationType notificationConfigurationType, Event event, Task task, OperationResult result){
    	

        for (EventHandlerType eventHandlerType : notificationConfigurationType.getHandler()) {
            processEvent(event, eventHandlerType, task, result);
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

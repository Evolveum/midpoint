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

package com.evolveum.midpoint.notifications.handlers;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventHandlerForkType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventHandlerType;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;

/**
 * @author mederly
 */
@Component
public class ForkHandler extends BaseHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ForkHandler.class);

    @PostConstruct
    public void init() {
        register(EventHandlerForkType.class);
    }

    @Override
    public boolean processEvent(Event event, EventHandlerType eventHandlerType, NotificationManager notificationManager, 
    		Task task, OperationResult result) {

        logStart(LOGGER, event, eventHandlerType);

        EventHandlerForkType eventHandlerForkType = (EventHandlerForkType) eventHandlerType;
        for (JAXBElement<? extends EventHandlerType> branchHandlerType : eventHandlerForkType.getHandler()) {
            notificationManager.processEvent(event, branchHandlerType.getValue(), task, result);
        }

        logEnd(LOGGER, event, eventHandlerType, true);

        return true;
    }

}

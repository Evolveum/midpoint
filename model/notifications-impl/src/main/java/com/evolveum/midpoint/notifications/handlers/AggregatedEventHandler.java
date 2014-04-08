/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import com.evolveum.midpoint.notifications.filters.CategoryFilter;
import com.evolveum.midpoint.notifications.filters.ExpressionFilter;
import com.evolveum.midpoint.notifications.filters.OperationFilter;
import com.evolveum.midpoint.notifications.filters.StatusFilter;
import com.evolveum.midpoint.notifications.notifiers.AccountPasswordNotifier;
import com.evolveum.midpoint.notifications.notifiers.GeneralNotifier;
import com.evolveum.midpoint.notifications.notifiers.SimpleAccountNotifier;
import com.evolveum.midpoint.notifications.notifiers.SimpleUserNotifier;
import com.evolveum.midpoint.notifications.notifiers.SimpleWorkflowNotifier;
import com.evolveum.midpoint.notifications.notifiers.UserPasswordNotifier;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Processor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountPasswordNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SimpleAccountNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SimpleUserNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SimpleWorkflowNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserPasswordNotifierType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;
import java.util.List;

/**
 * Handles now-aggregated event type (consisting of pointers to categories, operations, statuses, chained handlers,
 * notifiers, and so on). The original plug-in architecture had to be a bit abused because of the notifications
 * schema change.
 *
 * @author mederly
 */
@Component
public class AggregatedEventHandler extends BaseHandler {

    private static final Trace LOGGER = TraceManager.getTrace(AggregatedEventHandler.class);

    @Autowired
    private CategoryFilter categoryFilter;

    @Autowired
    private OperationFilter operationFilter;

    @Autowired
    private StatusFilter statusFilter;

    @Autowired
    private ExpressionFilter expressionFilter;

    @Autowired
    private ChainHandler chainHandler;

    @Autowired
    private ForkHandler forkHandler;

    @Autowired
    protected SimpleUserNotifier simpleUserNotifier;

    @Autowired
    protected SimpleAccountNotifier simpleAccountNotifier;

    @Autowired
    protected SimpleWorkflowNotifier simpleWorkflowNotifier;

    @Autowired
    protected UserPasswordNotifier userPasswordNotifier;

    @Autowired
    protected AccountPasswordNotifier accountPasswordNotifier;

    @Autowired
    protected GeneralNotifier generalNotifier;

    @PostConstruct
    public void init() {
        register(EventHandlerType.class);
    }

    @Override
    public boolean processEvent(Event event, EventHandlerType eventHandlerType, NotificationManager notificationManager, 
    		Task task, OperationResult result) throws SchemaException {

        logStart(LOGGER, event, eventHandlerType);

        boolean shouldContinue =
                categoryFilter.processEvent(event, eventHandlerType, notificationManager, task, result) &&
                operationFilter.processEvent(event, eventHandlerType, notificationManager, task, result) &&
                statusFilter.processEvent(event, eventHandlerType, notificationManager, task, result) &&
                expressionFilter.processEvent(event, eventHandlerType, notificationManager, task, result) &&
                chainHandler.processEvent(event, eventHandlerType, notificationManager, task, result) &&
                forkHandler.processEvent(event, eventHandlerType, notificationManager, task, result);

        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getSimpleUserNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getSimpleAccountNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getSimpleWorkflowNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getUserPasswordNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getAccountPasswordNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getGeneralNotifier(), notificationManager, task, result);

        logEnd(LOGGER, event, eventHandlerType, shouldContinue);
        return shouldContinue;
    }

    private boolean processNotifiers(Event event, List<? extends GeneralNotifierType> notifiers, NotificationManager notificationManager, Task task, OperationResult result) throws SchemaException {
        for (GeneralNotifierType generalNotifierType : notifiers) {
            boolean shouldContinue = notificationManager.getEventHandler(generalNotifierType).processEvent(event, generalNotifierType, notificationManager, task, result);
            if (!shouldContinue) {
                return false;
            }
        }
        return true;
    }
}

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

package com.evolveum.midpoint.notifications.impl.handlers;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.impl.NotificationManagerImpl;
import com.evolveum.midpoint.notifications.impl.helpers.CategoryFilterHelper;
import com.evolveum.midpoint.notifications.impl.helpers.ChainHelper;
import com.evolveum.midpoint.notifications.impl.helpers.ExpressionFilterHelper;
import com.evolveum.midpoint.notifications.impl.helpers.FocusTypeFilterHelper;
import com.evolveum.midpoint.notifications.impl.helpers.ForkHelper;
import com.evolveum.midpoint.notifications.impl.helpers.KindIntentFilterHelper;
import com.evolveum.midpoint.notifications.impl.helpers.OperationFilterHelper;
import com.evolveum.midpoint.notifications.impl.helpers.StatusFilterHelper;
import com.evolveum.midpoint.notifications.impl.notifiers.AccountPasswordNotifier;
import com.evolveum.midpoint.notifications.impl.notifiers.GeneralNotifier;
import com.evolveum.midpoint.notifications.impl.notifiers.SimpleResourceObjectNotifier;
import com.evolveum.midpoint.notifications.impl.notifiers.SimpleFocalObjectNotifier;
import com.evolveum.midpoint.notifications.impl.notifiers.SimpleUserNotifier;
import com.evolveum.midpoint.notifications.impl.notifiers.SimpleWorkflowNotifier;
import com.evolveum.midpoint.notifications.impl.notifiers.UserPasswordNotifier;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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
    private CategoryFilterHelper categoryFilter;

    @Autowired
    private OperationFilterHelper operationFilter;

    @Autowired
    private StatusFilterHelper statusFilter;

    @Autowired
    private KindIntentFilterHelper kindIntentFilter;

    @Autowired
    private FocusTypeFilterHelper focusTypeFilterHelper;

    @Autowired
    private ExpressionFilterHelper expressionFilter;

    @Autowired
    private ChainHelper chainHelper;

    @Autowired
    private ForkHelper forkHelper;

    @Autowired
    protected SimpleFocalObjectNotifier simpleFocalObjectNotifier;

    @Autowired
    protected SimpleUserNotifier simpleUserNotifier;

    @Autowired
    protected SimpleResourceObjectNotifier simpleResourceObjectNotifier;

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
                kindIntentFilter.processEvent(event, eventHandlerType, notificationManager, task, result) &&
                focusTypeFilterHelper.processEvent(event, eventHandlerType, notificationManager, task, result) &&
                expressionFilter.processEvent(event, eventHandlerType, notificationManager, task, result) &&
                chainHelper.processEvent(event, eventHandlerType, notificationManager, task, result) &&
                forkHelper.processEvent(event, eventHandlerType, notificationManager, task, result);

        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getSimpleUserNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getSimpleFocalObjectNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getSimpleResourceObjectNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getSimpleWorkflowNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getUserPasswordNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getUserRegistrationNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getPasswordResetNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getAccountPasswordNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getGeneralNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getCustomNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getSimpleCampaignNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getSimpleCampaignStageNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getSimpleReviewerNotifier(), notificationManager, task, result);
        shouldContinue = shouldContinue && processNotifiers(event, eventHandlerType.getSimpleTaskNotifier(), notificationManager, task, result);

        logEnd(LOGGER, event, eventHandlerType, shouldContinue);
        return shouldContinue;
    }

    private boolean processNotifiers(Event event, List<? extends EventHandlerType> notifiers, NotificationManager notificationManager, Task task, OperationResult result) throws SchemaException {
        for (EventHandlerType notifier : notifiers) {
            boolean shouldContinue = ((NotificationManagerImpl) notificationManager).getEventHandler(notifier).processEvent(event, notifier, notificationManager, task, result);
            if (!shouldContinue) {
                return false;
            }
        }
        return true;
    }
}

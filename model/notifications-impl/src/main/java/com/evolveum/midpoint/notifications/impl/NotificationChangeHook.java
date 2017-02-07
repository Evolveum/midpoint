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

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * One of interfaces of the notifier to midPoint.
 *
 * Used to catch user-related events.
 *
 * @author mederly
 */
@Component
public class NotificationChangeHook implements ChangeHook {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationChangeHook.class);

    public static final String HOOK_URI = "http://midpoint.evolveum.com/wf/notifier-hook-2";

    @Autowired
    private LightweightIdentifierGenerator lightweightIdentifierGenerator;

    @Autowired(required = true)
    private HookRegistry hookRegistry;

    @Autowired(required = true)
    private NotificationManager notificationManager;

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @Autowired
    private NotificationFunctionsImpl notificationsUtil;

    @PostConstruct
    public void init() {
        hookRegistry.registerChangeHook(HOOK_URI, this);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Notifier change hook registered.");
        }
    }

    @Override
    public HookOperationMode invoke(@NotNull ModelContext context, @NotNull Task task, @NotNull OperationResult result) {

        // todo in the future we should perhaps act in POSTEXECUTION state, but currently the clockwork skips this state
        if (context.getState() != ModelState.FINAL) {
            return HookOperationMode.FOREGROUND;
        }

        if (notificationManager.isDisabled()) {
            LOGGER.trace("Notifications are temporarily disabled, exiting the hook.");
            return HookOperationMode.FOREGROUND;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Notification change hook called with model context: " + context.debugDump());
        }

        if (context.getFocusContext() == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Focus context is null, exiting the hook.");
            }
            return HookOperationMode.FOREGROUND;
        }

        PrismObject object = context.getFocusContext().getObjectNew();
        if (object == null) {
            object = context.getFocusContext().getObjectOld();
        }
        if (object == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Focus context object is null, exiting the hook.");
            }
            return HookOperationMode.FOREGROUND;
        }

//        if (!UserType.class.isAssignableFrom(object.getCompileTimeClass())) {
//            if (LOGGER.isTraceEnabled()) {
//                LOGGER.trace("Focus object is not a User, exiting the hook.");
//            }
//            return HookOperationMode.FOREGROUND;
//        }

        Event event = createRequest(object, task, context);
        notificationManager.processEvent(event, task, result);

        return HookOperationMode.FOREGROUND;
    }

    @Override
    public void invokeOnException(@NotNull ModelContext context, @NotNull Throwable throwable, @NotNull Task task, @NotNull OperationResult result) {
        // todo implement this
    }

    private Event createRequest(PrismObject<? extends ObjectType> object, Task task,
                                ModelContext<UserType> modelContext) {

        ModelEvent event = new ModelEvent(lightweightIdentifierGenerator);
        event.setModelContext(modelContext);
		// TODO is this correct? it's not quite clear how we work with channel info in task / modelContext
		String channel = task.getChannel();
		if (channel == null) {
			channel = modelContext.getChannel();
		}
        event.setChannel(channel);

        if (task.getOwner() != null) {
            event.setRequester(new SimpleObjectRefImpl(notificationsUtil, task.getOwner().asObjectable()));
        } else {
            LOGGER.debug("No owner for task " + task + ", therefore no requester will be set for event " + event.getId());
        }

        // if no OID in object (occurs in 'add' operation), we artificially insert it into the object)
        if (object.getOid() == null && modelContext.getFocusContext() != null && modelContext.getFocusContext().getOid() != null) {
            object = object.clone();
            object.setOid(modelContext.getFocusContext().getOid());
        }
        event.setRequestee(new SimpleObjectRefImpl(notificationsUtil, object.asObjectable()));
        return event;
    }
}

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

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.notifications.events.ModelEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
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

    @Autowired(required = true)
    private HookRegistry hookRegistry;

    @Autowired(required = true)
    private NotificationManager notificationManager;

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @PostConstruct
    public void init() {
        hookRegistry.registerChangeHook(HOOK_URI, this);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Notifier change hook registered.");
        }
    }

    @Override
    public HookOperationMode invoke(ModelContext context, Task task, OperationResult result) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Entering notifier change hook in state " + context.getState());
        }

        // todo in the future we should perhaps act in POSTEXECUTION state, but currently the clockwork skips this state
        if (context.getState() != ModelState.FINAL) {
            return HookOperationMode.FOREGROUND;
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

        if (!UserType.class.isAssignableFrom(object.getCompileTimeClass())) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Focus object is not a User, exiting the hook.");
            }
            return HookOperationMode.FOREGROUND;
        }

        Event event = createRequest(object, task, context);
        if (event != null) {
            notificationManager.processEvent(event, result);
        }

        return HookOperationMode.FOREGROUND;
    }

    private Event createRequest(PrismObject<UserType> user, Task task,
                                ModelContext<UserType, ShadowType> modelContext) {

        ModelEvent event = new ModelEvent();
        event.setModelContext(modelContext);

        if (task.getOwner() != null) {
            event.setRequester(task.getOwner().asObjectable());
        } else {
            LOGGER.warn("No owner for task " + task + ", therefore no requester will be set for event " + event.getId());
        }

        event.setRequestee(user.asObjectable());
        event.setOperationStatus(OperationStatus.SUCCESS);          // todo recognize other states, if necessary
        return event;
    }
}

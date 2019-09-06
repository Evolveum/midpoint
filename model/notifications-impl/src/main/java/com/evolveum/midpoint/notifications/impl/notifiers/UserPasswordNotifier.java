/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserPasswordNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class UserPasswordNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(UserPasswordNotifier.class);

    @Autowired
    private NotificationFunctionsImpl notificationsUtil;

    @PostConstruct
    public void init() {
        register(UserPasswordNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof ModelEvent) || !((ModelEvent) event).hasFocusOfType(UserType.class)) {
            LOGGER.trace("UserPasswordNotifier is not applicable for this kind of event, continuing in the handler chain; event class = " + event.getClass());
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!event.isAlsoSuccess()) {       // TODO
            LOGGER.trace("Operation was not successful, exiting.");
            return false;
        }
        return event.getFocusPassword() != null;    // logging is done in the called method
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
        return "User password notification";
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
        return "Password for user " + notificationsUtil.getObjectType(event.getRequestee(), false, result).getName()
                + " is: " + event.getFocusPassword();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}

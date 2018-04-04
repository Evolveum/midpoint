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

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserPasswordNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * @author mederly
 */
@Component
public class UserPasswordNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(UserPasswordNotifier.class);

    @Autowired
    private MidpointFunctions midpointFunctions;

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
        if (!event.isSuccess()) {
            LOGGER.trace("Operation was not successful, exiting.");
            return false;
        }
        return getPasswordFromEvent((ModelEvent) event) != null;    // logging is done in the called method
    }

    private String getPasswordFromEvent(ModelEvent modelEvent) {
        if (modelEvent.getFocusDeltas().isEmpty()) {
            LOGGER.trace("No user deltas in event, exiting.");
            return null;
        }
        String password = getPasswordFromDeltas(modelEvent.getFocusDeltas());
        if (password != null) {
            LOGGER.trace("Found password in user executed delta(s), continuing.");
            return password;
        }
        //noinspection unchecked
        ObjectDelta<FocusType> focusPrimaryDelta = (ObjectDelta) modelEvent.getFocusPrimaryDelta();
        if (focusPrimaryDelta == null) {
            LOGGER.trace("No password in executed delta(s) and no primary delta, exiting.");
            return null;
        }
        password = getPasswordFromDeltas(singletonList(focusPrimaryDelta));
        if (password != null) {
            LOGGER.trace("Found password in user primary delta, continuing.");
            return password;
        } else {
            LOGGER.trace("No password in executed delta(s) nor in primary delta, exiting.");
            return null;
        }
    }

    private String getPasswordFromDeltas(List<ObjectDelta<FocusType>> deltas) {
        try {
            //noinspection unchecked
            return midpointFunctions.getPlaintextUserPasswordFromDeltas((List) deltas);
        } catch (EncryptionException e) {
            LoggingUtils.logException(LOGGER, "Couldn't decrypt password from user deltas: {}", e, DebugUtil.debugDump(deltas));
            return null;
        }
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
        return "User password notification";
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
        return "Password for user " + notificationsUtil.getObjectType(event.getRequestee(), false, result).getName()
                + " is: " + getPasswordFromEvent((ModelEvent) event);
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}

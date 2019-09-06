/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleUserNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class SimpleUserNotifier extends SimpleFocalObjectNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleUserNotifier.class);

    @PostConstruct
    public void init() {
        register(SimpleUserNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!super.quickCheckApplicability(event, generalNotifierType, result)) {
            return false;
        }
        if (!((ModelEvent) event).getFocusContext().isOfType(UserType.class)) {
            LOGGER.trace("Focus context type is not of UserType; skipping the notification");
            return false;
        }
        return true;
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}

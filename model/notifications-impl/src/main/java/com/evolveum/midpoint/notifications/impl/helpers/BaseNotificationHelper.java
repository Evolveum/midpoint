/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.helpers;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */
@Component
public abstract class BaseNotificationHelper {

    protected void logStart(Trace LOGGER, Event event, EventHandlerType eventHandlerType) {
        logStart(LOGGER, event, eventHandlerType, null);
    }

    protected void logStart(Trace LOGGER, Event event, EventHandlerType eventHandlerType, Object additionalData) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Starting processing event " + event + " with handler " +
                    eventHandlerType.getClass() + " (name: " + eventHandlerType.getName() +
                    (additionalData != null ? (", parameters: " + additionalData) :
                                             (", configuration: " + eventHandlerType)) +
                    ")");
        }
    }

    public void logEnd(Trace LOGGER, Event event, EventHandlerType eventHandlerType, boolean result) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Finishing processing event " + event + " result = " + result);
        }
    }

}

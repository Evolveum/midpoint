/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.helpers;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BaseEventHandlerType;

@Component
public abstract class BaseNotificationHelper {

    protected void logStart(Trace LOGGER, Event event, BaseEventHandlerType eventHandlerConfig) {
        logStart(LOGGER, event, eventHandlerConfig, null);
    }

    protected void logStart(Trace LOGGER, Event event, BaseEventHandlerType eventHandlerConfig, Object additionalData) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Starting processing event " + event + " with handler " +
                    eventHandlerConfig.getClass() + " (name: " + eventHandlerConfig.getName() +
                    (additionalData != null ? (", parameters: " + additionalData) :
                            (", configuration: " + eventHandlerConfig)) +
                    ")");
        }
    }

    public void logEnd(Trace LOGGER, Event event, BaseEventHandlerType eventHandlerConfig, boolean result) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Finishing processing event " + event + " result = " + result);
        }
    }
}

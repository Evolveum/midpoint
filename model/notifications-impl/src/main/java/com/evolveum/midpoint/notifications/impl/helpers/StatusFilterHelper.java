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
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BaseEventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;

@Component
public class StatusFilterHelper extends BaseNotificationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(StatusFilterHelper.class);

    public boolean processEvent(Event event, BaseEventHandlerType eventHandlerConfig) {

        if (eventHandlerConfig.getStatus().isEmpty()) {
            return true;
        }

        logStart(LOGGER, event, eventHandlerConfig, eventHandlerConfig.getStatus());

        boolean retval = false;

        for (EventStatusType eventStatusType : eventHandlerConfig.getStatus()) {
            if (eventStatusType == null) {
                LOGGER.warn("Filtering on null eventStatusType; filter = " + eventHandlerConfig);
            } else if (event.isStatusType(eventStatusType)) {
                retval = true;
                break;
            }
        }

        logEnd(LOGGER, event, eventHandlerConfig, retval);
        return retval;
    }
}

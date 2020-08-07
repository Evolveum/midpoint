/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.helpers;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */
@Component
public class StatusFilterHelper extends BaseNotificationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(StatusFilterHelper.class);

    public boolean processEvent(Event event, EventHandlerType eventHandlerType) {

        if (eventHandlerType.getStatus().isEmpty()) {
            return true;
        }

        logStart(LOGGER, event, eventHandlerType, eventHandlerType.getStatus());

        boolean retval = false;

        for (EventStatusType eventStatusType : eventHandlerType.getStatus()) {
            if (eventStatusType == null) {
                LOGGER.warn("Filtering on null eventStatusType; filter = " + eventHandlerType);
            } else if (event.isStatusType(eventStatusType)) {
                retval = true;
                break;
            }
        }

        logEnd(LOGGER, event, eventHandlerType, retval);
        return retval;
    }
}

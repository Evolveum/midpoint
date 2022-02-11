/*
 * Copyright (c) 2010-2013 Evolveum and contributors
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;

@Component
public class OperationFilterHelper extends BaseNotificationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(OperationFilterHelper.class);

    public boolean processEvent(Event event, BaseEventHandlerType eventHandlerConfig) {

        if (eventHandlerConfig.getOperation().isEmpty()) {
            return true;
        }

        logStart(LOGGER, event, eventHandlerConfig, eventHandlerConfig.getOperation());

        boolean retval = false;

        for (EventOperationType eventOperationType : eventHandlerConfig.getOperation()) {
            if (eventOperationType == null) {
                LOGGER.warn("Filtering on null eventOperationType; filter = " + eventHandlerConfig);
            } else if (event.isOperationType(eventOperationType)) {
                retval = true;
                break;
            }
        }

        logEnd(LOGGER, event, eventHandlerConfig, retval);
        return retval;
    }
}

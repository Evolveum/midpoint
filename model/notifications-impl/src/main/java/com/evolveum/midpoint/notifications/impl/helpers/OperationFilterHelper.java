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
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */
@Component
public class OperationFilterHelper extends BaseHelper {

    private static final Trace LOGGER = TraceManager.getTrace(OperationFilterHelper.class);

    public boolean processEvent(Event event, EventHandlerType eventHandlerType) {

        if (eventHandlerType.getOperation().isEmpty()) {
            return true;
        }

        logStart(LOGGER, event, eventHandlerType, eventHandlerType.getOperation());

        boolean retval = false;

        for (EventOperationType eventOperationType : eventHandlerType.getOperation()) {
            if (eventOperationType == null) {
                LOGGER.warn("Filtering on null eventOperationType; filter = " + eventHandlerType);
            } else if (event.isOperationType(eventOperationType)) {
                retval = true;
                break;
            }
        }

        logEnd(LOGGER, event, eventHandlerType, retval);
        return retval;
    }
}

/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.helpers;

import javax.xml.namespace.QName;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;

@Component
public class FocusTypeFilterHelper extends BaseHelper {

    private static final Trace LOGGER = TraceManager.getTrace(FocusTypeFilterHelper.class);

    public boolean processEvent(Event event, EventHandlerType eventHandlerType) {

        if (eventHandlerType.getFocusType().isEmpty()) {
            return true;
        }

        if (!(event instanceof ModelEvent)) {
            return true;            // or should we return false?
        }
        ModelEvent modelEvent = (ModelEvent) event;

        logStart(LOGGER, event, eventHandlerType, eventHandlerType.getStatus());

        boolean retval = false;

        for (QName focusType : eventHandlerType.getFocusType()) {
            if (focusType == null) {
                LOGGER.warn("Filtering on null focusType; filter = " + eventHandlerType);
            } else if (modelEvent.hasFocusOfType(focusType)) {
                retval = true;
                break;
            }
        }

        logEnd(LOGGER, event, eventHandlerType, retval);
        return retval;
    }
}

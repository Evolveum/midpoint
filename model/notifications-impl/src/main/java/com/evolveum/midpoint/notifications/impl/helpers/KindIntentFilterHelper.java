/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.helpers;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ResourceObjectEvent;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

@Component
public class KindIntentFilterHelper extends BaseHelper {

    private static final Trace LOGGER = TraceManager.getTrace(KindIntentFilterHelper.class);

    public boolean processEvent(Event event, EventHandlerType eventHandlerType) {

        if (eventHandlerType.getObjectKind().isEmpty() && eventHandlerType.getObjectIntent().isEmpty()) {
            return true;
        }

        if (!(event instanceof ResourceObjectEvent)) {
            return true;            // or should we return false?
        }
        ResourceObjectEvent resourceObjectEvent = (ResourceObjectEvent) event;

        logStart(LOGGER, event, eventHandlerType, eventHandlerType.getStatus());

        boolean retval = true;

        if (!eventHandlerType.getObjectKind().isEmpty()) {
            retval = false;
            for (ShadowKindType shadowKindType : eventHandlerType.getObjectKind()) {
                if (shadowKindType == null) {
                    LOGGER.warn("Filtering on null shadowKindType; filter = " + eventHandlerType);
                } else if (resourceObjectEvent.isShadowKind(shadowKindType)) {
                    retval = true;
                    break;
                }
            }
        }

        if (retval) {
            // now check the intent
            if (!eventHandlerType.getObjectIntent().isEmpty()) {
                retval = false;
                for (String intent : eventHandlerType.getObjectIntent()) {
                    if (intent == null) {
                        LOGGER.warn("Filtering on null intent; filter = " + eventHandlerType);
                    } else if (resourceObjectEvent.isShadowIntent(intent)) {
                        retval = true;
                        break;
                    }
                }
            }
        }

        logEnd(LOGGER, event, eventHandlerType, retval);
        return retval;
    }
}

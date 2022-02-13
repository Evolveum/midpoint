/*
 * Copyright (C) 2010-2022 Evolveum and contributors
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.BaseEventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

@Component
public class KindIntentFilterHelper extends BaseNotificationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(KindIntentFilterHelper.class);

    public boolean processEvent(Event event, BaseEventHandlerType eventHandlerConfig) {

        if (eventHandlerConfig.getObjectKind().isEmpty() && eventHandlerConfig.getObjectIntent().isEmpty()) {
            return true;
        }

        if (!(event instanceof ResourceObjectEvent)) {
            return true;            // or should we return false?
        }
        ResourceObjectEvent resourceObjectEvent = (ResourceObjectEvent) event;

        logStart(LOGGER, event, eventHandlerConfig, eventHandlerConfig.getStatus());

        boolean retval = true;

        if (!eventHandlerConfig.getObjectKind().isEmpty()) {
            retval = false;
            for (ShadowKindType shadowKindType : eventHandlerConfig.getObjectKind()) {
                if (shadowKindType == null) {
                    LOGGER.warn("Filtering on null shadowKindType; filter = " + eventHandlerConfig);
                } else if (resourceObjectEvent.isShadowKind(shadowKindType)) {
                    retval = true;
                    break;
                }
            }
        }

        if (retval) {
            // now check the intent
            if (!eventHandlerConfig.getObjectIntent().isEmpty()) {
                retval = false;
                for (String intent : eventHandlerConfig.getObjectIntent()) {
                    if (intent == null) {
                        LOGGER.warn("Filtering on null intent; filter = " + eventHandlerConfig);
                    } else if (resourceObjectEvent.isShadowIntent(intent)) {
                        retval = true;
                        break;
                    }
                }
            }
        }

        logEnd(LOGGER, event, eventHandlerConfig, retval);
        return retval;
    }
}

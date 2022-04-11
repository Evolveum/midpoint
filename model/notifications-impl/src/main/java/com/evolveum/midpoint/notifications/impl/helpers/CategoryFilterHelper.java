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
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;

@Component
public class CategoryFilterHelper extends BaseNotificationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(CategoryFilterHelper.class);

    public boolean processEvent(Event event, BaseEventHandlerType eventHandlerConfig) {

        if (eventHandlerConfig.getCategory().isEmpty()) {
            return true;
        }

        boolean retval = false;

        logStart(LOGGER, event, eventHandlerConfig, eventHandlerConfig.getCategory());

        for (EventCategoryType eventCategoryType : eventHandlerConfig.getCategory()) {

            if (eventCategoryType == null) {
                LOGGER.warn("Filtering on null EventCategoryType: " + eventHandlerConfig);
            } else if (event.isCategoryType(eventCategoryType)) {
                retval = true;
                break;
            }
        }

        logEnd(LOGGER, event, eventHandlerConfig, retval);
        return retval;
    }
}

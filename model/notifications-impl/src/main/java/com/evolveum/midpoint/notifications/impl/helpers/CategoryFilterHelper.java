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
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */
@Component
public class CategoryFilterHelper extends BaseNotificationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(CategoryFilterHelper.class);

    public boolean processEvent(Event event, EventHandlerType eventHandlerType) {

        if (eventHandlerType.getCategory().isEmpty()) {
            return true;
        }

        boolean retval = false;

        logStart(LOGGER, event, eventHandlerType, eventHandlerType.getCategory());

        for (EventCategoryType eventCategoryType : eventHandlerType.getCategory()) {

            if (eventCategoryType == null) {
                LOGGER.warn("Filtering on null EventCategoryType: " + eventHandlerType);
            } else if (event.isCategoryType(eventCategoryType)) {
                retval = true;
                break;
            }
        }

        logEnd(LOGGER, event, eventHandlerType, retval);
        return retval;
    }
}

/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl.helpers;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BaseEventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;

@Component
public class CategoryFilterHelper extends BaseNotificationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(CategoryFilterHelper.class);

    public boolean processEvent(
            ConfigurationItem<? extends BaseEventHandlerType> eventHandlerConfig, EventProcessingContext<?> ctx) {

        var categories = eventHandlerConfig.value().getCategory();
        if (categories.isEmpty()) {
            return true;
        }

        boolean retval = false;

        logStart(LOGGER, eventHandlerConfig, ctx, categories);

        for (EventCategoryType category : categories) {
            if (category == null) {
                LOGGER.warn("Filtering on null EventCategoryType: " + eventHandlerConfig);
            } else if (ctx.event().isCategoryType(category)) {
                retval = true;
                break;
            }
        }

        logEnd(LOGGER, eventHandlerConfig, ctx, retval);
        return retval;
    }
}

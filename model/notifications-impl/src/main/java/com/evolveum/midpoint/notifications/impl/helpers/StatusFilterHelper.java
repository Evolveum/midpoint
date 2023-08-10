/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.helpers;

import java.util.List;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BaseEventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;

@Component
public class StatusFilterHelper extends BaseNotificationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(StatusFilterHelper.class);

    public boolean processEvent(
            ConfigurationItem<? extends BaseEventHandlerType> handlerConfig, EventProcessingContext<?> ctx) {

        List<EventStatusType> statuses = handlerConfig.value().getStatus();
        if (statuses.isEmpty()) {
            return true;
        }

        logStart(LOGGER, handlerConfig, ctx, statuses);

        boolean retval = false;

        for (EventStatusType status : statuses) {
            if (status == null) {
                LOGGER.warn("Filtering on null eventStatusType; filter = " + handlerConfig);
            } else if (ctx.event().isStatusType(status)) {
                retval = true;
                break;
            }
        }

        logEnd(LOGGER, handlerConfig, ctx, retval);
        return retval;
    }
}

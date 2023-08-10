/*
 * Copyright (c) 2010-2013 Evolveum and contributors
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;

@Component
public class OperationFilterHelper extends BaseNotificationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(OperationFilterHelper.class);

    public boolean processEvent(
            ConfigurationItem<? extends BaseEventHandlerType> handlerConfig, EventProcessingContext<?> ctx) {

        List<EventOperationType> operations = handlerConfig.value().getOperation();
        if (operations.isEmpty()) {
            return true;
        }

        logStart(LOGGER, handlerConfig, ctx, operations);

        boolean retval = false;

        for (EventOperationType operation : operations) {
            if (operation == null) {
                LOGGER.warn("Filtering on null eventOperationType; filter = " + handlerConfig);
            } else if (ctx.event().isOperationType(operation)) {
                retval = true;
                break;
            }
        }

        logEnd(LOGGER, handlerConfig, ctx, retval);
        return retval;
    }
}

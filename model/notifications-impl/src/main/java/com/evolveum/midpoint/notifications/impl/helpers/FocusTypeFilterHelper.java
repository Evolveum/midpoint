/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.helpers;

import java.util.List;
import javax.xml.namespace.QName;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BaseEventHandlerType;

@Component
public class FocusTypeFilterHelper extends BaseNotificationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(FocusTypeFilterHelper.class);

    public boolean processEvent(
            ConfigurationItem<? extends BaseEventHandlerType> handlerConfig, EventProcessingContext<?> ctx) {
        List<QName> focusTypes = handlerConfig.value().getFocusType();
        if (focusTypes.isEmpty()) {
            return true;
        }

        if (!(ctx.event() instanceof ModelEvent modelEvent)) {
            return true; // or should we return false?
        }

        logStart(LOGGER, handlerConfig, ctx, focusTypes);

        boolean retval = false;

        for (QName focusType : focusTypes) {
            if (focusType == null) {
                LOGGER.warn("Filtering on null focusType; filter = " + handlerConfig);
            } else if (modelEvent.hasFocusOfType(focusType)) {
                retval = true;
                break;
            }
        }

        logEnd(LOGGER, handlerConfig, ctx, retval);
        return retval;
    }
}

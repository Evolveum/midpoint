/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.helpers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BaseEventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

@Component
public class ExpressionFilterHelper extends BaseNotificationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionFilterHelper.class);

    @Autowired private NotificationExpressionHelper expressionHelper;

    public boolean processEvent(Event event, BaseEventHandlerType eventHandlerConfig, Task task, OperationResult result) {

        if (eventHandlerConfig.getExpressionFilter().isEmpty()) {
            return true;
        }

        logStart(LOGGER, event, eventHandlerConfig, eventHandlerConfig.getExpressionFilter());

        boolean retval = true;

        for (ExpressionType expressionType : eventHandlerConfig.getExpressionFilter()) {
            if (!expressionHelper.evaluateBooleanExpressionChecked(
                    expressionType,
                    expressionHelper.getDefaultVariables(event, result),
                    "event filter expression",
                    task,
                    result)) {
                retval = false;
                break;
            }
        }

        logEnd(LOGGER, event, eventHandlerConfig, retval);
        return retval;
    }
}

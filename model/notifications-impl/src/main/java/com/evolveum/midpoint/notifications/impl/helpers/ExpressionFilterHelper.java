/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.helpers;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */
@Component
public class ExpressionFilterHelper extends BaseHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionFilterHelper.class);

    @Autowired private NotificationExpressionHelper expressionHelper;

    public boolean processEvent(Event event, EventHandlerType eventHandlerType, Task task, OperationResult result) {

        if (eventHandlerType.getExpressionFilter().isEmpty()) {
            return true;
        }

        logStart(LOGGER, event, eventHandlerType, eventHandlerType.getExpressionFilter());

        boolean retval = true;

        for (ExpressionType expressionType : eventHandlerType.getExpressionFilter()) {
            if (!expressionHelper.evaluateBooleanExpressionChecked(expressionType,
                    expressionHelper.getDefaultVariables(event, result), "event filter expression", task, result)) {
                retval = false;
                break;
            }
        }

        logEnd(LOGGER, event, eventHandlerType, retval);
        return retval;
    }
}

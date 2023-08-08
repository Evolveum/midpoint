/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.helpers;

import java.util.List;

import com.evolveum.midpoint.schema.config.ExpressionConfigItem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BaseEventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

@Component
public class ExpressionFilterHelper extends BaseNotificationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionFilterHelper.class);

    @Autowired private NotificationExpressionHelper expressionHelper;

    public boolean processEvent(
            ConfigurationItem<? extends BaseEventHandlerType> handlerConfig,
            EventProcessingContext<?> ctx,
            OperationResult result) {

        List<ExpressionType> filters = handlerConfig.value().getExpressionFilter();
        if (filters.isEmpty()) {
            return true;
        }

        logStart(LOGGER, handlerConfig, ctx, filters);

        boolean retval = true;

        for (ExpressionType filter : filters) {
            if (!expressionHelper.evaluateBooleanExpressionChecked(
                    ExpressionConfigItem.of(
                            filter,
                            handlerConfig.origin().toApproximate()), // TODO provide precise (child) origin here
                    expressionHelper.getDefaultVariables(ctx.event(), result),
                    "event filter expression",
                    ctx, result)) {
                retval = false;
                break;
            }
        }

        logEnd(LOGGER, handlerConfig, ctx, retval);
        return retval;
    }
}

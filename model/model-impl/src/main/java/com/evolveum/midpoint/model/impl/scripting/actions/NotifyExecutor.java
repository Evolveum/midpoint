/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.CustomEvent;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class NotifyExecutor extends BaseActionExecutor {

    @Autowired
    private LightweightIdentifierGenerator lightweightIdentifierGenerator;

    @Autowired(required = false)                            // During some tests this might be unavailable
    private NotificationManager notificationManager;

    private static final Trace LOGGER = TraceManager.getTrace(NotifyExecutor.class);

    private static final String NAME = "notify";
    private static final String PARAM_SUBTYPE = "subtype";
    private static final String PARAM_HANDLER = "handler";
    private static final String PARAM_STATUS = "status";
    private static final String PARAM_OPERATION = "operation";
    private static final String PARAM_FOR_WHOLE_INPUT = "forWholeInput";

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {

        String subtype = expressionHelper.getArgumentAsString(expression.getParameter(), PARAM_SUBTYPE, input, context, null, PARAM_SUBTYPE, globalResult);
        EventHandlerType handler = expressionHelper.getSingleArgumentValue(expression.getParameter(), PARAM_HANDLER, false, false,
                PARAM_HANDLER, input, context, EventHandlerType.class, globalResult);
        EventStatusType status = expressionHelper.getSingleArgumentValue(expression.getParameter(), PARAM_STATUS, false, false,
                PARAM_STATUS, input, context, EventStatusType.class, globalResult);
        EventOperationType operation = expressionHelper.getSingleArgumentValue(expression.getParameter(), PARAM_OPERATION, false, false,
                PARAM_OPERATION, input, context, EventOperationType.class, globalResult);
        boolean forWholeInput = expressionHelper.getArgumentAsBoolean(expression.getParameter(), PARAM_FOR_WHOLE_INPUT, input, context, false, PARAM_SUBTYPE, globalResult);

        if (handler != null) {
			checkRootAuthorization(globalResult, NAME);		// TODO explain that the reason is that handler is not null
		}

        if (status == null) {
            status = EventStatusType.SUCCESS;
        }
        if (operation == null) {
            operation = EventOperationType.ADD;
        }

        if (notificationManager == null) {
            throw new IllegalStateException("Notification manager is unavailable");
        }

        int eventCount = 0;
        if (forWholeInput) {
            Event event = new CustomEvent(lightweightIdentifierGenerator, subtype, handler, input.getData(), operation, status, context.getChannel());
            notificationManager.processEvent(event, context.getTask(), globalResult);
            eventCount++;
        } else {
            for (PipelineItem item: input.getData()) {
                PrismValue value = item.getValue();
                OperationResult result = operationsHelper.createActionResult(item, this, context, globalResult);
                context.checkTaskStop();
                Event event = new CustomEvent(lightweightIdentifierGenerator, subtype, handler, value, operation, status, context.getChannel());
                notificationManager.processEvent(event, context.getTask(), result);
                eventCount++;
                operationsHelper.trimAndCloneResult(result, globalResult, context);
            }
        }
        context.println("Produced " + eventCount + " event(s)");
        return input;
    }
}

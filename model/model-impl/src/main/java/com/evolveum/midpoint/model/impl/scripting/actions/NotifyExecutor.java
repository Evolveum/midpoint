/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicInteger;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.CustomEvent;
import com.evolveum.midpoint.notifications.api.events.factory.CustomEventFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.NotifyActionExpressionType;

/**
 * Executes "notify" actions.
 */
@Component
public class NotifyExecutor extends BaseActionExecutor {

    @Autowired(required = false) // During some tests this might be unavailable
    private NotificationManager notificationManager;

    @Autowired(required = false)
    private CustomEventFactory customEventFactory;

    private static final String NAME = "notify";
    private static final String PARAM_SUBTYPE = "subtype";
    private static final String PARAM_HANDLER = "handler";
    private static final String PARAM_STATUS = "status";
    private static final String PARAM_OPERATION = "operation";
    private static final String PARAM_FOR_WHOLE_INPUT = "forWholeInput";

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(NAME, NotifyActionExpressionType.class, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType action, PipelineData input, ExecutionContext context,
            OperationResult globalResult) throws ScriptExecutionException, SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        String subtype = expressionHelper.getActionArgument(String.class, action,
                NotifyActionExpressionType.F_SUBTYPE, PARAM_SUBTYPE, input, context, null, PARAM_SUBTYPE, globalResult);
        EventHandlerType handler = expressionHelper.getActionArgument(EventHandlerType.class, action,
                NotifyActionExpressionType.F_HANDLER, PARAM_HANDLER, input, context, null, PARAM_HANDLER, globalResult);
        EventStatusType status = expressionHelper.getActionArgument(EventStatusType.class, action,
                NotifyActionExpressionType.F_STATUS, PARAM_STATUS, input, context, EventStatusType.SUCCESS, PARAM_STATUS, globalResult);
        EventOperationType operation = expressionHelper.getActionArgument(EventOperationType.class, action,
                NotifyActionExpressionType.F_OPERATION, PARAM_OPERATION, input, context, EventOperationType.ADD, PARAM_OPERATION, globalResult);
        boolean forWholeInput = expressionHelper.getActionArgument(Boolean.class, action,
                NotifyActionExpressionType.F_FOR_WHOLE_INPUT, PARAM_FOR_WHOLE_INPUT, input, context, false, PARAM_FOR_WHOLE_INPUT, globalResult);

        if (handler != null) {
            checkRootAuthorization(context, globalResult, NAME); // TODO explain that the reason is that handler is not null
        }

        requireNonNull(notificationManager, "Notification manager is unavailable");
        requireNonNull(customEventFactory, "Custom event factory is unavailable");

        AtomicInteger eventCount = new AtomicInteger();
        if (forWholeInput) {
            CustomEvent event = customEventFactory.createEvent(subtype, handler, input.getData(), operation, status, context.getChannel());
            notificationManager.processEvent(event, context.getTask(), globalResult);
            eventCount.incrementAndGet();
        } else {
            iterateOverItems(input, context, globalResult,
                    (value, item, result) -> {
                        CustomEvent event = customEventFactory.createEvent(subtype, handler, value, operation, status, context.getChannel());
                        notificationManager.processEvent(event, context.getTask(), result);
                        eventCount.incrementAndGet();
                    },
                    (value, exception) -> {
                        context.println("Failed to notify on " + getDescription(value) + exceptionSuffix(exception));
                    });
        }
        context.println("Produced " + eventCount.get() + " event(s)");
        return input;
    }

    @Override
    String getActionName() {
        return NAME;
    }
}

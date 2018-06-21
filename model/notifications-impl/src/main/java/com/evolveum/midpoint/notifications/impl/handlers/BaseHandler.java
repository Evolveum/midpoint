/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.notifications.impl.handlers;

import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.notifications.api.EventHandler;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.impl.NotificationManagerImpl;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

import java.util.*;

/**
 * TODO remove duplicate code
 *
 * @author mederly
 */
@Component
public abstract class BaseHandler implements EventHandler {

    private static final Trace LOGGER = TraceManager.getTrace(BaseHandler.class);

    @Autowired
    protected NotificationManagerImpl notificationManager;

    @Autowired
    protected NotificationFunctionsImpl notificationsUtil;

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected ExpressionFactory expressionFactory;

    protected void register(Class<? extends EventHandlerType> clazz) {
        notificationManager.registerEventHandler(clazz, this);
    }

    protected void logStart(Trace LOGGER, Event event, EventHandlerType eventHandlerType) {
        logStart(LOGGER, event, eventHandlerType, null);
    }

    protected void logStart(Trace LOGGER, Event event, EventHandlerType eventHandlerType, Object additionalData) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Starting processing event " + event.shortDump() + " with handler " +
            		getHumanReadableHandlerDescription(eventHandlerType) + "\n  parameters: " +
            		(additionalData != null ? ("\n  parameters: " + additionalData) :
                        ("\n  configuration: " + eventHandlerType)));

        }
    }

    protected void logNotApplicable(Event event, String reason) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(
				"{} is not applicable for event {}, continuing in the handler chain; reason: {}",
				this.getClass().getSimpleName(), event.shortDump(), reason);
		}
	}

    protected String getHumanReadableHandlerDescription(EventHandlerType eventHandlerType) {
    	if (eventHandlerType.getName() != null) {
    		return eventHandlerType.getName();
    	} else {
    		return eventHandlerType.getClass().getSimpleName();
    	}
	}

	public static void staticLogStart(Trace LOGGER, Event event, String description, Object additionalData) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Starting processing event " + event + " with handler " +
                    description +
                    (additionalData != null ? (", parameters: " + additionalData) : ""));
        }
    }

    public void logEnd(Trace LOGGER, Event event, EventHandlerType eventHandlerType, boolean result) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Finishing processing event " + event + " result = " + result);
        }
    }

    // expressions

    // shortDesc = what is to be evaluated e.g. "event filter expression"
    protected boolean evaluateBooleanExpressionChecked(ExpressionType expressionType, ExpressionVariables expressionVariables,
    		String shortDesc, Task task, OperationResult result) {

        Throwable failReason;
        try {
            return evaluateBooleanExpression(expressionType, expressionVariables, shortDesc, task, result);
        } catch (ObjectNotFoundException | SchemaException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            failReason = e;
        }

        LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", failReason, shortDesc, expressionType);
        result.recordFatalError("Couldn't evaluate " + shortDesc, failReason);
        throw new SystemException(failReason);
    }

    protected boolean evaluateBooleanExpression(ExpressionType expressionType, ExpressionVariables expressionVariables, String shortDesc,
    		Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<Boolean> resultDef = new PrismPropertyDefinitionImpl<>(resultName, DOMUtil.XSD_BOOLEAN, prismContext);
        Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> expression = expressionFactory.makeExpression(expressionType, resultDef, shortDesc, task, result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, shortDesc, task, result);
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> exprResultTriple = ModelExpressionThreadLocalHolder.evaluateExpressionInContext(expression, params, task, result);

        Collection<PrismPropertyValue<Boolean>> exprResult = exprResultTriple.getZeroSet();
        if (exprResult.size() == 0) {
            return false;
        } else if (exprResult.size() > 1) {
            throw new IllegalStateException("Filter expression should return exactly one boolean value; it returned " + exprResult.size() + " ones");
        }
        Boolean boolResult = exprResult.iterator().next().getValue();
        return boolResult != null ? boolResult : false;
    }

    protected List<String> evaluateExpressionChecked(ExpressionType expressionType, ExpressionVariables expressionVariables,
    		String shortDesc, Task task, OperationResult result) {

        Throwable failReason;
        try {
            return evaluateExpression(expressionType, expressionVariables, shortDesc, task, result);
        } catch (ObjectNotFoundException | SchemaException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            failReason = e;
        }

        LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", failReason, shortDesc, expressionType);
        result.recordFatalError("Couldn't evaluate " + shortDesc, failReason);
        throw new SystemException(failReason);
    }

    private List<String> evaluateExpression(ExpressionType expressionType, ExpressionVariables expressionVariables,
    		String shortDesc, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<String> resultDef = new PrismPropertyDefinitionImpl<>(resultName, DOMUtil.XSD_STRING, prismContext);
        resultDef.setMaxOccurs(-1);

        Expression<PrismPropertyValue<String>,PrismPropertyDefinition<String>> expression = expressionFactory.makeExpression(expressionType, resultDef, shortDesc, task, result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, shortDesc, task, result);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> exprResult = ModelExpressionThreadLocalHolder
				.evaluateExpressionInContext(expression, params, task, result);

        List<String> retval = new ArrayList<>();
        for (PrismPropertyValue<String> item : exprResult.getZeroSet()) {
            retval.add(item.getValue());
        }
        return retval;
    }

    protected ExpressionVariables getDefaultVariables(Event event, OperationResult result) {
    	ExpressionVariables expressionVariables = new ExpressionVariables();
        Map<QName, Object> variables = new HashMap<>();
		event.createExpressionVariables(variables, result);
		expressionVariables.addVariableDefinitions(variables);
        return expressionVariables;
    }



}

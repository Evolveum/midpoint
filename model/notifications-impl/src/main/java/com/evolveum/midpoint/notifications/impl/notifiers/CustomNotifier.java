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

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.notifications.impl.api.transports.CustomTransport;
import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
import com.evolveum.midpoint.notifications.impl.handlers.AggregatedEventHandler;
import com.evolveum.midpoint.notifications.impl.handlers.BaseHandler;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CustomNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationMessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.NOTIFICATIONS;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;

/**
 * @author mederly
 */
@Component
public class CustomNotifier extends BaseHandler {

    private static final Trace DEFAULT_LOGGER = TraceManager.getTrace(CustomNotifier.class);

    @Autowired
    protected NotificationManager notificationManager;

    @Autowired
    protected NotificationFunctionsImpl notificationsUtil;

    @Autowired
    protected TextFormatter textFormatter;

    @Autowired
    protected AggregatedEventHandler aggregatedEventHandler;

    @Autowired
	private CustomTransport customTransport;

    @PostConstruct
    public void init() {
        register(CustomNotifierType.class);
    }

    @Override
    public boolean processEvent(Event event, EventHandlerType eventHandlerType, NotificationManager notificationManager, 
    		Task task, OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createSubresult(CustomNotifier.class.getName() + ".processEvent");

        logStart(getLogger(), event, eventHandlerType);

        boolean applies = aggregatedEventHandler.processEvent(event, eventHandlerType, notificationManager, task, result);

        if (applies) {
            CustomNotifierType config = (CustomNotifierType) eventHandlerType;
            ExpressionVariables variables = getDefaultVariables(event, result);

			if (event instanceof ModelEvent) {
				((ModelEvent) event).getModelContext().reportProgress(new ProgressInformation(NOTIFICATIONS, ENTERING));
			}

			List<String> transports = new ArrayList<>(config.getTransport());
			if (transports.isEmpty()) {
				transports.add(customTransport.getName());
			}

			try {
				for (String transportName : config.getTransport()) {
					variables.addVariableDefinition(SchemaConstants.C_TRANSPORT_NAME, transportName);
					Transport transport = notificationManager.getTransport(transportName);

					Message message = getMessageFromExpression(config, variables, task, result);
					if (message != null) {
						getLogger().trace("Sending notification via transport {}:\n{}", transportName, message);
						transport.send(message, transportName, event, task, result);
					} else {
						getLogger().debug("No message for transport {}, won't send anything", transportName);
					}
				}
			} finally {
				if (event instanceof ModelEvent) {
					((ModelEvent) event).getModelContext().reportProgress(
							new ProgressInformation(NOTIFICATIONS, result));
				}
			}
        }
        logEnd(getLogger(), event, eventHandlerType, applies);
        result.computeStatusIfUnknown();
        return true;            // not-applicable notifiers do not stop processing of other notifiers
    }

    protected Trace getLogger() {
        return DEFAULT_LOGGER;              // in case a subclass does not provide its own logger
    }

    private Message getMessageFromExpression(CustomNotifierType config, ExpressionVariables variables,
			Task task, OperationResult result) {
        if (config.getExpression() == null) {
			return null;
		}
		List<NotificationMessageType> messages;
		try {
			messages = evaluateExpression(config.getExpression(), variables,
					"message expression", task, result);
		} catch (ObjectNotFoundException|SchemaException|ExpressionEvaluationException e) {
			throw new SystemException("Couldn't evaluate custom notifier expression: " + e.getMessage(), e);
		}
		if (messages == null || messages.isEmpty()) {
			return null;
		} else if (messages.size() > 1) {
			getLogger().warn("Custom notifier returned more than one message: {}", messages);
		}
		return messages.get(0) != null ? new Message(messages.get(0)) : null;
    }

	private List<NotificationMessageType> evaluateExpression(ExpressionType expressionType, ExpressionVariables expressionVariables,
			String shortDesc, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException,
			ExpressionEvaluationException {

		QName resultName = new QName(SchemaConstants.NS_C, "result");
		PrismPropertyDefinition<NotificationMessageType> resultDef =
				new PrismPropertyDefinitionImpl<>(resultName, NotificationMessageType.COMPLEX_TYPE, prismContext);

		Expression<PrismPropertyValue<NotificationMessageType>,PrismPropertyDefinition<NotificationMessageType>> expression =
				expressionFactory.makeExpression(expressionType, resultDef, shortDesc, task, result);
		ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, shortDesc, task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<NotificationMessageType>> exprResult = ModelExpressionThreadLocalHolder
				.evaluateExpressionInContext(expression, params, task, result);
		return exprResult.getZeroSet().stream().map(PrismPropertyValue::getValue).collect(Collectors.toList());
	}

	@Override
    protected ExpressionVariables getDefaultVariables(Event event, OperationResult result) {
        ExpressionVariables variables = super.getDefaultVariables(event, result);
        variables.addVariableDefinition(SchemaConstants.C_TEXT_FORMATTER, textFormatter);
        return variables;
    }

}

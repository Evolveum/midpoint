/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
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

        OperationResult result = parentResult.createMinorSubresult(CustomNotifier.class.getName() + ".processEvent");

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
					variables.put(ExpressionConstants.VAR_TRANSPORT_NAME, transportName, String.class);
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
		} catch (ObjectNotFoundException | SchemaException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
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
			ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

		QName resultName = new QName(SchemaConstants.NS_C, "result");
		PrismPropertyDefinition<NotificationMessageType> resultDef =
				prismContext.definitionFactory().createPropertyDefinition(resultName, NotificationMessageType.COMPLEX_TYPE);

		Expression<PrismPropertyValue<NotificationMessageType>,PrismPropertyDefinition<NotificationMessageType>> expression =
				expressionFactory.makeExpression(expressionType, resultDef, MiscSchemaUtil.getExpressionProfile(), shortDesc, task, result);
		ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, shortDesc, task);
		PrismValueDeltaSetTriple<PrismPropertyValue<NotificationMessageType>> exprResult = ModelExpressionThreadLocalHolder
				.evaluateExpressionInContext(expression, params, task, result);
		return exprResult.getZeroSet().stream().map(PrismPropertyValue::getValue).collect(Collectors.toList());
	}
}

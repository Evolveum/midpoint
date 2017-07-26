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

package com.evolveum.midpoint.notifications.impl.api.transports;

import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CustomTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

/**
 * TODO clean up
 * @author mederly
 */
@Component
public class CustomTransport implements Transport {

    private static final Trace LOGGER = TraceManager.getTrace(CustomTransport.class);

    private static final String NAME = "custom";

    private static final String DOT_CLASS = CustomTransport.class.getName() + ".";

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected ExpressionFactory expressionFactory;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @Autowired
    private NotificationManager notificationManager;

    @PostConstruct
    public void init() {
        notificationManager.registerTransport(NAME, this);
    }

    @Override
    public void send(Message message, String transportName, Event event, Task task, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "send");
        result.addArbitraryObjectCollectionAsParam("message recipient(s)", message.getTo());
        result.addParam("message subject", message.getSubject());

        SystemConfigurationType systemConfiguration = NotificationFunctionsImpl
				.getSystemConfiguration(cacheRepositoryService, result);
        if (systemConfiguration == null || systemConfiguration.getNotificationConfiguration() == null) {
            String msg = "No notifications are configured. Custom notification to " + message.getTo() + " will not be sent.";
            LOGGER.warn(msg) ;
            result.recordWarning(msg);
            return;
        }

        String configName = transportName.length() > NAME.length() ? transportName.substring(NAME.length() + 1) : null;      // after "sms:"
        CustomTransportConfigurationType configuration = systemConfiguration.getNotificationConfiguration().getCustomTransport().stream()
                .filter(transportConfigurationType -> java.util.Objects.equals(configName, transportConfigurationType.getName()))
                .findFirst().orElse(null);

        if (configuration == null) {
            String msg = "Custom configuration '" + configName + "' not found. Custom notification to " + message.getTo() + " will not be sent.";
            LOGGER.warn(msg) ;
            result.recordWarning(msg);
            return;
        }

        String file = configuration.getRedirectToFile();
        if (file != null) {
            writeToFile(message, file, result);
            return;
        }

        try {
            evaluateExpression(configuration.getExpression(), getDefaultVariables(message, event),
                    "custom transport expression", task, result);
            LOGGER.trace("Custom transport expression execution finished");
            result.recordSuccess();
        } catch (Throwable t) {
            String msg = "Couldn't execute custom transport expression";
            LoggingUtils.logException(LOGGER, msg, t);
            result.recordFatalError(msg + ": " + t.getMessage(), t);
        }
    }

    // TODO deduplicate
    private void writeToFile(Message message, String file, OperationResult result) {
        try {
            TransportUtil.appendToFile(file, formatToFile(message));
            result.recordSuccess();
        } catch (IOException e) {
            LoggingUtils.logException(LOGGER, "Couldn't write to message redirect file {}", e, file);
            result.recordPartialError("Couldn't write to message redirect file " + file, e);
        }
    }

    private String formatToFile(Message mailMessage) {
        return "================ " + new Date() + " =======\n" + mailMessage.toString() + "\n\n";
    }

    // TODO deduplicate
    private void evaluateExpression(ExpressionType expressionType, ExpressionVariables expressionVariables,
    		String shortDesc, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<String> resultDef = new PrismPropertyDefinitionImpl<>(resultName, DOMUtil.XSD_STRING, prismContext);

        Expression<PrismPropertyValue<String>,PrismPropertyDefinition<String>> expression = expressionFactory.makeExpression(expressionType, resultDef, shortDesc, task, result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, shortDesc, task, result);
        ModelExpressionThreadLocalHolder.evaluateExpressionInContext(expression, params, task, result);
    }

    protected ExpressionVariables getDefaultVariables(Message message, Event event) throws UnsupportedEncodingException {
    	ExpressionVariables variables = new ExpressionVariables();
        variables.addVariableDefinition(SchemaConstants.C_MESSAGE, message);
        variables.addVariableDefinition(SchemaConstants.C_EVENT, event);
        return variables;
    }

    @Override
    public String getDefaultRecipientAddress(UserType recipient) {
        return "anything";
    }

    @Override
    public String getName() {
        return NAME;
    }
}
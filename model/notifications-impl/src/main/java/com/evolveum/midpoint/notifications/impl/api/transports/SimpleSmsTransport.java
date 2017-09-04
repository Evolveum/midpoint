/*
 * Copyright (c) 2010-2015 Evolveum
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
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Date;

/**
 * @author mederly
 */
@Component
public class SimpleSmsTransport implements Transport {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleSmsTransport.class);

    private static final String NAME = "sms";

    private static final String DOT_CLASS = SimpleSmsTransport.class.getName() + ".";

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

        SystemConfigurationType systemConfiguration = NotificationFunctionsImpl.getSystemConfiguration(cacheRepositoryService, new OperationResult("dummy"));
        if (systemConfiguration == null || systemConfiguration.getNotificationConfiguration() == null) {
            String msg = "No notifications are configured. SMS notification to " + message.getTo() + " will not be sent.";
            LOGGER.warn(msg) ;
            result.recordWarning(msg);
            return;
        }

        String smsConfigName = transportName.length() > NAME.length() ? transportName.substring(NAME.length() + 1) : null;      // after "sms:"
        SmsConfigurationType found = null;
        for (SmsConfigurationType smsConfigurationType: systemConfiguration.getNotificationConfiguration().getSms()) {
            if ((smsConfigName == null && smsConfigurationType.getName() == null) || (smsConfigName != null && smsConfigName.equals(smsConfigurationType.getName()))) {
                found = smsConfigurationType;
                break;
            }
        }

        if (found == null) {
            String msg = "SMS configuration '" + smsConfigName + "' not found. SMS notification to " + message.getTo() + " will not be sent.";
            LOGGER.warn(msg) ;
            result.recordWarning(msg);
            return;
        }

        SmsConfigurationType smsConfigurationType = found;
        String logToFile = smsConfigurationType.getLogToFile();
        if (logToFile != null) {
            TransportUtil.logToFile(logToFile, TransportUtil.formatToFileNew(message, transportName), LOGGER);
        }
        String file = smsConfigurationType.getRedirectToFile();
        if (file != null) {
            writeToFile(message, file, null, result);
            return;
        }

        if (smsConfigurationType.getGateway().isEmpty()) {
            String msg = "SMS gateway(s) are not defined, notification to " + message.getTo() + " will not be sent.";
            LOGGER.warn(msg) ;
            result.recordWarning(msg);
            return;
        }

        String from = smsConfigurationType.getDefaultFrom() != null ? smsConfigurationType.getDefaultFrom() : "";

        if (message.getTo().isEmpty()) {
            String msg = "There is no recipient to send the notification to.";
            LOGGER.warn(msg) ;
            result.recordWarning(msg);
            return;
        }

        String to = message.getTo().get(0);
        if (message.getTo().size() > 1) {
            String msg = "Currently it is possible to send the SMS to one recipient only. Among " + message.getTo() + " the chosen one is " + to + " (the first one).";
            LOGGER.warn(msg) ;
        }

        for (SmsGatewayConfigurationType smsGatewayConfigurationType : smsConfigurationType.getGateway()) {

            OperationResult resultForGateway = result.createSubresult(DOT_CLASS + "send.forGateway");
            resultForGateway.addContext("gateway name", smsGatewayConfigurationType.getName());

            try {
                String url = evaluateExpressionChecked(smsGatewayConfigurationType.getUrl(), getDefaultVariables(from, to, message),
                		"sms gateway url", task, result);
                LOGGER.debug("Sending SMS to URL " + url);

                if (smsGatewayConfigurationType.getRedirectToFile() != null) {
                    writeToFile(message, smsGatewayConfigurationType.getRedirectToFile(), url, resultForGateway);
                    result.computeStatus();
                    return;
                } else {
                    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                    ClientHttpRequest request = requestFactory.createRequest(new URI(url), HttpMethod.GET);
                    ClientHttpResponse response = request.execute();
                    LOGGER.debug("Result: " + response.getStatusCode() + "/" + response.getStatusText());
                    if (response.getStatusCode().series() != HttpStatus.Series.SUCCESSFUL) {
                        throw new SystemException("SMS gateway communication failed: " + response.getStatusCode() + ": " + response.getStatusText());
                    }
                    LOGGER.info("Message sent successfully to " + message.getTo() + " via gateway " + smsGatewayConfigurationType.getName() + ".");
                    resultForGateway.recordSuccess();
                    result.recordSuccess();
                    return;
                }

            } catch (Throwable t) {
                String msg = "Couldn't send SMS to " + message.getTo() + " via " + smsGatewayConfigurationType.getName() + ", trying another gateway, if there is any";
                LoggingUtils.logException(LOGGER, msg, t);
                resultForGateway.recordFatalError(msg, t);
            }
        }
        LOGGER.warn("No more SMS gateways to try, notification to " + message.getTo() + " will not be sent.") ;
        result.recordWarning("Notification to " + message.getTo() + " could not be sent.");
    }

    private void writeToFile(Message message, String file, String url, OperationResult result) {
        try {
            TransportUtil.appendToFile(file, formatToFile(message, url));
            result.recordSuccess();
        } catch (IOException e) {
            LoggingUtils.logException(LOGGER, "Couldn't write to SMS redirect file {}", e, file);
            result.recordPartialError("Couldn't write to SMS redirect file " + file, e);
        }
    }

    private String formatToFile(Message mailMessage, String url) {
        return "================ " + new Date() + " ======= " + (url != null ? url : "") + "\n" + mailMessage.toString() + "\n\n";
    }

    private String evaluateExpressionChecked(ExpressionType expressionType, ExpressionVariables expressionVariables,
    		String shortDesc, Task task, OperationResult result) {

        Throwable failReason;
        try {
            return evaluateExpression(expressionType, expressionVariables, shortDesc, task, result);
        } catch (ObjectNotFoundException e) {
            failReason = e;
        } catch (SchemaException e) {
            failReason = e;
        } catch (ExpressionEvaluationException e) {
            failReason = e;
        }

        LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", failReason, shortDesc, expressionType);
        result.recordFatalError("Couldn't evaluate " + shortDesc, failReason);
        throw new SystemException(failReason);
    }

    private String evaluateExpression(ExpressionType expressionType, ExpressionVariables expressionVariables,
    		String shortDesc, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<String> resultDef = new PrismPropertyDefinitionImpl(resultName, DOMUtil.XSD_STRING, prismContext);

        Expression<PrismPropertyValue<String>,PrismPropertyDefinition<String>> expression = expressionFactory.makeExpression(expressionType, resultDef, shortDesc, task, result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, shortDesc, task, result);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> exprResult = ModelExpressionThreadLocalHolder
				.evaluateExpressionInContext(expression, params, task, result);

        if (exprResult.getZeroSet().size() != 1) {
            throw new SystemException("Invalid number of return values (" + exprResult.getZeroSet().size() + "), expected 1.");
        }

        return exprResult.getZeroSet().iterator().next().getValue();
    }

    protected ExpressionVariables getDefaultVariables(String from, String to, Message message) throws UnsupportedEncodingException {

    	ExpressionVariables variables = new ExpressionVariables();

        variables.addVariableDefinition(SchemaConstants.C_FROM, from);
        variables.addVariableDefinition(SchemaConstants.C_TO, to);
        variables.addVariableDefinition(SchemaConstants.C_ENCODED_MESSAGE_TEXT, URLEncoder.encode(message.getBody(), "US-ASCII"));
        variables.addVariableDefinition(SchemaConstants.C_MESSAGE, message);

        return variables;
    }

    @Override
    public String getDefaultRecipientAddress(UserType recipient) {
        return recipient.getTelephoneNumber();
    }

    @Override
    public String getName() {
        return NAME;
    }
}
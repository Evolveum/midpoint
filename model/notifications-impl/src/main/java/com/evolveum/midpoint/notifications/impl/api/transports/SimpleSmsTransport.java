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

import com.evolveum.midpoint.notifications.impl.util.HttpUtil;
import com.evolveum.midpoint.prism.crypto.Protector;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author mederly
 */
@Component
public class SimpleSmsTransport implements Transport {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleSmsTransport.class);

    private static final String NAME = "sms";

    private static final String DOT_CLASS = SimpleSmsTransport.class.getName() + ".";

    @Autowired protected PrismContext prismContext;
    @Autowired protected ExpressionFactory expressionFactory;
	@Autowired private NotificationManager notificationManager;
	@Autowired
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;
	@Autowired protected Protector protector;

    @PostConstruct
    public void init() {
        notificationManager.registerTransport(NAME, this);
    }

    @Override
    public void send(Message message, String transportName, Event event, Task task, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "send");
        result.addArbitraryObjectCollectionAsParam("message recipient(s)", message.getTo());
        result.addParam("message subject", message.getSubject());

        SystemConfigurationType systemConfiguration = NotificationFunctionsImpl.getSystemConfiguration(cacheRepositoryService, result);
        if (systemConfiguration == null || systemConfiguration.getNotificationConfiguration() == null) {
            String msg = "No notifications are configured. SMS notification to " + message.getTo() + " will not be sent.";
            LOGGER.warn(msg) ;
            result.recordWarning(msg);
            return;
        }

        String smsConfigName = StringUtils.substringAfter(transportName, NAME + ":");
        SmsConfigurationType found = null;
        for (SmsConfigurationType smsConfigurationType: systemConfiguration.getNotificationConfiguration().getSms()) {
            if (StringUtils.isEmpty(smsConfigName) && smsConfigurationType.getName() == null
		            || StringUtils.isNotEmpty(smsConfigName) && smsConfigName.equals(smsConfigurationType.getName())) {
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
            writeToFile(message, file, null, emptyList(), null, result);
            return;
        }

        if (smsConfigurationType.getGateway().isEmpty()) {
            String msg = "SMS gateway(s) are not defined, notification to " + message.getTo() + " will not be sent.";
            LOGGER.warn(msg) ;
            result.recordWarning(msg);
            return;
        }

        String from;
        if (message.getFrom() != null) {
        	from = message.getFrom();
        } else if (smsConfigurationType.getDefaultFrom() != null) {
        	from = smsConfigurationType.getDefaultFrom();
        } else {
        	from = "";
        }

        if (message.getTo().isEmpty()) {
            String msg = "There is no recipient to send the notification to.";
            LOGGER.warn(msg) ;
            result.recordWarning(msg);
            return;
        }

        List<String> to = message.getTo();
        assert to.size() > 0;

        for (SmsGatewayConfigurationType smsGatewayConfigurationType : smsConfigurationType.getGateway()) {
            OperationResult resultForGateway = result.createSubresult(DOT_CLASS + "send.forGateway");
            resultForGateway.addContext("gateway name", smsGatewayConfigurationType.getName());
            try {
	            ExpressionVariables variables = getDefaultVariables(from, to, message);
            	HttpMethodType method = defaultIfNull(smsGatewayConfigurationType.getMethod(), HttpMethodType.GET);
	            ExpressionType urlExpression = defaultIfNull(smsGatewayConfigurationType.getUrlExpression(), smsGatewayConfigurationType.getUrl());
	            String url = evaluateExpressionChecked(urlExpression, variables, "sms gateway request url", task, result);
                LOGGER.debug("Sending SMS to URL {} (method {})", url, method);
                if (url == null) {
                	throw new IllegalArgumentException("No URL specified");
                }

	            List<String> headersList = evaluateExpressionsChecked(smsGatewayConfigurationType.getHeadersExpression(), variables,
			            "sms gateway request headers", task, result);
	            LOGGER.debug("Using request headers:\n{}", headersList);

	            String encoding = defaultIfNull(smsGatewayConfigurationType.getBodyEncoding(), StandardCharsets.ISO_8859_1.name());
                String body = evaluateExpressionChecked(smsGatewayConfigurationType.getBodyExpression(), variables,
		                "sms gateway request body", task, result);
	            LOGGER.debug("Using request body text (encoding: {}):\n{}", encoding, body);

	            if (smsGatewayConfigurationType.getLogToFile() != null) {
	            	TransportUtil.logToFile(smsGatewayConfigurationType.getLogToFile(), formatToFile(message, url, headersList, body), LOGGER);
	            }
                if (smsGatewayConfigurationType.getRedirectToFile() != null) {
                    writeToFile(message, smsGatewayConfigurationType.getRedirectToFile(), url, headersList, body, resultForGateway);
                    result.computeStatus();
                    return;
                } else {
	                HttpClientBuilder builder = HttpClientBuilder.create();
	                String username = smsGatewayConfigurationType.getUsername();
	                ProtectedStringType password = smsGatewayConfigurationType.getPassword();
	                if (username != null) {
		                CredentialsProvider provider = new BasicCredentialsProvider();
		                String plainPassword = password != null ? protector.decryptString(password) : null;
		                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, plainPassword);
		                provider.setCredentials(AuthScope.ANY, credentials);
		                builder = builder.setDefaultCredentialsProvider(provider);
	                }
	                HttpClient client = builder.build();
	                HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(client);
                    ClientHttpRequest request = requestFactory.createRequest(new URI(url), HttpUtil.toHttpMethod(method));
	                setHeaders(request, headersList);
	                if (body != null) {
		                request.getBody().write(body.getBytes(encoding));
	                }
	                ClientHttpResponse response = request.execute();
                    LOGGER.debug("Result: " + response.getStatusCode() + "/" + response.getStatusText());
                    if (response.getStatusCode().series() != HttpStatus.Series.SUCCESSFUL) {
                        throw new SystemException("SMS gateway communication failed: " + response.getStatusCode() + ": " + response.getStatusText());
                    }
                    LOGGER.info("Message sent successfully to {} via gateway {}.", message.getTo(), smsGatewayConfigurationType.getName());
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

	private void setHeaders(ClientHttpRequest request, List<String> headersList) {
		for (String headerAsString : headersList) {
			if (StringUtils.isEmpty(headerAsString)) {
				continue;
			}
			int i = headerAsString.indexOf(':');
			if (i < 0) {
				throw new IllegalArgumentException("Illegal header specification (expected was 'name: value' pair): " + headerAsString);
			}
			String headerName = headerAsString.substring(0, i);
			int headerValueIndex;
			if (i+1 == headerAsString.length() || headerAsString.charAt(i+1) != ' ') {
				// let's be nice and treat well the wrong case (there's no space after ':')
				headerValueIndex = i+1;
			} else {
				// correct case: ':' followed by space
				headerValueIndex = i+2;
			}
			String headerValue = headerAsString.substring(headerValueIndex);
			request.getHeaders().add(headerName, headerValue);
		}
	}

	private void writeToFile(Message message, String file, String url, List<String> headers, String body, OperationResult result) {
        try {
            TransportUtil.appendToFile(file, formatToFile(message, url, headers, body));
            result.recordSuccess();
        } catch (IOException e) {
            LoggingUtils.logException(LOGGER, "Couldn't write to SMS redirect file {}", e, file);
            result.recordPartialError("Couldn't write to SMS redirect file " + file, e);
        }
    }

    private String formatToFile(Message mailMessage, String url, List<String> headers, String body) {
        return "================ " + new Date() + " ======= " + (url != null ? url : "")
		        + "\nHeaders:\n" + headers
		        + "\n\nBody:\n" + body
		        + "\n\nFor message:\n" + mailMessage.toString() + "\n\n";
    }

    private String evaluateExpressionChecked(ExpressionType expressionType, ExpressionVariables expressionVariables,
    		String shortDesc, Task task, OperationResult result) {
        try {
            return evaluateExpression(expressionType, expressionVariables, false, shortDesc, task, result).get(0);
        } catch (ObjectNotFoundException | SchemaException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
	        LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", e, shortDesc, expressionType);
	        result.recordFatalError("Couldn't evaluate " + shortDesc, e);
	        throw new SystemException(e);
        }
    }

    @NotNull
    private List<String> evaluateExpressionsChecked(ExpressionType expressionType, ExpressionVariables expressionVariables,
    		@SuppressWarnings("SameParameterValue") String shortDesc, Task task, OperationResult result) {
        try {
            return evaluateExpression(expressionType, expressionVariables, true, shortDesc, task, result);
        } catch (ObjectNotFoundException | SchemaException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
	        LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", e, shortDesc, expressionType);
	        result.recordFatalError("Couldn't evaluate " + shortDesc, e);
	        throw new SystemException(e);
        }
    }

    // A little hack: for single-value cases we always return single-item list (even if the returned value is null)
    @NotNull
    private List<String> evaluateExpression(ExpressionType expressionType, ExpressionVariables expressionVariables,
    		boolean multipleValues, String shortDesc, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException,
		    ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
    	if (expressionType == null) {
    		return multipleValues ? emptyList() : singletonList(null);
	    }
        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<String> resultDef = new PrismPropertyDefinitionImpl<>(resultName, DOMUtil.XSD_STRING, prismContext);
        if (multipleValues) {
        	resultDef.setMaxOccurs(-1);
        }

        Expression<PrismPropertyValue<String>,PrismPropertyDefinition<String>> expression =
		        expressionFactory.makeExpression(expressionType, resultDef, shortDesc, task, result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, shortDesc, task, result);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> exprResult = ModelExpressionThreadLocalHolder
				.evaluateExpressionInContext(expression, params, task, result);

	    if (!multipleValues) {
		    if (exprResult.getZeroSet().size() > 1) {
			    throw new SystemException("Invalid number of return values (" + exprResult.getZeroSet().size() + "), expected at most 1.");
		    } else if (exprResult.getZeroSet().isEmpty()) {
		    	return singletonList(null);
		    } else {
		    	// single-valued response is treated below
		    }
	    }
	    return exprResult.getZeroSet().stream().map(ppv -> ppv.getValue()).collect(Collectors.toList());
    }

    protected ExpressionVariables getDefaultVariables(String from, List<String> to, Message message) throws UnsupportedEncodingException {
    	ExpressionVariables variables = new ExpressionVariables();
        variables.addVariableDefinition(SchemaConstants.C_FROM, from);
        variables.addVariableDefinition(SchemaConstants.C_ENCODED_FROM, URLEncoder.encode(from, "US-ASCII"));
        variables.addVariableDefinition(SchemaConstants.C_TO, to.get(0));
        variables.addVariableDefinition(SchemaConstants.C_TO_LIST, to);
	    List<String> encodedTo = new ArrayList<>();
	    for (String s : to) {
		    encodedTo.add(URLEncoder.encode(s, "US-ASCII"));
	    }
	    variables.addVariableDefinition(SchemaConstants.C_ENCODED_TO, encodedTo.get(0));
	    variables.addVariableDefinition(SchemaConstants.C_ENCODED_TO_LIST, encodedTo);
        variables.addVariableDefinition(SchemaConstants.C_MESSAGE_TEXT, message.getBody());
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
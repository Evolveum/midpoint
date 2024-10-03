/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.transport.impl;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.crypto.EncryptionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.ContextBuilder;
import org.apache.hc.client5.http.auth.AuthCache;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.SendingContext;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.api.transports.TransportSupport;
import com.evolveum.midpoint.notifications.impl.util.HttpUtil;
import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.ExpressionConfigItem;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Message transport sending SMS messages.
 */
public class SmsMessageTransport implements Transport<SmsTransportConfigurationType> {

    private static final Trace LOGGER = TraceManager.getTrace(SmsMessageTransport.class);

    private static final String DOT_CLASS = SmsMessageTransport.class.getName() + ".";

    private String name;
    private SmsTransportConfigurationType configuration;
    private TransportSupport transportSupport;

    @Override
    public void configure(
            @NotNull SmsTransportConfigurationType configuration,
            @NotNull TransportSupport transportSupport) {
        this.configuration = Objects.requireNonNull(configuration);
        name = Objects.requireNonNull(configuration.getName());
        this.transportSupport = transportSupport;
    }

    @Override
    public void send(Message message, String transportName, SendingContext ctx, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "send");
        result.addArbitraryObjectCollectionAsParam("message recipient(s)", message.getTo());
        result.addParam("message subject", message.getSubject());

        String logToFile = configuration.getLogToFile();
        if (logToFile != null) {
            TransportUtil.logToFile(logToFile, TransportUtil.formatToFileNew(message, transportName), LOGGER);
        }
        String file = configuration.getRedirectToFile();
        int optionsForFilteringRecipient = TransportUtil.optionsForFilteringRecipient(configuration);

        List<String> allowedRecipientTo = new ArrayList<>();
        List<String> forbiddenRecipientTo = new ArrayList<>();

        if (optionsForFilteringRecipient != 0) {
            TransportUtil.validateRecipient(
                    allowedRecipientTo, forbiddenRecipientTo, message.getTo(), configuration, ctx.task(), result,
                    transportSupport.expressionFactory(), ctx.expressionProfile(), LOGGER);

            if (file != null) {
                if (!forbiddenRecipientTo.isEmpty()) {
                    message.setTo(forbiddenRecipientTo);
                    writeToFile(message, file, null, emptyList(), null, result);
                }
                message.setTo(allowedRecipientTo);
            }

        } else if (file != null) {
            writeToFile(message, file, null, emptyList(), null, result);
            return;
        }

        if (configuration.getGateway().isEmpty()) {
            String msg = "SMS gateway(s) are not defined, notification to " + message.getTo() + " will not be sent.";
            LOGGER.warn(msg);
            result.recordWarning(msg);
            return;
        }

        String from;
        if (message.getFrom() != null) {
            from = message.getFrom();
        } else if (configuration.getDefaultFrom() != null) {
            from = configuration.getDefaultFrom();
        } else {
            from = "";
        }

        if (message.getTo().isEmpty()) {
            if (optionsForFilteringRecipient != 0) {
                String msg = "After recipient validation there is no recipient to send the notification to.";
                LOGGER.debug(msg);
                result.recordSuccess();
            } else {
                String msg = "There is no recipient to send the notification to.";
                LOGGER.warn(msg);
                result.recordWarning(msg);
            }
            return;
        }

        List<String> to = message.getTo();
        assert !to.isEmpty();

        for (SmsGatewayConfigurationType smsGatewayConfigurationType : configuration.getGateway()) {
            OperationResult resultForGateway = result.createSubresult(DOT_CLASS + "send.forGateway");
            resultForGateway.addContext("gateway name", smsGatewayConfigurationType.getName());
            try {
                VariablesMap variables = getDefaultVariables(from, to, message);
                HttpMethodType method = defaultIfNull(smsGatewayConfigurationType.getMethod(), HttpMethodType.GET);
                ExpressionType urlExpression = defaultIfNull(smsGatewayConfigurationType.getUrlExpression(), null);
                String url = evaluateExpressionChecked(urlExpression, variables, "sms gateway request url", ctx, result);
                String proxyHost = smsGatewayConfigurationType.getProxyHost();
                Integer proxyPort = smsGatewayConfigurationType.getProxyPort();
                LOGGER.debug("Sending SMS to URL {} via proxy host {} and port {} (method {})", url, proxyHost, proxyPort, method);
                if (url == null) {
                    throw new IllegalArgumentException("No URL specified");
                }
                List<String> headersList = evaluateExpressionsChecked(
                        smsGatewayConfigurationType.getHeadersExpression(), variables,
                        "sms gateway request headers", ctx, result);
                LOGGER.debug("Using request headers:\n{}", headersList);

                String encoding = defaultIfNull(smsGatewayConfigurationType.getBodyEncoding(), StandardCharsets.ISO_8859_1.name());
                String body = evaluateExpressionChecked(
                        smsGatewayConfigurationType.getBodyExpression(), variables,
                        "sms gateway request body", ctx, result);
                LOGGER.debug("Using request body text (encoding: {}):\n{}", encoding, body);

                if (smsGatewayConfigurationType.getLogToFile() != null) {
                    TransportUtil.logToFile(smsGatewayConfigurationType.getLogToFile(), formatToFile(message, url, headersList, body), LOGGER);
                }
                if (smsGatewayConfigurationType.getRedirectToFile() != null) {
                    writeToFile(message, smsGatewayConfigurationType.getRedirectToFile(), url, headersList, body, resultForGateway);
                    result.computeStatus();
                    return;
                } else {
                    HttpHost host = HttpHost.create(URI.create(url));
                    HttpHost pHost = StringUtils.isNotBlank(proxyHost) ? (proxyPort != null ? new HttpHost(proxyHost, proxyPort) : new HttpHost(proxyHost)) : null;

                    HttpClientBuilder builder = HttpClientBuilder.create();
                    String username = smsGatewayConfigurationType.getUsername();
                    ProtectedStringType password = smsGatewayConfigurationType.getPassword();
                    BasicCredentialsProvider provider = new BasicCredentialsProvider();
                    if (username != null) {
                        String plainPassword = password != null ? transportSupport.protector().decryptString(password) : null;
                        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, plainPassword.toCharArray());
                        provider.setCredentials(new AuthScope(host, null, null), credentials);
                        builder = builder.setDefaultCredentialsProvider(provider);
                    }
                    String proxyUsername = smsGatewayConfigurationType.getProxyUsername();
                    ProtectedStringType proxyPassword = smsGatewayConfigurationType.getProxyPassword();
                    if (StringUtils.isNotBlank(proxyHost)) {
                        if (StringUtils.isNotBlank(proxyUsername)) {
                            String plainProxyPassword = proxyPassword != null ? transportSupport.protector().decryptString(proxyPassword) : null;
                            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(proxyUsername, plainProxyPassword.toCharArray());
                            provider.setCredentials(new AuthScope(pHost), credentials);
                        }
                        builder = builder.setDefaultCredentialsProvider(provider);
                        builder = builder.setProxy(pHost);
                    }

                    HttpClient client = builder.build();
                    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(client);

                    // this is to setup preemptive authentication
                    requestFactory.setHttpContextFactory((m, uri) -> {
                        ContextBuilder ctxBuilder = ContextBuilder.create()
                                .useCredentialsProvider(provider);
                        if (username != null) {
                            Credentials c = provider.getCredentials(new AuthScope(host, null, null), null);
                            ctxBuilder = ctxBuilder.preemptiveBasicAuth(host, (UsernamePasswordCredentials) c);
                        }
                        if (proxyUsername != null) {
                            Credentials c = provider.getCredentials(new AuthScope(pHost, null, null), null);
                            ctxBuilder = ctxBuilder.preemptiveBasicAuth(pHost, (UsernamePasswordCredentials) c);
                        }

                        return ctxBuilder.build();
                    });

                    ClientHttpRequest request = requestFactory.createRequest(URI.create(url), HttpUtil.toHttpMethod(method));
                    setHeaders(request, headersList);
                    if (body != null) {
                        request.getBody().write(body.getBytes(encoding));
                    }
                    try (ClientHttpResponse response = request.execute()) {
                        LOGGER.debug("Result: " + response.getStatusCode() + "/" + response.getStatusText());
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new SystemException("SMS gateway communication failed: "
                                    + response.getStatusCode() + ": " + response.getStatusText());
                        }
                    }
                    LOGGER.debug("Message sent successfully to {} via gateway {}.", message.getTo(), smsGatewayConfigurationType.getName());
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
        LOGGER.warn("No more SMS gateways to try, notification to " + message.getTo() + " will not be sent.");
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
            if (i + 1 == headerAsString.length() || headerAsString.charAt(i + 1) != ' ') {
                // let's be nice and treat well the wrong case (there's no space after ':')
                headerValueIndex = i + 1;
            } else {
                // correct case: ':' followed by space
                headerValueIndex = i + 2;
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

    private String formatToFile(Message smsMessage, String url, List<String> headers, String body) {
        return "================ " + new Date() + " ======= " + (url != null ? url : "")
                + "\nHeaders:\n" + headers
                + "\n\nBody:\n" + body
                + "\n\nFor message:\n" + smsMessage.toString() + "\n\n";
    }

    private String evaluateExpressionChecked(
            ExpressionType expressionType, VariablesMap VariablesMap,
            String shortDesc, SendingContext ctx, OperationResult result) {
        try {
            return evaluateExpression(expressionType, VariablesMap, false, shortDesc, ctx, result).get(0);
        } catch (ObjectNotFoundException | SchemaException | ExpressionEvaluationException | CommunicationException |
                ConfigurationException | SecurityViolationException e) {
            LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", e, shortDesc, expressionType);
            result.recordFatalError("Couldn't evaluate " + shortDesc, e);
            throw new SystemException(e);
        }
    }

    @NotNull
    private List<String> evaluateExpressionsChecked(
            ExpressionType expressionType, VariablesMap VariablesMap,
            @SuppressWarnings("SameParameterValue") String shortDesc, SendingContext ctx, OperationResult result) {
        try {
            return evaluateExpression(expressionType, VariablesMap, true, shortDesc, ctx, result);
        } catch (ObjectNotFoundException | SchemaException | ExpressionEvaluationException | CommunicationException |
                ConfigurationException | SecurityViolationException e) {
            LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", e, shortDesc, expressionType);
            result.recordFatalError("Couldn't evaluate " + shortDesc, e);
            throw new SystemException(e);
        }
    }

    // A little hack: for single-value cases we always return single-item list (even if the returned value is null)
    @NotNull
    private List<String> evaluateExpression(
            ExpressionType expressionType, VariablesMap variablesMap,
            boolean multipleValues, String shortDesc, SendingContext ctx, OperationResult result)
            throws ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (expressionType == null) {
            return multipleValues ? emptyList() : singletonList(null);
        }
        QName resultName = new QName(SchemaConstants.NS_C, "result");
        MutablePrismPropertyDefinition<String> resultDef =
                transportSupport.prismContext().definitionFactory().createPropertyDefinition(resultName, DOMUtil.XSD_STRING);
        if (multipleValues) {
            resultDef.setMaxOccurs(-1);
        }

        var task = ctx.task();

        ExpressionFactory expressionFactory = transportSupport.expressionFactory();
        Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression =
                expressionFactory.makeExpression(
                        ExpressionConfigItem.of(expressionType, ConfigurationItemOrigin.undeterminedSafe()),
                        resultDef, ctx.expressionProfile(), shortDesc, task, result);
        ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null, variablesMap, shortDesc, task);
        eeContext.setExpressionFactory(expressionFactory);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> exprResult =
                ExpressionUtil.evaluateExpressionInContext(expression, eeContext, task, result);

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

    protected VariablesMap getDefaultVariables(String from, List<String> to, Message message)
            throws UnsupportedEncodingException {
        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_FROM, from, String.class);
        variables.put(ExpressionConstants.VAR_ENCODED_FROM,
                URLEncoder.encode(from, StandardCharsets.US_ASCII), String.class);
        variables.put(ExpressionConstants.VAR_TO, to.get(0), String.class);
        variables.put(ExpressionConstants.VAR_TO_LIST, to, List.class);
        List<String> encodedTo = new ArrayList<>();
        for (String s : to) {
            encodedTo.add(URLEncoder.encode(s, StandardCharsets.US_ASCII));
        }
        variables.put(ExpressionConstants.VAR_ENCODED_TO, encodedTo.get(0), String.class);
        variables.put(ExpressionConstants.VAR_ENCODED_TO_LIST, encodedTo, List.class);
        variables.put(ExpressionConstants.VAR_MESSAGE_TEXT, message.getBody(), String.class);
        variables.put(ExpressionConstants.VAR_ENCODED_MESSAGE_TEXT,
                URLEncoder.encode(message.getBody(), StandardCharsets.US_ASCII), String.class);
        variables.put(ExpressionConstants.VAR_MESSAGE, message, Message.class);
        return variables;
    }

    @Override
    public String getDefaultRecipientAddress(FocusType recipient) {
        return recipient.getTelephoneNumber();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SmsTransportConfigurationType getConfiguration() {
        return configuration;
    }
}

/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.transport.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.SendingContext;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.api.transports.TransportSupport;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.CustomTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * TODO: make this a good superclass for custom subclasses
 * E.g. expression is default behavior for sending, but it should be easily overridable method.
 */
public class CustomMessageTransport implements Transport<CustomTransportConfigurationType> {

    private static final Trace LOGGER = TraceManager.getTrace(CustomMessageTransport.class);

    private static final String DOT_CLASS = CustomMessageTransport.class.getName() + ".";

    private String name;
    private CustomTransportConfigurationType configuration;
    private TransportSupport transportSupport;

    @Override
    public void configure(
            @NotNull CustomTransportConfigurationType configuration,
            @NotNull TransportSupport transportSupport) {
        this.configuration = Objects.requireNonNull(configuration);
        name = Objects.requireNonNull(configuration.getName());
        this.transportSupport = Objects.requireNonNull(transportSupport);
    }

    @Override
    public void send(Message message, String transportNameIgnored, SendingContext ctx, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "send");
        result.addArbitraryObjectCollectionAsParam("message recipient(s)", message.getTo());
        result.addParam("message subject", message.getSubject());

        String logToFile = configuration.getLogToFile();
        if (logToFile != null) {
            TransportUtil.logToFile(logToFile, TransportUtil.formatToFileNew(message, name), LOGGER);
        }

        int optionsForFilteringRecipient = TransportUtil.optionsForFilteringRecipient(configuration);

        List<String> allowedRecipientTo = new ArrayList<>();
        List<String> forbiddenRecipientTo = new ArrayList<>();
        List<String> allowedRecipientCc = new ArrayList<>();
        List<String> forbiddenRecipientCc = new ArrayList<>();
        List<String> allowedRecipientBcc = new ArrayList<>();
        List<String> forbiddenRecipientBcc = new ArrayList<>();

        var task = ctx.task();

        String file = configuration.getRedirectToFile();
        if (optionsForFilteringRecipient != 0) {
            TransportUtil.validateRecipient(
                    allowedRecipientTo, forbiddenRecipientTo, message.getTo(), configuration, task, result,
                    transportSupport.expressionFactory(), ctx.expressionProfile(), LOGGER);
            TransportUtil.validateRecipient(
                    allowedRecipientCc, forbiddenRecipientCc, message.getCc(), configuration, task, result,
                    transportSupport.expressionFactory(), ctx.expressionProfile(), LOGGER);
            TransportUtil.validateRecipient(
                    allowedRecipientBcc, forbiddenRecipientBcc, message.getBcc(), configuration, task, result,
                    transportSupport.expressionFactory(), ctx.expressionProfile(), LOGGER);

            if (file != null) {
                if (!forbiddenRecipientTo.isEmpty() || !forbiddenRecipientCc.isEmpty() || !forbiddenRecipientBcc.isEmpty()) {
                    message.setTo(forbiddenRecipientTo);
                    message.setCc(forbiddenRecipientCc);
                    message.setBcc(forbiddenRecipientBcc);
                    writeToFile(message, file, result);
                }
                message.setTo(allowedRecipientTo);
                message.setCc(allowedRecipientCc);
                message.setBcc(allowedRecipientBcc);
            }

        } else if (file != null) {
            writeToFile(message, file, result);
            return;
        }

        try {
            evaluateExpression(
                    configuration.getExpression(),
                    getDefaultVariables(message, ctx.event()),
                    "custom transport expression", ctx, result);
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
    private void evaluateExpression(
            ExpressionType expressionBean, VariablesMap VariablesMap, String shortDesc, SendingContext ctx, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<String> resultDef = transportSupport.prismContext().definitionFactory().createPropertyDefinition(resultName, DOMUtil.XSD_STRING);

        var task = ctx.task();
        ExpressionFactory expressionFactory = transportSupport.expressionFactory();
        Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression =
                expressionFactory.makeExpression(
                        expressionBean != null ?
                                ExpressionConfigItem.of(expressionBean, ConfigurationItemOrigin.undeterminedSafe()) : null,
                        resultDef, ctx.expressionProfile(),
                        shortDesc, task, result);
        ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null, VariablesMap, shortDesc, task);
        eeContext.setExpressionFactory(expressionFactory);
        ExpressionUtil.evaluateExpressionInContext(expression, eeContext, task, result);
    }

    protected VariablesMap getDefaultVariables(Message message, Event event) throws UnsupportedEncodingException {
        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_MESSAGE, message, Message.class);
        variables.put(ExpressionConstants.VAR_EVENT, event, Event.class);
        return variables;
    }

    @Override
    public String getDefaultRecipientAddress(FocusType recipient) {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CustomTransportConfigurationType getConfiguration() {
        return configuration;
    }
}

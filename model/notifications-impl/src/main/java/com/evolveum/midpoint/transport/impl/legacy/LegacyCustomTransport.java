/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.transport.impl.legacy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.expression.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.api.transports.TransportSupport;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.transport.impl.TransportUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/** Legacy transport that should be removed after 4.5; type parameter is irrelevant. */
@Deprecated
@Component
public class LegacyCustomTransport implements Transport<GeneralTransportConfigurationType> {

    private static final Trace LOGGER = TraceManager.getTrace(LegacyCustomTransport.class);

    private static final String NAME = "custom";

    private static final String DOT_CLASS = LegacyCustomTransport.class.getName() + ".";

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected ExpressionFactory expressionFactory;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

    @Override
    public void send(Message message, String transportName, Event event, Task task, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "send");
        result.addArbitraryObjectCollectionAsParam("message recipient(s)", message.getTo());
        result.addParam("message subject", message.getSubject());

        SystemConfigurationType systemConfiguration =
                TransportUtil.getSystemConfiguration(cacheRepositoryService, result);
        if (systemConfiguration == null || systemConfiguration.getNotificationConfiguration() == null) {
            String msg = "No notifications are configured. Custom notification to " + message.getTo() + " will not be sent.";
            LOGGER.warn(msg);
            result.recordWarning(msg);
            return;
        }

        String configName = transportName.length() > NAME.length() ? transportName.substring(NAME.length() + 1) : null;      // after "sms:"
        LegacyCustomTransportConfigurationType configuration = systemConfiguration.getNotificationConfiguration().getCustomTransport().stream()
                .filter(transportConfigurationType -> java.util.Objects.equals(configName, transportConfigurationType.getName()))
                .findFirst().orElse(null);

        if (configuration == null) {
            String msg = "Custom configuration '" + configName + "' not found. Custom notification to " + message.getTo() + " will not be sent.";
            LOGGER.warn(msg);
            result.recordWarning(msg);
            return;
        }
        String logToFile = configuration.getLogToFile();
        if (logToFile != null) {
            TransportUtil.logToFile(logToFile, TransportUtil.formatToFileNew(message, transportName), LOGGER);
        }

        int optionsForFilteringRecipient = TransportUtil.optionsForFilteringRecipient(configuration);

        List<String> allowedRecipientTo = new ArrayList<>();
        List<String> forbiddenRecipientTo = new ArrayList<>();
        List<String> allowedRecipientCc = new ArrayList<>();
        List<String> forbiddenRecipientCc = new ArrayList<>();
        List<String> allowedRecipientBcc = new ArrayList<>();
        List<String> forbiddenRecipientBcc = new ArrayList<>();

        String file = configuration.getRedirectToFile();
        if (optionsForFilteringRecipient != 0) {
            TransportUtil.validateRecipient(allowedRecipientTo, forbiddenRecipientTo, message.getTo(), configuration, task, result,
                    expressionFactory, MiscSchemaUtil.getExpressionProfile(), LOGGER);
            TransportUtil.validateRecipient(allowedRecipientCc, forbiddenRecipientCc, message.getCc(), configuration, task, result,
                    expressionFactory, MiscSchemaUtil.getExpressionProfile(), LOGGER);
            TransportUtil.validateRecipient(allowedRecipientBcc, forbiddenRecipientBcc, message.getBcc(), configuration, task, result,
                    expressionFactory, MiscSchemaUtil.getExpressionProfile(), LOGGER);

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
    private void evaluateExpression(
            ExpressionType expressionType, VariablesMap VariablesMap, String shortDesc, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<String> resultDef = prismContext.definitionFactory().createPropertyDefinition(resultName, DOMUtil.XSD_STRING);

        Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression = expressionFactory.makeExpression(expressionType, resultDef, MiscSchemaUtil.getExpressionProfile(), shortDesc, task, result);
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
        return NAME;
    }

    @Override
    public void configure(@NotNull GeneralTransportConfigurationType configuration, @NotNull TransportSupport transportSupport) {
        // not called for legacy transport component
    }

    @Override
    public GeneralTransportConfigurationType getConfiguration() {
        return null;
    }
}

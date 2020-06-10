/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.api.transports;

import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * @author mederly
 */
public class TransportUtil {

    static void appendToFile(String filename, String text) throws IOException {
        FileWriter fw = new FileWriter(filename, true);
        fw.append(text);
        fw.close();
    }

    public static <T extends NotificationTransportConfigurationType> T getTransportConfiguration(String transportName, String baseTransportName,
            Function<NotificationConfigurationType, List<T>> getter, RepositoryService cacheRepositoryService,
            OperationResult result) {

        SystemConfigurationType systemConfiguration = NotificationFunctionsImpl.getSystemConfiguration(cacheRepositoryService, result);
        if (systemConfiguration == null || systemConfiguration.getNotificationConfiguration() == null) {
            return null;
        }

        String transportConfigName = transportName.length() > baseTransportName.length() ? transportName.substring(baseTransportName.length() + 1) : null;      // after e.g. "sms:" or "file:"
        for (T namedConfiguration: getter.apply(systemConfiguration.getNotificationConfiguration())) {
            if ((transportConfigName == null && namedConfiguration.getName() == null) || (transportConfigName != null && transportConfigName.equals(namedConfiguration.getName()))) {
                return namedConfiguration;
            }
        }
        return null;
    }

    public static void appendToFile(String fileName, String messageText, Trace logger, OperationResult result) {
        try {
            TransportUtil.appendToFile(fileName, messageText);
            result.recordSuccess();
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(logger, "Couldn't write the notification to a file {}", t, fileName);
            result.recordPartialError("Couldn't write the notification to a file " + fileName, t);
        }
    }

    public static void logToFile(String fileName, String messageText, Trace logger) {
        try {
            TransportUtil.appendToFile(fileName, messageText);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(logger, "Couldn't write the notification to a file {}", t, fileName);
        }
    }

    public static String formatToFileOld(Message message) {
        return "============================================ " + "\n" +new Date() + "\n" + message.toString() + "\n\n";
    }

    static String formatToFileNew(Message message, String transport) {
        return "================ " + new Date() + " ======= [" + transport + "]\n" + message.debugDump() + "\n\n";
    }

    private static boolean isRecipientAllowed(String recipient, NotificationTransportConfigurationType transportConfigurationType,
            Task task, OperationResult result, ExpressionFactory expressionFactory, ExpressionProfile expressionProfile, Trace logger) {
        if (optionsForFilteringRecipient(transportConfigurationType) > 1) {
            throw new IllegalArgumentException("Couldn't use more than one choice from 'blackList', 'whiteList' and 'recipientFilterExpression'");
        }
        ExpressionType filter = transportConfigurationType.getRecipientFilterExpression();
        if (filter != null) {
            ExpressionVariables variables = new ExpressionVariables();
            variables.put("recipientAddress", recipient, String.class);
            try {
                PrismPropertyValue<Boolean> allowedRecipient = ExpressionUtil.evaluateCondition(variables, filter, expressionProfile,
                        expressionFactory, "Recipient filter", task, result);
                if (allowedRecipient == null || allowedRecipient.getValue() == null) {
                    throw new IllegalArgumentException("Return value from expresion for filtering recipient is null");
                }
                return allowedRecipient.getValue();
            } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
                    | ConfigurationException | SecurityViolationException e) {
                LoggingUtils.logUnexpectedException(logger, "Couldn't execute filter for recipient", e);
            }
        }
        List<String> whiteList = transportConfigurationType.getWhiteList();
        if (!whiteList.isEmpty()) {
            for (String allowedRecipient : whiteList) {
                String regexAllowedRecipient = allowedRecipient.replace("*", ".{0,}");
                if (recipient.matches(regexAllowedRecipient)) {
                    return true;
                }
            }
            return false;
        }

        List<String> blackList = transportConfigurationType.getBlackList();
        for (String forbiddenRecipient : blackList) {
            String regexForbiddenRecipient = forbiddenRecipient.replace("*", ".{0,}");
            if (recipient.matches(regexForbiddenRecipient)) {
                return false;
            }
        }

        return true;
    }

    static int optionsForFilteringRecipient(
            NotificationTransportConfigurationType transportConfigurationType) {
        int choices = 0;
        if (transportConfigurationType.getRecipientFilterExpression() != null) {
            choices++;
        }
        if (!transportConfigurationType.getBlackList().isEmpty()) {
            choices++;
        }
        if (!transportConfigurationType.getWhiteList().isEmpty()) {
            choices++;
        }
        return choices;
    }

    public static void validateRecipient(List<String> allowedRecipient, List<String> forbiddenRecipient, List<String> recipients,
            NotificationTransportConfigurationType transportConfigurationType, Task task, OperationResult result, ExpressionFactory expressionFactory,
            ExpressionProfile expressionProfile, Trace logger) {
        for (String recipient : recipients) {
            if (TransportUtil.isRecipientAllowed(recipient, transportConfigurationType, task, result, expressionFactory, expressionProfile, logger)) {
                logger.debug("Recipient " + recipient + "is allowed");
                allowedRecipient.add(recipient);
            } else {
                logger.debug("Recipient " + recipient + "is forbidden");
                forbiddenRecipient.add(recipient);
            }
        }

    }
}

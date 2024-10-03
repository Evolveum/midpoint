/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.transport.impl;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;

public class TransportUtil {

    private static final Trace LOGGER = TraceManager.getTrace(TransportUtil.class);

    public static void appendToFile(String filename, String text) throws IOException {
        FileWriter fw = new FileWriter(filename, true);
        fw.append(text);
        fw.close();
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
        return "============================================ " + "\n" + new Date() + "\n" + message.toString() + "\n\n";
    }

    public static String formatToFileNew(Message message, String transport) {
        return "================ " + new Date() + " ======= [" + transport + "]\n" + message.debugDump() + "\n\n";
    }

    @Deprecated
    private static boolean isRecipientAllowed(String recipient, NotificationTransportConfigurationType transportConfigurationType,
            Task task, OperationResult result, ExpressionFactory expressionFactory, ExpressionProfile expressionProfile, Trace logger) {
        if (optionsForFilteringRecipient(transportConfigurationType) > 1) {
            throw new IllegalArgumentException("Couldn't use more than one choice from 'blackList', 'whiteList' and 'recipientFilterExpression'");
        }
        ExpressionType filter = transportConfigurationType.getRecipientFilterExpression();
        if (filter != null) {
            VariablesMap variables = new VariablesMap();
            variables.put("recipientAddress", recipient, String.class);
            try {
                PrismPropertyValue<Boolean> allowedRecipient = ExpressionUtil.evaluateCondition(variables, filter, expressionProfile,
                        expressionFactory, "Recipient filter", task, result);
                if (allowedRecipient == null || allowedRecipient.getValue() == null) {
                    throw new IllegalArgumentException("Return value from expression for filtering recipient is null");
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

    @Deprecated
    public static int optionsForFilteringRecipient(
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

    @Deprecated
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

    // beware, may return null if there's any problem getting sysconfig (e.g. during initial import)
    public static SystemConfigurationType getSystemConfiguration(
            RepositoryService repositoryService, OperationResult result) {
        return getSystemConfiguration(repositoryService, true, result);
    }

    public static SystemConfigurationType getSystemConfiguration(
            RepositoryService repositoryService, boolean errorIfNotFound, OperationResult result) {
        try {
            return repositoryService
                    .getObject(
                            SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                            null, result)
                    .asObjectable();
        } catch (ObjectNotFoundException | SchemaException e) {
            if (errorIfNotFound) {
                LoggingUtils.logException(LOGGER,
                        "Notification(s) couldn't be processed, because the system configuration couldn't be retrieved", e);
            } else {
                LoggingUtils.logExceptionOnDebugLevel(LOGGER,
                        "Notification(s) couldn't be processed, because the system configuration couldn't be retrieved", e);
            }
            return null;
        }
    }

    private static boolean isRecipientAllowed(
            String recipient, GeneralTransportConfigurationType transportConfigurationType,
            Task task, OperationResult result, ExpressionFactory expressionFactory, ExpressionProfile expressionProfile, Trace logger) {
        if (optionsForFilteringRecipient(transportConfigurationType) > 1) {
            throw new IllegalArgumentException("Couldn't use more than one choice from 'blackList', 'whiteList' and 'recipientFilterExpression'");
        }
        ExpressionType filter = transportConfigurationType.getRecipientFilterExpression();
        if (filter != null) {
            VariablesMap variables = new VariablesMap();
            variables.put("recipientAddress", recipient, String.class);
            try {
                PrismPropertyValue<Boolean> allowedRecipient = ExpressionUtil.evaluateCondition(variables, filter, expressionProfile,
                        expressionFactory, "Recipient filter", task, result);
                if (allowedRecipient == null || allowedRecipient.getValue() == null) {
                    throw new IllegalArgumentException("Return value from expression for filtering recipient is null");
                }
                return allowedRecipient.getValue();
            } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
                    | ConfigurationException | SecurityViolationException e) {
                LoggingUtils.logUnexpectedException(logger, "transportConfigurationType Couldn't execute filter for recipient", e);
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

    public static void validateRecipient(
            List<String> allowedRecipient,
            List<String> forbiddenRecipient,
            List<String> recipients,
            GeneralTransportConfigurationType transportConfigurationType,
            Task task,
            OperationResult result,
            ExpressionFactory expressionFactory,
            ExpressionProfile expressionProfile,
            Trace logger) {
        for (String recipient : recipients) {
            if (TransportUtil.isRecipientAllowed(
                    recipient, transportConfigurationType, task, result,
                    expressionFactory, expressionProfile, logger)) {
                logger.debug("Recipient " + recipient + "is allowed");
                allowedRecipient.add(recipient);
            } else {
                logger.debug("Recipient " + recipient + "is forbidden");
                forbiddenRecipient.add(recipient);
            }
        }

    }

    public static int optionsForFilteringRecipient(
            GeneralTransportConfigurationType transportConfigurationType) {
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

    public static Collection<String> filterBlankMailRecipients(Collection<String> recipients, String type, String subject) {
        var nonBlank = new ArrayList<String>();
        for (var recipient : recipients) {
            if (StringUtils.isNotBlank(recipient)) {
                nonBlank.add(recipient);
            } else {
                LOGGER.warn("Ignoring blank '{}' recipient in the mail message, subject: {}", type, subject);
            }
        }
        return nonBlank;
    }
}

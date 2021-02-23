/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import org.apache.cxf.common.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.notifications.impl.TransportRegistry;
import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
import com.evolveum.midpoint.notifications.impl.formatters.ValueFormatter;
import com.evolveum.midpoint.notifications.impl.handlers.AggregatedEventHandler;
import com.evolveum.midpoint.notifications.impl.handlers.BaseHandler;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Base class for most notifiers.
 */
@Component
public abstract class AbstractGeneralNotifier<E extends Event, N extends GeneralNotifierType> extends BaseHandler<E, N> {

    private static final Trace DEFAULT_LOGGER = TraceManager.getTrace(AbstractGeneralNotifier.class);

    private static final String OP_PROCESS_EVENT = AbstractGeneralNotifier.class.getName() + ".processEvent";
    private static final String OP_PREPARE_AND_SEND = AbstractGeneralNotifier.class.getName() + ".prepareAndSend";

    @Autowired protected NotificationManager notificationManager;
    @Autowired protected NotificationFunctionsImpl functions;
    @Autowired protected TextFormatter textFormatter;
    @Autowired protected ValueFormatter valueFormatter;
    @Autowired protected AggregatedEventHandler aggregatedEventHandler;
    @Autowired protected TransportRegistry transportRegistry;

    @Override
    public boolean processEvent(E event, N notifierConfiguration, Task task, OperationResult parentResult)
            throws SchemaException {

        OperationResult result = parentResult.subresult(OP_PROCESS_EVENT)
                .setMinor()
                .addContext("notifier", this.getClass().getName())
                .build();
        int messagesSent = 0;
        try {
            logStart(getLogger(), event, notifierConfiguration);

            boolean applies = false;
            if (!quickCheckApplicability(event, notifierConfiguration, result)) {
                // nothing to do -- an appropriate message should have been logged in quickCheckApplicability method
                result.recordNotApplicable();
            } else {
                if (aggregatedEventHandler.processEvent(event, notifierConfiguration, task, result)) {
                    if (!checkApplicability(event, notifierConfiguration, result)) {
                        // nothing to do -- an appropriate message should have been logged in checkApplicability method
                        result.recordNotApplicable();
                    } else if (notifierConfiguration.getTransport().isEmpty()) {
                        getLogger().warn("No transports for this notifier, exiting without sending any notifications.");
                        result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "No transports");
                    } else {
                        applies = true;
                        reportNotificationStart(event);
                        try {
                            VariablesMap variables = getDefaultVariables(event, result);
                            for (String transportName : notifierConfiguration.getTransport()) {
                                messagesSent += prepareAndSend(event, notifierConfiguration, variables, transportName, task, result);
                            }
                        } finally {
                            reportNotificationEnd(event, result);
                        }
                    }
                }
            }
            logEnd(getLogger(), event, applies);
            return true; // not-applicable notifiers do not stop processing of other notifiers
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.addReturn("messagesSent", messagesSent);
            result.computeStatusIfUnknown();
        }
    }

    private int prepareAndSend(E event, N notifierConfig, VariablesMap variables, String transportName,
            Task task, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.subresult(OP_PREPARE_AND_SEND)
                .setMinor()
                .addParam("transportName", transportName)
                .addContext("notifier", this.getClass().getName())
                .build();
        try {
            variables.put(ExpressionConstants.VAR_TRANSPORT_NAME, transportName, String.class);
            Transport transport = transportRegistry.getTransport(transportName);

            List<String> recipientsAddresses = getRecipientsAddresses(event, notifierConfig, variables,
                    getDefaultRecipient(event, notifierConfig, result), transportName, transport, task, result);
            result.addArbitraryObjectCollectionAsContext("recipientAddress", recipientsAddresses);

            if (!recipientsAddresses.isEmpty()) {

                String body;
                ExpressionType bodyExpression = notifierConfig.getBodyExpression();
                if (bodyExpression != null) {
                    body = getBodyFromExpression(event, bodyExpression, variables, task, result);
                } else {
                    body = getBody(event, notifierConfig, transportName, task, result);
                }
                if (body == null) {
                    getLogger().debug("Skipping notification as null body was provided for transport={}", transportName);
                    result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "No message body");
                    return 0;
                }

                String from = getFromFromExpression(event, notifierConfig, variables, task, result);
                String contentType = getContentTypeFromExpression(event, notifierConfig, variables, task, result);
                List<NotificationMessageAttachmentType> attachments = getAttachmentsFromExpression(event, notifierConfig, variables, task, result);

                String subject = getSubjectFromExpression(event, notifierConfig, variables, task, result);
                if (subject == null) {
                    subject = notifierConfig.getSubjectPrefix() != null ? notifierConfig.getSubjectPrefix() : "";
                    subject += getSubject(event, notifierConfig, transportName, task, result);
                }

                if (attachments == null) {
                    attachments = notifierConfig.getAttachment();
                    if (attachments == null || attachments.isEmpty()) {
                        attachments = getAttachment(event, notifierConfig, transportName, task, result);
                    }
                } else if (notifierConfig.getAttachment() != null) {
                    attachments.addAll(notifierConfig.getAttachment());
                }

                Message message = new Message();
                message.setBody(body);
                if (contentType != null) {
                    message.setContentType(contentType);
                } else if (notifierConfig.getContentType() != null) {
                    message.setContentType(notifierConfig.getContentType());
                } else if (getContentType() != null) {
                    message.setContentType(getContentType());
                }
                message.setSubject(subject);

                if (from != null) {
                    message.setFrom(from);
                }
                message.setTo(recipientsAddresses);
                message.setCc(getCcBccAddresses(notifierConfig.getCcExpression(), variables, "notification cc-expression", task, result));
                message.setBcc(getCcBccAddresses(notifierConfig.getBccExpression(), variables, "notification bcc-expression", task, result));

                if (attachments != null) {
                    message.setAttachments(attachments);
                }

                getLogger().trace("Sending notification via transport {}:\n{}", transportName, message);
                transport.send(message, transportName, event, task, result);
                return 1; // TODO only if really sent
            } else {
                getLogger().info("No recipients addresses for transport " + transportName + ", message corresponding to event " + event.getId() + " will not be send.");
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "No recipients");
                return 0;
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    protected String getContentType() {
        return null;
    }

    /**
     * Checks the event/notifier applicability _before_ nested filters are applied. So this check should be:
     * 1) quick
     * 2) safe - it should not make any assumptions about event content that would cause it to throw an exception
     * 3) filter out events that obviously do not match the notifier - e.g. simpleUserNotifier should ensure that
     *    the focus type is really UserType; this allows nested filters to assume existence of
     *    e.g. requestee.fullName element.
     */
    protected boolean quickCheckApplicability(E event, N generalNotifierType, OperationResult result) {
        return true;
    }

    protected boolean checkApplicability(E event, N generalNotifierType, OperationResult result) {
        return true;
    }

    protected String getSubject(E event, N generalNotifierType, String transport, Task task, OperationResult result) {
        return null;
    }

    protected String getBody(E event, N generalNotifierType, String transport, Task task, OperationResult result) throws SchemaException {
        return null;
    }

    // for future extension
    @SuppressWarnings("unused")
    protected List<NotificationMessageAttachmentType> getAttachment(E event, N generalNotifierType,
            String transportName, Task task, OperationResult result) {
        return null;
    }

    protected UserType getDefaultRecipient(E event, N generalNotifierType, OperationResult result) {
        ObjectType objectType = functions.getObjectType(event.getRequestee(), true, result);
        if (objectType instanceof UserType) {
            return (UserType) objectType;
        } else {
            return null;
        }
    }

    protected Trace getLogger() {
        return DEFAULT_LOGGER;              // in case a subclass does not provide its own logger
    }

    private List<String> getRecipientsAddresses(E event, N generalNotifierType, VariablesMap variables,
            UserType defaultRecipient, String transportName, Transport transport, Task task, OperationResult result) {
        List<String> addresses = new ArrayList<>();
        if (!generalNotifierType.getRecipientExpression().isEmpty()) {
            for (ExpressionType expressionType : generalNotifierType.getRecipientExpression()) {
                List<String> r = evaluateExpressionChecked(expressionType, variables, "notification recipient", task, result);
                if (r != null) {
                    addresses.addAll(r);
                }
            }
            if (addresses.isEmpty()) {
                getLogger().info("Notification for " + event + " will not be sent, because there are no known recipients.");
            }
        } else if (defaultRecipient == null) {
            getLogger().info("Unknown default recipient, notification will not be sent.");
        } else {
            String address = transport.getDefaultRecipientAddress(defaultRecipient);
            if (StringUtils.isEmpty(address)) {
                getLogger().info("Notification to " + defaultRecipient.getName() + " will not be sent, because the user has no address (mail, phone number, etc) for transport '" + transportName + "' set.");
            } else {
                addresses.add(address);
            }
        }
        return addresses;
    }

    @NotNull
    private List<String> getCcBccAddresses(List<ExpressionType> expressions, VariablesMap variables,
            String shortDesc, Task task, OperationResult result) {
        List<String> addresses = new ArrayList<>();
        for (ExpressionType expressionType : expressions) {
            List<String> r = evaluateExpressionChecked(expressionType, variables, shortDesc, task, result);
            if (r != null) {
                addresses.addAll(r);
            }
        }
        return addresses;
    }

    private String getSubjectFromExpression(E event, N generalNotifierType, VariablesMap variables,
            Task task, OperationResult result) {
        return getStringFromExpression(event, variables, task, result, generalNotifierType.getSubjectExpression(), "subject", false);
    }

    private String getFromFromExpression(E event, N generalNotifierType, VariablesMap variables,
            Task task, OperationResult result) {
        return getStringFromExpression(event, variables, task, result, generalNotifierType.getFromExpression(), "from", true);
    }

    private String getContentTypeFromExpression(E event, N generalNotifierType, VariablesMap variables,
            Task task, OperationResult result) {
        return getStringFromExpression(event, variables, task, result, generalNotifierType.getContentTypeExpression(), "contentType", true);
    }

    private String getStringFromExpression(E event, VariablesMap variables,
            Task task, OperationResult result, ExpressionType expression, String expressionTypeName, boolean canBeNull) {
        if (expression != null) {
            List<String> contentTypeList = evaluateExpressionChecked(expression, variables, expressionTypeName + " expression",
                    task, result);
            if (contentTypeList == null || contentTypeList.isEmpty()) {
                getLogger().info(expressionTypeName + " expression for event " + event.getId() + " returned nothing.");
                return canBeNull ? null : "";
            }
            if (contentTypeList.size() > 1) {
                getLogger().warn(expressionTypeName + " expression for event " + event.getId() + " returned more than 1 item.");
            }
            return contentTypeList.get(0);
        } else {
            return null;
        }
    }

    private String getBodyFromExpression(E event, @NotNull ExpressionType bodyExpression, VariablesMap variables,
            Task task, OperationResult result) {
        List<String> bodyList = evaluateExpressionChecked(bodyExpression, variables,
                "body expression", task, result);
        if (bodyList == null || bodyList.isEmpty()) {
            getLogger().warn("Body expression for event {} returned nothing.", event.getId());
            return null;
        } else {
            StringBuilder body = new StringBuilder();
            for (String s : bodyList) {
                body.append(s);
            }
            return body.toString();
        }
    }

    private List<NotificationMessageAttachmentType> getAttachmentsFromExpression(E event, N generalNotifierType, VariablesMap variables,
            Task task, OperationResult result) {
        if (generalNotifierType.getAttachmentExpression() != null) {
            List<NotificationMessageAttachmentType> attachment = evaluateNotificationMessageAttachmentTypeExpressionChecked(generalNotifierType.getAttachmentExpression(), variables, "contentType expression",
                    task, result);
            if (attachment == null) {
                getLogger().info("attachment expression for event " + event.getId() + " returned nothing.");
                return null;
            }
            return attachment;
        } else {
            return null;
        }
    }

    boolean isWatchAuxiliaryAttributes(N configuration) {
        return Boolean.TRUE.equals(configuration.isWatchAuxiliaryAttributes());
    }

    String formatRequester(E event, OperationResult result) {
        SimpleObjectRef requesterRef = event.getRequester();
        if (requesterRef == null) {
            return "(unknown or none)";
        }
        ObjectType requester = requesterRef.resolveObjectType(result, false);
        String name = PolyString.getOrig(requester.getName());
        if (requester instanceof UserType) {
            return name + " (" + PolyString.getOrig(((UserType) requester).getFullName()) + ")";
        } else {
            return name;
        }
    }

    void addRequesterAndChannelInformation(StringBuilder body, Event event, OperationResult result) {
        if (event.getRequester() != null) {
            body.append("Requester: ");
            try {
                ObjectType requester = event.getRequester().resolveObjectType(result, false);
                if (requester instanceof UserType) {
                    UserType requesterUser = (UserType) requester;
                    body.append(requesterUser.getFullName()).append(" (").append(requester.getName()).append(")");
                } else {
                    body.append(ObjectTypeUtil.toShortString(requester));
                }
            } catch (RuntimeException e) {
                body.append("couldn't be determined: ").append(e.getMessage());
                LoggingUtils.logUnexpectedException(getLogger(), "Couldn't determine requester for a notification", e);
            }
            body.append("\n");
        }
        body.append("Channel: ").append(event.getChannel()).append("\n\n");
    }

}

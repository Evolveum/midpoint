/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.notifiers;

import static com.evolveum.midpoint.schema.constants.ExpressionConstants.VAR_RECIPIENT;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.api.transports.TransportService;
import com.evolveum.midpoint.notifications.impl.NotificationFunctions;
import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
import com.evolveum.midpoint.notifications.impl.formatters.ValueFormatter;
import com.evolveum.midpoint.notifications.impl.handlers.AggregatedEventHandler;
import com.evolveum.midpoint.notifications.impl.handlers.BaseHandler;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.util.SubscriptionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Base class for most notifiers.
 */
@Component
public abstract class AbstractGeneralNotifier<E extends Event, N extends GeneralNotifierType>
        extends BaseHandler<E, N> {

    private static final Trace DEFAULT_LOGGER = TraceManager.getTrace(AbstractGeneralNotifier.class);

    private static final String OP_PROCESS_EVENT = AbstractGeneralNotifier.class.getName() + ".processEvent";
    private static final String OP_PREPARE_AND_SEND = AbstractGeneralNotifier.class.getName() + ".prepareAndSend";

    @Autowired protected NotificationManager notificationManager;
    @Autowired protected NotificationFunctions functions;
    @Autowired protected TextFormatter textFormatter;
    @Autowired protected ValueFormatter valueFormatter;
    @Autowired protected AggregatedEventHandler aggregatedEventHandler;
    @Autowired protected TransportService transportService;

    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private LocalizationService localizationService;

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
                            // TODO unify with transportConfig
                            for (String transportName : notifierConfiguration.getTransport()) {
                                messagesSent += prepareAndSendMessages(
                                        event, notifierConfiguration, variables, transportName, task, result);
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

    /** Creates recipient list and sends a message for each recipient. */
    private int prepareAndSendMessages(E event, N notifierConfig, VariablesMap variables, String transportName,
            Task task, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.subresult(OP_PREPARE_AND_SEND)
                .setMinor()
                .addParam("transportName", transportName)
                .addContext("notifier", this.getClass().getName())
                .build();
        try {
            variables.put(ExpressionConstants.VAR_TRANSPORT_NAME, transportName, String.class);
            Transport<?> transport = transportService.getTransport(transportName);

            List<RecipientExpressionResultType> recipients =
                    getRecipients(event, notifierConfig, variables, task, result);
            result.addArbitraryObjectCollectionAsContext("recipients", recipients);

            if (recipients.isEmpty()) {
                getLogger().debug("No recipients for transport {}, message corresponding"
                        + " to event {} will not be send.", transportName, event.getId());
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "No recipients");
                return 0;
            }

            // TODO: Here we have string addresses already, this does not allow transport to have
            //  its own strategy (e.g. a default one) to obtain the address from the focus type.
            //  We should work here with recipient structure that can be either focus (for later
            //  address resolution) or address (string, as is now).
            //  Before this "recipientAddressExpression" in GeneralTransportConfigurationType makes
            //  no sense (also see related TODO for that type in common-notifications-3.xsd).
            //  Technically, recipientExpression could return either String (current state) or Focus,
            //  code can handle either of that with focus being resolved later by the transport.

            int sentMessages = 0;
            for (RecipientExpressionResultType recipient : recipients) {
                sentMessages += prepareAndSendMessage(event,
                        notifierConfig, variables, transport, transportName, recipient, task, result);
            }
            return sentMessages;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private int prepareAndSendMessage(E event, N notifierConfig, VariablesMap variables,
            @NotNull Transport<?> transport, @Deprecated String transportName,
            RecipientExpressionResultType recipient, Task task, OperationResult result)
            throws SchemaException {
        // TODO this is what we want in 4.6, parameter must go
        //  But this will also mean rewriting existing tests from legacy to new transport style.
        // String transportName = transport.getName();

        String address = getRecipientAddress(event, transport, recipient, task, result);
        if (address == null) {
            getLogger().debug("Skipping notification as no recipient address was provided for transport '{}'.", transportName);
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "No recipient address provided be notifier or transport");
            return 0;
        }

        MessageTemplateContentType messageTemplateContent = findMessageContent(notifierConfig, recipient, result);

        String body = getBody(event, notifierConfig, messageTemplateContent, variables, transportName, task, result);
        if (body == null) {
            getLogger().debug("Skipping notification as null body was provided for transport '{}'.", transportName);
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "No message body");
            return 0;
        }

        String subscriptionFooter =
                SubscriptionUtil.missingSubscriptionAppeal(systemObjectCache, localizationService,
                        LocalizationUtil.toLocale(focusLanguageOrLocale(recipient)));
        if (subscriptionFooter != null) {
            body += '\n' + subscriptionFooter;
        }

        String contentType = notifierConfig.getContentType();
        if (contentType == null && messageTemplateContent != null) {
            contentType = messageTemplateContent.getContentType();
        }
        if (contentType == null) {
            // Message template does not have contentTypeExpression, no need to check.
            contentType = getContentTypeFromExpression(event, notifierConfig, variables, task, result);
        }
        if (contentType == null) {
            // default hardcoded in notifier classes
            contentType = getContentType();
        }

        ExpressionType subjectExpression = notifierConfig.getSubjectExpression();
        if (subjectExpression == null && messageTemplateContent != null) {
            subjectExpression = messageTemplateContent.getSubjectExpression();
        }
        String subject;
        if (subjectExpression != null) {
            subject = getStringFromExpression(event, variables, task, result, subjectExpression, "subject", false);
        } else {
            String subjectPrefix = notifierConfig.getSubjectPrefix();
            String defaultSubject = getSubject(event, notifierConfig, transportName, task, result);
            subject = Strings.isNullOrEmpty(subjectPrefix)
                    ? defaultSubject // can be null
                    : subjectPrefix + Strings.nullToEmpty(defaultSubject); // here we don't want nulls, but ""
        }

        List<NotificationMessageAttachmentType> attachments = new ArrayList<>();
        ExpressionType attachmentExpression = notifierConfig.getAttachmentExpression();
        if (attachmentExpression == null && messageTemplateContent != null) {
            attachmentExpression = messageTemplateContent.getAttachmentExpression();
        }

        if (attachmentExpression != null) {
            attachments.addAll(getAttachmentsFromExpression(event, attachmentExpression, variables, task, result));
        }
        if (messageTemplateContent != null) {
            attachments.addAll(messageTemplateContent.getAttachment());
        }
        attachments.addAll(notifierConfig.getAttachment());
        if (attachments.isEmpty() && attachmentExpression == null) {
            List<NotificationMessageAttachmentType> defaultAttachments =
                    getAttachment(event, notifierConfig, transportName, task, result);
            if (defaultAttachments != null) {
                attachments.addAll(defaultAttachments);
            }
        }

        // setting prepared message content
        Message message = new Message();
        message.setBody(body);
        message.setSubject(subject);
        message.setContentType(contentType);
        message.setAttachments(attachments);

        // setting addressing information
        message.setFrom(getFromFromExpression(event, notifierConfig, variables, task, result));
        message.setTo(List.of(address));
        message.setCc(getCcBccAddresses(notifierConfig.getCcExpression(),
                variables, "notification cc-expression", task, result));
        message.setBcc(getCcBccAddresses(notifierConfig.getBccExpression(),
                variables, "notification bcc-expression", task, result));

        getLogger().trace("Sending notification via transport {}:\n{}", transportName, message);
        transport.send(message, transportName, event, task, result);
        return 1;
    }

    @Nullable
    private String getRecipientAddress(E event, @NotNull Transport<?> transport,
            RecipientExpressionResultType recipient, Task task, OperationResult result) {
        String address = recipient.getAddress();
        if (address == null) {
            ObjectReferenceType recipientRef = recipient.getRecipientRef();
            if (recipientRef != null) {
                // TODO the recipient object from ref may lack telephoneNumber, email, of has old data.
                //  This happens when actor is logged in (e.g. administrator) and changed some of this info
                //  and did not re-login.
                Objectable object = recipientRef.asReferenceValue().getOriginObject();
                if (object instanceof FocusType) {
                    return getRecipientAddressFromFocus(event, transport, (FocusType) object, task, result);
                }
            }
        }
        return address;
    }

    private String getRecipientAddressFromFocus(E event,
            Transport<?> transport, FocusType focus, Task task, OperationResult result) {
        GeneralTransportConfigurationType transportConfiguration = transport.getConfiguration();
        if (transportConfiguration != null) {
            // Null can happen for legacy and Dummy transport, but these don't support address from focus anyway.
            ExpressionType recipientAddressExpression = transportConfiguration.getRecipientAddressExpression();
            if (recipientAddressExpression != null) {
                VariablesMap variables = new VariablesMap();
                variables.put(VAR_RECIPIENT, focus, FocusType.class);
                return getStringFromExpression(event, variables, task, result,
                        recipientAddressExpression, "recipient address expression", true);
            }
        }
        return transport.getDefaultRecipientAddress(focus);
    }

    @Nullable
    private String getBody(E event, N notifierConfig, MessageTemplateContentType messageContent,
            VariablesMap variables, String transportName, Task task, OperationResult result) throws SchemaException {
        ExpressionType bodyExpression = notifierConfig.getBodyExpression();
        if (bodyExpression == null && messageContent != null) {
            bodyExpression = messageContent.getBodyExpression();
        }
        if (bodyExpression != null) {
            return getBodyFromExpression(event, bodyExpression, variables, task, result);
        } else {
            // default hardcoded in notifier classes
            return getBody(event, notifierConfig, transportName, task, result);
        }
    }

    @Nullable
    private MessageTemplateContentType findMessageContent(
            N notifierConfig, RecipientExpressionResultType recipient, OperationResult result) {
        ObjectReferenceType messageTemplateRef = notifierConfig.getMessageTemplateRef();
        if (messageTemplateRef != null) {
            MessageTemplateType messageTemplate = (MessageTemplateType) functions.getObject(messageTemplateRef, true, result);
            if (messageTemplate == null) {
                getLogger().warn("Message template with OID {} not found, content will be constructed"
                        + " from the notifier: {}", messageTemplateRef.getOid(), notifierConfig);
            } else {
                MessageTemplateContentType content = messageTemplate.getDefaultContent();
                ObjectReferenceType recipientRef = recipient.getRecipientRef();
                if (recipientRef != null) {
                    MessageTemplateContentType localizedContent = findLocalizedContent(messageTemplate, recipientRef);
                    if (localizedContent != null) {
                        inheritAttachmentSetupFromDefaultContent(localizedContent, content);
                        content = localizedContent; // otherwise it's default content
                    }
                }
                return content;
            }
        }
        return null;
    }

    private void inheritAttachmentSetupFromDefaultContent(
            MessageTemplateContentType localizedContent, MessageTemplateContentType defaultContent) {
        List<NotificationMessageAttachmentType> localizedAttachments = localizedContent.getAttachment();
        List<NotificationMessageAttachmentType> defaultAttachments = defaultContent.getAttachment();
        if (localizedAttachments.isEmpty() && !defaultAttachments.isEmpty()) {
            localizedAttachments.addAll(defaultAttachments);
        }

        ExpressionType defaultAttachmentExpression = defaultContent.getAttachmentExpression();
        if (localizedContent.getAttachmentExpression() == null && defaultAttachmentExpression != null) {
            localizedContent.setAttachmentExpression(defaultAttachmentExpression);
        }
    }

    private MessageTemplateContentType findLocalizedContent(
            @NotNull MessageTemplateType messageTemplate, @NotNull ObjectReferenceType recipientRef) {
        FocusType recipientFocus = (FocusType) recipientRef.asReferenceValue().getOriginObject();
        String recipientLocale = FocusTypeUtil.languageOrLocale(recipientFocus);
        if (recipientLocale != null) {
            // TODO: Currently supports only equal strings - add matching of en-US to en if en-US is not available, etc.
            for (LocalizedMessageTemplateContentType localizedContent : messageTemplate.getLocalizedContent()) {
                if (recipientLocale.equals(localizedContent.getLanguage())) {
                    return localizedContent;
                }
            }
        }
        return null;
    }

    private String focusLanguageOrLocale(RecipientExpressionResultType recipient) {
        ObjectReferenceType recipientRef = recipient.getRecipientRef();
        if (recipientRef != null) {
            return FocusTypeUtil.languageOrLocale(
                    (FocusType) recipientRef.asReferenceValue().getOriginObject());
        }
        return null;
    }

    protected String getContentType() {
        return null;
    }

    /**
     * Checks the event/notifier applicability _before_ nested filters are applied. So this check should be:
     * 1) quick
     * 2) safe - it should not make any assumptions about event content that would cause it to throw an exception
     * 3) filter out events that obviously do not match the notifier - e.g. simpleUserNotifier should ensure that
     * the focus type is really UserType; this allows nested filters to assume existence of
     * e.g. requestee.fullName element.
     */
    protected boolean quickCheckApplicability(E event, N generalNotifierType, OperationResult result) {
        return true;
    }

    /**
     * Checks the event/notifier applicability _after_ nested filters are applied.
     */
    protected boolean checkApplicability(E event, N generalNotifierType, OperationResult result) {
        return true;
    }

    protected String getSubject(E event, N generalNotifierType, String transport, Task task, OperationResult result) {
        return null;
    }

    /** Returns default body if no body expression is used. */
    protected String getBody(E event, N generalNotifierType, String transport, Task task, OperationResult result)
            throws SchemaException {
        return null;
    }

    /**
     * Returns default attachments, only used if no attachment is specified in notifier or message template.
     * This is not called even if some attachment expression is specified and returns null because that
     * explicitly means "no attachments".
     */
    protected List<NotificationMessageAttachmentType> getAttachment(
            E event, N generalNotifierType, String transportName, Task task, OperationResult result) {
        return null;
    }

    protected UserType getDefaultRecipient(E event, OperationResult result) {
        ObjectType objectType = functions.getObject(event.getRequestee(), true, result);
        if (objectType instanceof UserType) {
            return (UserType) objectType;
        } else {
            return null;
        }
    }

    protected Trace getLogger() {
        return DEFAULT_LOGGER; // in case a subclass does not provide its own logger
    }

    private List<RecipientExpressionResultType> getRecipients(E event,
            N generalNotifierType, VariablesMap variables, Task task, OperationResult result) {
        List<RecipientExpressionResultType> recipients = new ArrayList<>();
        for (ExpressionType expressionType : generalNotifierType.getRecipientExpression()) {
            List<RecipientExpressionResultType> r = expressionHelper.evaluateRecipientExpressionChecked(
                    expressionType, variables, "notification recipient", task, result);
            if (r != null) {
                recipients.addAll(r);
            }
        }

        if (recipients.isEmpty()) {
            getLogger().trace("No recipients from expression, trying default recipient from notifier; event: {}", event);
            UserType defaultRecipient = getDefaultRecipient(event, result);
            if (defaultRecipient != null) {
                RecipientExpressionResultType recipient = new RecipientExpressionResultType();
                ObjectReferenceType ref = new ObjectReferenceType();
                ref.asReferenceValue().setOriginObject(defaultRecipient);
                recipient.setRecipientRef(ref);
                recipients.add(recipient);
            }
        }

        return recipients;
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

    private String getFromFromExpression(E event, N generalNotifierType, VariablesMap variables,
            Task task, OperationResult result) {
        return getStringFromExpression(event, variables,
                task, result, generalNotifierType.getFromExpression(), "from", true);
    }

    private String getContentTypeFromExpression(E event, N generalNotifierType, VariablesMap variables,
            Task task, OperationResult result) {
        return getStringFromExpression(event, variables,
                task, result, generalNotifierType.getContentTypeExpression(), "contentType", true);
    }

    private String getStringFromExpression(E event, VariablesMap variables,
            Task task, OperationResult result, ExpressionType expression, String expressionTypeName, boolean canBeNull) {
        if (expression != null) {
            List<String> resultValues = evaluateExpressionChecked(
                    expression, variables, expressionTypeName + " expression", task, result);
            if (resultValues == null || resultValues.isEmpty()) {
                getLogger().debug(expressionTypeName + " expression for event " + event.getId() + " returned nothing.");
                return canBeNull ? null : "";
            }
            if (resultValues.size() > 1) {
                getLogger().warn(expressionTypeName + " expression for event " + event.getId() + " returned more than 1 item.");
            }
            return resultValues.get(0);
        } else {
            return null;
        }
    }

    private String getBodyFromExpression(E event, @NotNull ExpressionType bodyExpression, VariablesMap variables,
            Task task, OperationResult result) {
        List<String> bodyList = evaluateExpressionChecked(
                bodyExpression, variables, "body expression", task, result);
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

    private @NotNull List<NotificationMessageAttachmentType> getAttachmentsFromExpression(E event,
            @NotNull ExpressionType attachmentExpression, VariablesMap variables, Task task, OperationResult result) {
        List<NotificationMessageAttachmentType> attachment = expressionHelper.evaluateAttachmentExpressionChecked(
                attachmentExpression, variables, "attachment expression", task, result);
        if (attachment == null) {
            getLogger().debug("attachment expression for event {} returned nothing.", event.getId());
            return List.of();
        }
        return attachment;
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

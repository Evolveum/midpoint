/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.notifiers;

import static com.evolveum.midpoint.schema.constants.ExpressionConstants.VAR_RECIPIENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;

import com.google.common.base.Strings;
import org.apache.commons.collections4.CollectionUtils;
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
import com.evolveum.midpoint.repo.common.subscription.SubscriptionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Base class for most notifiers, i.e. handlers that produce notifications out of events.
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
    public boolean processEvent(
            @NotNull ConfigurationItem<? extends N> notifierConfig,
            @NotNull EventProcessingContext<? extends E> ctx,
            @NotNull OperationResult parentResult)
            throws SchemaException {

        OperationResult result = parentResult.subresult(OP_PROCESS_EVENT)
                .setMinor()
                .addContext("notifier", this.getClass().getName())
                .build();
        int messagesSent = 0;
        E event = ctx.event();
        try {
            logStart(getLogger(), notifierConfig, ctx);

            boolean applies = false;
            if (!quickCheckApplicability(notifierConfig, ctx, result)) {
                // nothing to do -- an appropriate message should have been logged in quickCheckApplicability method
                result.recordNotApplicable();
            } else {
                if (aggregatedEventHandler.processEvent(notifierConfig, ctx, result)) {
                    if (!checkApplicability(notifierConfig, ctx, result)) {
                        // nothing to do -- an appropriate message should have been logged in checkApplicability method
                        result.recordNotApplicable();
                    } else if (notifierConfig.value().getTransport().isEmpty()) {
                        getLogger().warn("No transports for this notifier, exiting without sending any notifications.");
                        result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "No transports");
                    } else {
                        applies = true;
                        reportNotificationStart(event);
                        try {
                            VariablesMap variables = getDefaultVariables(event, result);
                            // TODO unify with transportConfig
                            for (String transportName : notifierConfig.value().getTransport()) {
                                messagesSent += prepareAndSendMessages(
                                        notifierConfig, variables, transportName, ctx, result);
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
            result.recordException(t);
            throw t;
        } finally {
            result.addReturn("messagesSent", messagesSent);
            result.close();
        }
    }

    /** Creates recipient list and sends a message for each recipient. */
    private int prepareAndSendMessages(
            ConfigurationItem<? extends N> notifierConfig, VariablesMap variables, String transportName,
            EventProcessingContext<? extends E> ctx, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.subresult(OP_PREPARE_AND_SEND)
                .setMinor()
                .addParam("transportName", transportName)
                .addContext("notifier", this.getClass().getName())
                .build();
        try {
            var event = ctx.event();
            variables.put(ExpressionConstants.VAR_TRANSPORT_NAME, transportName, String.class);
            Transport<?> transport = transportService.getTransport(transportName);

            List<RecipientExpressionResultType> recipients = getRecipients(notifierConfig, variables, ctx, result);
            result.addArbitraryObjectCollectionAsContext("recipients", recipients);

            if (recipients.isEmpty()) {
                getLogger().debug("No recipients for transport {}, message corresponding"
                        + " to event {} will not be send.", transportName, event.getId());
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "No recipients");
                return 0;
            }

            boolean sendSeparateMessageToEachRecipient = sendSeparateMessageToEachRecipient(notifierConfig);
            int sentMessages = 0;

            if (sendSeparateMessageToEachRecipient) {
                // TODO: Here we have string addresses already, this does not allow transport to have
                //  its own strategy (e.g. a default one) to obtain the address from the focus type.
                //  We should work here with recipient structure that can be either focus (for later
                //  address resolution) or address (string, as is now).
                //  Before this "recipientAddressExpression" in GeneralTransportConfigurationType makes
                //  no sense (also see related TODO for that type in common-notifications-3.xsd).
                //  Technically, recipientExpression could return either String (current state) or Focus,
                //  code can handle either of that with focus being resolved later by the transport.

                for (RecipientExpressionResultType recipient : recipients) {
                    Message message = prepareMessage(notifierConfig, variables, transport, transportName, recipient, ctx, result);
                    String address = getRecipientAddress(transport, transportName, recipient, ctx, result);
                    sentMessages +=
                            sendMessage(message, transport, transportName, Collections.singletonList(address), ctx, result);
                }
            } else {
                Message message = prepareMessage(notifierConfig, variables, transport, transportName, null, ctx, result);
                List<String> recipientAddresses = collectRecipientAddresses(transport, transportName, recipients, ctx, result);
                sentMessages = sendMessage(message, transport, transportName, recipientAddresses, ctx, result);
                if (sentMessages > 0) {
                    sentMessages = recipientAddresses.size();
                }
            }
            return sentMessages;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private boolean sendSeparateMessageToEachRecipient(ConfigurationItem<? extends N> notifierConfig) {
        NotificationSendingStrategyType sendingStrategy = notifierConfig.value().getNotificationSendingStrategy();
        return sendingStrategy == null || NotificationSendingStrategyType.SEPARATE_NOTIFICATION_TO_EACH_RECIPIENT.equals(sendingStrategy);
    }

    private Message prepareMessage(
            ConfigurationItem<? extends N> notifierConfig, VariablesMap variables,
            @NotNull Transport<?> transport, @Deprecated String transportName,
            @Nullable RecipientExpressionResultType recipient,
            EventProcessingContext<? extends E> ctx, OperationResult result)
            throws SchemaException {
        MessageTemplateContentType messageTemplateContent = findMessageContent(notifierConfig.value(), recipient, result);

        String body = getBody(notifierConfig, messageTemplateContent, variables, transportName, ctx, result);
        if (body == null) {
            return new Message();
        }

        Locale locale = recipient != null ? LocalizationUtil.toLocale(focusLanguageOrLocale(recipient)) : null;
        String subscriptionFooter =
                SubscriptionUtil.missingSubscriptionAppeal(localizationService, locale);
        if (subscriptionFooter != null) {
            body += '\n' + subscriptionFooter;
        }

        String contentType = notifierConfig.value().getContentType();
        if (contentType == null && messageTemplateContent != null) {
            contentType = messageTemplateContent.getContentType();
        }
        if (contentType == null) {
            // Message template does not have contentTypeExpression, no need to check.
            contentType = getContentTypeFromExpression(notifierConfig, variables, ctx, result);
        }
        if (contentType == null) {
            // default hardcoded in notifier classes
            contentType = getContentType();
        }

        ExpressionType subjectExpression = notifierConfig.value().getSubjectExpression();
        if (subjectExpression == null && messageTemplateContent != null) {
            subjectExpression = messageTemplateContent.getSubjectExpression();
        }
        String subject;
        if (subjectExpression != null) {
            subject = getStringFromExpression(
                    subjectExpression,
                    ConfigurationItemOrigin.undeterminedSafe(), // TODO
                    "subject",
                    false,
                    variables,
                    ctx, result);
        } else {
            String subjectPrefix = notifierConfig.value().getSubjectPrefix();
            String defaultSubject = getSubject(notifierConfig, transportName, ctx, result);
            subject = Strings.isNullOrEmpty(subjectPrefix)
                    ? defaultSubject // can be null
                    : subjectPrefix + Strings.nullToEmpty(defaultSubject); // here we don't want nulls, but ""
        }

        List<NotificationMessageAttachmentType> attachments = new ArrayList<>();
        ExpressionType attachmentExpression = notifierConfig.value().getAttachmentExpression();
        if (attachmentExpression == null && messageTemplateContent != null) {
            attachmentExpression = messageTemplateContent.getAttachmentExpression();
        }

        if (attachmentExpression != null) {
            attachments.addAll(
                    getAttachmentsFromExpression(
                            attachmentExpression,
                            ConfigurationItemOrigin.undeterminedSafe(), // TODO
                            variables,
                            ctx, result));
        }
        if (messageTemplateContent != null) {
            attachments.addAll(messageTemplateContent.getAttachment());
        }
        attachments.addAll(notifierConfig.value().getAttachment());
        if (attachments.isEmpty() && attachmentExpression == null) {
            var defaultAttachments = getAttachment(notifierConfig, transportName, ctx, result);
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
        message.setFrom(getFromFromExpression(notifierConfig, variables, ctx, result));
        message.setCc(getCcBccAddresses(notifierConfig.value().getCcExpression(),
                variables, "notification cc-expression", ctx, result));
        message.setBcc(getCcBccAddresses(notifierConfig.value().getBccExpression(),
                variables, "notification bcc-expression", ctx, result));
        return message;
    }

    private String getRecipientAddress(@NotNull Transport<?> transport, @Deprecated String transportName,
            RecipientExpressionResultType recipient, EventProcessingContext<? extends E> ctx, OperationResult result) {
        List<String> address = collectRecipientAddresses(transport, transportName, Collections.singletonList(recipient), ctx,
                result);
        return CollectionUtils.isNotEmpty(address) ? address.get(0) : null;
    }

    private List<String> collectRecipientAddresses(@NotNull Transport<?> transport, @Deprecated String transportName,
            List<RecipientExpressionResultType> recipients,
            EventProcessingContext<? extends E> ctx, OperationResult result) {
        List<String> recipientAddresses = new ArrayList<>();
        recipients.forEach(r -> {
                    String address = getRecipientAddress(transport, r, ctx, result);
                    if (address == null) {
                        getLogger().debug("Skipping notification as no recipient address was provided or determined for transport '{}'.", transportName);
                        result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "No recipient address provided/determined for notifier or transport, recipient: ");
                    }
                    recipientAddresses.add(address);
                }
        );
        return recipientAddresses;
    }

    private int sendMessage(
            @NotNull Message message,
            @NotNull Transport<?> transport, @Deprecated String transportName,
            List<String> addresses,
            EventProcessingContext<? extends E> ctx, OperationResult result) {
        // TODO this is what we want in 4.6, parameter must go
        //  But this will also mean rewriting existing tests from legacy to new transport style.
        // String transportName = transport.getName();

        String body = message.getBody();
        if (body == null) {
            getLogger().debug("Skipping notification as null body was provided for transport '{}'.", transportName);
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "No message body");
            return 0;
        }

        message.setTo(addresses);

        getLogger().trace("Sending notification via transport {}:\n{}", transportName, message);
        transport.send(
                message, transportName, ctx.sendingContext(), result);
        return 1;
    }

    @Nullable
    private String getRecipientAddress(
            @NotNull Transport<?> transport, RecipientExpressionResultType recipient,
            EventProcessingContext<? extends E> ctx, OperationResult result) {
        String address = recipient.getAddress();
        if (address == null) {
            ObjectReferenceType recipientRef = recipient.getRecipientRef();
            if (recipientRef != null) {
                // TODO the recipient object from ref may lack telephoneNumber, email, of has old data.
                //  This happens when actor is logged in (e.g. administrator) and changed some of this info
                //  and did not re-login.
                Objectable object = recipientRef.asReferenceValue().getOriginObject();
                if (object instanceof FocusType) {
                    return getRecipientAddressFromFocus(transport, (FocusType) object, ctx, result);
                }
            }
        }
        return address;
    }

    private String getRecipientAddressFromFocus(
            Transport<?> transport, FocusType focus, EventProcessingContext<? extends E> ctx, OperationResult result) {
        GeneralTransportConfigurationType transportConfiguration = transport.getConfiguration();
        if (transportConfiguration != null) {
            // Null can happen for legacy and Dummy transport, but these don't support address from focus anyway.
            ExpressionType recipientAddressExpression = transportConfiguration.getRecipientAddressExpression();
            if (recipientAddressExpression != null) {
                VariablesMap variables = new VariablesMap();
                variables.put(VAR_RECIPIENT, focus, FocusType.class);
                return getStringFromExpression(
                        recipientAddressExpression,
                        ConfigurationItemOrigin.undeterminedSafe(), // TODO
                        "recipient address expression",
                        true,
                        variables, ctx, result);
            }
        }
        return transport.getDefaultRecipientAddress(focus);
    }

    @Nullable
    private String getBody(
            ConfigurationItem<? extends N> notifierConfig, MessageTemplateContentType messageContent,
            VariablesMap variables, String transportName, EventProcessingContext<? extends E> ctx, OperationResult result)
            throws SchemaException {
        ExpressionType bodyExpression = notifierConfig.value().getBodyExpression();
        if (bodyExpression == null && messageContent != null) {
            bodyExpression = messageContent.getBodyExpression();
        }
        if (bodyExpression != null) {
            return getBodyFromExpression(
                    bodyExpression,
                    ConfigurationItemOrigin.undeterminedSafe(), // TODO
                    variables,
                    ctx, result);
        } else {
            // default hardcoded in notifier classes
            return getBody(notifierConfig, transportName, ctx, result);
        }
    }

    @Nullable
    private MessageTemplateContentType findMessageContent(
            N notifierConfigBean, @Nullable RecipientExpressionResultType recipient, OperationResult result) {
        ObjectReferenceType messageTemplateRef = notifierConfigBean.getMessageTemplateRef();
        if (messageTemplateRef != null) {
            MessageTemplateType messageTemplate =
                    (MessageTemplateType) functions.getObject(messageTemplateRef, true, result);
            if (messageTemplate == null) {
                getLogger().warn("Message template with OID {} not found, content will be constructed"
                        + " from the notifier: {}", messageTemplateRef.getOid(), notifierConfigBean);
            } else {
                MessageTemplateContentType content = messageTemplate.getDefaultContent();
                if (recipient != null) {
                    ObjectReferenceType recipientRef = recipient.getRecipientRef();
                    if (recipientRef != null) {
                        MessageTemplateContentType localizedContent = findLocalizedContent(messageTemplate, recipientRef);
                        if (localizedContent != null) {
                            inheritAttachmentSetupFromDefaultContent(localizedContent, content);
                            content = localizedContent; // otherwise it's default content
                        }
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
            defaultAttachments.forEach(n -> localizedAttachments.add(n.clone()));
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
     *
     * . quick
     * . safe - it should not make any assumptions about event content that would cause it to throw an exception
     * . filter out events that obviously do not match the notifier - e.g. simpleUserNotifier should ensure that
     * the focus type is really UserType; this allows nested filters to assume existence of
     * e.g. requestee.fullName element.
     */
    protected boolean quickCheckApplicability(
            ConfigurationItem<? extends N> notifierConfig,
            EventProcessingContext<? extends E> ctx,
            OperationResult result) {
        return true;
    }

    /**
     * Checks the event/notifier applicability _after_ nested filters are applied.
     */
    protected boolean checkApplicability(
            ConfigurationItem<? extends N> notifierConfig,
            EventProcessingContext<? extends E> ctx,
            OperationResult result) {
        return true;
    }

    protected String getSubject(
            ConfigurationItem<? extends N> notifierConfig,
            String transport,
            EventProcessingContext<? extends E> ctx,
            OperationResult result) {
        return null;
    }

    /** Returns default body if no body expression is used. */
    protected String getBody(
            ConfigurationItem<? extends N> notifierConfig,
            String transport,
            EventProcessingContext<? extends E> ctx,
            OperationResult result)
            throws SchemaException {
        return null;
    }

    /**
     * Returns default attachments, only used if no attachment is specified in notifier or message template.
     * This is not called even if some attachment expression is specified and returns null because that
     * explicitly means "no attachments".
     */
    protected List<NotificationMessageAttachmentType> getAttachment(
            ConfigurationItem<? extends N> notifierConfig,
            String transportName,
            EventProcessingContext<? extends E> ctx,
            OperationResult result) {
        return null;
    }

    // TODO why are we limited to users here?
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

    private List<RecipientExpressionResultType> getRecipients(
            ConfigurationItem<? extends N> config, VariablesMap variables,
            EventProcessingContext<? extends E> ctx, OperationResult result) {

        var event = ctx.event();
        List<RecipientExpressionResultType> recipients = new ArrayList<>();
        for (ExpressionType expressionBean : config.value().getRecipientExpression()) {
            List<RecipientExpressionResultType> r = expressionHelper.evaluateRecipientExpressionChecked(
                    expressionBean, config.origin().toApproximate(), // TODO precise origin
                    variables, "notification recipient", ctx, result);
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
    private List<String> getCcBccAddresses(
            List<ExpressionType> expressions, VariablesMap variables,
            String shortDesc, EventProcessingContext<?> ctx, OperationResult result) {
        List<String> addresses = new ArrayList<>();
        for (ExpressionType expression : expressions) {
            List<String> r = evaluateExpressionChecked(
                    expression, ConfigurationItemOrigin.undeterminedSafe(), // TODO origin
                    variables, shortDesc, ctx, result);
            if (r != null) {
                addresses.addAll(r);
            }
        }
        return addresses;
    }

    private String getFromFromExpression(
            ConfigurationItem<? extends N> notifierConfig,
            VariablesMap variables,
            EventProcessingContext<? extends E> ctx, OperationResult result) {
        return getStringFromExpression(
                notifierConfig.value().getFromExpression(),
                notifierConfig.origin().toApproximate(), // TODO precise origin
                "from",
                true,
                variables,
                ctx, result);
    }

    private String getContentTypeFromExpression(
            ConfigurationItem<? extends N> notifierConfig, VariablesMap variables,
            EventProcessingContext<? extends E> ctx, OperationResult result) {
        return getStringFromExpression(
                notifierConfig.value().getContentTypeExpression(),
                notifierConfig.origin().toApproximate(), // TODO precise origin
                "contentType",
                true,
                variables,
                ctx, result);
    }

    private String getStringFromExpression(
            @Nullable ExpressionType expression,
            @NotNull ConfigurationItemOrigin origin,
            String expressionTypeName,
            boolean canBeNull,
            VariablesMap variables,
            @NotNull EventProcessingContext<?> ctx,
            @NotNull OperationResult result) {
        if (expression != null) {
            List<String> resultValues = evaluateExpressionChecked(
                    expression, origin, variables, expressionTypeName + " expression", ctx, result);
            if (resultValues == null || resultValues.isEmpty()) {
                getLogger().debug(expressionTypeName + " expression for event " + ctx.getEventId() + " returned nothing.");
                return canBeNull ? null : "";
            }
            if (resultValues.size() > 1) {
                getLogger().warn(expressionTypeName + " expression for event " + ctx.getEventId() + " returned more than 1 item.");
            }
            return resultValues.get(0);
        } else {
            return null;
        }
    }

    private String getBodyFromExpression(
            @NotNull ExpressionType bodyExpression, @NotNull ConfigurationItemOrigin origin, VariablesMap variables,
            EventProcessingContext<?> ctx, OperationResult result) {
        List<String> bodyList = evaluateExpressionChecked(
                bodyExpression, origin, variables, "body expression", ctx, result);
        if (bodyList == null || bodyList.isEmpty()) {
            getLogger().warn("Body expression for event {} returned nothing.", ctx.getEventId());
            return null;
        } else {
            StringBuilder body = new StringBuilder();
            for (String s : bodyList) {
                body.append(s);
            }
            return body.toString();
        }
    }

    private @NotNull List<NotificationMessageAttachmentType> getAttachmentsFromExpression(
            @NotNull ExpressionType attachmentExpression,
            @NotNull ConfigurationItemOrigin origin,
            VariablesMap variables,
            EventProcessingContext<? extends E> ctx, OperationResult result) {
        List<NotificationMessageAttachmentType> attachment =
                expressionHelper.evaluateAttachmentExpressionChecked(
                        attachmentExpression,
                        origin,
                        variables, "attachment expression",
                        ctx, result);
        if (attachment == null) {
            getLogger().debug("attachment expression for event {} returned nothing.", ctx.getEventId());
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

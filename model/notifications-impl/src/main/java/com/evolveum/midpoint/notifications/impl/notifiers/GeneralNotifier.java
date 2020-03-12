/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
import com.evolveum.midpoint.notifications.impl.handlers.AggregatedEventHandler;
import com.evolveum.midpoint.notifications.impl.handlers.BaseHandler;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.cxf.common.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.NOTIFICATIONS;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;

/**
 *
 */
@Component
public class GeneralNotifier extends BaseHandler {

    private static final Trace DEFAULT_LOGGER = TraceManager.getTrace(GeneralNotifier.class);

    @Autowired
    protected NotificationManager notificationManager;

    @Autowired
    protected NotificationFunctionsImpl functions;

    @Autowired
    protected TextFormatter textFormatter;

    @Autowired
    protected AggregatedEventHandler aggregatedEventHandler;

    @PostConstruct
    public void init() {
        register(GeneralNotifierType.class);
    }

    @Override
    public boolean processEvent(Event event, EventHandlerType eventHandlerType, NotificationManager notificationManager,
            Task task, OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createMinorSubresult(GeneralNotifier.class.getName() + ".processEvent");

        logStart(getLogger(), event, eventHandlerType);

        boolean applies = aggregatedEventHandler.processEvent(event, eventHandlerType, notificationManager, task, result);

        if (applies) {

            GeneralNotifierType generalNotifierType = (GeneralNotifierType) eventHandlerType;

            if (!quickCheckApplicability(event, generalNotifierType, result)) {

                // nothing to do -- an appropriate message has to be logged in quickCheckApplicability method

            } else {

                if (!checkApplicability(event, generalNotifierType, result)) {
                    // nothing to do -- an appropriate message has to be logged in checkApplicability method
                } else if (generalNotifierType.getTransport().isEmpty()) {
                    getLogger().warn("No transports for this notifier, exiting without sending any notifications.");
                } else {

                    ExpressionVariables variables = getDefaultVariables(event, result);

                    if (event instanceof ModelEvent) {
                        ((ModelEvent) event).getModelContext().reportProgress(new ProgressInformation(NOTIFICATIONS, ENTERING));
                    }

                    try {
                        for (String transportName : generalNotifierType.getTransport()) {

                            variables.put(ExpressionConstants.VAR_TRANSPORT_NAME, transportName, String.class);
                            Transport transport = notificationManager.getTransport(transportName);

                            List<String> recipientsAddresses = getRecipientsAddresses(event, generalNotifierType, variables,
                                    getDefaultRecipient(event, generalNotifierType, result), transportName, transport, task, result);

                            if (!recipientsAddresses.isEmpty()) {

                                String body;
                                ExpressionType bodyExpression = generalNotifierType.getBodyExpression();
                                if (bodyExpression != null) {
                                    body = getBodyFromExpression(event, bodyExpression, variables, task, result);
                                } else {
                                    body = getBody(event, generalNotifierType, transportName, task, result);
                                }
                                if (body == null) {
                                    getLogger().debug("Skipping notification as null body was provided for transport={}", transportName);
                                    continue;
                                }

                                String subject = getSubjectFromExpression(event, generalNotifierType, variables, task, result);
                                String from = getFromFromExpression(event, generalNotifierType, variables, task, result);
                                String contentType = getContentTypeFromExpression(event, generalNotifierType, variables, task, result);
                                List<NotificationMessageAttachmentType> attachments = getAttachmentsFromExpression(event, generalNotifierType, variables, task, result);

                                if (subject == null) {
                                    subject = generalNotifierType.getSubjectPrefix() != null ? generalNotifierType.getSubjectPrefix() : "";
                                    subject += getSubject(event, generalNotifierType, transportName, task, result);
                                }

                                if (attachments == null) {
                                    attachments = generalNotifierType.getAttachment();
                                    if (attachments == null) {
                                        attachments = getAttachment(event, generalNotifierType, transportName, task, result);
                                    }
                                } else if (generalNotifierType.getAttachment() != null) {
                                    attachments.addAll(generalNotifierType.getAttachment());
                                }

                                Message message = new Message();
                                message.setBody(body);
                                if (contentType != null) {
                                    message.setContentType(contentType);
                                } else if (generalNotifierType.getContentType() != null) {
                                    message.setContentType(generalNotifierType.getContentType());
                                } else if (getContentType() != null) {
                                    message.setContentType(getContentType());
                                }
                                message.setSubject(subject);

                                if (from != null) {
                                    message.setFrom(from);
                                }
                                message.setTo(recipientsAddresses);
                                message.setCc(getCcBccAddresses(generalNotifierType.getCcExpression(), variables, "notification cc-expression", task, result));
                                message.setBcc(getCcBccAddresses(generalNotifierType.getBccExpression(), variables, "notification bcc-expression", task, result));

                                if (attachments != null) {
                                    message.setAttachments(attachments);
                                }

                                getLogger().trace("Sending notification via transport {}:\n{}", transportName, message);
                                transport.send(message, transportName, event, task, result);
                            } else {
                                getLogger().info("No recipients addresses for transport " + transportName + ", message corresponding to event " + event.getId() + " will not be send.");
                            }
                        }
                    } finally {
                        if (event instanceof ModelEvent) {
                            ((ModelEvent) event).getModelContext().reportProgress(
                                    new ProgressInformation(NOTIFICATIONS, result));
                        }
                    }
                }
            }
        }
        logEnd(getLogger(), event, eventHandlerType, applies);
        result.computeStatusIfUnknown();
        return true;            // not-applicable notifiers do not stop processing of other notifiers
    }

    protected String getContentType() {
        return null;
    }

    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        return true;
    }

    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        return true;
    }

    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
        return null;
    }

    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) throws SchemaException {
        return null;
    }

    @SuppressWarnings("WeakerAccess") // open for future extension
    protected List<NotificationMessageAttachmentType> getAttachment(Event event, GeneralNotifierType generalNotifierType,
            String transportName, Task task, OperationResult result) {
        return null;
    }

    protected UserType getDefaultRecipient(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
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

    protected List<String> getRecipientsAddresses(Event event, GeneralNotifierType generalNotifierType, ExpressionVariables variables,
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
    protected List<String> getCcBccAddresses(List<ExpressionType> expressions, ExpressionVariables variables,
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

    protected String getSubjectFromExpression(Event event, GeneralNotifierType generalNotifierType, ExpressionVariables variables,
            Task task, OperationResult result) {
        return getStringFromExpression(event, variables, task, result, generalNotifierType.getSubjectExpression(), "subject", false);
    }

    protected String getFromFromExpression(Event event, GeneralNotifierType generalNotifierType, ExpressionVariables variables,
            Task task, OperationResult result) {
        return getStringFromExpression(event, variables, task, result, generalNotifierType.getFromExpression(), "from", true);
    }

    protected String getContentTypeFromExpression(Event event, GeneralNotifierType generalNotifierType, ExpressionVariables variables,
            Task task, OperationResult result) {
        return getStringFromExpression(event, variables, task, result, generalNotifierType.getContentTypeExpression(), "contentType", true);
    }

    protected String getStringFromExpression(Event event, ExpressionVariables variables,
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

    @NotNull
    private String getBodyFromExpression(Event event, @NotNull ExpressionType bodyExpression, ExpressionVariables variables,
            Task task, OperationResult result) {
        List<String> bodyList = evaluateExpressionChecked(bodyExpression, variables,
                "body expression", task, result);
        if (bodyList == null || bodyList.isEmpty()) {
            getLogger().warn("Body expression for event " + event.getId() + " returned nothing.");
            return "";
        }
        StringBuilder body = new StringBuilder();
        for (String s : bodyList) {
            body.append(s);
        }
        return body.toString();
    }

    private List<NotificationMessageAttachmentType> getAttachmentsFromExpression(Event event, GeneralNotifierType generalNotifierType, ExpressionVariables variables,
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

    // TODO implement more efficiently
    // precondition: delta is MODIFY delta
    boolean deltaContainsOtherPathsThan(ObjectDelta<? extends ObjectType> delta, List<ItemPath> paths) {

        for (ItemDelta itemDelta : delta.getModifications()) {
            if (!NotificationFunctionsImpl.isAmongHiddenPaths(itemDelta.getPath(), paths)) {
                return true;
            }
        }
        return false;
    }

    boolean isWatchAuxiliaryAttributes(GeneralNotifierType generalNotifierType) {
        return Boolean.TRUE.equals((generalNotifierType).isWatchAuxiliaryAttributes());
    }

    protected void appendModifications(StringBuilder body, ObjectDelta<? extends ObjectType> delta, List<ItemPath> hiddenPaths, Boolean showValuesBoolean) {

        boolean showValues = !Boolean.FALSE.equals(showValuesBoolean);
        for (ItemDelta<?,?> itemDelta : delta.getModifications()) {
            if (NotificationFunctionsImpl.isAmongHiddenPaths(itemDelta.getPath(), hiddenPaths)) {
                continue;
            }
            body.append(" - ");
            body.append(formatPath(itemDelta));

            if (showValues) {
                body.append(":\n");

                if (itemDelta.isAdd()) {
                    for (PrismValue prismValue : itemDelta.getValuesToAdd()) {
                        body.append(" --- ADD: ");
                        body.append(prismValue.debugDump(2));
                        body.append("\n");
                    }
                }
                if (itemDelta.isDelete()) {
                    for (PrismValue prismValue : itemDelta.getValuesToDelete()) {
                        body.append(" --- DELETE: ");
                        body.append(prismValue.debugDump(2));
                        body.append("\n");
                    }
                }
                if (itemDelta.isReplace()) {
                    for (PrismValue prismValue : itemDelta.getValuesToReplace()) {
                        body.append(" --- REPLACE: ");
                        body.append(prismValue.debugDump(2));
                        body.append("\n");
                    }
                }
            } else {
                body.append("\n");
            }
        }
    }

    private String formatPath(ItemDelta itemDelta) {
        if (itemDelta.getDefinition() != null && itemDelta.getDefinition().getDisplayName() != null) {
            return itemDelta.getDefinition().getDisplayName();
        }
        StringBuilder sb = new StringBuilder();
        for (Object segment : itemDelta.getPath().getSegments()) {
            if (ItemPath.isName(segment)) {
                if (sb.length() > 0) {
                    sb.append("/");
                }
                sb.append(ItemPath.toName(segment).getLocalPart());
            }
        }
        return sb.toString();
    }

    public String formatRequester(Event event, OperationResult result) {
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

}

/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.impl.NotificationFuctionsImpl;
import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
import com.evolveum.midpoint.notifications.impl.handlers.AggregatedEventHandler;
import com.evolveum.midpoint.notifications.impl.handlers.BaseHandler;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.cxf.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.NOTIFICATIONS;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;

/**
 * @author mederly
 */
@Component
public class GeneralNotifier extends BaseHandler {

    private static final Trace DEFAULT_LOGGER = TraceManager.getTrace(GeneralNotifier.class);

    @Autowired
    protected NotificationManager notificationManager;

    @Autowired
    protected NotificationFuctionsImpl notificationsUtil;

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

        OperationResult result = parentResult.createSubresult(GeneralNotifier.class.getName() + ".processEvent");

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
                        ((ModelEvent) event).getModelContext().reportProgress(
                                new ProgressInformation(NOTIFICATIONS, ENTERING));
                    }

                    try {
                        for (String transportName : generalNotifierType.getTransport()) {

                            variables.addVariableDefinition(SchemaConstants.C_TRANSPORT_NAME, transportName);
                            Transport transport = notificationManager.getTransport(transportName);

                            List<String> recipientsAddresses = getRecipientsAddresses(event, generalNotifierType, variables,
                                    getDefaultRecipient(event, generalNotifierType, result), transportName, transport, task, result);

                            if (!recipientsAddresses.isEmpty()) {
                            	
                                String body = getBodyFromExpression(event, generalNotifierType, variables, task, result);
                                String subject = getSubjectFromExpression(event, generalNotifierType, variables, task, result);
                                String from = getFromFromExpression(event, generalNotifierType, variables, task, result);
                                String contentType = getContentTypeFromExpression(event, generalNotifierType, variables, task, result);

                                if (body == null) {
                                    body = getBody(event, generalNotifierType, transportName, task, result);
                                }
                                if (subject == null) {
                                    subject = generalNotifierType.getSubjectPrefix() != null ? generalNotifierType.getSubjectPrefix() : "";
                                    subject += getSubject(event, generalNotifierType, transportName, task, result);
                                }

                                Message message = new Message();
                                message.setBody(body != null ? body : "");
                                message.setContentType(contentType);
                                message.setSubject(subject);
                                
                                if (from != null)message.setFrom(from);
                                message.setTo(recipientsAddresses);                      // todo cc/bcc recipients
                                List<String> cc = getCcAddresses(event, generalNotifierType, variables,  task, result);
                                if (cc != null)message.setCc(cc);
                                List<String> bcc = getBccAddresses(event, generalNotifierType, variables,  task, result);
                                if (bcc != null)message.setBcc(bcc);;

                                getLogger().trace("Sending notification via transport {}:\n{}", transportName, message);
                                transport.send(message, transportName, task, result);
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

    protected UserType getDefaultRecipient(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        ObjectType objectType = notificationsUtil.getObjectType(event.getRequestee(), true, result);
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
    
    protected List<String> getCcAddresses(Event event, GeneralNotifierType generalNotifierType, ExpressionVariables variables, 
    	Task task, OperationResult result) {
    	List<String> addresses = null;
        if (!generalNotifierType.getCcExpression().isEmpty()) {
        	addresses = new ArrayList<>();
            for (ExpressionType expressionType : generalNotifierType.getCcExpression()) {
                List<String> r = evaluateExpressionChecked(expressionType, variables, "notification cc-recipient", task, result);
                if (r != null) {
                    addresses.addAll(r);
                }
            }
            
        }
        return addresses;
    }
    
    protected List<String> getBccAddresses(Event event, GeneralNotifierType generalNotifierType, ExpressionVariables variables, 
    	Task task, OperationResult result) {
    	List<String> addresses = null;
        if (!generalNotifierType.getBccExpression().isEmpty()) {
        	addresses = new ArrayList<>();
            for (ExpressionType expressionType : generalNotifierType.getBccExpression()) {
                List<String> r = evaluateExpressionChecked(expressionType, variables, "notification cc-recipient", task, result);
                if (r != null) {
                    addresses.addAll(r);
                }
            }
            
        }
        return addresses;
    }

    protected String getSubjectFromExpression(Event event, GeneralNotifierType generalNotifierType, ExpressionVariables variables, 
    		Task task, OperationResult result) {
        if (generalNotifierType.getSubjectExpression() != null) {
            List<String> subjectList = evaluateExpressionChecked(generalNotifierType.getSubjectExpression(), variables, "subject expression", 
            		task, result);
            if (subjectList == null || subjectList.isEmpty()) {
                getLogger().warn("Subject expression for event " + event.getId() + " returned nothing.");
                return "";
            }
            if (subjectList.size() > 1) {
                getLogger().warn("Subject expression for event " + event.getId() + " returned more than 1 item.");
            }
            return subjectList.get(0);
        } else {
            return null;
        }
    }
    
    protected String getFromFromExpression(Event event, GeneralNotifierType generalNotifierType, ExpressionVariables variables, 
    		Task task, OperationResult result) {
        if (generalNotifierType.getFromExpression() != null) {
            List<String> fromList = evaluateExpressionChecked(generalNotifierType.getFromExpression(), variables, "from expression", 
            		task, result);
            if (fromList == null || fromList.isEmpty()) {
                getLogger().info("from expression for event " + event.getId() + " returned nothing.");
            }
            if (fromList.size() > 1) {
                getLogger().warn("from expression for event " + event.getId() + " returned more than 1 item.");
            }
            return fromList.get(0);
        } else {
            return null;
        }
    }
    
    protected String getContentTypeFromExpression(Event event, GeneralNotifierType generalNotifierType, ExpressionVariables variables, 
    		Task task, OperationResult result) {
        if (generalNotifierType.getContentTypeExpression() != null) {
            List<String> contentTypeList = evaluateExpressionChecked(generalNotifierType.getContentTypeExpression(), variables, "contentType expression", 
            		task, result);
            if (contentTypeList == null || contentTypeList.isEmpty()) {
                getLogger().info("contentType expression for event " + event.getId() + " returned nothing.");
            }
            if (contentTypeList.size() > 1) {
                getLogger().warn("contentType expression for event " + event.getId() + " returned more than 1 item.");
            }
            return contentTypeList.get(0);
        } else {
            return null;
        }
    }

    protected String getBodyFromExpression(Event event, GeneralNotifierType generalNotifierType, ExpressionVariables variables, 
    		Task task, OperationResult result) {
        if (generalNotifierType.getBodyExpression() != null) {
            List<String> bodyList = evaluateExpressionChecked(generalNotifierType.getBodyExpression(), variables, 
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
        } else {
            return null;
        }
    }

    // TODO implement more efficiently
    // precondition: delta is MODIFY delta
    protected boolean deltaContainsOtherPathsThan(ObjectDelta<? extends ObjectType> delta, List<ItemPath> paths) {

        for (ItemDelta itemDelta : delta.getModifications()) {
            if (!NotificationFuctionsImpl.isAmongHiddenPaths(itemDelta.getPath(), paths)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isWatchAuxiliaryAttributes(GeneralNotifierType generalNotifierType) {
        return Boolean.TRUE.equals((generalNotifierType).isWatchAuxiliaryAttributes());
    }

    protected void appendModifications(StringBuilder body, ObjectDelta<? extends ObjectType> delta, List<ItemPath> hiddenPaths, Boolean showValuesBoolean) {

        boolean showValues = !Boolean.FALSE.equals(showValuesBoolean);
        for (ItemDelta<?,?> itemDelta : delta.getModifications()) {
            if (NotificationFuctionsImpl.isAmongHiddenPaths(itemDelta.getPath(), hiddenPaths)) {
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
        for (ItemPathSegment itemPathSegment : itemDelta.getPath().getSegments()) {
            if (itemPathSegment instanceof NameItemPathSegment) {
                NameItemPathSegment nameItemPathSegment = (NameItemPathSegment) itemPathSegment;
                if (sb.length() > 0) {
                    sb.append("/");
                }
                sb.append(nameItemPathSegment.getName().getLocalPart());
            }
        }
        return sb.toString();
    }

    @Override
    protected ExpressionVariables getDefaultVariables(Event event, OperationResult result) {
        ExpressionVariables variables = super.getDefaultVariables(event, result);
        variables.addVariableDefinition(SchemaConstants.C_TEXT_FORMATTER, textFormatter);
        return variables;
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

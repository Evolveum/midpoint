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

package com.evolveum.midpoint.notifications.notifiers;

import com.evolveum.midpoint.notifications.NotificationManager;
import com.evolveum.midpoint.notifications.NotificationsUtil;
import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.notifications.handlers.BaseHandler;
import com.evolveum.midpoint.notifications.transports.Message;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.apache.cxf.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
@Component
public abstract class GeneralNotifier extends BaseHandler {

    private static final Trace LOGGER = TraceManager.getTrace(GeneralNotifier.class);

    @Autowired
    protected NotificationManager notificationManager;

    @Autowired
    protected NotificationsUtil notificationsUtil;

    @PostConstruct
    public void init() {
        register(GeneralNotifierType.class);
    }

    @Override
    public boolean processEvent(Event event, EventHandlerType eventHandlerType, NotificationManager notificationManager, OperationResult result) throws SchemaException {

        logStart(getLogger(), event, eventHandlerType);

        GeneralNotifierType generalNotifierType = (GeneralNotifierType) eventHandlerType;

        boolean retval;

        // executing embedded filters
        boolean filteredOut = false;
        for (JAXBElement<? extends EventHandlerType> handlerType : generalNotifierType.getHandler()) {
            if (!notificationManager.processEvent(event, handlerType.getValue(), result)) {
                filteredOut = true;
                break;
            }
        }

        if (filteredOut) {
            LOGGER.trace("Filtered out by embedded filter");
            retval = true;
        } else if (!checkApplicability(event, generalNotifierType, result)) {
            retval = true;      // message has to be logged in checkApplicability method
        } else if (generalNotifierType.getTransport().isEmpty()) {
            LOGGER.warn("No transports for this notifier, exiting without sending any notifications.");
            retval = true;
        }
        else {

            Map<QName,Object> variables = getDefaultVariables(event, result);

            for (String transport : generalNotifierType.getTransport()) {

                variables.put(SchemaConstants.C_TRANSPORT, transport);
                List<String> recipients = getRecipients(event, generalNotifierType, variables, getDefaultRecipient(event, generalNotifierType, result), result);

                if (!recipients.isEmpty()) {

                    String body = getBodyFromExpression(event, generalNotifierType, variables, result);
                    String subject = getSubjectFromExpression(event, generalNotifierType, variables, result);

                    if (body == null) {
                        body = getBody(event, generalNotifierType, transport, result);
                    }
                    if (subject == null) {
                        subject = generalNotifierType.getSubjectPrefix() != null ? generalNotifierType.getSubjectPrefix() : "";
                        subject += getSubject(event, generalNotifierType, transport, result);
                    }

                    Message message = new Message();
                    message.setBody(body != null ? body : "");
                    message.setContentType("text/plain");           // todo make more flexible
                    message.setSubject(subject != null ? subject : "");
                    message.setTo(recipients);                      // todo cc/bcc recipients

                    notificationManager.getTransport(transport).send(message, transport, result);
                } else {
                    LOGGER.info("No recipients for transport " + transport + ", message corresponding to event " + event.getId() + " will not be send.");
                }
            }

            retval = true;
        }
        logEnd(getLogger(), event, eventHandlerType, retval);
        return retval;
    }

    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        return true;
    }

    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) {
        return null;
    }

    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) throws SchemaException {
        return null;
    }

    protected UserType getDefaultRecipient(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        ObjectType objectType = notificationsUtil.getObjectType(event.getRequestee(), result);
        if (objectType instanceof UserType) {
            return (UserType) objectType;
        } else {
            return null;
        }
    }

    protected Trace getLogger() {
        return LOGGER;
    }

    protected List<String> getRecipients(Event event, GeneralNotifierType generalNotifierType, Map<QName, Object> variables, UserType defaultRecipient, OperationResult result) {
        List<String> recipients = new ArrayList<String>();
        if (!generalNotifierType.getRecipientExpression().isEmpty()) {
            for (ExpressionType expressionType : generalNotifierType.getRecipientExpression()) {
                List<String> r = evaluateExpressionChecked(expressionType, variables, "notification recipient", result);
                if (r != null) {
                    recipients.addAll(r);
                }
            }
            if (recipients.isEmpty()) {
                LOGGER.info("Notification for " + event + " will not be sent, because there are no known recipients.");
            }
        } else if (defaultRecipient == null) {
            LOGGER.info("Unknown default recipient, notification will not be sent.");
        } else {
            String email = defaultRecipient.getEmailAddress();
            if (StringUtils.isEmpty(email)) {
                LOGGER.info("Notification to " + defaultRecipient.getName() + " will not be sent, because the user has no mail address set.");
            } else {
                recipients.add(email);
            }
        }
        return recipients;
    }

    protected String getSubjectFromExpression(Event event, GeneralNotifierType generalNotifierType, Map<QName, Object> variables, OperationResult result) {
        if (generalNotifierType.getSubjectExpression() != null) {
            List<String> subjectList = evaluateExpressionChecked(generalNotifierType.getSubjectExpression(), variables, "subject expression", result);
            if (subjectList == null || subjectList.isEmpty()) {
                LOGGER.warn("Subject expression for event " + event.getId() + " returned nothing.");
                return "";
            }
            if (subjectList.size() > 1) {
                LOGGER.warn("Subject expression for event " + event.getId() + " returned more than 1 item.");
            }
            return subjectList.get(0);
        } else {
            return null;
        }
    }

    protected String getBodyFromExpression(Event event, GeneralNotifierType generalNotifierType, Map<QName, Object> variables, OperationResult result) {
        if (generalNotifierType.getBodyExpression() != null) {
            List<String> bodyList = evaluateExpressionChecked(generalNotifierType.getBodyExpression(), variables, "body expression", result);
            if (bodyList == null || bodyList.isEmpty()) {
                LOGGER.warn("Body expression for event " + event.getId() + " returned nothing.");
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
}

/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.notifications.notifiers;

import com.evolveum.midpoint.notifications.NotificationConstants;
import com.evolveum.midpoint.notifications.NotificationManager;
import com.evolveum.midpoint.notifications.OperationStatus;
import com.evolveum.midpoint.notifications.request.AccountNotificationRequest;
import com.evolveum.midpoint.notifications.request.NotificationRequest;
import com.evolveum.midpoint.notifications.NotificationsUtil;
import com.evolveum.midpoint.notifications.transports.MailMessage;
import com.evolveum.midpoint.notifications.transports.MailSender;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NotificationConfigurationEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NotifierConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.cxf.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.Date;

/**
 * @author mederly
 */
@Component
public class SimpleAccountOperationNotifier implements Notifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleAccountOperationNotifier.class);

    public static final String PARAMETER_SUBJECT_PREFIX = "subjectPrefix";
    public static final String PARAMETER_TECHNICAL_INFORMATION = "technicalInformation";

    @Autowired(required = true)
    private MailSender mailSender;

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    public static final QName NAME = new QName(SchemaConstants.NS_C, "simpleAccountOperationNotifier");

    @Autowired(required = true)
    private NotificationManager notificationManager;

    @PostConstruct
    public void init() {
        notificationManager.registerNotifier(NAME, this);
    }

    @Override
    public void notify(NotificationRequest request,
                       NotificationConfigurationEntryType notificationConfigurationEntry,
                       NotifierConfigurationType notifierConfiguration,
                       OperationResult result) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("SimpleAccountOperationNotifier was called; notification request = " + request);
        }

        if (!(request instanceof AccountNotificationRequest)) {
            LOGGER.error("SimpleAccountOperationNotifier got called with incompatible notification request; class = " + request.getClass());
            return;
        }

        AccountNotificationRequest accountNotificationRequest = (AccountNotificationRequest) request;

        UserType userType = request.getUser();
        if (userType == null) {
            LOGGER.info("Unknown owner of changed account, notification will not be sent.");
            return;
        }

        String email = userType.getEmailAddress();
        if (StringUtils.isEmpty(email)) {
            LOGGER.info("Notification to " + userType.getName() + " will not be sent, because the user has no mail address set.");
            return;
        }

        StringBuilder body = new StringBuilder();
        StringBuilder subject = new StringBuilder(NotificationsUtil.getNotifierParameter(notifierConfiguration, PARAMETER_SUBJECT_PREFIX, ""));
        boolean techInfo = "true".equals(NotificationsUtil.getNotifierParameter(notifierConfiguration, PARAMETER_TECHNICAL_INFORMATION, "false"));
        prepareMessageText(accountNotificationRequest, body, subject, techInfo);

        MailMessage mailMessage = new MailMessage();
        mailMessage.setBody(body.toString());
        mailMessage.setContentType("text/plain");
        mailMessage.setSubject(subject.toString());
        mailMessage.setTo(email);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Sending mail message " + mailMessage);
        }
        mailSender.send(mailMessage, result);
    }

    private void prepareMessageText(AccountNotificationRequest request, StringBuilder body, StringBuilder subject, boolean techInfo) {

        UserType requestee = request.getUser();
        ResourceOperationDescription rod = request.getAccountOperationDescription();
        ObjectDelta<ResourceObjectShadowType> delta = (ObjectDelta<ResourceObjectShadowType>) rod.getObjectDelta();

        body.append("Notification about account-related operation\n\n");
        body.append("User (requestee): " + requestee.getFullName() + " (" + requestee.getName() + ", oid " + requestee.getOid() + ")\n");
        body.append("Notification created on: " + new Date() + "\n\n");
        body.append("Resource: " + rod.getResource().asObjectable().getName() + " (oid " + rod.getResource().getOid() + ")\n");
        boolean named;
        if (rod.getCurrentShadow() != null && rod.getCurrentShadow().asObjectable().getName() != null) {
            body.append("Account: " + rod.getCurrentShadow().asObjectable().getName() + "\n");
            named = true;
        } else {
            named = false;
        }
        body.append("\n");

        body.append((named ? "The" : "An") + " account ");
        switch (request.getOperationStatus()) {
            case SUCCESS: body.append("has been successfully "); break;
            case IN_PROGRESS: body.append("has been ATTEMPTED to be "); break;
            case FAILURE: body.append("FAILED to be "); break;
        }
        if (delta.isAdd()) {
            body.append("created on the resource.\n\n");
            subject.append("Account creation notification");
        } else if (delta.isModify()) {
            body.append("modified on the resource. Modified attributes are:\n");
            for (ItemDelta itemDelta : delta.getModifications()) {
                body.append(" - " + itemDelta.getName().getLocalPart() + "\n");
            }
            body.append("\n");
            subject.append("Account modification notification");
        } else if (delta.isDelete()) {
            body.append("removed from the resource.\n\n");
            subject.append("Account deletion notification");
        }

        if (request.getOperationStatus() == OperationStatus.IN_PROGRESS) {
            body.append("The operation will be retried.\n\n");
        }

        if (techInfo) {
            body.append("----------------------------------------\n");
            body.append("Technical information:\n\n");
            body.append(rod.debugDump(2));
        }
    }

//    private String getLocalPart(QName name) {
//        if (name == null) {
//            return null;
//        } else {
//            return name.getLocalPart();
//        }
//    }
//
//    private String getResourceName(AccountShadowType account) {
//        String oid = null;
//        if (account.getResource() != null) {
//            if (account.getResource().getName() != null) {
//                return account.getResource().getName().getOrig();
//            }
//            oid = account.getResource().getOid();
//        } else {
//            if (account.getResourceRef() != null) {
//                oid = account.getResourceRef().getOid();
//            }
//        }
//        if (oid == null) {
//            return ("(unknown resource)");
//        }
//        return NotificationsUtil.getResourceNameFromRepo(cacheRepositoryService, oid, new OperationResult("dummy"));
//    }
//
//    private void listAccounts(StringBuilder messageText, List<String> lines) {
//        boolean first = true;
//        for (String line : lines) {
//            if (first) {
//                first = false;
//            } else {
//                messageText.append(",\n");
//            }
//            messageText.append(line);
//        }
//        messageText.append(".\n\n");
//    }

}

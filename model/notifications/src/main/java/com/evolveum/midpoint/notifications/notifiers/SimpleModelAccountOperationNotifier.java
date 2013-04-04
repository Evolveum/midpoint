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

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.notifications.NotificationConstants;
import com.evolveum.midpoint.notifications.NotificationManager;
import com.evolveum.midpoint.notifications.request.NotificationRequest;
import com.evolveum.midpoint.notifications.NotificationsUtil;
import com.evolveum.midpoint.notifications.transports.MailMessage;
import com.evolveum.midpoint.notifications.transports.MailSender;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NotificationConfigurationEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.NotifierConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.cxf.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * THIS IS CURRENTLY A BIT OBSOLETE. Account changes are listened to a the level of provisioning.
 * This notifier will be changed to 'see' e.g. midpoint user creation or midpoint password changes.
 *
 * @author mederly
 */
@Component
public class SimpleModelAccountOperationNotifier implements Notifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleModelAccountOperationNotifier.class);

    public static final String PARAMETER_SUBJECT = "subject";

    @Autowired(required = true)
    private MailSender mailSender;

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    public static final QName NAME = new QName(SchemaConstants.NS_C, "simpleModelAccountOperationNotifier");

    @Autowired(required = true)
    private NotificationManager notificationManager;

    @PostConstruct
    public void init() {
//        notificationManager.registerNotifier(NAME, this);
    }

    @Override
    public void notify(NotificationRequest request,
                       NotificationConfigurationEntryType notificationConfigurationEntry,
                       NotifierConfigurationType notifierConfiguration,
                       OperationResult result) {

        UserType userType = request.getUser();
        String email = userType.getEmailAddress();
        if (StringUtils.isEmpty(email)) {
            LOGGER.info("Notification to " + userType.getName() + " will not be sent, because the user has no mail address set.");
            return;
        }

      // todo todo todo
//        List<ObjectDelta<AccountShadowType>> accountDeltas = (List<ObjectDelta<AccountShadowType>>) request.getParameter(NotificationConstants.ACCOUNT_DELTAS);
//        ModelContext<UserType, AccountShadowType> modelContext = (ModelContext<UserType, AccountShadowType>) request.getParameter(NotificationConstants.MODEL_CONTEXT);
//
//        List<QName> situations = notificationConfigurationEntry.getSituation();
//
//        StringBuilder messageText = prepareMessageText(request, accountDeltas, modelContext, situations);
//
//        MailMessage mailMessage = new MailMessage();
//        mailMessage.setBody(messageText.toString());
//        mailMessage.setContentType("text/plain");
//        String subject = NotificationsUtil.getNotifierParameter(notifierConfiguration, PARAMETER_SUBJECT, "Account operation notification");
//        mailMessage.setSubject(subject);
//        mailMessage.setTo(email);
//
//        if (LOGGER.isTraceEnabled()) {
//            LOGGER.trace("Sending mail message " + mailMessage);
//        }
//        mailSender.send(mailMessage);
    }

    private StringBuilder prepareMessageText(NotificationRequest request, List<ObjectDelta<ShadowType>> accountDeltas, ModelContext<UserType, ShadowType> modelContext, List<QName> situations) {
        StringBuilder messageText = new StringBuilder();

        List<String> created = new ArrayList<String>();
        List<String> modified = new ArrayList<String>();
        List<String> deleted = new ArrayList<String>();
//        for (ObjectDelta<AccountShadowType> accountDelta : accountDeltas) {
//            if (accountDelta.isAdd()) {
//                created.add(accountDelta);
//            }
//            if (accountDelta.isModify()) {
//                modified.add(accountDelta);
//            }
//            if (accountDelta.isDelete()) {
//                deleted.add(accountDelta);
//            }
//        }

        for (ModelProjectionContext<ShadowType> projectionContext : modelContext.getProjectionContexts()) {
            // todo fixme this is copied from NotifierChangeHook, as distinguishing between add/modify account is not working reliably
            ObjectDelta<ShadowType> delta = projectionContext.getPrimaryDelta();
            if (delta == null) {
                delta = projectionContext.getSecondaryDelta();
            }
            if (delta == null) {
                LOGGER.warn("Null account delta in projection context " + projectionContext + ", skipping it.");
                continue;
            }

            if (delta.isAdd() || SynchronizationPolicyDecision.ADD.equals(projectionContext.getSynchronizationPolicyDecision())) {
            	ShadowType newAccount = projectionContext.getObjectNew().asObjectable();
                String name = newAccount.getName() != null ? newAccount.getName().getOrig() : "an account";
                created.add(" - " + name + " on " + getResourceName(newAccount));
            } else if (delta.isModify()) {
            	ShadowType newAccount = projectionContext.getObjectNew().asObjectable();
                StringBuilder sb = new StringBuilder();
                sb.append(" - " + newAccount.getName() + " on " + getResourceName(newAccount));
                sb.append(", changing attributes: ");
                boolean first = true;
                for (ItemDelta itemDelta : delta.getModifications()) {
                    if (!first) {
                        sb.append(", ");
                    } else {
                        first = false;
                    }
                    if (itemDelta.getDefinition() != null && itemDelta.getDefinition().getDisplayName() != null) {
                        sb.append(itemDelta.getDefinition().getDisplayName());
                    } else {
                        sb.append(getLocalPart(itemDelta.getName()));
                    }
                }
                modified.add(sb.toString());
            } else if (delta.isDelete()) {
            	ShadowType oldAccount = projectionContext.getObjectOld().asObjectable();
                deleted.add(" - " + oldAccount.getName() + " on " + getResourceName(oldAccount));
            }
        }

        boolean showCreated = situations.contains(NotificationConstants.ACCOUNT_CREATION_QNAME);
        boolean showModified = situations.contains(NotificationConstants.ACCOUNT_MODIFICATION_QNAME);
        boolean showDeleted = situations.contains(NotificationConstants.ACCOUNT_DELETION_QNAME);

        if (showCreated && !created.isEmpty()) {
            if (created.size() == 1) {
                messageText.append("The following account has been created:\n");
            } else {
                messageText.append("The following " + created.size() + " accounts have been created:\n");
            }
            listAccounts(messageText, created);
        }
        if (showModified && !modified.isEmpty()) {
            if (modified.size() == 1) {
                messageText.append("The following account has been modified:\n");
            } else {
                messageText.append("The following " + modified.size() + " accounts have been modified:\n");
            }
            listAccounts(messageText, modified);
        }
        if (showDeleted && !deleted.isEmpty()) {
            if (deleted.size() == 1) {
                messageText.append("The following account has been deleted:\n");
            } else {
                messageText.append("The following " + deleted.size() + " accounts have been deleted:\n");
            }
            listAccounts(messageText, deleted);
        }

        messageText.append("This message was generated for user " + request.getUser().getName() + " on " + new java.util.Date() + ".\n");
        return messageText;
    }

    private String getLocalPart(QName name) {
        if (name == null) {
            return null;
        } else {
            return name.getLocalPart();
        }
    }

    private String getResourceName(ShadowType account) {
        String oid = null;
        if (account.getResource() != null) {
            if (account.getResource().getName() != null) {
                return account.getResource().getName().getOrig();
            }
            oid = account.getResource().getOid();
        } else {
            if (account.getResourceRef() != null) {
                oid = account.getResourceRef().getOid();
            }
        }
        if (oid == null) {
            return ("(unknown resource)");
        }
        return NotificationsUtil.getResourceNameFromRepo(cacheRepositoryService, oid, new OperationResult("dummy"));
    }

    private void listAccounts(StringBuilder messageText, List<String> lines) {
        boolean first = true;
        for (String line : lines) {
            if (first) {
                first = false;
            } else {
                messageText.append(",\n");
            }
            messageText.append(line);
        }
        messageText.append(".\n\n");
    }

}

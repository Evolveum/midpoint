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

package com.evolveum.midpoint.notifications.transports;

import com.evolveum.midpoint.notifications.NotificationsUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MailConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MailServerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * @author mederly
 */
@Component
public class MailSender {

    private static final Trace LOGGER = TraceManager.getTrace(MailSender.class);

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    public void send(MailMessage mailMessage) {

        PrismObject<SystemConfigurationType> systemConfiguration = NotificationsUtil.getSystemConfiguration(cacheRepositoryService, new OperationResult("dummy"));
        if (systemConfiguration == null || systemConfiguration.asObjectable().getNotificationConfiguration() == null
                || systemConfiguration.asObjectable().getNotificationConfiguration().getMail() == null
                || systemConfiguration.asObjectable().getNotificationConfiguration().getMail().getServer().isEmpty()) {
            LOGGER.warn("Mail server(s) are not defined, mail notification to " + mailMessage.getTo() + " will not be sent.") ;
            return;
        }

        MailConfigurationType mailConfigurationType = systemConfiguration.asObjectable().getNotificationConfiguration().getMail();
        String from = mailConfigurationType.getDefaultFrom() != null ? mailConfigurationType.getDefaultFrom() : "nobody@nowhere.org";

        for (MailServerConfigurationType mailServerConfigurationType : mailConfigurationType.getServer()) {

            Properties properties = System.getProperties();
            properties.setProperty( "mail.smtp.host", mailServerConfigurationType.getHost());
            if (mailServerConfigurationType.getPort() != null) {
                properties.setProperty("mail.smtp.port", String.valueOf(mailServerConfigurationType.getPort()));
            }
            Session session = Session.getDefaultInstance(properties);

            try {
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(mailMessage.getTo()));
                message.setSubject(mailMessage.getSubject());
                message.setContent(mailMessage.getBody(), mailMessage.getContentType());
                Transport.send(message);
                LOGGER.info("Message sent successfully to " + mailMessage.getTo() + " via server " + mailServerConfigurationType.getHost() + ".");
                return;
            } catch (MessagingException mex) {
                LoggingUtils.logException(LOGGER, "Couldn't send mail message to " + mailMessage.getTo() + " via " + mailServerConfigurationType.getHost() + ", trying another mail server, if there is any", mex);
            }
        }
        LOGGER.warn("No more mail servers to try, mail notification to " + mailMessage.getTo() + " will not be sent.") ;
    }
}
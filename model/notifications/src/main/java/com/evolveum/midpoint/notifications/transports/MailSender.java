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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MailTransportSecurityType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import org.apache.commons.lang.StringUtils;
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

    private static final String DOT_CLASS = MailSender.class.getName() + ".";

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    public void send(MailMessage mailMessage, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "send");
        result.addParam("mailMessage recipient", mailMessage.getTo());
        result.addParam("mailMessage subject", mailMessage.getSubject());

        PrismObject<SystemConfigurationType> systemConfiguration = NotificationsUtil.getSystemConfiguration(cacheRepositoryService, new OperationResult("dummy"));
        if (systemConfiguration == null || systemConfiguration.asObjectable().getNotificationConfiguration() == null
                || systemConfiguration.asObjectable().getNotificationConfiguration().getMail() == null
                || systemConfiguration.asObjectable().getNotificationConfiguration().getMail().getServer().isEmpty()) {
            String msg = "Mail server(s) are not defined, mail notification to " + mailMessage.getTo() + " will not be sent.";
            LOGGER.warn(msg) ;
            result.recordWarning(msg);
            return;
        }

        MailConfigurationType mailConfigurationType = systemConfiguration.asObjectable().getNotificationConfiguration().getMail();
        String from = mailConfigurationType.getDefaultFrom() != null ? mailConfigurationType.getDefaultFrom() : "nobody@nowhere.org";

        for (MailServerConfigurationType mailServerConfigurationType : mailConfigurationType.getServer()) {

            OperationResult resultForServer = result.createSubresult(DOT_CLASS + "send.forServer");
            resultForServer.addContext("server", mailServerConfigurationType.getHost());
            resultForServer.addContext("port", mailServerConfigurationType.getPort());

            Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", mailServerConfigurationType.getHost());
            if (mailServerConfigurationType.getPort() != null) {
                properties.setProperty("mail.smtp.port", String.valueOf(mailServerConfigurationType.getPort()));
            }
            MailTransportSecurityType mailTransportSecurityType = mailServerConfigurationType.getTransportSecurity();

            boolean sslEnabled = false, starttlsEnable = false, starttlsRequired = false;
            switch (mailTransportSecurityType) {
                case STARTTLS_ENABLED: starttlsEnable = true; break;
                case STARTTLS_REQUIRED: starttlsEnable = true; starttlsRequired = true; break;
                case SSL: sslEnabled = true; break;
            }
            properties.put("mail.smtp.ssl.enable", "" + sslEnabled);
            properties.put("mail.smtp.starttls.enable", "" + starttlsEnable);
            properties.put("mail.smtp.starttls.required", "" + starttlsRequired);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Using mail properties: ");
                for (Object key : properties.keySet()) {
                    if (key instanceof String && ((String) key).startsWith("mail.")) {
                        LOGGER.debug(" - " + key + " = " + properties.get(key));
                    }
                }
            }

            Session session = Session.getInstance(properties);

            if (mailConfigurationType.isDebug() == Boolean.TRUE) {
                session.setDebug(true);
            }

            try {
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(mailMessage.getTo()));
                message.setSubject(mailMessage.getSubject());
                message.setContent(mailMessage.getBody(), mailMessage.getContentType());
                Transport t = session.getTransport("smtp");
                if (StringUtils.isNotEmpty(mailServerConfigurationType.getUsername())) {
                    t.connect(mailServerConfigurationType.getUsername(), mailServerConfigurationType.getPassword());
                } else {
                    t.connect();
                }
                t.sendMessage(message, message.getAllRecipients());
                LOGGER.info("Message sent successfully to " + mailMessage.getTo() + " via server " + mailServerConfigurationType.getHost() + ".");
                resultForServer.recordSuccess();
                result.recordSuccess();
                return;
            } catch (MessagingException mex) {
                String msg = "Couldn't send mail message to " + mailMessage.getTo() + " via " + mailServerConfigurationType.getHost() + ", trying another mail server, if there is any";
                LoggingUtils.logException(LOGGER, msg, mex);
                resultForServer.recordFatalError(msg, mex);
            }
        }
        LOGGER.warn("No more mail servers to try, mail notification to " + mailMessage.getTo() + " will not be sent.") ;
        result.recordWarning("Mail notification to " + mailMessage.getTo() + " could not be sent.");
    }
}
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

package com.evolveum.midpoint.notifications.impl.api.transports;

import java.util.Date;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.evolveum.midpoint.notifications.api.events.Event;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailServerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailTransportSecurityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import static com.evolveum.midpoint.notifications.impl.api.transports.TransportUtil.formatToFileOld;

/**
 * @author mederly
 */
@Component
public class MailTransport implements Transport {

    private static final Trace LOGGER = TraceManager.getTrace(MailTransport.class);

    private static final String NAME = "mail";

    private static final String DOT_CLASS = MailTransport.class.getName() + ".";

    @Autowired
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @Autowired
    private Protector protector;

    @Autowired
    private NotificationManager notificationManager;

    @PostConstruct
    public void init() {
        notificationManager.registerTransport(NAME, this);
    }

    @Override
    public void send(Message mailMessage, String transportName, Event event, Task task, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "send");
        result.addArbitraryObjectCollectionAsParam("mailMessage recipient(s)", mailMessage.getTo());
        result.addParam("mailMessage subject", mailMessage.getSubject());

        SystemConfigurationType systemConfiguration = NotificationFunctionsImpl.getSystemConfiguration(cacheRepositoryService, new OperationResult("dummy"));

//        if (systemConfiguration == null) {
//        	String msg = "No notifications are configured. Mail notification to " + mailMessage.getTo() + " will not be sent.";
//        	 LOGGER.warn(msg) ;
//             result.recordWarning(msg);
//             return;
//        }
//
//        MailConfigurationType mailConfigurationType = null;
//        SecurityPolicyType securityPolicyType = NotificationFuctionsImpl.getSecurityPolicyConfiguration(systemConfiguration.getGlobalSecurityPolicyRef(), cacheRepositoryService, result);
//        if (securityPolicyType != null && securityPolicyType.getAuthentication() != null && securityPolicyType.getAuthentication().getMailAuthentication() != null) {
//        	for (MailAuthenticationPolicyType mailAuthenticationPolicy : securityPolicyType.getAuthentication().getMailAuthentication()) {
//        		if (mailAuthenticationPolicy.getNotificationConfiguration() != null ){
//        			mailConfigurationType = mailAuthenticationPolicy.getNotificationConfiguration().getMail();
//        		}
//        	}
//        }

        if (systemConfiguration == null  || systemConfiguration.getNotificationConfiguration() == null
                || systemConfiguration.getNotificationConfiguration().getMail() == null) {
            String msg = "No notifications are configured. Mail notification to " + mailMessage.getTo() + " will not be sent.";
            LOGGER.warn(msg) ;
            result.recordWarning(msg);
            return;
        }

//		if (mailConfigurationType == null) {
			MailConfigurationType mailConfigurationType = systemConfiguration.getNotificationConfiguration().getMail();
//		}
		String logToFile = mailConfigurationType.getLogToFile();
		if (logToFile != null) {
			TransportUtil.logToFile(logToFile, formatToFileOld(mailMessage), LOGGER);
		}
        String redirectToFile = mailConfigurationType.getRedirectToFile();
        if (redirectToFile != null) {
            TransportUtil.appendToFile(redirectToFile, formatToFileOld(mailMessage), LOGGER, result);
            return;
        }

        if (mailConfigurationType.getServer().isEmpty()) {
            String msg = "Mail server(s) are not defined, mail notification to " + mailMessage.getTo() + " will not be sent.";
            LOGGER.warn(msg) ;
            result.recordWarning(msg);
            return;
        }

        long start = System.currentTimeMillis();

        String defaultFrom = mailConfigurationType.getDefaultFrom() != null ? mailConfigurationType.getDefaultFrom() : "nobody@nowhere.org";

        for (MailServerConfigurationType mailServerConfigurationType : mailConfigurationType.getServer()) {

            OperationResult resultForServer = result.createSubresult(DOT_CLASS + "send.forServer");
            final String host = mailServerConfigurationType.getHost();
            resultForServer.addContext("server", host);
            resultForServer.addContext("port", mailServerConfigurationType.getPort());

            Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", host);
            if (mailServerConfigurationType.getPort() != null) {
                properties.setProperty("mail.smtp.port", String.valueOf(mailServerConfigurationType.getPort()));
            }
            MailTransportSecurityType mailTransportSecurityType = mailServerConfigurationType.getTransportSecurity();

            boolean sslEnabled = false, starttlsEnable = false, starttlsRequired = false;
			if (mailTransportSecurityType != null) {
				switch (mailTransportSecurityType) {
					case STARTTLS_ENABLED:
						starttlsEnable = true;
						break;
					case STARTTLS_REQUIRED:
						starttlsEnable = true;
						starttlsRequired = true;
						break;
					case SSL:
						sslEnabled = true;
						break;
				}
			}
            properties.put("mail.smtp.ssl.enable", "" + sslEnabled);
            properties.put("mail.smtp.starttls.enable", "" + starttlsEnable);
            properties.put("mail.smtp.starttls.required", "" + starttlsRequired);
            if (Boolean.TRUE.equals(mailConfigurationType.isDebug())) {
                properties.put("mail.debug", "true");
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Using mail properties: ");
                for (Object key : properties.keySet()) {
                    if (key instanceof String && ((String) key).startsWith("mail.")) {
                        LOGGER.debug(" - " + key + " = " + properties.get(key));
                    }
                }
            }

            task.recordState("Sending notification mail via " + host);

            Session session = Session.getInstance(properties);

            try {
                MimeMessage mimeMessage = new MimeMessage(session);
                String from = mailMessage.getFrom() != null ? mailMessage.getFrom() : defaultFrom;
                mimeMessage.setFrom(new InternetAddress(from));

               	for (String recipient : mailMessage.getTo()) {
               		mimeMessage.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(recipient));
               	}
                for (String recipientCc : mailMessage.getCc()) {
                    mimeMessage.addRecipient(javax.mail.Message.RecipientType.CC, new InternetAddress(recipientCc));
                }
                for (String recipientBcc : mailMessage.getBcc()) {
                    mimeMessage.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(recipientBcc));
                }
                mimeMessage.setSubject(mailMessage.getSubject(), "utf-8");
                String contentType = mailMessage.getContentType();
                if (StringUtils.isEmpty(contentType)) {
                    contentType = "text/plain; charset=UTF-8";
                }
                mimeMessage.setContent(mailMessage.getBody(), contentType);
                javax.mail.Transport t = session.getTransport("smtp");
                if (StringUtils.isNotEmpty(mailServerConfigurationType.getUsername())) {
                    ProtectedStringType passwordProtected = mailServerConfigurationType.getPassword();
                    String password = null;
                    if (passwordProtected != null) {
                        try {
                            password = protector.decryptString(passwordProtected);
                        } catch (EncryptionException e) {
                            String msg = "Couldn't send mail message to " + mailMessage.getTo() + " via " + host + ", because the plaintext password value couldn't be obtained. Trying another mail server, if there is any.";
                            LoggingUtils.logException(LOGGER, msg, e);
                            resultForServer.recordFatalError(msg, e);
                            continue;
                        }
                    }
                    t.connect(mailServerConfigurationType.getUsername(), password);
                } else {
                    t.connect();
                }
                t.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
                LOGGER.info("Message sent successfully to " + mailMessage.getTo() + " via server " + host + ".");
                resultForServer.recordSuccess();
                result.recordSuccess();
                long duration = System.currentTimeMillis() - start;
                task.recordState("Notification mail sent successfully via " + host + ", in " + duration + " ms overall.");
                task.recordNotificationOperation(NAME, true, duration);
                return;
            } catch (MessagingException e) {
                String msg = "Couldn't send mail message to " + mailMessage.getTo() + " via " + host + ", trying another mail server, if there is any";
                LoggingUtils.logException(LOGGER, msg, e);
                resultForServer.recordFatalError(msg, e);
                task.recordState("Error sending notification mail via " + host);
            }
        }
        LOGGER.warn("No more mail servers to try, mail notification to " + mailMessage.getTo() + " will not be sent.") ;
        result.recordWarning("Mail notification to " + mailMessage.getTo() + " could not be sent.");
        task.recordNotificationOperation(NAME, false, System.currentTimeMillis() - start);
    }


    private String formatToFile(Message mailMessage) {
        return "============================================ " + "\n" +new Date() + "\n" + mailMessage.toString() + "\n\n";
    }

    @Override
    public String getDefaultRecipientAddress(UserType recipient) {
        return recipient.getEmailAddress();
    }

    @Override
    public String getName() {
        return NAME;
    }
}

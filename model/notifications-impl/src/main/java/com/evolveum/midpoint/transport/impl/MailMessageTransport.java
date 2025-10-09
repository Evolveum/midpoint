/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.transport.impl;

import static com.evolveum.midpoint.transport.impl.TransportUtil.filterBlankMailRecipients;
import static com.evolveum.midpoint.transport.impl.TransportUtil.formatToFileOld;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.SendingContext;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.api.transports.TransportSupport;
import com.evolveum.midpoint.common.MimeTypeUtil;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * Message transport sending mail messages.
 */
public class MailMessageTransport implements Transport<MailTransportConfigurationType> {

    private static final Trace LOGGER = TraceManager.getTrace(MailMessageTransport.class);

    private static final String DOT_CLASS = MailMessageTransport.class.getName() + ".";

    private String name;
    private MailTransportConfigurationType configuration;
    private TransportSupport transportSupport;

    @Override
    public void configure(
            @NotNull MailTransportConfigurationType configuration,
            @NotNull TransportSupport transportSupport) {
        this.configuration = Objects.requireNonNull(configuration);
        name = Objects.requireNonNull(configuration.getName());
        this.transportSupport = transportSupport;
    }

    @Override
    public void send(Message mailMessage, String transportName, SendingContext ctx, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "send");
        result.addArbitraryObjectCollectionAsParam("mailMessage recipient(s)", mailMessage.getTo());
        result.addParam("mailMessage subject", mailMessage.getSubject());

        String logToFile = configuration.getLogToFile();
        if (logToFile != null) {
            TransportUtil.logToFile(logToFile, formatToFileOld(mailMessage), LOGGER);
        }
        String redirectToFile = configuration.getRedirectToFile();
        if (redirectToFile != null && TransportUtil.optionsForFilteringRecipient(configuration) == 0) {
            TransportUtil.appendToFile(redirectToFile, formatToFileOld(mailMessage), LOGGER, result);
            return;
        }

        filterAndSetRecipients(mailMessage, ctx, result);

        if (isRecipientsOrServersEmpty(mailMessage, result)) {
            return;
        }

        sendViaMailServers(mailMessage, ctx, result);
    }

    /**
     * Filters recipients using {@link TransportUtil#validateRecipient(List, List, List, GeneralTransportConfigurationType, Task, OperationResult, ExpressionFactory, ExpressionProfile, Trace)
     * TransportUtil.validateRecipient()} and assigns them to the mailMessage parameter.
     */
    private void filterAndSetRecipients(Message mailMessage, SendingContext ctx, OperationResult result) {
        int optionsForFilteringRecipient = TransportUtil.optionsForFilteringRecipient(configuration);
        List<String> allowedRecipientTo = new ArrayList<>();
        List<String> forbiddenRecipientTo = new ArrayList<>();
        List<String> allowedRecipientCc = new ArrayList<>();
        List<String> forbiddenRecipientCc = new ArrayList<>();
        List<String> allowedRecipientBcc = new ArrayList<>();
        List<String> forbiddenRecipientBcc = new ArrayList<>();
        var task = ctx.task();
        String redirectToFile = configuration.getRedirectToFile();
        if (optionsForFilteringRecipient != 0) {
            TransportUtil.validateRecipient(allowedRecipientTo, forbiddenRecipientTo,
                    mailMessage.getTo(), configuration, task, result,
                    transportSupport.expressionFactory(), ctx.expressionProfile(), LOGGER);
            TransportUtil.validateRecipient(allowedRecipientCc, forbiddenRecipientCc,
                    mailMessage.getCc(), configuration, task, result,
                    transportSupport.expressionFactory(), ctx.expressionProfile(), LOGGER);
            TransportUtil.validateRecipient(allowedRecipientBcc, forbiddenRecipientBcc,
                    mailMessage.getBcc(), configuration, task, result,
                    transportSupport.expressionFactory(), ctx.expressionProfile(), LOGGER);

            if (redirectToFile != null) {
                if (!forbiddenRecipientTo.isEmpty() || !forbiddenRecipientCc.isEmpty() || !forbiddenRecipientBcc.isEmpty()) {
                    mailMessage.setTo(forbiddenRecipientTo);
                    mailMessage.setCc(forbiddenRecipientCc);
                    mailMessage.setBcc(forbiddenRecipientBcc);
                    TransportUtil.appendToFile(redirectToFile, formatToFileOld(mailMessage), LOGGER, result);
                }
                mailMessage.setTo(allowedRecipientTo);
                mailMessage.setCc(allowedRecipientCc);
                mailMessage.setBcc(allowedRecipientBcc);
            }
        }
    }

    /**
     * Validates presence of at least one recipient and mail server and logs whichever fails.
     *
     * @return true if any check fails, otherwise false
     */
    private boolean isRecipientsOrServersEmpty(Message mailMessage, OperationResult result) {
        int optionsForFilteringRecipient = TransportUtil.optionsForFilteringRecipient(configuration);
        if (optionsForFilteringRecipient != 0 && mailMessage.getTo().isEmpty()) {
            String msg = "No recipient found after recipient validation.";
            LOGGER.debug(msg);
            result.recordSuccess();
            return true;
        }
        if (configuration.getServer().isEmpty()) {
            String msg = "Mail server(s) are not defined, mail notification to " + mailMessage.getTo() + " will not be sent.";
            LOGGER.warn(msg);
            result.recordWarning(msg);
            return true;
        }
        Collection<String> actualTo = filterBlankMailRecipients(mailMessage.getTo(), "to", mailMessage.getSubject());
        Collection<String> actualCc = filterBlankMailRecipients(mailMessage.getCc(), "cc", mailMessage.getSubject());
        Collection<String> actualBcc = filterBlankMailRecipients(mailMessage.getBcc(), "bcc", mailMessage.getSubject());
        if (actualTo.isEmpty() && actualCc.isEmpty() && actualBcc.isEmpty()) {
            String msg = "No recipients found after excluding blank ones; the message will not be sent";
            LOGGER.warn(msg);
            result.recordWarning(msg);
            return true;
        }
        return false;
    }

    /**
     * Composes the email and tries to send it via each server configuration iteratively until first success.
     */
    private void sendViaMailServers(Message mailMessage, SendingContext ctx, OperationResult result) {
        Collection<String> actualTo = filterBlankMailRecipients(mailMessage.getTo(), "to", mailMessage.getSubject());
        Collection<String> actualCc = filterBlankMailRecipients(mailMessage.getCc(), "cc", mailMessage.getSubject());
        Collection<String> actualBcc = filterBlankMailRecipients(mailMessage.getBcc(), "bcc", mailMessage.getSubject());
        long start = System.currentTimeMillis();
        boolean sent = false;
        var task = ctx.task();

        for (MailServerConfigurationType mailServerConfigurationType : configuration.getServer()) {
            OperationResult resultForServer = result.createSubresult(DOT_CLASS + "send.forServer");
            final String host = mailServerConfigurationType.getHost();
            final boolean isOAuth = mailServerConfigurationType.getOauth2Authentication() != null;
            resultForServer.addContext("server", host);
            resultForServer.addContext("port", mailServerConfigurationType.getPort());
            resultForServer.addContext("auth_type", isOAuth ? "oauth2" : "basic");

            Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", host);
            if (mailServerConfigurationType.getPort() != null) {
                properties.setProperty("mail.smtp.port", String.valueOf(mailServerConfigurationType.getPort()));
            }

            defineTransportSecurity(mailServerConfigurationType, properties, isOAuth);

            if (Boolean.TRUE.equals(configuration.isDebug())) {
                properties.put("mail.debug", "true");
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Using mail properties: ");
                for (Object key : properties.keySet()) {
                    if (key instanceof String && ((String) key).startsWith("mail.")) {
                        LOGGER.debug(" - {} = {}", key, properties.get(key));
                    }
                }
            }
            task.recordStateMessage("Sending notification mail via " + host);
            Session session = Session.getInstance(properties);

            try {
                MimeMessage mimeMessage = composeMimeMessage(session, mailMessage, actualTo, actualCc, actualBcc);
                if (mimeMessage == null) {
                    return;
                }

                try (jakarta.mail.Transport t = session.getTransport("smtp")) {
                    if (isOAuth) {
                        authenticateViaOauth(mailServerConfigurationType.getOauth2Authentication(), actualTo, host, resultForServer, t);
                    } else {
                        authenticateViaBasicAuth(mailServerConfigurationType, actualTo, host, resultForServer, t);
                    }

                    if (t.isConnected()) {
                        t.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
                        t.close();
                        LOGGER.debug("Message sent successfully to " + actualTo + " via server " + host + ".");
                        resultForServer.recordSuccess();
                        result.recordSuccess();
                        long duration = System.currentTimeMillis() - start;
                        task.recordStateMessage("Notification mail sent successfully via " + host + ", in " + duration + " ms overall.");
                        task.recordNotificationOperation(name, true, duration);
                        sent = true;
                        break;
                    }
                }
            } catch (MessagingException e) {
                String msg = "Couldn't send mail message to " + actualTo + " via " + host + ", trying another mail server, if there is any.";
                LoggingUtils.logException(LOGGER, msg, e);
                resultForServer.recordFatalError(msg, e);
                task.recordStateMessage("Error sending notification mail via " + host);
            }
        }

        if (!sent) {
            LOGGER.warn("No more mail servers to try, mail notification to " + actualTo + " will not be sent.");
            result.recordWarning("Mail notification to " + actualTo + " could not be sent.");
            task.recordNotificationOperation(name, false, System.currentTimeMillis() - start);
        }
    }

    /**
     * Connects to the smtp server using the Oauth2 client credentials flow or logs reason for failure.
     */
    private void authenticateViaOauth(OAuth2CredentialsType oauth2, Collection<String> actualTo, String host, OperationResult resultForServer, jakarta.mail.Transport t) throws MessagingException {
        ProtectedStringType clientSecretProtected = oauth2.getClientSecret();
        if (clientSecretProtected == null) {
            String msg = "Couldn't send mail message to " + actualTo + " via " + host + ", because the client secret is not set. Trying another mail server, if there is any.";
            LOGGER.error(msg);
            resultForServer.recordFatalError(msg);
            return;
        }

        String clientSecret;
        try {
            clientSecret = transportSupport.protector().decryptString(clientSecretProtected);
        } catch (EncryptionException e) {
            String msg = "Couldn't send mail message to " + actualTo + " via " + host + ", because the plaintext client secret value couldn't be obtained. Trying another mail server, if there is any.";
            LoggingUtils.logException(LOGGER, msg, e);
            resultForServer.recordFatalError(msg, e);
            return;
        }

        LOGGER.debug(
                "Attempting OAuth2 authentication for user {} on endpoint {}",
                oauth2.getUsername(),
                oauth2.getTokenEndpoint()
        );

        String accessToken;
        try {
            accessToken = OAuth2TokenService.getAccessToken(oauth2, clientSecret);
        } catch (OAuth2TokenRetrievalException e) {
            String msg = "Couldn't send mail message to " + actualTo + " via " + host + ", because the server didn't return the access token. Trying another mail server, if there is any.";
            LoggingUtils.logException(LOGGER, msg, e);
            resultForServer.recordFatalError(msg, e);
            return;
        }

        LOGGER.debug(
                "OAuth2 authentication successful for user {} on endpoint {}",
                oauth2.getUsername(),
                oauth2.getTokenEndpoint()
        );

        t.connect(host, configuration.getDefaultFrom(), accessToken);
    }

    /**
     * Connects to the smtp server using the basic authentication or logs reason for failure.
     */
    private void authenticateViaBasicAuth(MailServerConfigurationType mailServerConfigurationType, Collection<String> actualTo, String host, OperationResult resultForServer, jakarta.mail.Transport t) throws MessagingException {
        BasicAuthenticationType basicAuth = mailServerConfigurationType.getBasicAuthentication();
        String username = (basicAuth != null) ? basicAuth.getUsername() : mailServerConfigurationType.getUsername();
        ProtectedStringType passwordProtected = (basicAuth != null) ? basicAuth.getPassword() : mailServerConfigurationType.getPassword();

        if (StringUtils.isNotEmpty(username)) {
            String password = null;
            if (passwordProtected != null) {
                try {
                    password = transportSupport.protector().decryptString(passwordProtected);
                } catch (EncryptionException e) {
                    String msg = "Couldn't send mail message to " + actualTo + " via " + host + ", because the plaintext password value couldn't be obtained. Trying another mail server, if there is any.";
                    LoggingUtils.logException(LOGGER, msg, e);
                    resultForServer.recordFatalError(msg, e);
                    return;
                }
            }
            t.connect(mailServerConfigurationType.getUsername(), password);
        } else {
            t.connect();
        }
    }

    /**
     * Sets correct properties (http headers) for given authentication type.
     */
    private void defineTransportSecurity(MailServerConfigurationType mailServerConfigurationType, Properties properties, boolean isOAuth) {
        MailTransportSecurityType mailTransportSecurityType = mailServerConfigurationType.getTransportSecurity();
        boolean sslEnabled = false, starttlsEnable = isOAuth, starttlsRequired = isOAuth;
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

        if (isOAuth) {
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        }
        properties.put("mail.smtp.ssl.enable", String.valueOf(sslEnabled));
        properties.put("mail.smtp.starttls.enable", String.valueOf(starttlsEnable));
        properties.put("mail.smtp.starttls.required", String.valueOf(starttlsRequired));
    }

    /**
     * Creates MimeMessage or logs reason for failure.
     *
     * @return null if an expected error occurs, MimeMessage otherwise
     */
    private MimeMessage composeMimeMessage(Session session, Message mailMessage, Collection<String> actualTo, Collection<String> actualCc, Collection<String> actualBcc) throws MessagingException {
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setSentDate(new Date());
        String defaultFrom = configuration.getDefaultFrom() != null ? configuration.getDefaultFrom() : "nobody@nowhere.org";
        String from = mailMessage.getFrom() != null ? mailMessage.getFrom() : defaultFrom;
        mimeMessage.setFrom(new InternetAddress(from));
        for (String recipient : actualTo) {
            mimeMessage.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(recipient));
        }
        for (String recipientCc : actualCc) {
            mimeMessage.addRecipient(jakarta.mail.Message.RecipientType.CC, new InternetAddress(recipientCc));
        }
        for (String recipientBcc : actualBcc) {
            mimeMessage.addRecipient(jakarta.mail.Message.RecipientType.BCC, new InternetAddress(recipientBcc));
        }
        mimeMessage.setSubject(mailMessage.getSubject(), StandardCharsets.UTF_8.name());
        String contentType = mailMessage.getContentType();
        if (StringUtils.isEmpty(contentType)) {
            contentType = "text/plain; charset=UTF-8";
        }
        BodyPart messageBody = new MimeBodyPart();
        messageBody.setContent(mailMessage.getBody(), contentType);
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBody);
        for (NotificationMessageAttachmentType attachment : mailMessage.getAttachments()) {
            if (attachment.getContent() != null || attachment.getContentFromFile() != null) {
                String fileName;
                BodyPart attachmentBody = new MimeBodyPart();
                if (attachment.getContent() != null) {
                    try {
                        Object content = RawType.getValue(attachment.getContent());
                        if (content == null) {
                            throw new SchemaException();
                        }
                        attachmentBody.setContent(content, attachment.getContentType());
                    } catch (SchemaException e) {
                        LOGGER.warn("RawType " + attachment.getContent() + " isn't possible to parse.");
                        return null;
                    }
                    if (StringUtils.isBlank(attachment.getFileName())) {
                        fileName = "attachment";
                    } else {
                        fileName = attachment.getFileName();
                    }
                } else {
                    if (!Files.isReadable(Paths.get(attachment.getContentFromFile()))) {
                        LOGGER.warn("File " + attachment.getContentFromFile() + " non exist or isn't readable.");
                        return null;
                    }
                    DataSource source = new FileDataSource(attachment.getContentFromFile()) {
                        @Override
                        public String getContentType() {
                            return attachment.getContentType();
                        }
                    };
                    attachmentBody.setDataHandler(new DataHandler(source));
                    if (StringUtils.isBlank(attachment.getFileName())) {
                        fileName = source.getName();
                    } else {
                        fileName = attachment.getFileName();
                    }
                }
                if (!fileName.contains(".")) {
                    fileName += MimeTypeUtil.getExtension(attachment.getContentType());
                }
                attachmentBody.setFileName(fileName);
                if (!StringUtils.isBlank(attachment.getContentId())) {
                    attachmentBody.setHeader("Content-ID", attachment.getContentId());
                }
                multipart.addBodyPart(attachmentBody);
            } else {
                LOGGER.warn("NotificationMessageAttachmentType doesn't contain content.");
            }
        }
        mimeMessage.setContent(multipart);
        return mimeMessage;
    }

    @Override
    public String getDefaultRecipientAddress(FocusType recipient) {
        return recipient.getEmailAddress();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MailTransportConfigurationType getConfiguration() {
        return configuration;
    }
}

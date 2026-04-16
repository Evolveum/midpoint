/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.transport.impl;

import java.util.*;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailTransportConfigurationType;

import static org.testng.AssertJUnit.*;

/**
 * Tests for address parsing behavior in {@link MailMessageTransport}
 * when mail.mime.address.strict property is configured.
 */
public class MailMessageTransportAddressParsingTest {

    private static final String VALID_ADDRESS = "user@example.com";
    private static final String INVALID_ADDRESS = "user...name@example.com";

    private MailMessageTransport transport;

    @BeforeMethod
    public void setUp() {
        transport = new MailMessageTransport();

        MailTransportConfigurationType config = new MailTransportConfigurationType();
        config.setName("test-mail");
        config.setDefaultFrom(VALID_ADDRESS);

        transport.configure(config, null);
    }

    @Test
    public void testStrictMode_validAddress() throws Exception {
        Session session = createSession(null); // default = strict
        Message message = createMessage(VALID_ADDRESS);

        MimeMessage result = transport.composeMimeMessage(session, message, List.of(VALID_ADDRESS), List.of(), List.of());

        assertNotNull("MimeMessage should be created for valid address in strict mode", result);
        assertEquals(VALID_ADDRESS, result.getFrom()[0].toString());
    }

    @Test(expectedExceptions = MessagingException.class)
    public void testStrictMode_invalidAddress() throws Exception {
        Session session = createSession(null); // default = strict
        Message message = createMessage(VALID_ADDRESS);

        transport.composeMimeMessage(session, message, List.of(INVALID_ADDRESS), List.of(), List.of());
    }

    @Test
    public void testNonStrictMode_invalidAddress() throws Exception {
        Session session = createSession("false"); // non-strict
        Message message = createMessage(VALID_ADDRESS);

        MimeMessage result = transport.composeMimeMessage(session, message, List.of(INVALID_ADDRESS), List.of(), List.of());

        assertNotNull("MimeMessage should be created for invalid address in non-strict mode", result);
        assertEquals(INVALID_ADDRESS, result.getAllRecipients()[0].toString());
    }

    @Test
    public void testNonStrictMode_validAddress() throws Exception {
        Session session = createSession("false"); // non-strict
        Message message = createMessage(VALID_ADDRESS);

        MimeMessage result = transport.composeMimeMessage(session, message, List.of(VALID_ADDRESS), List.of(), List.of());

        assertNotNull("MimeMessage should be created for valid address in non-strict mode", result);
        assertEquals(VALID_ADDRESS, result.getFrom()[0].toString());
    }

    private Session createSession(String strictValue) {
        Properties props = new Properties();
        if (strictValue != null) {
            props.setProperty("mail.mime.address.strict", strictValue);
        }
        return Session.getInstance(props);
    }

    private Message createMessage(String from) {
        Message message = new Message();
        message.setFrom(from);
        message.setSubject("Test Subject");
        message.setBody("Test Body");
        message.setContentType("text/plain; charset=UTF-8");
        return message;
    }
}

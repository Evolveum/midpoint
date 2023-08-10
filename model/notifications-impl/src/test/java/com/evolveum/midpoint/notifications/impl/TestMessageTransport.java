/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.notifications.api.transports.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CustomTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Lightweight test transport.
 * Can be created in two ways:
 *
 * * Calling the constructor with name.
 * This is only useful for the simplest cases without using system configuration.
 * * Specifying its class name for custom transport and calling sysconfig modify.
 * In this case the change will cause transport service to register the transport
 * and its {@link #configure} method will be called.
 * The instance then can be obtained by {@link TransportService#getTransport}.
 *
 * Afterwards, use {@link #getMessages()} and {@link #clearMessages()} as needed.
 */
public class TestMessageTransport implements Transport<CustomTransportConfigurationType> {

    private static final Trace LOGGER = TraceManager.getTrace(TestMessageTransport.class);

    private static final String DOT_CLASS = TestMessageTransport.class.getName() + ".";

    private String name;

    private final List<Message> messages = new ArrayList<>();

    private CustomTransportConfigurationType configuration;

    @SuppressWarnings("unused")
    public TestMessageTransport() {
        // for reflection if used as a type for custom transport
    }

    public TestMessageTransport(String name) {
        this.name = name;
    }

    @Override
    public void send(Message message, String name, SendingContext ctx, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "send");
        messages.add(message);
        LOGGER.info("Recorded a message " + message);
        result.recordSuccess();
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void clearMessages() {
        messages.clear();
    }

    @Override
    public String getDefaultRecipientAddress(FocusType recipient) {
        return recipient.getEmailAddress() != null ? recipient.getEmailAddress() : "no-address";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void configure(@NotNull CustomTransportConfigurationType configuration, @NotNull TransportSupport transportSupport) {
        this.configuration = configuration;
        name = configuration.getName();
    }

    @Override
    public CustomTransportConfigurationType getConfiguration() {
        return configuration;
    }
}

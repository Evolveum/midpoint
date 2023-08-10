/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.SendingContext;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.api.transports.TransportSupport;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralTransportConfigurationType;

public class DummyTransport implements Transport<GeneralTransportConfigurationType>, DebugDumpable {

    public static final String DEFAULT_NAME = "dummy";

    private static final Trace LOGGER = TraceManager.getTrace(DummyTransport.class);

    private static final String DOT_CLASS = DummyTransport.class.getName() + ".";

    private final String name;

    // TODO: Convert to single name transport, use multiple dummy instances for multiple names.
    //  Using transport names with : will have no special meaning in the future (hopefully after 4.6).
    private final Map<String, List<Message>> messages = new HashMap<>();

    public DummyTransport() {
        this(DEFAULT_NAME);
    }

    public DummyTransport(String name) {
        this.name = name;
    }

    @Override
    public void send(Message message, String name, SendingContext ctx, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "send");

        if (!messages.containsKey(name)) {
            messages.put(name, new ArrayList<>());
        }
        messages.get(name).add(message);

        LOGGER.info("Recorded a message " + message + " to dummy transport buffer named " + name);

        result.recordSuccess();
    }

    public List<Message> getMessages(String transportName) {
        return messages.get(transportName);
    }

    public Map<String, List<Message>> getMessages() {
        return messages;
    }

    public void clearMessages() {
        messages.clear();
    }

    @Override
    public String getDefaultRecipientAddress(FocusType recipient) {
        return recipient.getEmailAddress() != null ? recipient.getEmailAddress() : "dummyAddress";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("(\n");
        for (Map.Entry<String, List<Message>> entry : messages.entrySet()) {
            DebugUtil.debugDumpWithLabelLn(sb, entry.getKey(), entry.getValue(), indent + 1);
        }
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public void configure(@NotNull GeneralTransportConfigurationType configuration, @NotNull TransportSupport transportSupport) {
        // not called for legacy transport component
    }

    @Override
    public GeneralTransportConfigurationType getConfiguration() {
        return null;
    }

    public void assertNoMessages() {
        assertThat(getMessages()).as("messages").isEmpty();
    }
}

/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
@Component
public class DummyTransport implements Transport, DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(DummyTransport.class);

    private static final String DOT_CLASS = DummyTransport.class.getName() + ".";
    public static final String NAME = "dummy";

    // if NotificationManager is not found, this transport will simply be disabled
    // it is legal for modules, in which tests do not need it
    @Autowired(required = false)
    private NotificationManager notificationManager;

    @PostConstruct
    public void init() {
        if (notificationManager != null) {
            notificationManager.registerTransport(NAME, this);
        } else {
            LOGGER.info("NotificationManager is not available, skipping the registration.");
        }
    }

    private Map<String,List<Message>> messages = new HashMap<>();

    @Override
    public void send(Message message, String name, Event event, Task task, OperationResult parentResult) {

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

    public Map<String,List<Message>> getMessages() {
        return messages;
    }

    public void clearMessages() {
        messages = new HashMap<>();
    }

    @Override
    public String getDefaultRecipientAddress(UserType recipient) {
        return recipient.getEmailAddress() != null ? recipient.getEmailAddress() : "dummyAddress";
    }

    @Override
    public String getName() {
        return "dummy";
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
}

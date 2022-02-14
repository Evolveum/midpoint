/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.api.transports;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Message transport service component which facilitates sending the messages via transports.
 * It also works as a transport registry.
 */
public interface TransportService {

    void registerTransport(String name, Transport<?> transport);

    Transport<?> getTransport(String name);

    void send(Message message, String transportName, Event event, Task task, OperationResult parentResult);
}

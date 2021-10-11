/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;

import org.jetbrains.annotations.NotNull;

/**
 * Handles notification events.
 */
public interface NotificationManager {

    void registerTransport(String name, Transport transport);

    void processEvent(@NotNull Event event, Task task, OperationResult result);

    boolean processEvent(@NotNull Event event, EventHandlerType eventHandlerBean, Task task, OperationResult result);

    boolean isDisabled();

    void setDisabled(boolean disabled);
}

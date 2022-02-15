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
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Contract for a message transport instance, which is mostly SPI type contract.
 * Transport should not be used directly, {@link TransportService} should be used instead.
 *
 * @param <C> configuration type related to the transport
 */
public interface Transport<C extends GeneralTransportConfigurationType> {

    // transportName is used only by some legacy transports when key:subname style is used
    void send(Message message, @Deprecated String transportName, Event event, Task task, OperationResult parentResult);

    String getDefaultRecipientAddress(UserType recipient);

    String getName();

    // not-null for new transports, but legacy transports return null (remove after 4.6 cleanup if that happens)
    C getConfiguration();

    void init(C configuration, TransportSupport transportSupport);
}

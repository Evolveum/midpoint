/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.api.transports;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Message transport service component which facilitates sending the messages via transports.
 * It also works as a transport registry.
 */
public interface TransportService {

    void registerTransport(@NotNull Transport<?> transport);

    /** Returns transport with the specified name or throws if no such transport exists. */
    Transport<?> getTransport(String name);

    void send(Message message, String transportName, SendingContext ctx, OperationResult parentResult);

    /*
    TODO: Do we want this? What should a disabled transport service do? Add partial error to result? Just log?
     This would be analog to the same methods on NotificationManager.
    boolean isDisabled();
    void setDisabled(boolean disabled);
    */
}

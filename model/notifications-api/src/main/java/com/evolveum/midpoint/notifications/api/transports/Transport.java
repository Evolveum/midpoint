/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.api.transports;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralTransportConfigurationType;

/**
 * Contract for a message transport instance, which is mostly SPI type contract.
 * Transport should not be used directly, {@link TransportService} should be used instead.
 *
 * @param <C> configuration type related to the transport
 */
public interface Transport<C extends GeneralTransportConfigurationType> {

    /**
     * Configures transport instance - this must be fast and exception free.
     * This is not the place to start any connections or sessions; this is called after sysconfig is changed.
     *
     * @param configuration portion of the configuration relevant to this transport
     * @param transportSupport support object with dependencies
     */
    void configure(@NotNull C configuration, @NotNull TransportSupport transportSupport);

    /**
     * Sends the message via this transport.
     *
     * TODO:
     *
     * * transportName is used only by some legacy transports when key:subname style is used, it will be removed;
     * * event should be delivered differently, for other message sources (e.g. report email) there is no event,
     * but there may be other object of interest (the report itself?);
     * * it would be best to deliver some "context" information to facilitate customizations on various levels,
     * e.g. to inform that notifications need to be redirected to different file than configured in the transport config.
     */
    void send(Message message, @Deprecated String transportName, SendingContext ctx, OperationResult parentResult);

    String getDefaultRecipientAddress(FocusType recipient);

    String getName();

    // not-null for new transports, but legacy transports return null (remove after 4.6 cleanup if that happens)
    C getConfiguration();
}

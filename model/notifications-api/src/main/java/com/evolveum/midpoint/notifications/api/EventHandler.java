/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.api;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BaseEventHandlerType;

/** Handles a single notification event. */
public interface EventHandler<E extends Event, C extends BaseEventHandlerType> {

    /**
     * Processes event (embedded in the context) by the handler, represented by this object and provided configuration.
     * Returns true if we should continue with processing of this event, false otherwise.
     */
    boolean processEvent(
            @NotNull ConfigurationItem<? extends C> handlerConfig,
            @NotNull EventProcessingContext<? extends E> ctx,
            @NotNull OperationResult result)
            throws SchemaException;

    /**
     * Returns type of events this handler is capable of handling.
     */
    @NotNull Class<E> getEventType();

    /**
     * Returns type of configuration objects for this event handler.
     * The handler is selected based on exact match of this type.
     */
    @NotNull Class<? extends C> getEventHandlerConfigurationType();
}

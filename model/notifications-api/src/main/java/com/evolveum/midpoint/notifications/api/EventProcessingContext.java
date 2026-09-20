/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.api;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.SendingContext;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

/**
 * Global context for processing a single event.
 */
@Experimental
public record EventProcessingContext<E extends Event>(
        @NotNull E event,
        @NotNull Task task
) {

    public @NotNull LightweightIdentifier getEventId() {
        return event.getId();
    }

    public SendingContext sendingContext() {
        return new SendingContext(event, task);
    }

    public Class<? extends Event> getEventClass() {
        return event.getClass();
    }
}

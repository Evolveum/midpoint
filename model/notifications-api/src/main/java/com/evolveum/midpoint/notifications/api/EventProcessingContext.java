/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.SendingContext;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

/**
 * Global context for processing a single event.
 *
 * @param defaultExpressionProfile Expression profile to be used if no other is specified.
 * (Currently, no others are provided. But later we may determine ones e.g. when expressions come from message template objects.)
 */
@Experimental
public record EventProcessingContext<E extends Event>(
        @NotNull E event,
        @NotNull ExpressionProfile defaultExpressionProfile,
        @NotNull Task task
) {

    public @NotNull LightweightIdentifier getEventId() {
        return event.getId();
    }

    public SendingContext sendingContext() {
        return new SendingContext(defaultExpressionProfile, event, task);
    }

    public Class<? extends Event> getEventClass() {
        return event.getClass();
    }
}

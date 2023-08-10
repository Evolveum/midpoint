/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.schema.config.EventHandlerConfigItem;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Handles notification events.
 */
public interface NotificationManager {

    default void processEvent(@NotNull Event event, Task task, OperationResult result) {
        processEvent(event, null, null, task, result);
    }

    @Contract("_, !null, null, _, _ -> fail")
    void processEvent(
            @NotNull Event event,
            @Nullable EventHandlerConfigItem customHandler,
            @Nullable ExpressionProfile customHandlerExpressionProfile,
            @NotNull Task task,
            @NotNull OperationResult result);

    boolean isDisabled();

    void setDisabled(boolean disabled);
}

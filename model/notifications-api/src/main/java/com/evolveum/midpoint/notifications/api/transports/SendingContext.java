/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.transports;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;

import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contextual information related to sending of a message.
 *
 * @param expressionProfile profile to be used when evaluating expressions during the operation
 * @param event conceptually, it doesn't much belong here - TODO review
 */
@Experimental
public record SendingContext(
        @NotNull ExpressionProfile expressionProfile,
        @Nullable Event event,
        @NotNull Task task) {
}

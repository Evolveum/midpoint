/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.smart.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.Nullable;

/**
 * Caller-side context propagated with a service call.
 */
public record ClientCallContext(
        @Nullable Task task,
        @Nullable OperationResult result,
        @Nullable ResourceType resource) {

    public static ClientCallContext empty() {
        return new ClientCallContext(null, null, null);
    }

    public static ClientCallContext of(
            @Nullable Task task,
            @Nullable OperationResult result,
            @Nullable ResourceType resource) {
        return new ClientCallContext(task, result, resource);
    }
}

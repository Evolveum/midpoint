/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.ResourceOperationStatus;

public class ResourceObjectOperationReturnValue<T> extends ResourceObjectOperationResult {

    @Nullable private final T returnValue;

    ResourceObjectOperationReturnValue(@Nullable T returnValue, @NotNull ResourceOperationStatus status) {
        super(status);
        this.returnValue = returnValue;
    }

    @Nullable T getReturnValue() {
        return returnValue;
    }

    public static <T> ResourceObjectOperationReturnValue<T> wrap(@Nullable T returnValue, @NotNull OperationResult result) {
        return new ResourceObjectOperationReturnValue<>(
                returnValue, ResourceOperationStatus.fromResult(result, null));
    }

    @Override
    public void shortDump(StringBuilder sb) {
        super.shortDump(sb);
        sb.append(": ");
        sb.append(returnValue);
    }
}

/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.result.ResourceOperationStatus;

/**
 * Superclass of UCF "modifying" operation results that return something, i.e. for add and modify.
 *
 * @author semancik
 */
public class UcfOperationReturnValue<T> extends UcfOperationResult {

    @Nullable private final T returnValue;

    UcfOperationReturnValue(@Nullable T returnValue, @NotNull ResourceOperationStatus status) {
        super(status);
        this.returnValue = returnValue;
    }

    public @Nullable T getReturnValue() {
        return returnValue;
    }

    @Override
    public void shortDump(StringBuilder sb) {
        super.shortDump(sb);
        sb.append(": ");
        sb.append(returnValue);
    }
}

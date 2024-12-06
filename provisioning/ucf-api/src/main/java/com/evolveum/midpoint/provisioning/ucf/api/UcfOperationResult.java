/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.result.ResourceOperationStatus;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Superclass of the return values of all UCF "modifying" operations, i.e. for add, modify, and delete.
 *
 * Originally, this was part of a more generic concept, but it was transformed to a simple, single-use data structure.
 */
public abstract class UcfOperationResult implements ShortDumpable {

    /**
     * Wraps the actual status (success, failure, in progress), asynchronous operation reference,
     * and the operation type (manual, asynchronous).
     */
    @NotNull private final ResourceOperationStatus opStatus;

    UcfOperationResult(@NotNull ResourceOperationStatus opStatus) {
        this.opStatus = opStatus;
    }

    public @NotNull ResourceOperationStatus getOpStatus() {
        return opStatus;
    }

    public @NotNull OperationResultStatus getStatus() {
        return opStatus.getStatus();
    }

    public @Nullable String getAsynchronousOperationReference() {
        return opStatus.getAsynchronousOperationReference();
    }

    public @Nullable PendingOperationTypeType getOperationType() {
        return opStatus.getOperationType();
    }

    public boolean isInProgress() {
        return opStatus.isInProgress();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        opStatus.shortDump(sb);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }
}

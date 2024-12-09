/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.provisioning.ucf.api.UcfOperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.result.ResourceOperationStatus;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

/**
 * Superclass of the return values of all resource-object-level "modifying" operations (add, modify, delete).
 *
 * Originally, this was part of a more generic concept, but it was transformed to a simple, single-use data structure.
 *
 * @see UcfOperationResult
 */
public class ResourceObjectOperationResult implements ShortDumpable {

    /**
     * Wraps the actual status (success, failure, in progress), asynchronous operation reference,
     * and the operation type (manual, asynchronous).
     */
    @NotNull private final ResourceOperationStatus opStatus;

    /**
     * Was this a "quantum" operation, whose result may not be immediately visible when trying to read the data?
     * Currently set but not used anywhere.
     */
    private boolean quantumOperation;

    public @NotNull ResourceOperationStatus getOpStatus() {
        return opStatus;
    }

    ResourceObjectOperationResult(@NotNull ResourceOperationStatus opStatus) {
        this.opStatus = opStatus;
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

    void setQuantumOperation(boolean value) {
        // TODO This is an idea of old, but never finished. We set the information here, but actually make no use of it.
        //  Please implement it if needed.
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

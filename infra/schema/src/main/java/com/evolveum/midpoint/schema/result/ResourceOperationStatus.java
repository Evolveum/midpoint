/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.result;

import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * Represents the status of a "modifying" operation (add, modify, delete) on a resource.
 *
 * == Note about `fromResult` methods that create objects of this type
 *
 * The content of these objects overlaps with the content of {@link OperationResult}. However, the latter is much more generic,
 * hence the relevant data were extracted and are stored separately here. To allow simple creation of these objects, there
 * are many utility methods named `fromResult` that create objects of this type from {@link OperationResult}.
 *
 * Common requirements is that the source {@link OperationResult} must be closed (so its status is available), and
 * the asynchronous operation reference must be present at the top level.
 */
@SuppressWarnings("ClassCanBeRecord")
public class ResourceOperationStatus implements Serializable, ShortDumpable {

    /** Status of the operation. We are interested, in particular, whether it is still in progress, or not. */
    @NotNull private final OperationResultStatus status;

    /** The same as {@link OperationResult#asynchronousOperationReference}. */
    @Nullable private final String asynchronousOperationReference;

    /**
     * Is this operation of a special type, like manual or asynchronous? This knowledge often resides in the connector,
     * hence it is a part of the return value.
     */
    @Nullable private final PendingOperationTypeType operationType;

    public ResourceOperationStatus(
            @NotNull OperationResultStatus status,
            @Nullable String asynchronousOperationReference,
            @Nullable PendingOperationTypeType operationType) {
        this.status = status;
        this.asynchronousOperationReference = asynchronousOperationReference;
        this.operationType = operationType;
    }

    /** See the note in class javadoc. */
    public static @NotNull ResourceOperationStatus fromResult(
            @NotNull OperationResult result, @Nullable PendingOperationTypeType operationType) {
        return new ResourceOperationStatus(
                stateNonNull(result.getStatus(), "operation result status"),
                result.getAsynchronousOperationReference(),
                operationType);
    }

    public static @NotNull ResourceOperationStatus success() {
        return new ResourceOperationStatus(OperationResultStatus.SUCCESS, null, null);
    }

    public @NotNull OperationResultStatus getStatus() {
        return status;
    }

    public @Nullable String getAsynchronousOperationReference() {
        return asynchronousOperationReference;
    }

    public @Nullable PendingOperationTypeType getOperationType() {
        return operationType;
    }

    public static ResourceOperationStatus of(@NotNull OperationResultStatus status) {
        return new ResourceOperationStatus(status, null, null);
    }

    public boolean isInProgress() {
        return status == OperationResultStatus.IN_PROGRESS;
    }

    public @NotNull ResourceOperationStatus withStatus(@NotNull OperationResultStatus newStatus) {
        return new ResourceOperationStatus(newStatus, asynchronousOperationReference, operationType);
    }

    @Override
    public void shortDump(StringBuilder sb) {
        if (operationType != null) {
            sb.append("type=").append(operationType.value()).append(",");
        }
        sb.append("status=").append(status);
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

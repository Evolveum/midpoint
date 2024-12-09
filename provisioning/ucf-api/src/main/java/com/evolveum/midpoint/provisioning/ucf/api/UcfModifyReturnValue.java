/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.delta.PropertyDelta;

import com.evolveum.midpoint.schema.result.ResourceOperationStatus;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

import org.jetbrains.annotations.Nullable;

/**
 * Result of UCF `modifyObject` operation.
 */
public class UcfModifyReturnValue extends UcfOperationReturnValue<Collection<PropertyModificationOperation<?>>> {

    private UcfModifyReturnValue(
            @NotNull Collection<PropertyModificationOperation<?>> operations,
            @NotNull ResourceOperationStatus status) {
        super(operations, status);
    }

    /** See the note in {@link ResourceOperationStatus}. */
    public static UcfModifyReturnValue fromResult(
            @NotNull Collection<PropertyModificationOperation<?>> operations,
            @NotNull OperationResult result,
            @Nullable PendingOperationTypeType operationType) {
        return new UcfModifyReturnValue(
                operations,
                ResourceOperationStatus.fromResult(result, operationType));
    }

    /** See the note in {@link ResourceOperationStatus}. */
    public static UcfModifyReturnValue fromResult(
            @NotNull Collection<PropertyModificationOperation<?>> operations,
            @NotNull OperationResult operationResult) {
        return fromResult(operations, operationResult, null);
    }

    /** See the note in {@link ResourceOperationStatus}. */
    public static UcfModifyReturnValue fromResult(
            @NotNull OperationResult result, @Nullable PendingOperationTypeType operationType) {
        return fromResult(List.of(), result, operationType);
    }

    public static @NotNull UcfModifyReturnValue empty() {
        return new UcfModifyReturnValue(
                List.of(), ResourceOperationStatus.success());
    }

    public @NotNull Collection<PropertyModificationOperation<?>> getExecutedOperations() {
        return Objects.requireNonNull(getReturnValue());
    }

    public Collection<? extends PropertyDelta<?>> getExecutedOperationsAsPropertyDeltas() {
        Collection<PropertyDelta<?>> deltas = new ArrayList<>();
        for (PropertyModificationOperation<?> operation : getExecutedOperations()) {
            deltas.add(operation.getPropertyDelta());
        }
        return deltas;
    }
}

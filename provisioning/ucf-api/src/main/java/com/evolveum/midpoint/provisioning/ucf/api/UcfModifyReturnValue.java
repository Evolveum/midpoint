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

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

import org.jetbrains.annotations.Nullable;

/**
 * Result of UCF `modifyObject` operation.
 *
 * (Despite the superclass name, it is currently just a wrapper for returned data.)
 */
public class UcfModifyReturnValue extends AsynchronousOperationReturnValue<Collection<PropertyModificationOperation<?>>> {

    private UcfModifyReturnValue(
            @NotNull Collection<PropertyModificationOperation<?>> operations,
            @NotNull OperationResult operationResult,
            @Nullable PendingOperationTypeType operationType) {
        super(operations, operationResult);
        setOperationType(operationType);
    }

    public static UcfModifyReturnValue of(
            @NotNull Collection<PropertyModificationOperation<?>> operations,
            @NotNull OperationResult operationResult,
            @Nullable PendingOperationTypeType operationType) {
        return new UcfModifyReturnValue(operations, operationResult, operationType);
    }

    public static UcfModifyReturnValue of(
            @NotNull Collection<PropertyModificationOperation<?>> operations,
            @NotNull OperationResult operationResult) {
        return new UcfModifyReturnValue(operations, operationResult, null);
    }

    public static UcfModifyReturnValue of(@NotNull OperationResult operationResult) {
        return new UcfModifyReturnValue(List.of(), operationResult, null);
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

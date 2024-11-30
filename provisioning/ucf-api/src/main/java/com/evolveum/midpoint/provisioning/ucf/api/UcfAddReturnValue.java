/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.schema.processor.ShadowAttribute;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

import org.jetbrains.annotations.Nullable;

/**
 * Result of UCF `addObject` operation.
 *
 * (Despite the superclass name, it is currently just a wrapper for returned data.)
 */
public class UcfAddReturnValue extends AsynchronousOperationReturnValue<Collection<ShadowAttribute<?, ?, ?, ?>>> {

    private UcfAddReturnValue(
            @NotNull Collection<ShadowAttribute<?, ?, ?, ?>> returnValue,
            @NotNull OperationResult operationResult,
            @Nullable PendingOperationTypeType operationType) {
        super(returnValue, operationResult);
        setOperationType(operationType);
    }

    public static UcfAddReturnValue of(
            @NotNull Collection<ShadowAttribute<?, ?, ?, ?>> attributes,
            @NotNull OperationResult operationResult,
            @NotNull PendingOperationTypeType operationType) {
        return new UcfAddReturnValue(attributes, operationResult, operationType);
    }

    public static UcfAddReturnValue of(
            @NotNull Collection<ShadowAttribute<?, ?, ?, ?>> attributes,
            @NotNull OperationResult operationResult) {
        return new UcfAddReturnValue(attributes, operationResult, null);
    }

    public static UcfAddReturnValue of(@NotNull OperationResult operationResult) {
        return new UcfAddReturnValue(List.of(), operationResult, null);
    }

    /**
     * Some or all attributes of the created object - if the UCF connector supports this.
     *
     * @see ConnectorInstance#addObject(PrismObject, SchemaAwareUcfExecutionContext, OperationResult)
     */
    public @Nullable Collection<ShadowAttribute<?, ?, ?, ?>> getKnownCreatedObjectAttributes() {
        return getReturnValue();
    }
}

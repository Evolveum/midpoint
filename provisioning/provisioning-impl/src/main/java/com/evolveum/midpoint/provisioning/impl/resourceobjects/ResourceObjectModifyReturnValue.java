/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.prism.delta.PropertyDelta;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.ucf.api.UcfModifyReturnValue;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Result of resource object `modify` operation.
 * (Despite of the superclass name, it is synchronous in nature.)
 *
 * @see UcfModifyReturnValue
 */
public class ResourceObjectModifyReturnValue extends AsynchronousOperationReturnValue<Collection<PropertyDelta<?>>> {

    private ResourceObjectModifyReturnValue(
            @NotNull Collection<PropertyDelta<?>> returnValue, @NotNull OperationResult operationResult) {
        super(returnValue, operationResult);
    }

    public static ResourceObjectModifyReturnValue of(
            @NotNull Collection<PropertyDelta<?>> modifications,
            @NotNull OperationResult result,
            PendingOperationTypeType operationType) {
        var rv = new ResourceObjectModifyReturnValue(modifications, result);
        rv.setOperationType(operationType);
        return rv;
    }

    public static ResourceObjectModifyReturnValue of(@NotNull OperationResult result) {
        return new ResourceObjectModifyReturnValue(List.of(), result);
    }

    public @NotNull Collection<PropertyDelta<?>> getExecutedDeltas() {
        return Objects.requireNonNull(getReturnValue());
    }
}

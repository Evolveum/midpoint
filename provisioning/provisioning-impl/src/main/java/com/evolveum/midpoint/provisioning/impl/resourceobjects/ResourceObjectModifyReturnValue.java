/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.prism.delta.PropertyDelta;

import com.evolveum.midpoint.schema.result.ResourceOperationStatus;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.ucf.api.UcfModifyReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Result of resource object `modify` operation.
 *
 * @see UcfModifyReturnValue
 */
public class ResourceObjectModifyReturnValue extends ResourceObjectOperationReturnValue<Collection<PropertyDelta<?>>> {

    private ResourceObjectModifyReturnValue(
            @NotNull Collection<PropertyDelta<?>> returnValue, @NotNull ResourceOperationStatus opStatus) {
        super(returnValue, opStatus);
    }

    static ResourceObjectModifyReturnValue fromResult(
            @NotNull Collection<PropertyDelta<?>> modifications,
            @NotNull OperationResult result,
            PendingOperationTypeType operationType) {
        return new ResourceObjectModifyReturnValue(
                modifications,
                ResourceOperationStatus.fromResult(result, operationType));
    }

    public static ResourceObjectModifyReturnValue fromResult(@NotNull OperationResult result) {
        return fromResult(List.of(), result, null);
    }

    public @NotNull Collection<PropertyDelta<?>> getExecutedDeltas() {
        return Objects.requireNonNull(getReturnValue());
    }
}

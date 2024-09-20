/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.provisioning.ucf.api.UcfAddReturnValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.Nullable;

/**
 * Return value of resource object `add` operation.
 * (Despite of the superclass name, it is synchronous in nature.)
 *
 * @see UcfAddReturnValue
 */
public class ResourceObjectAddReturnValue extends AsynchronousOperationReturnValue<ResourceObjectShadow> {

    private ResourceObjectAddReturnValue(@Nullable ResourceObjectShadow returnValue, @NotNull OperationResult operationResult) {
        super(returnValue, operationResult);
    }

    public static ResourceObjectAddReturnValue of(
            @NotNull ResourceObjectShadow object,
            @NotNull OperationResult result,
            PendingOperationTypeType operationType) {
        var rv = new ResourceObjectAddReturnValue(object, result);
        rv.setOperationType(operationType);
        return rv;
    }

    public static ResourceObjectAddReturnValue of(@NotNull OperationResult result) {
        return new ResourceObjectAddReturnValue(null, result);
    }
}

/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of UCF `deleteObject` operation.
 *
 * (Despite the superclass name, it is currently just a wrapper for returned data.)
 */
public class UcfDeleteReturnValue extends AsynchronousOperationResult {

    private UcfDeleteReturnValue(@NotNull OperationResult operationResult, @Nullable PendingOperationTypeType operationType) {
        super(operationResult);
        setOperationType(operationType);
    }

    public static UcfDeleteReturnValue of(
            @NotNull OperationResult operationResult,
            @NotNull PendingOperationTypeType operationType) {
        return new UcfDeleteReturnValue(operationResult, operationType);
    }

    public static UcfDeleteReturnValue of(@NotNull OperationResult operationResult) {
        return new UcfDeleteReturnValue(operationResult, null);
    }
}

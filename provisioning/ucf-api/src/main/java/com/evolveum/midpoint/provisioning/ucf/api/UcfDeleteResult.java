/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.ResourceOperationStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

/**
 * Result of UCF `deleteObject` operation.
 */
public class UcfDeleteResult extends UcfOperationResult {

    private UcfDeleteResult(@NotNull ResourceOperationStatus status) {
        super(status);
    }

    /** See the note in {@link ResourceOperationStatus}. */
    public static UcfDeleteResult fromResult(
            @NotNull OperationResult result,
            @Nullable PendingOperationTypeType operationType) {
        return new UcfDeleteResult(ResourceOperationStatus.fromResult(result, operationType));
    }

    /** See the note in {@link ResourceOperationStatus}. */
    public static UcfDeleteResult fromResult(@NotNull OperationResult result) {
        return fromResult(result, null);
    }
}

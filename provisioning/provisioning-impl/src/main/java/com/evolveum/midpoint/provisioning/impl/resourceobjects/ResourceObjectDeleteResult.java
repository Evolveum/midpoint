/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.ucf.api.UcfDeleteResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.ResourceOperationStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

/**
 * Result of resource object `delete` operation.
 *
 * @see UcfDeleteResult
 */
public class ResourceObjectDeleteResult extends ResourceObjectOperationResult {

    private ResourceObjectDeleteResult(@NotNull ResourceOperationStatus status) {
        super(status);
    }

    /** See the note in {@link ResourceOperationStatus}. */
    static ResourceObjectDeleteResult fromResult(@NotNull OperationResult result, PendingOperationTypeType operationType) {
        return new ResourceObjectDeleteResult(
                ResourceOperationStatus.fromResult(result, operationType));
    }
}

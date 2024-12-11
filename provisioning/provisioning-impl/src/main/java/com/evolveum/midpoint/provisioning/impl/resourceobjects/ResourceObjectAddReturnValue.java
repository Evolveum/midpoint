/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.UcfAddReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.ResourceOperationStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;

/**
 * Return value of resource object `add` operation.
 *
 * @see UcfAddReturnValue
 * @see ResourceObjectConverter#addResourceObject(ProvisioningContext, ResourceObjectShadow,
 * OperationProvisioningScriptsType, ConnectorOperationOptions, boolean, OperationResult)
 */
public class ResourceObjectAddReturnValue extends ResourceObjectOperationReturnValue<ResourceObjectShadow> {

    private ResourceObjectAddReturnValue(@NotNull ResourceObjectShadow returnValue, @NotNull ResourceOperationStatus opStatus) {
        super(returnValue, opStatus);
    }

    /** See the note in {@link ResourceOperationStatus}. */
    static ResourceObjectAddReturnValue fromResult(
            @NotNull ResourceObjectShadow object,
            @NotNull OperationResult currentResult,
            @NotNull ResourceOperationStatus ucfOpStatus) {
        return new ResourceObjectAddReturnValue(
                object,
                ResourceOperationStatus.fromResult(currentResult, ucfOpStatus.getOperationType()));
    }

    /**
     * The object that was created (or was submitted to be created) on the resource.
     *
     * - Resource-object-level details (simulated validity, simulated references) should *not* be present here.
     * - Primary identifier should be present (if the creation took place).
     * - Volatile attributes should be present as well (if the creation took place), assuming they are correctly defined.
     */
    public @NotNull ResourceObjectShadow getCreatedObject() {
        return Objects.requireNonNull(getReturnValue());
    }
}

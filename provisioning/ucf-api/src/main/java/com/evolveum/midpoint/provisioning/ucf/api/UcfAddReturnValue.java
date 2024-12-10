/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ShadowAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.ResourceOperationStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

/**
 * Result of UCF `addObject` operation.
 *
 * @see ConnectorInstance#addObject(PrismObject, SchemaAwareUcfExecutionContext, OperationResult)
 */
public class UcfAddReturnValue extends UcfOperationReturnValue<Collection<ShadowAttribute<?, ?, ?, ?>>> {

    private UcfAddReturnValue(
            @NotNull Collection<ShadowAttribute<?, ?, ?, ?>> returnValue, @NotNull ResourceOperationStatus status) {
        super(returnValue, status);
    }

    private static UcfAddReturnValue fromResult(
            @NotNull Collection<ShadowAttribute<?, ?, ?, ?>> attributes,
            @NotNull OperationResult result,
            @Nullable PendingOperationTypeType operationType) {
        return new UcfAddReturnValue(
                attributes,
                ResourceOperationStatus.fromResult(result, operationType));
    }

    /** See the note in {@link ResourceOperationStatus}. */
    public static UcfAddReturnValue fromResult(
            @NotNull Collection<ShadowAttribute<?, ?, ?, ?>> attributes,
            @NotNull OperationResult result) {
        return fromResult(attributes, result, null);
    }

    /** See the note in {@link ResourceOperationStatus}. */
    public static UcfAddReturnValue fromResult(@NotNull OperationResult result, @Nullable PendingOperationTypeType operationType) {
        return fromResult(List.of(), result, operationType);
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

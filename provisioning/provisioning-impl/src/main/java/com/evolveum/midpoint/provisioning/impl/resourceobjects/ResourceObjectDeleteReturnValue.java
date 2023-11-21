/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.ucf.api.UcfDeleteReturnValue;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Result of resource object `delete` operation.
 * (Despite of the superclass name, it is synchronous in nature.)
 *
 * @see UcfDeleteReturnValue
 */
public class ResourceObjectDeleteReturnValue extends AsynchronousOperationResult {

    private ResourceObjectDeleteReturnValue(@NotNull OperationResult operationResult) {
        super(operationResult);
    }

    public static ResourceObjectDeleteReturnValue of(@NotNull OperationResult result, PendingOperationTypeType operationType) {
        var rv = new ResourceObjectDeleteReturnValue(result);
        rv.setOperationType(operationType);
        return rv;
    }

    public static ResourceObjectDeleteReturnValue of(@NotNull OperationResult result) {
        return new ResourceObjectDeleteReturnValue(result);
    }
}

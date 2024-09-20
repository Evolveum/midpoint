/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

/**
 * Internal interface to handle each object found during by {@link ResourceObjectSearchOperation}.
 * (It is not the inner class of it, because that class is package-private.)
 *
 * Note that the {@link ResourceObjectFound} instances passed on are lazily-initializable.
 */
@Experimental
public interface ResourceObjectHandler {

    boolean handle(@NotNull ResourceObjectFound resourceObject, @NotNull OperationResult result)
            throws SchemaException;
}

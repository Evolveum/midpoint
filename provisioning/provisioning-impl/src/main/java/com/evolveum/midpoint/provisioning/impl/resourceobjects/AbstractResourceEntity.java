/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.impl.InitializableObjectMixin;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.AbstractShadowedEntity;
import com.evolveum.midpoint.provisioning.util.InitializationState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;

/**
 * Processing of retrieved resource objects that is common for search and sync operations.
 *
 * Main responsibilities:
 *
 * - shadow acquisition in emergency and ultra-emergency modes
 * - shadow management (update, creation of shadowed object)
 * - support for initialization and provisioning context information
 *
 * Note that resource object, repo shadow, and shadowed objects are not held here, as their semantics are different
 * for objects and changes. They are accessed by abstract ("SPI") methods.
 *
 * @see ResourceObjectFound
 * @see ResourceObjectChange
 *
 * @see AbstractShadowedEntity
 */
public abstract class AbstractResourceEntity implements InitializableObjectMixin {

    /**
     * Context of the processing that was known when the search or sync operation was invoked.
     * It contains the original (caller) task and only approximate (if any) class/kind/intent specification.
     */
    @NotNull final ProvisioningContext originalCtx;

    /**
     * The {@link #originalCtx} with the worker task applied. Available since initialization start.
     */
    ProvisioningContext globalCtx;

    /** The {@link #globalCtx} refined as needed during initialization. */
    ProvisioningContext effectiveCtx;

    /** State of the processing. */
    @NotNull private final InitializationState initializationState = InitializationState.created();

    /** Useful beans local to the Resource objects package. */
    @NotNull final ResourceObjectsBeans b = ResourceObjectsBeans.get();

    AbstractResourceEntity(@NotNull ProvisioningContext originalCtx) {
        this.originalCtx = originalCtx;
    }

    @Override
    public void initializeInternalCommon(Task task, OperationResult result) {
        globalCtx = originalCtx.spawn(task);
    }

    public @NotNull InitializationState getInitializationState() {
        return initializationState;
    }

    public @NotNull ProvisioningContext getEffectiveCtx() {
        return MiscUtil.getFirstNonNullRequired(effectiveCtx, globalCtx, originalCtx);
    }
}

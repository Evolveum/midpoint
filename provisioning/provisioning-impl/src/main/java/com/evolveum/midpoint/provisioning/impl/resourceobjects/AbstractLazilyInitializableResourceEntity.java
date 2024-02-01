/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.impl.LazilyInitializableMixin;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.AbstractLazilyInitializableShadowedEntity;
import com.evolveum.midpoint.provisioning.util.InitializationState;
import com.evolveum.midpoint.util.MiscUtil;

/**
 * *Lazily-initializable* representation of retrieved resource objects and resource object changes.
 *
 * Note that resource object, repo shadow, and shadowed objects are not held here, as their semantics are different
 * for objects and changes. They are accessed by abstract ("SPI") methods.
 *
 * @see ResourceObjectFound
 * @see ResourceObjectChange
 *
 * @see AbstractLazilyInitializableShadowedEntity
 */
public abstract class AbstractLazilyInitializableResourceEntity implements LazilyInitializableMixin {

    /**
     * Context of the processing that was known when the search or sync operation was invoked.
     * It contains the original (caller) task and only approximate (if any) class/kind/intent specification.
     */
    @NotNull final ProvisioningContext originalCtx;

    /** The {@link #originalCtx} refined as needed during initialization. */
    ProvisioningContext effectiveCtx;

    /** State of the processing. */
    @NotNull final InitializationState initializationState = InitializationState.created();

    /** Should the associations be fetched during initialization? Probably will be replaced by "retrieve" options. */
    final boolean fetchAssociations;

    /** Useful beans local to the Resource objects package. */
    @NotNull final ResourceObjectsBeans b = ResourceObjectsBeans.get();

    AbstractLazilyInitializableResourceEntity(@NotNull ProvisioningContext originalCtx, boolean fetchAssociations) {
        this.originalCtx = originalCtx;
        this.fetchAssociations = fetchAssociations;
    }

    public @NotNull InitializationState getInitializationState() {
        return initializationState;
    }

    public @NotNull ProvisioningContext getEffectiveCtx() {
        return MiscUtil.getFirstNonNullRequired(effectiveCtx, originalCtx);
    }
}

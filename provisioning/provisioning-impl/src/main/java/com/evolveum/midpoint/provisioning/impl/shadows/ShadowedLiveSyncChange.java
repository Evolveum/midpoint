/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.schema.AcknowledgementSink;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectLiveSyncChange;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.ChangeProcessingBeans;

/**
 * Adopted Live Sync change. The client should implement the {@link AcknowledgementSink} interface.
 */
public class ShadowedLiveSyncChange extends ShadowedChange<ResourceObjectLiveSyncChange> {

    public ShadowedLiveSyncChange(@NotNull ResourceObjectLiveSyncChange resourceObjectChange, boolean simulate,
            ChangeProcessingBeans beans) {
        super(resourceObjectChange, simulate, beans);
    }

    public PrismProperty<?> getToken() {
        return resourceObjectChange.getToken();
    }
}

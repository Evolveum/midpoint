/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.provisioning.api.LiveSyncToken;
import com.evolveum.midpoint.schema.AcknowledgementSink;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectLiveSyncChange;

/**
 * Shadowed Live Sync change. The client should implement the {@link AcknowledgementSink} interface.
 */
public class ShadowedLiveSyncChange extends ShadowedChange<ResourceObjectLiveSyncChange> {

    public ShadowedLiveSyncChange(@NotNull ResourceObjectLiveSyncChange resourceObjectChange) {
        super(resourceObjectChange);
    }

    public LiveSyncToken getToken() {
        return resourceObjectChange.getToken();
    }

    @Override
    protected String getDefaultChannel() {
        return SchemaConstants.CHANNEL_LIVE_SYNC_URI;
    }
}

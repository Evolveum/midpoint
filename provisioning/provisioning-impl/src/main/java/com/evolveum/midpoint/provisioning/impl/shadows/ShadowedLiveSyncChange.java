/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

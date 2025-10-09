/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectAsyncChange;

import com.evolveum.midpoint.schema.AcknowledgementSink;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.NotNull;

/**
 * Adopted "async update" change.
 */
public class ShadowedAsyncChange
        extends ShadowedChange<ResourceObjectAsyncChange>
        implements AcknowledgementSink {

    public ShadowedAsyncChange(@NotNull ResourceObjectAsyncChange resourceObjectChange) {
        super(resourceObjectChange);
    }

    @Override
    public void acknowledge(boolean release, OperationResult result) {
        resourceObjectChange.acknowledge(release, result);
    }

    @Override
    protected String getDefaultChannel() {
        return SchemaConstants.CHANNEL_ASYNC_UPDATE_URI;
    }
}

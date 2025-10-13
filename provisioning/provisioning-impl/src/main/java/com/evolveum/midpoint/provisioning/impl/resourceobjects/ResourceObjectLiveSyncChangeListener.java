/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.provisioning.api.LiveSyncToken;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Processor of live sync changes emitted by {@link ResourceObjectConverter#fetchChanges(ProvisioningContext, LiveSyncToken,
 * Integer, ResourceObjectLiveSyncChangeListener, OperationResult)}.
 *
 * Do not confuse with {@link ResourceObjectChangeListener}. (We should probably rename one of these interfaces.)
 */
public interface ResourceObjectLiveSyncChangeListener {

    /**
     * Called when given change has to be processed.
     *
     * @param change The change.
     * @return false if the processing of changes has to be stopped
     */
    boolean onChange(ResourceObjectLiveSyncChange change, OperationResult result);
}

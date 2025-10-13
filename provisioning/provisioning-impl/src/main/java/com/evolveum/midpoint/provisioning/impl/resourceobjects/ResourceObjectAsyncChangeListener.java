/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Processor of async changes emitted by {@link ResourceObjectConverter#listenForAsynchronousUpdates(ProvisioningContext,
 * ResourceObjectAsyncChangeListener, OperationResult)}.
 *
 * Do not confuse with {@link ResourceObjectChangeListener}. (We should probably rename one of these interfaces.)
 */
public interface ResourceObjectAsyncChangeListener {

    /**
     * Called when given change has to be processed.
     */
    void onChange(ResourceObjectAsyncChange change, Task listenerTask, OperationResult result);
}

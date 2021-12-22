/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

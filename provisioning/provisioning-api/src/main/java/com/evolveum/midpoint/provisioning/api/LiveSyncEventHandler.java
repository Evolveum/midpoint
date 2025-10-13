/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Handles changes retrieved by {@link ProvisioningService#synchronize(ResourceShadowCoordinates, LiveSyncOptions,
 * LiveSyncTokenStorage, LiveSyncEventHandler, Task, OperationResult)} method.
 */
@Experimental
public interface LiveSyncEventHandler extends SynchronizationEventHandler<LiveSyncEvent> {

    /**
     * Invoked when no more events are to be expected during the current synchronization operation.
     * The typical reasons are: no more livesync changes, or the task was suspended, or the event handler
     * signalled to stop the processing.
     *
     * Should do necessary cleanup, e.g. wait for workers to finish.
     * All requests should be acknowledged (positively or negatively) when this method returns.
     *
     * Should not throw an exception.
     */
    void allEventsSubmitted(OperationResult result);
}

/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

/**
 * Handles changes retrieved by {@link ProvisioningService#synchronize(ResourceShadowDiscriminator, Task, TaskPartitionDefinitionType, LiveSyncEventHandler, OperationResult)} method.
 */
@Experimental
public interface LiveSyncEventHandler extends SynchronizationEventHandler<LiveSyncEvent> {

    /**
     * Invoked when no more events are to be expected during the current synchronization operation.
     * The typical reasons are: no more livesync changes, or the task was suspended, or the event handler
     * signalled to stop the processing.
     *
     * Should do necessary cleanup, e.g. wait for workers to finish.
     *
     * TODO should we require this method to ack all pending events?
     */
    void allEventsSubmitted(OperationResult result);

}

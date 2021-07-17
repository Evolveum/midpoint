/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.util.OperationExecutionRecorderForTasks.Target;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Request to process a containerable.
 */
@Experimental
public class ContainerableProcessingRequest<C extends Containerable> extends ItemProcessingRequest<C> {

    public ContainerableProcessingRequest(int sequentialNumber, C item,
            @NotNull AbstractIterativeActivityExecution<C, ?, ?, ?> activityExecution) {
        super(sequentialNumber, item, activityExecution);
    }

    /**
     * This can be overridden in cases when we know how to derive a name or display name for the containerable.
     */
    @Override
    public @NotNull IterationItemInformation getIterationItemInformation() {
        return new IterationItemInformation(
                "seq#" + getSequentialNumber(), // TODO very brutal hack, reconsider
                null,
                getType(item),
                null);
    }

    @Override
    public Target getOperationExecutionRecordingTarget() {
        return null;
    }

    @Override
    public String getObjectOidToRecordRetryTrigger() {
        return null;
    }

    @Override
    public void acknowledge(boolean release, OperationResult result) {
        // Nothing to acknowledge here.
    }

    @Override
    public @Nullable String getItemOid() {
        return null;
    }

    @Override
    public @Nullable SynchronizationSituationType getSynchronizationSituationOnProcessingStart() {
        return null;
    }

    @Override
    public String toString() {
        return "ContainerableProcessingRequest{" +
                "item=" + item +
                ", identifier='" + identifier + '\'' +
                '}';
    }
}

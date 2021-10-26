/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.processing;

import com.evolveum.midpoint.repo.common.activity.run.IterativeActivityRun;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.util.OperationExecutionRecorderForTasks.Target;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

/**
 * Request to process a generic data item.
 */
@Experimental
public class GenericProcessingRequest<T> extends ItemProcessingRequest<T> {

    public GenericProcessingRequest(int sequentialNumber, T item,
            @NotNull IterativeActivityRun<T, ?, ?, ?> activityRun) {
        super(sequentialNumber, item, activityRun);
    }

    /**
     * This can be overridden in cases when we know how to derive a name or display name or the type for the item.
     */
    @Override
    public @NotNull IterationItemInformation getIterationItemInformation() {
        return new IterationItemInformation(
                "seq#" + getSequentialNumber(), // TODO very brutal hack, reconsider
                null,
                null,
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
        return "GenericProcessingRequest{" +
                "item=" + item +
                ", identifier='" + identifier + '\'' +
                '}';
    }
}

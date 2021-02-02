/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.SynchronizationEvent;
import com.evolveum.midpoint.repo.common.task.AbstractIterativeItemProcessor;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.util.OperationExecutionRecorderForTasks;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * TODO
 *
 * It is comparable on the sequence number.
 */
public class SyncItemProcessingRequest<SE extends SynchronizationEvent>
        extends ItemProcessingRequest<SE>
        implements Comparable<SyncItemProcessingRequest<SE>> {

    SyncItemProcessingRequest(SE item, AbstractIterativeItemProcessor<SE, ?, ?, ?, ?> itemProcessor) {
        super(item, itemProcessor);
    }

    @Override
    public Object getCorrelationValue() {
        return getItem().getCorrelationValue();
    }

    @Override
    public OperationExecutionRecorderForTasks.Target getOperationExecutionRecordingTarget() {
        ResourceObjectShadowChangeDescription changeDescription = getItem().getChangeDescription();
        if (changeDescription != null && changeDescription.getCurrentShadow() != null) {
            return createRecordingTargetForObject(changeDescription.getCurrentShadow());
        } else {
            return new OperationExecutionRecorderForTasks.Target(null, ShadowType.COMPLEX_TYPE,
                    getMalformedEventIdentification(), getRootTaskOid(), TaskType.class);
        }
    }

    /** TODO (eventually also move to SynchronizationEvent) */
    private String getMalformedEventIdentification() {
        return getItem().toString();
    }

    @Override
    public String getObjectOidToRecordRetryTrigger() {
        SE event = getItem();
        if (event.getShadowOid() != null) {
            return event.getShadowOid();
        }
        ResourceObjectShadowChangeDescription changeDescription = event.getChangeDescription();
        if (changeDescription != null && changeDescription.getCurrentShadow() != null) {
            return changeDescription.getCurrentShadow().getOid(); // TODO
        } else {
            return null;
        }
    }

    @Override
    public @NotNull IterationItemInformation getIterationItemInformation() {
        ResourceObjectShadowChangeDescription changeDescription = getItem().getChangeDescription();
        if (changeDescription != null && changeDescription.getCurrentShadow() != null) {
            return new IterationItemInformation(changeDescription.getCurrentShadow());
        } else {
            return new IterationItemInformation(); // TODO
        }
    }

    @Override
    public void acknowledge(boolean release, OperationResult result) {
        item.acknowledge(release, result);
    }

    @Override
    public int compareTo(@NotNull SyncItemProcessingRequest<SE> o) {
        return item.compareTo(o.item);
    }
}

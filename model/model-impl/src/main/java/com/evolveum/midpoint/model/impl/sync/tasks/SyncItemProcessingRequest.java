/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.task.CorrelatableProcessingRequest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

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

import org.jetbrains.annotations.Nullable;

/**
 * TODO
 *
 * It is comparable on the sequence number.
 */
public class SyncItemProcessingRequest<SE extends SynchronizationEvent>
        extends ItemProcessingRequest<SE>
        implements Comparable<SyncItemProcessingRequest<SE>>, CorrelatableProcessingRequest {

    SyncItemProcessingRequest(SE item, AbstractIterativeItemProcessor<SE, ?, ?, ?, ?> itemProcessor) {
        super(item, itemProcessor);
    }

    @Override
    public Object getCorrelationValue() {
        return getItem().getCorrelationValue();
    }

    @Override
    public OperationExecutionRecorderForTasks.Target getOperationExecutionRecordingTarget() {
        PrismObject<ShadowType> shadowedObject = getItem().getShadowedObject();
        if (shadowedObject != null) {
            return createRecordingTargetForObject(shadowedObject);
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
        return getItemOid();
    }

    @Override
    public @NotNull IterationItemInformation getIterationItemInformation() {
        ResourceObjectShadowChangeDescription changeDescription = getItem().getChangeDescription();
        if (changeDescription != null && changeDescription.getShadowedResourceObject() != null) {
            return new IterationItemInformation(changeDescription.getShadowedResourceObject());
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

    @Override
    public @Nullable String getItemOid() {
        SE event = getItem();
        if (event.getShadowOid() != null) {
            return event.getShadowOid();
        }
        ResourceObjectShadowChangeDescription changeDescription = event.getChangeDescription();
        if (changeDescription != null) {
            return changeDescription.getShadowedResourceObject().getOid(); // TODO
        } else {
            return null;
        }
    }

    // TODO!!!
    @Override
    public @Nullable SynchronizationSituationType getSynchronizationSituationOnProcessingStart() {
        ResourceObjectShadowChangeDescription changeDescription = item.getChangeDescription();
        if (changeDescription != null) {
            return changeDescription.getShadowedResourceObject().asObjectable().getSynchronizationSituation();
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "SyncItemProcessingRequest{" +
                "item=" + item +
                ", identifier='" + identifier + '\'' +
                '}';
    }
}

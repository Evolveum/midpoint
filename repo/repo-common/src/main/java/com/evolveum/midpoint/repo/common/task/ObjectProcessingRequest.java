/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.repo.common.util.OperationExecutionRecorderForTasks.Target;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.Nullable;

/**
 * Request to process an object.
 */
public class ObjectProcessingRequest<O extends ObjectType> extends ItemProcessingRequest<PrismObject<O>> {

    ObjectProcessingRequest(PrismObject<O> item, AbstractIterativeItemProcessor<PrismObject<O>, ?, ?, ?, ?> itemProcessor) {
        super(item, itemProcessor);
    }

    @Override
    public @NotNull IterationItemInformation getIterationItemInformation() {
        return new IterationItemInformation(item);
    }

    @Override
    public Target getOperationExecutionRecordingTarget() {
        return createRecordingTargetForObject(getItem());
    }

    @Override
    public String getObjectOidToRecordRetryTrigger() {
        return getItem().asObjectable().getOid(); // TODO
    }

    @Override
    public void acknowledge(boolean release, OperationResult result) {
        // Nothing to acknowledge here.
    }

    @Override
    public @Nullable String getItemOid() {
        return item.getOid();
    }

    @Override
    public @Nullable SynchronizationSituationType getSynchronizationSituationOnProcessingStart() {
        O object = item.asObjectable();
        return object instanceof ShadowType ? ((ShadowType) object).getSynchronizationSituation() : null;
    }
}

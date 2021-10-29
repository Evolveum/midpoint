/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.processing;

import com.evolveum.midpoint.repo.common.activity.run.IterativeActivityRun;
import com.evolveum.midpoint.repo.common.util.OperationExecutionRecorderForTasks.Target;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.Nullable;

/**
 * Request to process an object.
 *
 * It is a subtype of {@link ContainerableProcessingRequest} that provides more specific implementations
 * of the methods like {@link #getItemOid()} or {@link #getIterationItemInformation()}.
 *
 * TODO Reconsider if this is a good idea. But it seems so: an alternative would be
 *  a lot of if-then-else commands in the method bodies.
 */
public class ObjectProcessingRequest<O extends ObjectType> extends ContainerableProcessingRequest<O> {

    ObjectProcessingRequest(int sequentialNumber, O item,
            @NotNull IterativeActivityRun<O, ?, ?, ?> activityRun) {
        super(sequentialNumber, item, activityRun);
    }

    @Override
    public @NotNull IterationItemInformation getIterationItemInformation() {
        return new IterationItemInformation(item.asPrismObject());
    }

    @Override
    public Target getOperationExecutionRecordingTarget() {
        return createRecordingTargetForObject(getItem().asPrismObject());
    }

    @Override
    public String getObjectOidToRecordRetryTrigger() {
        return getItem().getOid(); // TODO
    }

    @Override
    public @Nullable String getItemOid() {
        return item.getOid();
    }

    @Override
    public @Nullable SynchronizationSituationType getSynchronizationSituationOnProcessingStart() {
        return item instanceof ShadowType ? ((ShadowType) item).getSynchronizationSituation() : null;
    }

    @Override
    public String toString() {
        return "ObjectProcessingRequest{" +
                "item=" + item +
                ", identifier='" + identifier + '\'' +
                '}';
    }
}

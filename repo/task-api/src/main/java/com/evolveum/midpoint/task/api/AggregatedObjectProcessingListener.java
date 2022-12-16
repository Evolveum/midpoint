/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TODO
 */
public interface AggregatedObjectProcessingListener {

    /**
     * Called after a specific item was processed. (Or after the "item-less" processing ended.)
     *
     * @param stateBefore The state of the object before the processing
     * @param executedDelta (Aggregated) delta that was executed (or attempted to be executed) - if any
     * @param simulatedDelta (Aggregated) delta that would be executed if the execution mode was real - if any
     * @param result Operation result under which the necessary actions are carried out
     */
    <O extends ObjectType> void onItemProcessed(
            @Nullable O stateBefore,
            @Nullable ObjectDelta<O> executedDelta,
            @Nullable ObjectDelta<O> simulatedDelta,
            @NotNull OperationResult result);
}

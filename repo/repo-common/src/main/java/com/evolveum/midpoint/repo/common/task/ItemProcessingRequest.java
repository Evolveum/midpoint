/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.schema.AcknowledgementSink;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/**
 * <p>Holds an item that is scheduled for processing.</p>
 *
 * <p>Besides the item itself it provides so called <i>correlation value</i> that is used to correctly order requests
 * that relate to the same midPoint or resource object - for example, two async changes related to a given account.</p>
 */
public abstract class ItemProcessingRequest<I> implements AcknowledgementSink {

    @NotNull protected final I item;
    @NotNull private final AbstractIterativeItemProcessor<I, ?, ?, ?, ?> itemProcessor;

    public ItemProcessingRequest(@NotNull I item, @NotNull AbstractIterativeItemProcessor<I, ?, ?, ?, ?> itemProcessor) {
        this.item = item;
        this.itemProcessor = itemProcessor;
    }

    public @NotNull I getItem() {
        return item;
    }

    /**
     * @return The value against which we match other requests to be aligned with this one.
     */
    public Object getCorrelationValue() {
        return null;
    }

    /**
     * @return Object to which we will write an operation execution record. If null or not existing,
     * an alternative target is to be found.
     */
    public abstract ObjectType getObjectToReportOperation();

    public void done() {
        // TODO
    }

    public abstract @NotNull IterationItemInformation getIterationItemInformation();

    public boolean process(RunningTask workerTask, OperationResult result) {
        ItemProcessingGatekeeper<I> administrator = new ItemProcessingGatekeeper<>(this, itemProcessor, workerTask);
        return administrator.process(result);
    }
}

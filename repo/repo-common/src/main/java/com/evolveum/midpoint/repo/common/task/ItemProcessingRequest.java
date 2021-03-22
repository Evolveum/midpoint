/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.util.OperationExecutionRecorderForTasks;
import com.evolveum.midpoint.schema.AcknowledgementSink;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.task.api.RunningTask;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Objects;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

/**
 * <p>Holds an item that is scheduled for processing.</p>
 *
 * <p>Besides the item itself it provides so called <i>correlation value</i> that is used to correctly order requests
 * that relate to the same midPoint or resource object - for example, two async changes related to a given account.</p>
 */
public abstract class ItemProcessingRequest<I> implements AcknowledgementSink {

    @NotNull protected final I item;
    @NotNull private final AbstractIterativeItemProcessor<I, ?, ?, ?, ?> itemProcessor;

    /**
     * Unique identifier of this request. Not to be confused with requestIdentifier used for auditing purposes!
     *
     * Most probably it will be replaced by something different.
     */
    @Experimental // maybe will be removed
    @NotNull protected final String identifier;

    public ItemProcessingRequest(@NotNull I item, @NotNull AbstractIterativeItemProcessor<I, ?, ?, ?, ?> itemProcessor) {
        this.item = item;
        this.itemProcessor = itemProcessor;
        this.identifier = itemProcessor.createItemProcessingRequestIdentifier();
    }

    public @NotNull I getItem() {
        return item;
    }

    /**
     * @return Object to which we will write an operation execution record (plus auxiliary information).
     */
    public abstract OperationExecutionRecorderForTasks.Target getOperationExecutionRecordingTarget();

    @NotNull
    protected OperationExecutionRecorderForTasks.Target createRecordingTargetForObject(PrismObject<? extends ObjectType> object) {
        return new OperationExecutionRecorderForTasks.Target(object, getType(object), getOrig(object.getName()),
                getRootTaskOid(), TaskType.class);
    }

    /**
     * @return OID of object to which we put a trigger causing operation retry (if known)
     */
    public abstract String getObjectOidToRecordRetryTrigger();

    public abstract @NotNull IterationItemInformation getIterationItemInformation();

    public boolean process(RunningTask workerTask, OperationResult result) {
        ItemProcessingGatekeeper<I> administrator = new ItemProcessingGatekeeper<>(this, itemProcessor, workerTask);
        return administrator.process(result);
    }

    protected PrismContext getPrismContext() {
        return itemProcessor.taskExecution.getPrismContext();
    }

    protected @NotNull String getRootTaskOid() {
        return itemProcessor.getTaskExecution().getRootTaskOid();
    }

    protected @NotNull QName getType(PrismObject<?> object) {
        return Objects.requireNonNull(
                getPrismContext().getSchemaRegistry().determineTypeForClass(object.getCompileTimeClass()));
    }

    /**
     * OID of the object connected to the item being processed (usually the object itself or related shadow).
     *
     * TODO reconsider
     */
    public abstract @Nullable String getItemOid();

    /**
     * TODO reconsider
     */
    public abstract @Nullable SynchronizationSituationType getSynchronizationSituationOnProcessingStart();

    public String getIdentifier() {
        return identifier;
    }
}

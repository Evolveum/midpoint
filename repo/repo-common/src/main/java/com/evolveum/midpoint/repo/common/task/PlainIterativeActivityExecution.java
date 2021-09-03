/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import org.jetbrains.annotations.Nullable;

/**
 * Execution of a plain iterative activity.
 *
 * Basically, here we implement abstract methods of {@link IterativeActivityExecution} generally
 * either by doing nothing or delegating to {@link PlainIterativeActivityExecutionSpecifics} instance.
 *
 * @param <I> Items being processed.
 * @param <WD> Work definition type.
 * @param <AH> Activity handler type.
 * @param <WS> Work (business) state type.
 */
public class PlainIterativeActivityExecution<
        I,
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType>
        extends IterativeActivityExecution<
            I,
            WD,
            AH,
            WS,
            PlainIterativeActivityExecution<I, WD, AH, ?>,
            PlainIterativeActivityExecutionSpecifics<I>> {

    public PlainIterativeActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context,
            @NotNull String shortNameCapitalized,
            @NotNull PlainIterativeSpecificsSupplier<I, WD, AH> specificsSupplier) {
        super(context, shortNameCapitalized, specificsSupplier);
    }

    @Override
    protected void prepareItemSource(OperationResult result) throws ActivityExecutionException, CommonException {
        // Nothing to do here. Item source preparation can be done in iterateOverItems method.
    }

    @Override
    protected @Nullable Integer determineExpectedTotal(OperationResult opResult) throws CommonException {
        return executionSpecifics.determineExpectedTotal(opResult);
    }

    @Override
    protected void iterateOverItemsInBucket(OperationResult result) throws CommonException {
        executionSpecifics.iterateOverItemsInBucket(bucket, result);
    }

    @Override
    protected @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        return executionSpecifics.getDefaultErrorAction();
    }

    @Override
    public boolean processItem(@NotNull ItemProcessingRequest<I> request, @NotNull RunningTask workerTask, OperationResult result)
            throws ActivityExecutionException, CommonException {
        return executionSpecifics.processItem(request, workerTask, result);
    }

    @FunctionalInterface
    public interface PlainIterativeSpecificsSupplier<I, WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>>
            extends IterativeActivityExecution.SpecificsSupplier<
            PlainIterativeActivityExecution<I, WD, AH, ?>,
            PlainIterativeActivityExecutionSpecifics<I>> {
    }
}

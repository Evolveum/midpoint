/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

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
public abstract class PlainIterativeActivityExecution<
        I,
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType>
        extends IterativeActivityExecution<I, WD, AH, WS>
        implements PlainIterativeActivityExecutionSpecifics<I> {

    public PlainIterativeActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context,
            @NotNull String shortNameCapitalized) {
        super(context, shortNameCapitalized);
    }

    @Override
    protected void prepareItemSourceForCurrentBucket(OperationResult result) {
        // Nothing to do here. Item source preparation can be done in iterateOverItems method.
    }

    /** We simply do not support repository-related item-counting options in plain-iterative activity executions. */
    @Override
    protected boolean isInRepository(OperationResult result) {
        return false;
    }
}

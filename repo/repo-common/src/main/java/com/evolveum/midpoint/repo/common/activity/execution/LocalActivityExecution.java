/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityRealizationStateType;

import org.jetbrains.annotations.NotNull;

public abstract class LocalActivityExecution<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        BS extends AbstractActivityWorkStateType> extends AbstractActivityExecution<WD, AH, BS> {

    protected LocalActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context) {
        super(context);
    }

    @Override
    protected @NotNull ActivityExecutionResult executeInternal(OperationResult result)
            throws ActivityExecutionException, CommonException {

        activityState.markInProgressLocal(result); // The realization state might be "in progress" already.

        getTreeStateOverview().recordExecutionStart(this, result);
        ActivityExecutionResult executionResult = null;
        try {
            executionResult = executeLocal(result);
        } finally {
            getTreeStateOverview().recordExecutionFinish(this, executionResult, result);
        }

        return executionResult;
    }

    protected abstract @NotNull ActivityExecutionResult executeLocal(OperationResult result)
            throws ActivityExecutionException, CommonException;
}

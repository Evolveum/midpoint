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

import org.jetbrains.annotations.NotNull;

/**
 * The "real" execution of an activity - i.e. not a delegation nor a distribution.
 *
 * Responsibilities at this level of abstraction:
 *
 * 1. records execution start/stop in the tree state overview,
 * 2. records execution start/stop in the item processing statistics (execution records),
 * 3. updates progress information (clears uncommitted on start).
 */
public abstract class LocalActivityExecution<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        BS extends AbstractActivityWorkStateType> extends AbstractActivityExecution<WD, AH, BS> {

    /** When did the execution start? */
    private long startTimestamp;

    protected LocalActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context) {
        super(context);
    }

    @Override
    protected @NotNull ActivityExecutionResult executeInternal(OperationResult result)
            throws ActivityExecutionException {

        activityState.markInProgressLocal(result); // The realization state might be "in progress" already.

        updateStateOnExecutionStart(result);
        ActivityExecutionResult executionResult;
        try {
            executionResult = executeLocal(result);
        } catch (Exception e) {
            executionResult = ActivityExecutionResult.handleException(e, this);
        }

        updateStateOnExecutionFinish(result, executionResult);
        return executionResult;
    }

    private void updateStateOnExecutionStart(OperationResult result) throws ActivityExecutionException {
        startTimestamp = System.currentTimeMillis();
        getTreeStateOverview().recordExecutionStart(this, result);
        if (supportsExecutionRecords()) {
            activityState.getLiveStatistics().getLiveItemProcessing().recordExecutionStart(startTimestamp);
        }
        activityState.getLiveProgress().clearUncommitted();
    }

    private void updateStateOnExecutionFinish(OperationResult result, ActivityExecutionResult executionResult)
            throws ActivityExecutionException {
        getTreeStateOverview().recordExecutionFinish(this, executionResult, result);
        if (supportsExecutionRecords()) {
            activityState.getLiveStatistics().getLiveItemProcessing()
                    .recordExecutionEnd(startTimestamp, System.currentTimeMillis());
        }
    }

    private boolean supportsExecutionRecords() {
        // Temporary solution: activities that have persistent/semi-persistent state are those that execute in short cycles
        // (like live sync, various scanners, and so on). We usually do not want to store execution records for these.
        return doesSupportStatistics() && activityStateDefinition.isSingleRealization();
    }

    protected abstract @NotNull ActivityExecutionResult executeLocal(OperationResult result)
            throws ActivityExecutionException, CommonException;
}

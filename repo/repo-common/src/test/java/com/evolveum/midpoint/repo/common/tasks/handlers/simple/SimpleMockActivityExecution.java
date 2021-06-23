/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.simple;

import com.evolveum.midpoint.repo.common.activity.state.ActivityItemProcessingStatistics;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.schema.statistics.IterativeOperationStartInfo;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemProcessingOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.execution.ActivityExecutionResult;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.execution.LocalActivityExecution;
import com.evolveum.midpoint.repo.common.tasks.handlers.CommonMockActivityHelper;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

/**
 * TODO
 */
class SimpleMockActivityExecution
        extends LocalActivityExecution<SimpleMockWorkDefinition, SimpleMockActivityHandler, AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleMockActivityExecution.class);

    SimpleMockActivityExecution(
            @NotNull ExecutionInstantiationContext<SimpleMockWorkDefinition, SimpleMockActivityHandler> context) {
        super(context);
    }

    @Override
    protected @NotNull ActivityExecutionResult executeLocal(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        String message = activity.getWorkDefinition().getMessage();

        ActivityItemProcessingStatistics.Operation operation = activityState.getLiveItemProcessingStatistics()
                .recordOperationStart(new IterativeOperationStartInfo(
                        new IterationItemInformation(message, null, null, null)));

        MiscUtil.sleepIgnoringInterruptedException(10); // to avoid wall clock time of 0 (failing throughput-assuming tests)

        LOGGER.info("Message: {}", message);
        getRecorder().recordExecution(message);

        CommonMockActivityHelper helper = getActivityHandler().getMockHelper();
        helper.increaseExecutionCount(this, result);

        try {
            helper.failIfNeeded(this, activity.getWorkDefinition().getInitialFailures());
            QualifiedItemProcessingOutcomeType qualifiedOutcome =
                    new QualifiedItemProcessingOutcomeType(getPrismContext())
                            .outcome(ItemProcessingOutcomeType.SUCCESS);
            operation.done(qualifiedOutcome, null);
            incrementProgress(qualifiedOutcome);
        } catch (Exception e) {
            QualifiedItemProcessingOutcomeType qualifiedOutcome =
                    new QualifiedItemProcessingOutcomeType(getPrismContext())
                            .outcome(ItemProcessingOutcomeType.FAILURE);
            operation.done(qualifiedOutcome, e);
            incrementProgress(qualifiedOutcome);
            throw e;
        }

        return standardExecutionResult();
    }

    @NotNull
    private MockRecorder getRecorder() {
        return activity.getHandler().getRecorder();
    }

    @Override
    public void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "current recorder state", getRecorder(), indent+1);
    }

    @Override
    public boolean supportsStatistics() {
        return true;
    }

    @Override
    public boolean supportsSynchronizationStatistics() {
        return false;
    }

    @Override
    public boolean supportsActionsExecuted() {
        return false;
    }
}

/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.correlator.tasks;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class CorrelationActivityRun
        extends SearchBasedActivityRun<ShadowType, CorrelationWorkDefinition, CorrelationActivityHandler, AbstractActivityWorkStateType> {

    private CorrelationDefinitionType correlationDefinition;

    public CorrelationActivityRun(ActivityRunInstantiationContext<CorrelationWorkDefinition, CorrelationActivityHandler> ctx) {
        super(ctx, "Correlation");
        setInstanceReady();
    }

    @Override
    public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        if (!super.beforeRun(result)) {
            return false;
        }
        if (!isAnyPreview()) {
            throw new ActivityRunException("This activity is supported only in preview execution mode",
                    OperationResultStatus.FATAL_ERROR, TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR);
        }
        this.correlationDefinition = getWorkDefinition().resolveCorrelators();
        return true;
    }

    @Override
    public boolean processItem(@NotNull ShadowType shadow, @NotNull ItemProcessingRequest<ShadowType> req,
            RunningTask task, OperationResult result) {
        final CorrelationResult correlationResult = correlate(shadow);

        final SimulationTransaction simulationTransaction = getSimulationTransaction();
        if (simulationTransaction != null) {
            simulationTransaction.writeSimulationData(null, task, result);
        }

        return true;
    }

    private CorrelationResult correlate(@NotNull ShadowType shadow) {
        return null;
    }

}

/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import static com.evolveum.midpoint.model.api.ModelPublicConstants.*;

import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractCompositeActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.ActivityExecutionResult;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

/**
 * Nothing special here. This activity execution is just a shell for executing the children.
 */
class ReconciliationActivityExecution
        extends AbstractCompositeActivityExecution<
            ReconciliationWorkDefinition,
            ReconciliationActivityHandler,
            AbstractActivityWorkStateType> {

    ReconciliationActivityExecution(
            @NotNull ExecutionInstantiationContext<ReconciliationWorkDefinition, ReconciliationActivityHandler> context) {
        super(context);
    }

    @Override
    protected @NotNull ActivityExecutionResult executeLocal(OperationResult result) throws ActivityExecutionException, CommonException {
        ActivityExecutionResult executionResult = super.executeLocal(result);
        sendReconciliationResult(executionResult);
        return executionResult;
    }

    /**
     * Note that handling of the reconciliation result works only if the reconciliation activity is executed locally.
     */
    private void sendReconciliationResult(@NotNull ActivityExecutionResult executionResult) {
        ReconciliationResultListener listener = getActivityHandler().getReconciliationResultListener();
        if (listener != null) {
            listener.process(
                    ReconciliationResult.fromActivityExecution(this, executionResult));
        }
    }

    @Nullable OperationCompletionActivityExecution getOperationCompletionExecution() {
        try {
            return (OperationCompletionActivityExecution) activity.getChild(RECONCILIATION_OPERATION_COMPLETION_ID)
                    .getExecution();
        } catch (SchemaException e) {
            throw new IllegalStateException(e); // Occurs only during children map initialization
        }
    }

    @Nullable ResourceObjectsReconciliationActivityExecution getResourceReconciliationExecution() {
        try {
            return (ResourceObjectsReconciliationActivityExecution) activity.getChild(RECONCILIATION_RESOURCE_OBJECTS_ID)
                    .getExecution();
        } catch (SchemaException e) {
            throw new IllegalStateException(e); // Occurs only during children map initialization
        }
    }

    @Nullable RemainingShadowsActivityExecution getRemainingShadowsExecution() {
        try {
            return (RemainingShadowsActivityExecution) activity.getChild(RECONCILIATION_REMAINING_SHADOWS_ID)
                    .getExecution();
        } catch (SchemaException e) {
            throw new IllegalStateException(e); // Occurs only during children map initialization
        }
    }

    @NotNull List<PartialReconciliationActivityExecution<?>> getPartialActivityExecutions() {
        return activity.getChildrenCopy().stream()
                .map(Activity::getExecution)
                .filter(childExec -> childExec instanceof PartialReconciliationActivityExecution)
                .map(childExec -> (PartialReconciliationActivityExecution<?>) childExec)
                .collect(Collectors.toList());
    }
}

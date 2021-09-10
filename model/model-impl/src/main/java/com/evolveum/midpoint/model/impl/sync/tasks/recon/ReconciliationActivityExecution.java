/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import static com.evolveum.midpoint.model.api.ModelPublicConstants.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.util.exception.SystemException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.CompositeActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.ActivityExecutionResult;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

/**
 * The reason of existence of this class is to send {@link ReconciliationResult} object to {@link ReconciliationResultListener}
 * after the whole activity finishes.
 *
 * (Of course, this works only when the whole activity is executed "locally" within a single task.
 * But it is used in tests, where this condition generally holds.)
 */
class ReconciliationActivityExecution
        extends CompositeActivityExecution<
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
        return getChildExecution(RECONCILIATION_OPERATION_COMPLETION_ID);
    }

    @Nullable ResourceObjectsReconciliationActivityExecution getResourceReconciliationExecution() {
        return getChildExecution(RECONCILIATION_RESOURCE_OBJECTS_ID);
    }

    @Nullable RemainingShadowsActivityExecution getRemainingShadowsExecution() {
        return getChildExecution(RECONCILIATION_REMAINING_SHADOWS_ID);
    }

    @Nullable private <T> T getChildExecution(String id) {
        try {
            //noinspection unchecked
            return (T) activity.getChild(id).getExecution();
        } catch (SchemaException e) {
            throw new SystemException(e); // Occurs only during children map initialization
        }
    }

    @NotNull List<PartialReconciliationActivityExecution> getPartialActivityExecutionsList() {
        return activity.getChildrenCopy().stream()
                .map(Activity::getExecution)
                .map(e -> (PartialReconciliationActivityExecution) e)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}

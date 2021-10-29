/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.processing;

import com.evolveum.midpoint.repo.common.activity.run.IterativeActivityRun;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemReportingConditionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Evaluates conditions related to item processing (for reporting, for core processing). */
class ItemProcessingConditionEvaluator {

    /** Run of the related activity. */
    @NotNull private final IterativeActivityRun<?, ?, ?, ?> activityRun;

    /** Request to be processed. */
    @NotNull private final ItemProcessingRequest<?> request;

    ItemProcessingConditionEvaluator(ItemProcessingGatekeeper<?> itemProcessingGatekeeper) {
        activityRun = itemProcessingGatekeeper.getActivityRun();
        request = itemProcessingGatekeeper.getRequest();
    }

    boolean legacyIntervalRejects(Integer interval) {
        return interval != null &&
                (intervalRejects(interval) || activityRun.isNonScavengingWorker());
    }

    private boolean intervalRejects(int interval) {
        return interval == 0 || activityRun.getItemsProcessed() % interval != 0;
    }

    /** Beware, no conditions means "true". */
    boolean anyItemReportingConditionApplies(List<? extends ItemReportingConditionType> conditions,
            OperationResult result) {
        return anyItemReportingConditionApplies(conditions, null, result);
    }

    /** Beware, no conditions means "true". */
    boolean anyItemReportingConditionApplies(List<? extends ItemReportingConditionType> conditions,
            AdditionalVariableProvider additionalVariableProvider, OperationResult result) {
        return conditions.isEmpty() ||
                conditions.stream()
                        .anyMatch(condition -> itemReportingConditionApplies(condition, additionalVariableProvider, result));
    }

    private boolean itemReportingConditionApplies(@NotNull ItemReportingConditionType condition,
            @Nullable AdditionalVariableProvider additionalVariableProvider, OperationResult result) {

        if (condition.getInterval() != null && intervalRejects(condition.getInterval())) {
            return false;
        }

        if (Boolean.TRUE.equals(condition.isFirstWorker()) && activityRun.isNonScavengingWorker()) {
            return false;
        }

        return evaluateConditionDefaultTrue(condition.getExpression(), additionalVariableProvider, result);
    }

    boolean evaluateConditionDefaultTrue(@Nullable ExpressionType expression,
            @Nullable AdditionalVariableProvider additionalVariableProvider, OperationResult result) {

        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_REQUEST, request, ItemProcessingRequest.class);
        variables.put(ExpressionConstants.VAR_ITEM, request.getItem(), request.getItem().getClass());
        if (additionalVariableProvider != null) {
            additionalVariableProvider.provide(variables);
        }

        try {
            return ExpressionUtil.evaluateConditionDefaultTrue(variables, expression, null,
                    activityRun.getBeans().expressionFactory, "item condition expression",
                    activityRun.getRunningTask(), result);
        } catch (CommonException e) {
            throw new SystemException("Couldn't evaluate 'before item' condition: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    interface AdditionalVariableProvider {
        void provide(@NotNull VariablesMap variables);
    }
}

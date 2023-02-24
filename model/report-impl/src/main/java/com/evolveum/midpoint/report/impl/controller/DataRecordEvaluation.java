/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.report.impl.ReportBeans;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.report.impl.controller.GenericSupport.evaluateCondition;
import static com.evolveum.midpoint.util.MiscUtil.configNonNull;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SubreportUseType.*;

/**
 * Evaluates a single data record - gradually evaluating subreports in embedded or joined mode.
 */
class DataRecordEvaluation<R> {

    private static final Trace LOGGER = TraceManager.getTrace(DataRecordEvaluation.class);

    private final int sequentialNumber;
    @NotNull private final R record;
    @NotNull private final VariablesMap variables;
    @NotNull private final RunningTask workerTask;
    @NotNull private final ReportType report;
    @NotNull private final ObjectCollectionReportEngineConfigurationType configuration;
    @NotNull private final CollectionExportController.RowEmitter<R> rowEmitter;
    @NotNull private final List<SubreportParameterType> subReportDefinitionsSorted;

    DataRecordEvaluation(
            int sequentialNumber,
            @NotNull R record,
            @NotNull VariablesMap variables,
            @NotNull RunningTask workerTask,
            @NotNull ReportType report,
            @NotNull ObjectCollectionReportEngineConfigurationType configuration,
            @NotNull CollectionExportController.RowEmitter<R> rowEmitter) {

        this.sequentialNumber = sequentialNumber;
        this.record = record;
        this.variables = variables;
        this.workerTask = workerTask;
        this.report = report;
        this.configuration = configuration;
        this.rowEmitter = rowEmitter;
        this.subReportDefinitionsSorted =
                configuration.getSubreport().stream()
                        .sorted(Comparator.comparingInt(s -> ObjectUtils.defaultIfNull(s.getOrder(), Integer.MAX_VALUE)))
                        .collect(Collectors.toList());
    }

    public void evaluate(OperationResult result) throws ConfigurationException {
        if (conditionHolds(result)) {
            evaluateFromSubreport(0, result);
        } else {
            LOGGER.trace("Condition excludes processing of #{}:{}", sequentialNumber, record);
        }
    }

    private void evaluateFromSubreport(int index, OperationResult result) throws ConfigurationException {
        if (index == subReportDefinitionsSorted.size()) {
            rowEmitter.emit(sequentialNumber, record, variables, workerTask, result);
            return;
        }
        SubreportParameterType subreportDef = subReportDefinitionsSorted.get(index);
        String subReportName = configNonNull(subreportDef.getName(), () -> "Unnamed subreport definition: " + subreportDef);
        @Nullable TypedValue<?> subReportResultTyped =
                getSingleTypedValue(
                        ReportBeans.get().reportService.evaluateSubreport(
                                report.asPrismObject(), variables, subreportDef, workerTask, result));
        SubreportUseType use = Objects.requireNonNullElse(subreportDef.getUse(), EMBEDDED);
        if (use == EMBEDDED) {
            variables.put(subReportName, subReportResultTyped);
            evaluateFromSubreport(index + 1, result);
            variables.remove(subReportName);
        } else {
            stateCheck(
                    use == INNER_JOIN || use == LEFT_JOIN,
                    "Unsupported use value for %s: %s", subReportName, use);
            List<?> subReportValues = getAsList(subReportResultTyped);
            for (Object subReportValue : subReportValues) {
                variables.put(
                        subReportName,
                        TypedValue.of(getRealValue(subReportValue), Object.class));
                evaluateFromSubreport(index + 1, result);
                variables.remove(subReportName);
            }
            if (subReportValues.isEmpty() && use == LEFT_JOIN) {
                // Null is the best alternative to represent "no element" generated from the joined subreport.
                variables.put(subReportName, null, Object.class);
                evaluateFromSubreport(index + 1, result);
                variables.remove(subReportName);
            }
        }
    }

    // Quite a hackery, for now. Should be reconsidered some day.
    private Object getRealValue(Object value) {
        if (value instanceof Item) {
            return ((Item<?, ?>) value).getRealValue();
        } else if (value instanceof PrismValue) {
            return ((PrismValue) value).getRealValue();
        } else {
            return value;
        }
    }

    private @Nullable TypedValue<?> getSingleTypedValue(@NotNull VariablesMap map) {
        if (map.isEmpty()) {
            return null;
        }
        if (map.size() > 1) {
            throw new IllegalStateException("Expected zero or single entry, got more: " + map);
        }
        return map.values().iterator().next();
    }

    private @NotNull List<?> getAsList(@Nullable TypedValue<?> typedValue) {
        Object value = typedValue != null ? typedValue.getValue() : null;
        if (value instanceof Collection) {
            return List.copyOf((Collection<?>) value);
        } else if (value != null) {
            return List.of(value);
        } else {
            return List.of();
        }
    }

    private boolean conditionHolds(OperationResult result) {
        ExpressionType condition = configuration.getCondition();
        if (condition == null) {
            return true;
        }
        try {
            return evaluateCondition(condition, variables, ReportBeans.get().expressionFactory, workerTask, result);
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't evaluate condition for report record {} in {}", e, record, report);
            return false;
        }
    }
}

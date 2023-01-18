/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes metrics for (individual) processed object.
 *
 * Separated from {@link SimulationResultManagerImpl} for understandability.
 *
 * @see AggregatedMetricsComputation
 */
class ObjectMetricsComputation {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectMetricsComputation.class);

    @NotNull private final ModelCommonBeans beans = ModelCommonBeans.get();

    @NotNull private final ProcessedObject<?> processedObject;
    @NotNull private final List<GlobalSimulationMetricDefinitionType> metricDefinitions;
    @NotNull private final Task task;

    private ObjectMetricsComputation(
            @NotNull ProcessedObject<?> processedObject,
            @NotNull List<GlobalSimulationMetricDefinitionType> metricDefinitions,
            @NotNull Task task) {
        this.processedObject = processedObject;
        this.metricDefinitions = metricDefinitions;
        this.task = task;
    }

    static List<ProcessedObjectSimulationMetricValueType> computeAll(
            ProcessedObject<?> processedObject,
            List<GlobalSimulationMetricDefinitionType> metricDefinitions,
            Task task,
            OperationResult result) throws CommonException {
        return new ObjectMetricsComputation(processedObject, metricDefinitions, task)
                .computeAll(result);
    }

    private List<ProcessedObjectSimulationMetricValueType> computeAll(OperationResult result) throws CommonException {
        List<ProcessedObjectSimulationMetricValueType> values = new ArrayList<>();
        for (GlobalSimulationMetricDefinitionType metricDefinition : metricDefinitions) {
            OriginalSimulationMetricComputationType computation = metricDefinition.getObjectValue();
            if (computation == null) {
                continue;
            }
            SimulationResultProcessedObjectPredicateType domain = metricDefinition.getDomain();
            if (domain != null && !processedObject.matches(domain, task, result)) {
                continue;
            }
            String identifier = metricDefinition.getIdentifier();
            ExpressionType expression = computation.getExpression();
            BigDecimal value = computeObjectMetricValue(identifier, expression, result);
            LOGGER.trace("Value for metric '{}': {}", identifier, value);
            if (value != null) {
                values.add(
                        new ProcessedObjectSimulationMetricValueType()
                                .identifier(identifier)
                                .value(value));
            }
        }
        return values;
    }

    private BigDecimal computeObjectMetricValue(
            String identifier, ExpressionType expression, OperationResult result) {
        if (expression == null) {
            LOGGER.warn("Metric definition without an expression - ignoring: {}", identifier);
            return null;
        }
        ItemDefinition<?> outputDefinition = PrismContext.get().definitionFactory()
                .createPropertyDefinition(ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_DECIMAL);
        VariablesMap variables = new VariablesMap();
        variables.put(
                ExpressionConstants.VAR_PROCESSED_OBJECT, processedObject, ProcessedObjectImpl.class);
        try {
            PrismPropertyValue<BigDecimal> value = ExpressionUtil.evaluateExpression(
                    variables,
                    outputDefinition,
                    expression,
                    MiscSchemaUtil.getExpressionProfile(),
                    beans.expressionFactory,
                    "metric expression evaluation",
                    task,
                    result);
            return value != null ? value.getRealValue() : null;
        } catch (CommonException e) {
            throw new SystemException(
                    String.format(
                            "Couldn't evaluate expression for metric '%s': %s", identifier, e.getMessage()),
                    e);
        }
    }
}

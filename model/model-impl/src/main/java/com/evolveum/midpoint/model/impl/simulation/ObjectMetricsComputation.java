/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
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

/**
 * Computes metrics for (individual) processed object.
 *
 * Separated from {@link SimulationResultManagerImpl} for understandability.
 *
 * @see AggregatedMetricsComputation
 */
class ObjectMetricsComputation<O extends ObjectType> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectMetricsComputation.class);

    @NotNull private final ModelCommonBeans beans = ModelCommonBeans.get();

    @NotNull private final ProcessedObjectImpl<O> processedObject;
    @NotNull private final LensElementContext<O> elementContext;
    @NotNull private final SimulationResultImpl simulationResult;
    @NotNull private final Collection<SimulationMetricDefinitionType> explicitMetricDefinitions;
    @NotNull private final Task task;

    private ObjectMetricsComputation(
            @NotNull ProcessedObjectImpl<O> processedObject,
            @NotNull LensElementContext<O> elementContext,
            @NotNull SimulationResultImpl simulationResult,
            @NotNull Collection<SimulationMetricDefinitionType> explicitMetricDefinitions,
            @NotNull Task task) {
        this.processedObject = processedObject;
        this.elementContext = elementContext;
        this.simulationResult = simulationResult;
        this.explicitMetricDefinitions = explicitMetricDefinitions;
        this.task = task;
    }

    static <O extends ObjectType> List<SimulationProcessedObjectMetricValueType> computeAll(
            ProcessedObjectImpl<O> processedObject,
            LensElementContext<O> elementContext,
            SimulationResultImpl simulationResult,
            Collection<SimulationMetricDefinitionType> explicitMetricDefinitions,
            Task task,
            OperationResult result) throws CommonException {
        return new ObjectMetricsComputation<>(processedObject, elementContext, simulationResult, explicitMetricDefinitions, task)
                .computeAll(result);
    }

    private List<SimulationProcessedObjectMetricValueType> computeAll(OperationResult result) throws CommonException {
        List<SimulationProcessedObjectMetricValueType> values = new ArrayList<>();
        for (SimulationMetricDefinitionType metricDefinition : explicitMetricDefinitions) {
            if (!simulationResult.isExplicitMetricEnabled(metricDefinition)) {
                continue;
            }
            SimulationMetricComputationType computation = metricDefinition.getComputation();
            if (computation == null) {
                continue;
            }
            SimulationObjectPredicateType domain = computation.getDomain();
            if (domain != null && !processedObject.matches(domain, task, result)) {
                continue;
            }
            String identifier = metricDefinition.getIdentifier();

            ExpressionType valueExpression = computation.getValueExpression();
            BigDecimal computedValue = computeObjectMetricValue(identifier, valueExpression, result);

            SimulationObjectPredicateType selectionPredicate = computation.getSelection();
            Boolean selected = selectionPredicate != null ? processedObject.matches(selectionPredicate, task, result) : null;

            LOGGER.trace("Computation for metric '{}' yielded value = {}, selected = {}", identifier, computedValue, selected);
            if (computedValue != null || selected != null) {
                values.add(
                        new SimulationProcessedObjectMetricValueType()
                                .identifier(identifier)
                                .selected(selected != null ? selected : defaultSelected(computedValue))
                                .value(computedValue != null ? computedValue : defaultValue(selected)));
            }
        }
        return values;
    }

    private BigDecimal defaultValue(boolean selected) {
        return selected ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    private boolean defaultSelected(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal computeObjectMetricValue(
            String identifier, ExpressionType expression, OperationResult result) {
        if (expression == null) {
            LOGGER.warn("Metric definition without an expression - ignoring: {}", identifier);
            return null;
        }
        ItemDefinition<?> outputDefinition = PrismContext.get().definitionFactory()
                .newPropertyDefinition(ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_DECIMAL);
        VariablesMap variables = new VariablesMap();
        variables.put(
                ExpressionConstants.VAR_PROCESSED_OBJECT, processedObject, ProcessedObjectImpl.class);
        variables.put(
                ExpressionConstants.VAR_MODEL_ELEMENT_CONTEXT, elementContext, LensElementContext.class);
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

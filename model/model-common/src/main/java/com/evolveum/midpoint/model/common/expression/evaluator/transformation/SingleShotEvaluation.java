/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.transformation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.common.expression.TransformationValueMetadataComputer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.DeltaSetTripleUtil;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TransformExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueTransformationEvaluationModeType;

/**
 * Application of expression evaluator in "single shot" ("absolute") mode:
 * (1) If there's no delta, the expression is evaluated once. All the results go into the zero set.
 * (2) If there's a delta, expression is evaluated twice (for old and for new values). The result is computed as the difference of the results.
 */
class SingleShotEvaluation<V extends PrismValue, D extends ItemDefinition<?>, E extends TransformExpressionEvaluatorType>
        extends TransformationalEvaluation<V, D, E> {

    private static final Trace LOGGER = TraceManager.getTrace(SingleShotEvaluation.class);

    private static final String OP_EVALUATE_EXPRESSION = SingleShotEvaluation.class.getName() + ".evaluateExpression";

    SingleShotEvaluation(ExpressionEvaluationContext context, OperationResult parentResult,
            AbstractValueTransformationExpressionEvaluator<V, D, E> evaluator) {
        super(context, parentResult, evaluator);
    }

    PrismValueDeltaSetTriple<V> evaluate() throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        recordEvaluationStart(ValueTransformationEvaluationModeType.SINGLE_SHOT);

        PrismValueDeltaSetTriple<V> outputTriple;
        if (context.hasDeltas()) {
            outputTriple = evaluateAbsoluteExpressionWithDeltas();
        } else {
            outputTriple = evaluateAbsoluteExpressionWithoutDeltas();
        }

        addValueMetadata(outputTriple.getPlusSet());
        addValueMetadata(outputTriple.getZeroSet());
        //addValueMetadata(outputTriple.getMinusSet(), parentResult);
        recordEvaluationEnd(outputTriple);
        return outputTriple;
    }

    private PrismValueDeltaSetTriple<V> evaluateAbsoluteExpressionWithDeltas() throws ExpressionEvaluationException,
            ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

        Collection<V> outputForOldState = context.isSkipEvaluationMinus() ? null : evaluateExpressionInState(false);
        Collection<V> outputForNewState = context.isSkipEvaluationPlus() ? null : evaluateExpressionInState(true);

        return DeltaSetTripleUtil.diffPrismValueDeltaSetTriple(outputForOldState, outputForNewState);
    }

    @NotNull
    private PrismValueDeltaSetTriple<V> evaluateAbsoluteExpressionWithoutDeltas() throws ExpressionEvaluationException,
            ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        // No need to execute twice. There is no change.
        Collection<V> outputForNewState = evaluateExpressionInState(true);

        return DeltaSetTripleUtil.allToZeroSet(outputForNewState);
    }

    private Collection<V> evaluateExpressionInState(boolean useNewValues) throws ExpressionEvaluationException, ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.subresult(OP_EVALUATE_EXPRESSION)
                .setMinor()
                .addContext("newValues", useNewValues)
                .addContext("context", context.getContextDescription())
                .addContext("evaluator", getClass().getName())
                .build();
        try {
            VariablesMap staticVariables = new VariablesMap();
            addVariablesToStaticVariables(staticVariables, useNewValues);
            addSourcesToStaticVariables(staticVariables, useNewValues);
            assert !staticVariables.haveDeltas();

            var vtCtx = new ValueTransformationContext(
                    context, staticVariables, useNewValues,
                    (useNewValues ? "(new) " : "(old) ") + context.getContextDescription());

            List<V> evalResults = evaluator.transformSingleValue(vtCtx, result);

            return removeEmptyOutputValues(evalResults);
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void addVariablesToStaticVariables(VariablesMap staticVariables, boolean useNewValues) {
        if (useNewValues) {
            staticVariables.addVariableDefinitionsNew(context.getVariables());
        } else {
            staticVariables.addVariableDefinitionsOld(context.getVariables());
        }
    }

    private void addSourcesToStaticVariables(VariablesMap scriptVariables, boolean useNewValues) {
        for (Source<?,?> source: context.getSources()) {
            LOGGER.trace("source: {}", source);

            Object value;
            if (useNewValues) {
                value = getRealContent(source.getItemNew(), source.getResidualPath());
            } else {
                value = getRealContent(source.getItemOld(), source.getResidualPath());
            }
            String name = source.getName().getLocalPart();
            scriptVariables.addVariableDefinition(name, value, source.getDefinition());
        }
    }

    private Object getRealContent(Item<?,?> item, ItemPath residualPath) {
        if (residualPath == null || residualPath.isEmpty()) {
            return item;
        }
        if (item == null) {
            return null;
        }
        return item.find(residualPath);
    }

    private Collection<V> removeEmptyOutputValues(List<V> evalResults) {
        if (evalResults == null || evalResults.isEmpty()) {
            return null;
        }
        Collection<V> outputSet = new ArrayList<>(evalResults.size());
        for (V pval: evalResults) {
            if (pval instanceof PrismPropertyValue<?>) {
                if (((PrismPropertyValue<?>) pval).getValue() == null) {
                    continue;
                }
                Object realValue = ((PrismPropertyValue<?>)pval).getValue();
                if (realValue instanceof String) {
                    if (((String)realValue).isEmpty()) {
                        continue;
                    }
                }
                if (realValue instanceof PolyString) {
                    if (((PolyString)realValue).isEmpty()) {
                        continue;
                    }
                }
            }
            outputSet.add(pval);
        }
        return outputSet;
    }

    private void addValueMetadata(@NotNull Collection<V> output) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        TransformationValueMetadataComputer valueMetadataComputer = context.getValueMetadataComputer();

        if (valueMetadataComputer == null) {
            // TODO clear existing metadata?
            LOGGER.trace("No value metadata computer present, skipping metadata computation.");
        } else {
            // FIXME: Here we should have some source values list probably
            var sourceValues = Collections.<PrismValue>emptyList();
            ValueMetadataType outputValueMetadata = valueMetadataComputer.compute(sourceValues, parentResult);
            ValueMetadata metadata = PrismContext.get().getValueMetadataFactory().createEmpty();
            metadata.addMetadataValue(outputValueMetadata.asPrismContainerValue());
            for (var oVal: output)  {
                if (oVal != null) {
                    oVal.setValueMetadata(metadata.clone());
                }
            }
        }
    }
}

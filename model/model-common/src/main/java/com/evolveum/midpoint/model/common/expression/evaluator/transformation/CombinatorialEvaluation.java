/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.transformation;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TransformExpressionEvaluatorType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueTransformationEvaluationModeType;

import com.evolveum.prism.xml.ns._public.types_3.DeltaSetTripleType;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Application of expression evaluator in "combinatorial" ("relative") mode.
 *
 * Each combinations of values from sources is evaluated separately and the resulting values
 * are sorted out into plus-minus-zero sets.
 */
class CombinatorialEvaluation<V extends PrismValue, D extends ItemDefinition, E extends TransformExpressionEvaluatorType> extends TransformationalEvaluation<V, D, E>  {

    private static final Trace LOGGER = TraceManager.getTrace(CombinatorialEvaluation.class);

    /**
     * Configuration of the evaluator.
     */
    @NotNull private final E evaluatorBean;

    /**
     * Plus-Minus-Zero sets of input values for individual sources (with some "fake nulls" magic).
     * Ordered according to the order of sources.
     */
    @NotNull final List<SourceTriple<?,?>> sourceTripleList;

    /**
     * Merged all values from individual sources (adding null when there are no values for a given source).
     * Ordered according to the order of sources.
     */
    @NotNull private final List<SourceValues> sourceValuesList;

    /**
     * Pre-compiled condition expression, to be evaluated for individual value combinations.
     */
    final Expression<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> conditionExpression;

    /**
     * Resulting output triple (plus-minus-zero).
     */
    @NotNull final PrismValueDeltaSetTriple<V> outputTriple;

    CombinatorialEvaluation(ExpressionEvaluationContext context, OperationResult parentResult,
            AbstractValueTransformationExpressionEvaluator<V, D, E> evaluator) throws SecurityViolationException, ObjectNotFoundException, SchemaException {
        super(context, parentResult, evaluator);
        this.evaluatorBean = evaluator.getExpressionEvaluatorBean();
        this.sourceTripleList = createSourceTriplesList();
        this.sourceValuesList = SourceValues.fromSourceTripleList(sourceTripleList);
        this.conditionExpression = createConditionExpression();
        this.outputTriple = prismContext.deltaFactory().createPrismValueDeltaSetTriple();
    }

    PrismValueDeltaSetTriple<V> evaluate() throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        recordEvaluationStart();
        try {
            MiscUtil.carthesian(sourceValuesList, valuesTuple -> {
                // This lambda will be called for all combinations of all values in the sources
                // The pvalues parameter is a list of values; each values comes from a difference source.
                try (ValueTupleTransformation<V> valueTupleTransformation = new ValueTupleTransformation<>(valuesTuple, this, parentResult)) {
                    valueTupleTransformation.evaluate();
                }
            });
        } catch (TunnelException e) {
            unwrapTunnelException(e);
        }

        cleanUpOutputTriple();
        recordEvaluationEnd(outputTriple);
        return outputTriple;
    }

    private void recordEvaluationStart() throws SchemaException {
        if (trace != null) {
            super.recordEvaluationStart(ValueTransformationEvaluationModeType.COMBINATORIAL);
            int i = 0;
            for (SourceTriple<?, ?> sourceTriple : sourceTripleList) {
                trace.getSource().get(i)
                        .setDeltaSetTriple(
                                DeltaSetTripleType.fromDeltaSetTriple(sourceTriple, prismContext));
                i++;
            }
        }
    }

    private Expression<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> createConditionExpression()
            throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        if (evaluatorBean.getCondition() != null) {
            return ExpressionUtil.createCondition(evaluatorBean.getCondition(), context.getExpressionProfile(),
                    context.getExpressionFactory(), "condition in " + context.getContextDescription(), context.getTask(),
                    parentResult);
        } else {
            return null;
        }
    }

    private List<SourceTriple<?,?>> createSourceTriplesList() {
        Collection<Source<?, ?>> sources = context.getSources();
        List<SourceTriple<?,?>> sourceTriples = new ArrayList<>(sources.size());
        for (Source<?,?> source: sources) {
            //noinspection unchecked
            sourceTriples.add(
                    createSourceTriple((Source) source));
        }
        return sourceTriples;
    }

    private SourceTriple<V, D> createSourceTriple(Source<V, D> source) {
        SourceTriple<V, D> sourceTriple = new SourceTriple<>(source, prismContext);
        ItemDelta<V, D> delta = source.getDelta();
        if (delta != null) {
            sourceTriple.merge(delta.toDeltaSetTriple(source.getItemOld()));
        } else {
            if (source.getItemOld() != null) {
                sourceTriple.addAllToZeroSet(source.getItemOld().getValues());
            }
        }
        if (evaluator.isIncludeNullInputs()) {
            addFakeNullValues(sourceTriple, source);
        }
        LOGGER.trace("Processed source {} triple\n{}", source.getName().getLocalPart(), sourceTriple.debugDumpLazily(1));
        return sourceTriple;
    }

    private void addFakeNullValues(SourceTriple<V, D> sourceTriple, Source<V, D> source) {
        // Make sure that we properly handle the "null" states, i.e. the states when we enter
        // "empty" value and exit "empty" value for a property
        // We need this to properly handle "negative" expressions, i.e. expressions that return non-null
        // value for null input. We need to make sure such expressions receive the null input when needed
        boolean oldEmpty = Item.hasNoValues(source.getItemOld());
        boolean newEmpty = Item.hasNoValues(source.getItemNew());

        if (oldEmpty) {
            if (newEmpty) {
                if (sourceTriple.hasMinusSet()) {
                    // special case: change empty -> empty, but there is still a delete delta
                    // so it seems something was deleted. This is strange case, but we prefer the delta over
                    // the absolute states (which may be out of date).
                    // Similar case than that of non-empty -> empty (see below)
                    sourceTriple.addToPlusSet(null);
                }
            } else {
                // change empty -> non-empty: we are removing "null" value
                sourceTriple.addToMinusSet(null);
            }
        } else {
            if (newEmpty) {
                // change non-empty -> empty: we are adding "null" value
                sourceTriple.addToPlusSet(null);
            } else {
                // non-empty -> non-empty: nothing to do here
            }
        }
    }

    private void cleanUpOutputTriple() {
        Collection<V> minusSet = outputTriple.getMinusSet();
        Collection<V> plusSet = outputTriple.getPlusSet();
        Iterator<V> plusIter = plusSet.iterator();
        while (plusIter.hasNext()) {
            V plusVal = plusIter.next();
            if (minusSet.contains(plusVal)) {
                plusIter.remove();
                minusSet.remove(plusVal);
                outputTriple.addToZeroSet(plusVal);
            }
        }
    }

    private void unwrapTunnelException(TunnelException e) throws ExpressionEvaluationException, ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        Throwable originalException = e.getCause();
        if (originalException instanceof ExpressionEvaluationException) {
            throw (ExpressionEvaluationException)originalException;
        } else if (originalException instanceof ObjectNotFoundException) {
            throw (ObjectNotFoundException)originalException;
        } else if (originalException instanceof SchemaException) {
            throw (SchemaException)originalException;
        } else if (originalException instanceof CommunicationException) {
            throw (CommunicationException)originalException;
        } else if (originalException instanceof ConfigurationException) {
            throw (ConfigurationException)originalException;
        } else if (originalException instanceof SecurityViolationException) {
            throw (SecurityViolationException)originalException;
        } else if (originalException instanceof RuntimeException) {
            throw (RuntimeException)originalException;
        } else {
            throw new IllegalStateException("Unexpected exception: "+e+": "+e.getMessage(),e);
        }
    }
}

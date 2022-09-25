/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.transformation;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
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
class CombinatorialEvaluation<V extends PrismValue, D extends ItemDefinition<?>, E extends TransformExpressionEvaluatorType>
        extends TransformationalEvaluation<V, D, E>  {

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
     * Which triples do have which sets? (Looking for plus and zero sets.)
     * Special value of null in the set means that the respective source is empty.
     */
    @NotNull private final List<Set<PlusMinusZero>> setsOccupiedPlusZero = new ArrayList<>();

    /**
     * Which triples do have which sets? (Looking for minus and zero sets.)
     * Special value of null in the set means that the respective source is empty.
     */
    @NotNull private final List<Set<PlusMinusZero>> setsOccupiedMinusZero = new ArrayList<>();

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
        this.conditionExpression = createConditionExpression();
        this.outputTriple = prismContext.deltaFactory().createPrismValueDeltaSetTriple();
        computeSetsOccupied();
    }

    PrismValueDeltaSetTriple<V> evaluate() throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        recordEvaluationStart();
        try {
            transformValues();
        } catch (TunnelException e) {
            unwrapTunnelException(e);
        }

        cleanUpOutputTriple();
        recordEvaluationEnd(outputTriple);
        return outputTriple;
    }

    /**
     * Going through combinations of all non-negative and non-positive values and transform each tuple found.
     * It is done in two steps:
     *
     * 1) Specific plus-minus-zero sets are selected for each source, combining non-negative to PLUS (except for all zeros
     * that go into ZERO), and then combining non-positive to MINUS (except for all zeros that are skipped because they are
     * already computed).
     *
     * 2) All values from selected sets are combined together.
     */
    private void transformValues() {
        // The skipEvaluationPlus is evaluated within.
        transformToPlusAndZeroSets();

        // But for minus we can prune this branch even here.
        if (!context.isSkipEvaluationMinus()) {
            transformToMinusSets();
        }
    }

    private void transformToPlusAndZeroSets() {
        MiscUtil.carthesian(setsOccupiedPlusZero, sets -> {
            if (isAllZeros(sets)) {
                transform(sets, PlusMinusZero.ZERO);
            } else {
                if (!context.isSkipEvaluationPlus()) {
                    transform(sets, PlusMinusZero.PLUS);
                }
            }
        });
    }

    private void transformToMinusSets() {
        MiscUtil.carthesian(setsOccupiedMinusZero, sets -> {
            if (isAllZeros(sets)) {
                // already done
            } else {
                transform(sets, PlusMinusZero.MINUS);
            }
        });
    }

    private boolean isAllZeros(List<PlusMinusZero> sets) {
        return sets.stream().allMatch(set -> set == null || set == PlusMinusZero.ZERO);
    }

    private void transform(List<PlusMinusZero> sets, PlusMinusZero outputSet) {
        List<Collection<PrismValue>> domains = createDomainsForSets(sets);
        logDomainsForSets(domains, sets, outputSet);
        MiscUtil.carthesian(domains, valuesTuple -> {
            try (ValueTupleTransformation<V> valueTupleTransformation =
                    new ValueTupleTransformation<>(sets, valuesTuple, outputSet, this, parentResult)) {
                valueTupleTransformation.evaluate();
            }
        });
    }

    private void logDomainsForSets(List<Collection<PrismValue>> domains, List<PlusMinusZero> sets, PlusMinusZero outputSet) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Domains for sets, targeting {}:", outputSet);
            Iterator<PlusMinusZero> setsIterator = sets.iterator();
            for (Collection<PrismValue> domain : domains) {
                LOGGER.trace(" - set: {}, domain: {}", setsIterator.next(), domain);
            }
        }
    }

    private List<Collection<PrismValue>> createDomainsForSets(List<PlusMinusZero> sets) {
        List<Collection<PrismValue>> domains = new ArrayList<>();
        Iterator<PlusMinusZero> setsIterator = sets.iterator();
        for (SourceTriple<?, ?> sourceTriple : sourceTripleList) {
            PlusMinusZero set = setsIterator.next();
            Collection<PrismValue> domain;
            if (set != null) {
                //noinspection unchecked
                domain = (Collection<PrismValue>) sourceTriple.getSet(set);
            } else {
                // This is a special value ensuring that the tuple computation will run at least once
                // even for empty sources.
                domain = Collections.singleton(null);
            }
            domains.add(domain);
        }
        return domains;
    }

    private void computeSetsOccupied() {
        for (SourceTriple<?, ?> sourceTriple : sourceTripleList) {
            Set<PlusMinusZero> setOccupiedPlusZero = new HashSet<>();
            Set<PlusMinusZero> setOccupiedMinusZero = new HashSet<>();
            if (sourceTriple.isEmpty()) {
                setOccupiedPlusZero.add(null);
                setOccupiedMinusZero.add(null);
            } else {
                if (sourceTriple.hasPlusSet()) {
                    setOccupiedPlusZero.add(PlusMinusZero.PLUS);
                }
                if (sourceTriple.hasMinusSet()) {
                    setOccupiedMinusZero.add(PlusMinusZero.MINUS);
                }
                if (sourceTriple.hasZeroSet()) {
                    setOccupiedPlusZero.add(PlusMinusZero.ZERO);
                    setOccupiedMinusZero.add(PlusMinusZero.ZERO);
                }
            }
            setsOccupiedPlusZero.add(setOccupiedPlusZero);
            setsOccupiedMinusZero.add(setOccupiedMinusZero);
            LOGGER.trace("Adding setsOccupiedPlusZero: {}, setOccupiedMinusZero: {} for source {}",
                    setOccupiedPlusZero, setOccupiedMinusZero, sourceTriple.getName());
        }
    }

    private void recordEvaluationStart() throws SchemaException {
        if (trace != null) {
            super.recordEvaluationStart(ValueTransformationEvaluationModeType.COMBINATORIAL);
            int i = 0;
            for (SourceTriple<?, ?> sourceTriple : sourceTripleList) {
                trace.getSource().get(i)
                        .setDeltaSetTriple(
                                DeltaSetTripleType.fromDeltaSetTriple(sourceTriple));
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

    private List<SourceTriple<?,?>> createSourceTriplesList() throws SchemaException {
        Collection<Source<?, ?>> sources = context.getSources();
        List<SourceTriple<?,?>> sourceTriples = new ArrayList<>(sources.size());
        for (Source<?,?> source: sources) {
            //noinspection unchecked,rawtypes
            sourceTriples.add(
                    createSourceTriple((Source) source));
        }
        return sourceTriples;
    }

    private SourceTriple<V, D> createSourceTriple(Source<V, D> source) throws SchemaException {
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
            addFakeNullValues(sourceTriple);
        }
        LOGGER.trace("Processed source {} triple\n{}", source.getName().getLocalPart(), sourceTriple.debugDumpLazily(1));
        return sourceTriple;
    }

    /**
     * We have to ensure that no source would block computation of neither "non-positive"
     * nor "non-negative" values by not providing any inputs in respective sets.
     *
     * TODO Verify the correctness of this algorithm.
     */
    private void addFakeNullValues(SourceTriple<V, D> sourceTriple) {
        if (sourceTriple.hasZeroSet()) {
            // This is good. Because we have zero set, it will be applied both for non-positive
            // and non-negative computations. Nothing has to be done.
            return;
        }

        boolean hasPlus = sourceTriple.hasPlusSet();
        boolean hasMinus = sourceTriple.hasMinusSet();

        if (!hasPlus && !hasMinus) {
            // We have nothing in plus nor minus sets. Both non-positive/non-negative cases can
            // be treated by including null into zero set.
            sourceTriple.addToZeroSet(null);
        } else if (!hasPlus) {
            // Only plus set is empty. So let's treat that one.
            sourceTriple.addToPlusSet(null);
        } else if (!hasMinus) {
            // Only minus set is empty. Do the same.
            sourceTriple.addToMinusSet(null);
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

/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.construction;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.DeltaMapTriple;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.util.HumanReadableDescribable;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.prism.delta.PlusMinusZero.*;

/**
 * Collects evaluated constructions from evaluatedAssignmentTriple into a single-level triple.
 * The collected evaluated constructions are neatly sorted by "key", which is usually {@link ConstructionTargetKey}
 * or {@link PersonaKey}.
 *
 * @param <K> Key type
 *
 * @author Radovan Semancik
 */
public class ConstructionCollector<
        AH extends AssignmentHolderType,
        K extends HumanReadableDescribable,
        ACT extends AbstractConstructionType,
        AC extends AbstractConstruction<AH,ACT,EC>,
        EC extends EvaluatedAbstractConstruction<AH>> {

    /**
     * Result of the computation: keyed delta triples of "packs". A pack is basically a set of evaluated constructions with
     * some additional information about the pack as such (hasValidAssignment, forceRecon).
     */
    private final DeltaMapTriple<K, EvaluatedConstructionPack<EC>> evaluatedConstructionMapTriple;

    /**
     * Method that extracts constructions of given type - resource objects or personas -
     * (in the form of triple i.e. added/deleted/kept) from evaluated assignment.
     */
    private final Function<EvaluatedAssignmentImpl<AH>, DeltaSetTriple<AC>> constructionTripleExtractor;

    /**
     * Method that generates indexing key for constructions, under which they are collected.
     */
    private final FailableLensFunction<EC, K> keyGenerator;

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionCollector.class);

    public ConstructionCollector(Function<EvaluatedAssignmentImpl<AH>, DeltaSetTriple<AC>> constructionTripleExtractor, FailableLensFunction<EC, K> keyGenerator) {
        this.constructionTripleExtractor = constructionTripleExtractor;
        this.keyGenerator = keyGenerator;
        this.evaluatedConstructionMapTriple = PrismContext.get().deltaFactory().createDeltaMapTriple();
    }

    public DeltaMapTriple<K, EvaluatedConstructionPack<EC>> getEvaluatedConstructionMapTriple() {
        return evaluatedConstructionMapTriple;
    }

    public void collect(DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        evaluatedConstructionMapTriple.clear();

        // We cannot decide on zero/plus/minus sets here. What we need is absolute classification of the evaluated assignments
        // with regard to focus old state.
        for (EvaluatedAssignmentImpl<AH> evaluatedAssignment : evaluatedAssignmentTriple.getAllValues()) {
            collectToConstructionMapFromEvaluatedAssignments(evaluatedAssignment, evaluatedAssignment.getAbsoluteMode());
        }
    }

    private void collectToConstructionMapFromEvaluatedAssignments(
            EvaluatedAssignmentImpl<AH> evaluatedAssignment,
            PlusMinusZero mode)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        LOGGER.trace("Level1: Collecting constructions from evaluated assignment (mode={}):\n{}",
                mode, evaluatedAssignment.debugDumpLazily(1));
        DeltaSetTriple<AC> constructionTriple = constructionTripleExtractor.apply(evaluatedAssignment);
        collectToConstructionMapFromConstructions(evaluatedAssignment, constructionTriple.getZeroSet(), mode, ZERO);
        collectToConstructionMapFromConstructions(evaluatedAssignment, constructionTriple.getPlusSet(), mode, PLUS);
        collectToConstructionMapFromConstructions(evaluatedAssignment, constructionTriple.getMinusSet(), mode, MINUS);
    }

    private void collectToConstructionMapFromConstructions(
            EvaluatedAssignmentImpl<AH> evaluatedAssignment,
            Collection<AC> constructions,
            PlusMinusZero mode1,
            PlusMinusZero mode2)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        for (AC construction : constructions) {
            LOGGER.trace("Level2: Collecting evaluated constructions from construction (mode1={}, mode2={}):\n{}",
                    mode1, mode2, construction.debugDumpLazily(1));
            DeltaSetTriple<EC> evaluatedConstructionTriple = construction.getEvaluatedConstructionTriple();
            if (evaluatedConstructionTriple != null) {
                collectToConstructionMapFromEvaluatedConstructions(evaluatedAssignment, evaluatedConstructionTriple.getZeroSet(), mode1, mode2, ZERO);
                collectToConstructionMapFromEvaluatedConstructions(evaluatedAssignment, evaluatedConstructionTriple.getPlusSet(), mode1, mode2, PLUS);
                collectToConstructionMapFromEvaluatedConstructions(evaluatedAssignment, evaluatedConstructionTriple.getMinusSet(), mode1, mode2, MINUS);
            }
        }
    }

    private void collectToConstructionMapFromEvaluatedConstructions(
            EvaluatedAssignmentImpl<AH> evaluatedAssignment,
            Collection<EC> evaluatedConstructions,
            PlusMinusZero mode1,
            PlusMinusZero mode2,
            PlusMinusZero mode3)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        for (EC evaluatedConstruction : evaluatedConstructions) {
            LOGGER.trace("Level3: Collecting evaluated construction (mode1={}, mode2={}, mode3={}):\n{}",
                    mode1, mode2, mode3, evaluatedConstruction.debugDumpLazily(1));

            AbstractConstruction<AH, ?, ?> construction = evaluatedConstruction.getConstruction();

            if (construction.isIgnored()) {
                LOGGER.trace("-> construction {} is ignored, skipping the evaluated construction", construction);
                continue;
            }

            PlusMinusZero mode =
                    PlusMinusZero.compute(
                            PlusMinusZero.compute(mode1, mode2),
                            mode3);
            if (mode == null) {
                LOGGER.trace("-> computed mode is null, skipping the evaluated construction");
                continue;
            }

            // Ugly and temporary hack - some constructions going to plus/minus sets based on validity change
            // FIXME MID-6404
            if (mode == ZERO) {
                if (!construction.getWasValid() && construction.isValid()) {
                    mode = PLUS;
                } else if (construction.getWasValid() && !construction.isValid()) {
                    mode = MINUS;
                }
            }

            LOGGER.trace("-> resulting mode (after adjustments): {}", mode);
            Map<K, EvaluatedConstructionPack<EC>> evaluatedConstructionMap = evaluatedConstructionMapTriple.getMap(mode);
            if (evaluatedConstructionMap == null) {
                throw new IllegalStateException("No construction map for mode: " + mode);
            }

            K key = keyGenerator.apply(evaluatedConstruction);
            LOGGER.trace("-> putting under key: {}", key);

            EvaluatedConstructionPack<EC> evaluatedConstructionPack;
            if (evaluatedConstructionMap.containsKey(key)) {
                evaluatedConstructionPack = evaluatedConstructionMap.get(key);
            } else {
                evaluatedConstructionPack = new EvaluatedConstructionPack<>();
                evaluatedConstructionMap.put(key, evaluatedConstructionPack);
            }

            evaluatedConstructionPack.add(evaluatedConstruction);
            if (evaluatedAssignment.isValid()) {
                evaluatedConstructionPack.setHasValidAssignment(true);
            }
            if (evaluatedAssignment.isForceRecon()) {
                evaluatedConstructionPack.setForceRecon(true);
            }
        }
    }
}

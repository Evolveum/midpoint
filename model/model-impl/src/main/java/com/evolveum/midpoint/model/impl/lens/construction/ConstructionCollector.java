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

/**
 * Collects evaluated constructions from evaluatedAssignmentTriple into a single-level triple.
 * The collected evaluated constructions are neatly sorted by "key", which is usually ResourceShadowDiscriminator or PersonaKey.
 *
 * @param <K> Key type
 *
 * @author Radovan Semancik
 */
public class ConstructionCollector<AH extends AssignmentHolderType, K extends HumanReadableDescribable, ACT extends AbstractConstructionType, AC extends AbstractConstruction<AH,ACT,EC>, EC extends EvaluatedAbstractConstruction<AH>> {

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

    public ConstructionCollector(Function<EvaluatedAssignmentImpl<AH>, DeltaSetTriple<AC>> constructionTripleExtractor, FailableLensFunction<EC, K> keyGenerator, PrismContext prismContext) {
        this.constructionTripleExtractor = constructionTripleExtractor;
        this.keyGenerator = keyGenerator;
        this.evaluatedConstructionMapTriple = prismContext.deltaFactory().createDeltaMapTriple();
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

    private void collectToConstructionMapFromEvaluatedAssignments(EvaluatedAssignmentImpl<AH> evaluatedAssignment, PlusMinusZero mode)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        LOGGER.trace("Collecting constructions from evaluated assignment:\n{}", evaluatedAssignment.debugDumpLazily(1));
        DeltaSetTriple<AC> constructionTriple = constructionTripleExtractor.apply(evaluatedAssignment);
        collectToConstructionMapFromConstructions(evaluatedAssignment, constructionTriple.getZeroSet(), mode, PlusMinusZero.ZERO);
        collectToConstructionMapFromConstructions(evaluatedAssignment, constructionTriple.getPlusSet(), mode, PlusMinusZero.PLUS);
        collectToConstructionMapFromConstructions(evaluatedAssignment, constructionTriple.getMinusSet(), mode, PlusMinusZero.MINUS);
    }

    private void collectToConstructionMapFromConstructions(
            EvaluatedAssignmentImpl<AH> evaluatedAssignment, Collection<AC> constructions, PlusMinusZero mode1, PlusMinusZero mode2)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        for (AC construction : constructions) {
            LOGGER.trace("Collecting evaluated constructions from construction (mode1={}, mode2={}):\n{}",
                    mode1, mode2, construction.debugDumpLazily(1));
            DeltaSetTriple<EC> evaluatedConstructionTriple = construction.getEvaluatedConstructionTriple();
            if (evaluatedConstructionTriple != null) {
                collectToConstructionMapFromEvaluatedConstructions(evaluatedAssignment, evaluatedConstructionTriple.getZeroSet(), mode1, mode2, PlusMinusZero.ZERO);
                collectToConstructionMapFromEvaluatedConstructions(evaluatedAssignment, evaluatedConstructionTriple.getPlusSet(), mode1, mode2, PlusMinusZero.PLUS);
                collectToConstructionMapFromEvaluatedConstructions(evaluatedAssignment, evaluatedConstructionTriple.getMinusSet(), mode1, mode2, PlusMinusZero.MINUS);
            }
        }
    }

    private void collectToConstructionMapFromEvaluatedConstructions(
            EvaluatedAssignmentImpl<AH> evaluatedAssignment, Collection<EC> evaluatedConstructions, PlusMinusZero mode1, PlusMinusZero mode2, PlusMinusZero mode3)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        for (EC evaluatedConstruction : evaluatedConstructions) {
            AbstractConstruction<AH, ?, ?> construction = evaluatedConstruction.getConstruction();

            if (construction.isIgnored()) {
                LOGGER.trace("Construction {} is ignored, skipping {}", construction, evaluatedConstruction);
                continue;
            }

            PlusMinusZero mode = PlusMinusZero.compute(PlusMinusZero.compute(mode1, mode2), mode3);
            if (mode == null) {
                continue;
            }

            // Ugly and temporary hack - some constructions going to plus/minus sets based on validity change
            // FIXME MID-6404
            if (mode == PlusMinusZero.ZERO) {
                if (!construction.getWasValid() && construction.isValid()) {
                    mode = PlusMinusZero.PLUS;
                } else if (construction.getWasValid() && !construction.isValid()) {
                    mode = PlusMinusZero.MINUS;
                }
            }

            Map<K, EvaluatedConstructionPack<EC>> evaluatedConstructionMap = evaluatedConstructionMapTriple.getMap(mode);
            if (evaluatedConstructionMap == null) { // should not occur
                continue;
            }

            K key = keyGenerator.apply(evaluatedConstruction);

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

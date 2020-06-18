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
 * The collected evaluated constructions are neatly sorted by "key", which is usually ResourceShadowDiscriminator.
 *
 * @author Radovan Semancik
 */
public class ConstructionCollector<AH extends AssignmentHolderType, K extends HumanReadableDescribable, ACT extends AbstractConstructionType, AC extends AbstractConstruction<AH,ACT,EC>, EC extends EvaluatedConstructible<AH>> {

    private DeltaMapTriple<K, EvaluatedConstructionPack<EC>> evaluatedConstructionMapTriple;

    private final LensContext<AH> context;
    private final Function<EvaluatedAssignmentImpl<AH>, DeltaSetTriple<AC>> constructionTripleExtractor;
    private final FailableLensFunction<EC, K> keyGenerator;
    private final PrismContext prismContext;

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionCollector.class);

    public ConstructionCollector(LensContext<AH> context, Function<EvaluatedAssignmentImpl<AH>, DeltaSetTriple<AC>> constructionTripleExtractor, FailableLensFunction<EC, K> keyGenerator, PrismContext prismContext) {
        this.context = context;
        this.constructionTripleExtractor = constructionTripleExtractor;
        this.keyGenerator = keyGenerator;
        this.prismContext = prismContext;
    }

    public DeltaMapTriple<K, EvaluatedConstructionPack<EC>> getEvaluatedConstructionMapTriple() {
        return evaluatedConstructionMapTriple;
    }

    public void collect(DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple)
                    throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        evaluatedConstructionMapTriple = prismContext.deltaFactory().createDeltaMapTriple();

        collectToConstructionMapFromEvaluatedAssignments(evaluatedAssignmentTriple.getZeroSet(), PlusMinusZero.ZERO);
        collectToConstructionMapFromEvaluatedAssignments(evaluatedAssignmentTriple.getPlusSet(), PlusMinusZero.PLUS);
        collectToConstructionMapFromEvaluatedAssignments(evaluatedAssignmentTriple.getMinusSet(), PlusMinusZero.MINUS);
    }

    private void collectToConstructionMapFromEvaluatedAssignments(Collection<EvaluatedAssignmentImpl<AH>> evaluatedAssignments, PlusMinusZero mode) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        for (EvaluatedAssignmentImpl<AH> evaluatedAssignment: evaluatedAssignments) {
            LOGGER.trace("Collecting constructions from evaluated assignment:\n{}", evaluatedAssignment.debugDumpLazily(1));
            DeltaSetTriple<AC> constructionTriple = constructionTripleExtractor.apply(evaluatedAssignment);
            collectToConstructionMapFromConstructions(evaluatedAssignment, constructionTriple.getZeroSet(), mode, PlusMinusZero.ZERO);
            collectToConstructionMapFromConstructions(evaluatedAssignment, constructionTriple.getPlusSet(), mode, PlusMinusZero.PLUS);
            collectToConstructionMapFromConstructions(evaluatedAssignment, constructionTriple.getMinusSet(), mode, PlusMinusZero.MINUS);
        }
    }

    private void collectToConstructionMapFromConstructions(
            EvaluatedAssignmentImpl<AH> evaluatedAssignment, Collection<AC> constructions, PlusMinusZero mode1, PlusMinusZero mode2)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        for (AC construction : constructions) {
            LOGGER.trace("Collecting evaluated constructions from construction:\n{}", construction.debugDumpLazily(1));
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
            if (evaluatedConstruction.getConstruction().isIgnored()) {
                LOGGER.trace("Construction {} is ignored, skipping {}", evaluatedConstruction.getConstruction(), evaluatedConstruction);
                continue;
            }

            PlusMinusZero mode = PlusMinusZero.compute(PlusMinusZero.compute(mode1, mode2), mode3);
            Map<K, EvaluatedConstructionPack<EC>> evaluatedConstructionMap = evaluatedConstructionMapTriple.getMap(mode);
            if (evaluatedConstructionMap == null) {
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

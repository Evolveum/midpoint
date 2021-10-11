/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import com.evolveum.midpoint.prism.PrismContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.AbstractConstruction;
import com.evolveum.midpoint.model.impl.lens.ConstructionPack;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.FailableLensFunction;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.delta.DeltaMapTriple;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.util.HumanReadableDescribable;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 *
 */
@Component
public class ConstructionProcessor {

    @Autowired private PrismContext prismContext;

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionProcessor.class);

    public <F extends FocusType, K extends HumanReadableDescribable, T extends AbstractConstruction>
    DeltaMapTriple<K, ConstructionPack<T>> processConstructions(LensContext<F> context,
            DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
            Function<EvaluatedAssignmentImpl<F>, DeltaSetTriple<T>> constructionTripleExtractor,
            FailableLensFunction<T, K> keyGenerator, ComplexConstructionConsumer<K, T> consumer)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        // We will be collecting the evaluated account constructions into these three maps.
        // It forms a kind of delta set triple for the account constructions.
        DeltaMapTriple<K, ConstructionPack<T>> constructionMapTriple = prismContext.deltaFactory().createDeltaMapTriple();
        collectToConstructionMaps(context, evaluatedAssignmentTriple, constructionMapTriple, constructionTripleExtractor, keyGenerator);

        LOGGER.trace("constructionMapTriple:\n{}", constructionMapTriple.debugDumpLazily());

        // Now we are processing constructions from all the three sets once again. We will create projection contexts
        // for them if not yet created. Now we will do the usual routing for converting the delta triples to deltas.
        // I.e. zero means unchanged, plus means added, minus means deleted. That will be recorded in the SynchronizationPolicyDecision.
        // We will also collect all the construction triples to projection context. These will be used later for computing
        // actual attribute deltas (in consolidation processor).
        for (K key : constructionMapTriple.unionKeySets()) {

            boolean cont = consumer.before(key);
            if (!cont) {
                continue;
            }

            String desc = key.toHumanReadableDescription();

            ConstructionPack<T> zeroConstructionPack = constructionMapTriple.getZeroMap().get(key);
            ConstructionPack<T> plusConstructionPack = constructionMapTriple.getPlusMap().get(key);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Processing construction packs for {}", key.toHumanReadableDescription());
                if (zeroConstructionPack == null) {
                    LOGGER.trace("ZERO construction pack: null");
                } else {
                    LOGGER.trace("ZERO construction pack (hasValidAssignment={}, hasStrongConstruction={})\n{}",
                            zeroConstructionPack.hasValidAssignment(), zeroConstructionPack.hasStrongConstruction(),
                            zeroConstructionPack.debugDump(1));
                }
                if (plusConstructionPack == null) {
                    LOGGER.trace("PLUS construction pack: null");
                } else {
                    LOGGER.trace("PLUS construction pack (hasValidAssignment={}, hasStrongConstruction={})\n{}",
                            plusConstructionPack.hasValidAssignment(), plusConstructionPack.hasStrongConstruction(),
                            plusConstructionPack.debugDump(1));
                }
            }

            // SITUATION: The construction is ASSIGNED
            if (plusConstructionPack != null && plusConstructionPack.hasStrongConstruction()) {

                if (plusConstructionPack.hasValidAssignment()) {
                    if (zeroConstructionPack != null && zeroConstructionPack.hasValidAssignment()) {
                        LOGGER.trace("Construction {}: unchanged (valid) + assigned (valid)", desc);
                        consumer.onUnchangedValid(key, desc);
                    } else {
                        LOGGER.trace("Construction {}: assigned (valid)", desc);
                        consumer.onAssigned(key, desc);
                    }
                } else if (zeroConstructionPack != null && zeroConstructionPack.hasValidAssignment()) {
                    LOGGER.trace("Construction {}: unchanged (valid) + assigned (invalid)", desc);
                    consumer.onUnchangedValid(key, desc);
                } else {
                    // Just ignore it, do not even create projection context
                    LOGGER.trace("Construction {} ignoring: assigned (invalid)", desc);
                }

            // SITUATION: The projection should exist (is valid), there is NO CHANGE in assignments
            } else if (zeroConstructionPack != null && zeroConstructionPack.hasValidAssignment() && zeroConstructionPack.hasStrongConstruction()) {

                LOGGER.trace("Construction {}: unchanged (valid)", desc);
                consumer.onUnchangedValid(key, desc);

            // SITUATION: The projection is both ASSIGNED and UNASSIGNED
            } else if (constructionMapTriple.getPlusMap().containsKey(key) && constructionMapTriple.getMinusMap().containsKey(key) &&
                    plusConstructionPack != null && plusConstructionPack.hasStrongConstruction()) {
                // Account was removed and added in the same operation. This is the case if e.g. one role is
                // removed and another is added and they include the same account.
                // Keep original account state

                ConstructionPack<T> plusPack = constructionMapTriple.getPlusMap().get(key);
                ConstructionPack<T> minusPack = constructionMapTriple.getMinusMap().get(key);

                if (plusPack.hasValidAssignment() && minusPack.hasValidAssignment()) {

                    LOGGER.trace("Construction {}: both assigned and unassigned (valid)", desc);
                    consumer.onUnchangedValid(key, desc);

                } else if (!plusPack.hasValidAssignment() && !minusPack.hasValidAssignment()) {
                    // Just ignore it, do not even create projection context
                    LOGGER.trace("Construction {} ignoring: both assigned and unassigned (invalid)", desc);

                } else if (plusPack.hasValidAssignment() && !minusPack.hasValidAssignment()) {
                    // Assignment became valid. Same as if it was assigned.
                    LOGGER.trace("Construction {}: both assigned and unassigned (invalid->valid)", desc);
                    consumer.onAssigned(key, desc);

                } else if (!plusPack.hasValidAssignment() && minusPack.hasValidAssignment()) {
                    // Assignment became invalid. Same as if it was unassigned.

                    LOGGER.trace("Construction {}: both assigned and unassigned (valid->invalid)", desc);
                    consumer.onUnassigned(key, desc);

                } else {
                    throw new IllegalStateException("Whoops!?!");
                }

            // SITUATION: The projection is UNASSIGNED
            } else if (constructionMapTriple.getMinusMap().containsKey(key)) {

                LOGGER.trace("Construction {}: unassigned", desc);
                consumer.onUnassigned(key, desc);

            // SITUATION: The projection should exist (invalid), there is NO CHANGE in assignments
            } else if (constructionMapTriple.getZeroMap().containsKey(key) && !constructionMapTriple.getZeroMap().get(key).hasValidAssignment()) {

                LOGGER.trace("Construction {}: unchanged (invalid)", desc);
                consumer.onUnchangedInvalid(key, desc);

            // This is a legal state: projection was assigned, but it only has weak construction (no strong)
            // We do not need to do anything. But we want to log the message
            // and we do not want the "looney" error below.
            } else if (plusConstructionPack != null && !plusConstructionPack.hasStrongConstruction()) {

                // Just ignore it, do not even create projection context
                LOGGER.trace("Construction {} ignoring: assigned (weak only)", desc);

            // This is a legal state: projection is unchanged, but it only has weak construction (no strong)
            // We do not need to do anything. But we want to log the message
            // and we do not want the "looney" error below.
            } else if (zeroConstructionPack != null && !zeroConstructionPack.hasStrongConstruction()) {

                // Just ignore it, do not even create projection context
                LOGGER.trace("Construction {} ignoring: unchanged (weak only)", desc);


            } else {
                throw new IllegalStateException("Construction " + desc + " went looney");
            }

            consumer.after(key, desc, constructionMapTriple);
        }

        return constructionMapTriple;
    }

    private <F extends FocusType, K, T extends AbstractConstruction> void collectToConstructionMaps(LensContext<F> context,
            DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
            DeltaMapTriple<K, ConstructionPack<T>> constructionMapTriple,
            Function<EvaluatedAssignmentImpl<F>, DeltaSetTriple<T>> constructionTripleExtractor, FailableLensFunction<T, K> keyGenerator)
                    throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        collectToConstructionMapFromEvaluatedAssignments(context, evaluatedAssignmentTriple.getZeroSet(), constructionMapTriple, constructionTripleExtractor, keyGenerator, PlusMinusZero.ZERO);
        collectToConstructionMapFromEvaluatedAssignments(context, evaluatedAssignmentTriple.getPlusSet(), constructionMapTriple, constructionTripleExtractor, keyGenerator, PlusMinusZero.PLUS);
        collectToConstructionMapFromEvaluatedAssignments(context, evaluatedAssignmentTriple.getMinusSet(), constructionMapTriple, constructionTripleExtractor, keyGenerator, PlusMinusZero.MINUS);
    }

    private <F extends FocusType, K, T extends AbstractConstruction> void collectToConstructionMapFromEvaluatedAssignments(LensContext<F> context,
            Collection<EvaluatedAssignmentImpl<F>> evaluatedAssignments,
            DeltaMapTriple<K, ConstructionPack<T>> constructionMapTriple, Function<EvaluatedAssignmentImpl<F>, DeltaSetTriple<T>> constructionTripleExtractor, FailableLensFunction<T, K> keyGenerator, PlusMinusZero mode) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        for (EvaluatedAssignmentImpl<F> evaluatedAssignment: evaluatedAssignments) {
            LOGGER.trace("Collecting constructions from evaluated assignment:\n{}", evaluatedAssignment.debugDumpLazily(1));
            DeltaSetTriple<T> constructionTriple = constructionTripleExtractor.apply(evaluatedAssignment);
            collectToConstructionMapFromEvaluatedConstructions(context, evaluatedAssignment, constructionTriple.getZeroSet(), constructionMapTriple, keyGenerator, mode, PlusMinusZero.ZERO);
            collectToConstructionMapFromEvaluatedConstructions(context, evaluatedAssignment, constructionTriple.getPlusSet(), constructionMapTriple, keyGenerator, mode, PlusMinusZero.PLUS);
            collectToConstructionMapFromEvaluatedConstructions(context, evaluatedAssignment, constructionTriple.getMinusSet(), constructionMapTriple, keyGenerator, mode, PlusMinusZero.MINUS);
        }
    }

    private <F extends FocusType, K, T extends AbstractConstruction> void collectToConstructionMapFromEvaluatedConstructions(
            LensContext<F> context, EvaluatedAssignmentImpl<F> evaluatedAssignment, Collection<T> evaluatedConstructions,
            DeltaMapTriple<K, ConstructionPack<T>> constructionMapTriple, FailableLensFunction<T, K> keyGenerator,
            PlusMinusZero mode1, PlusMinusZero mode2)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        for (T construction : evaluatedConstructions) {
            if (construction.isIgnored()) {
                LOGGER.trace("Construction {} is ignored, skipping", construction);
                continue;
            }

            PlusMinusZero mode = PlusMinusZero.compute(mode1, mode2);
            Map<K, ConstructionPack<T>> constructionMap = constructionMapTriple.getMap(mode);
            if (constructionMap == null) {
                continue;
            }

            K key = keyGenerator.apply(construction);

            ConstructionPack<T> constructionPack;
            if (constructionMap.containsKey(key)) {
                constructionPack = constructionMap.get(key);
            } else {
                constructionPack = new ConstructionPack<>();
                constructionMap.put(key, constructionPack);
            }

            constructionPack.add(context.getPrismContext().itemFactory().createPropertyValue(construction));
            if (evaluatedAssignment.isValid()) {
                constructionPack.setHasValidAssignment(true);
            }
            if (evaluatedAssignment.isForceRecon()) {
                constructionPack.setForceRecon(true);
            }

        }
    }

}

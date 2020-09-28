/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.function.Function;

import com.evolveum.midpoint.model.impl.lens.construction.*;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.FailableLensFunction;
import com.evolveum.midpoint.prism.delta.DeltaMapTriple;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.util.HumanReadableDescribable;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Radovan Semancik
 */
@Component
public class ConstructionProcessor {

    @Autowired private PrismContext prismContext;

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionProcessor.class);

    /**
     * Categorizes assigned constructions (resource object or persona ones) from evaluated assignments into
     * other structures by calling appropriate methods on the consumer.
     *
     * @param evaluatedAssignmentTriple Constructions collected during assignment evaluation.
     * @param constructionTripleExtractor Method that extracts constructions of given type - resource objects or personas -
     *                                    (in the form of triple i.e. added/deleted/kept) from evaluated assignment. See {@link ConstructionCollector}.
     * @param keyGenerator Method that generates indexing key for constructions, under which they are collected. See K type.
     * @param consumer Object that receives categorized constructions (via methods like onAssigned, onUnchangedValid, ...).
     * @param <AH> Focus type
     * @param <K> Indexing key type. Currently, for resource object constructions it is {@link com.evolveum.midpoint.schema.ResourceShadowDiscriminator};
     *            for personas it is {@link com.evolveum.midpoint.model.impl.lens.PersonaProcessor.PersonaKey} (type+subtypes).
     * @param <ACT> Construction bean type.
     * @param <AC> Construction type.
     * @param <EC> Evaluated construction type.
     *
     * @return Constructions sorted out by status (plus/minus/zero) and indexing key (e.g. {@link com.evolveum.midpoint.schema.ResourceShadowDiscriminator}).
     */
    @SuppressWarnings("JavadocReference")
    public <AH extends AssignmentHolderType, K extends HumanReadableDescribable, ACT extends AbstractConstructionType, AC extends AbstractConstruction<AH,ACT,EC>, EC extends EvaluatedAbstractConstruction<AH>>
    DeltaMapTriple<K, EvaluatedConstructionPack<EC>> distributeConstructions(DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple,
            Function<EvaluatedAssignmentImpl<AH>, DeltaSetTriple<AC>> constructionTripleExtractor,
            FailableLensFunction<EC, K> keyGenerator, ComplexConstructionConsumer<K, EC> consumer)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        // We will be collecting the evaluated account constructions into these three maps.
        // It forms a kind of delta set triple for the account/persona constructions.
        ConstructionCollector<AH, K, ACT, AC, EC> constructionCollector =
                new ConstructionCollector<>(constructionTripleExtractor, keyGenerator, prismContext);
        constructionCollector.collect(evaluatedAssignmentTriple);
        DeltaMapTriple<K, EvaluatedConstructionPack<EC>> evaluatedConstructionMapTriple = constructionCollector.getEvaluatedConstructionMapTriple();

        LOGGER.trace("evaluatedConstructionMapTriple:\n{}", evaluatedConstructionMapTriple.debugDumpLazily(1));

        // Now we are processing constructions from all the three sets once again. We will create projection contexts
        // for them if not yet created. Now we will do the usual routine for converting the delta triples to deltas.
        // I.e. zero means unchanged, plus means added, minus means deleted. That will be recorded in the SynchronizationPolicyDecision.
        // We will also collect all the construction triples to projection context. These will be used later for computing
        // actual attribute deltas (in consolidation processor).
        for (K key : evaluatedConstructionMapTriple.unionKeySets()) {

            boolean cont = consumer.before(key);
            if (!cont) {
                continue;
            }

            String desc = key.toHumanReadableDescription();

            EvaluatedConstructionPack<EC> zeroEvaluatedConstructionPack = evaluatedConstructionMapTriple.getZeroMap().get(key);
            EvaluatedConstructionPack<EC> plusEvaluatedConstructionPack = evaluatedConstructionMapTriple.getPlusMap().get(key);

            logConstructionPacks(key, zeroEvaluatedConstructionPack, plusEvaluatedConstructionPack);

            distributeConstructionPacks(key, zeroEvaluatedConstructionPack, plusEvaluatedConstructionPack,
                    consumer, evaluatedConstructionMapTriple, desc);

            consumer.after(key, desc, evaluatedConstructionMapTriple);
        }

        return evaluatedConstructionMapTriple;
    }

    private <AH extends AssignmentHolderType, K extends HumanReadableDescribable, EC extends EvaluatedAbstractConstruction<AH>> void logConstructionPacks(K key, EvaluatedConstructionPack<EC> zeroEvaluatedConstructionPack, EvaluatedConstructionPack<EC> plusEvaluatedConstructionPack) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing evaluated construction packs for {}", key.toHumanReadableDescription());
            if (zeroEvaluatedConstructionPack == null) {
                LOGGER.trace("ZERO evaluated construction pack: null");
            } else {
                LOGGER.trace("ZERO evaluated construction pack (hasValidAssignment={}, hasStrongConstruction={})\n{}",
                        zeroEvaluatedConstructionPack.hasValidAssignment(), zeroEvaluatedConstructionPack.hasNonWeakConstruction(),
                        zeroEvaluatedConstructionPack.debugDump(1));
            }
            if (plusEvaluatedConstructionPack == null) {
                LOGGER.trace("PLUS evaluated construction pack: null");
            } else {
                LOGGER.trace("PLUS evaluated construction pack (hasValidAssignment={}, hasStrongConstruction={})\n{}",
                        plusEvaluatedConstructionPack.hasValidAssignment(), plusEvaluatedConstructionPack.hasNonWeakConstruction(),
                        plusEvaluatedConstructionPack.debugDump(1));
            }
        }
    }

    /**
     * This is the heart of the processing: we call appropriate state-related methods (onAssigned, onUnassigned, onUnchangedXXX)
     * on the construction consumer.
     */
    private <AH extends AssignmentHolderType, K extends HumanReadableDescribable, EC extends EvaluatedAbstractConstruction<AH>>
    void distributeConstructionPacks(K key, EvaluatedConstructionPack<EC> zeroEvaluatedConstructionPack,
            EvaluatedConstructionPack<EC> plusEvaluatedConstructionPack, ComplexConstructionConsumer<K, EC> consumer,
            DeltaMapTriple<K, EvaluatedConstructionPack<EC>> evaluatedConstructionMapTriple, String desc) throws SchemaException {
        // SITUATION: The construction is ASSIGNED
        if (plusEvaluatedConstructionPack != null && plusEvaluatedConstructionPack.hasNonWeakConstruction()) {

            if (plusEvaluatedConstructionPack.hasValidAssignment()) {
                if (zeroEvaluatedConstructionPack != null && zeroEvaluatedConstructionPack.hasValidAssignment()) {
                    LOGGER.trace("Construction {}: unchanged (valid) + assigned (valid)", desc);
                    consumer.onUnchangedValid(key, desc);
                } else {
                    LOGGER.trace("Construction {}: assigned (valid)", desc);
                    consumer.onAssigned(key, desc);
                }
            } else if (zeroEvaluatedConstructionPack != null && zeroEvaluatedConstructionPack.hasValidAssignment()) {
                LOGGER.trace("Construction {}: unchanged (valid) + assigned (invalid)", desc);
                consumer.onUnchangedValid(key, desc);
            } else {
                // Just ignore it, do not even create projection context
                LOGGER.trace("Construction {} ignoring: assigned (invalid)", desc);
            }

        // SITUATION: The projection should exist (is valid), there is NO CHANGE in assignments
        } else if (zeroEvaluatedConstructionPack != null && zeroEvaluatedConstructionPack.hasValidAssignment() && zeroEvaluatedConstructionPack.hasNonWeakConstruction()) {

            LOGGER.trace("Construction {}: unchanged (valid)", desc);
            consumer.onUnchangedValid(key, desc);

        // SITUATION: The projection is both ASSIGNED and UNASSIGNED; TODO evaluatedConstructionMapTriple.plusMap.get(key) is the same as plusEvaluatedConstructionPack, isn't it?
        } else if (evaluatedConstructionMapTriple.getPlusMap().containsKey(key) && evaluatedConstructionMapTriple.getMinusMap().containsKey(key) &&
                plusEvaluatedConstructionPack != null && plusEvaluatedConstructionPack.hasNonWeakConstruction()) {
            // Account was removed and added in the same operation. This is the case if e.g. one role is
            // removed and another is added and they include the same account.
            // Keep original account state

            EvaluatedConstructionPack<EC> plusPack = evaluatedConstructionMapTriple.getPlusMap().get(key);
            EvaluatedConstructionPack<EC> minusPack = evaluatedConstructionMapTriple.getMinusMap().get(key);

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
        } else if (evaluatedConstructionMapTriple.getMinusMap().containsKey(key)) {

            LOGGER.trace("Construction {}: unassigned", desc);
            consumer.onUnassigned(key, desc);

        // SITUATION: The projection should exist (invalid), there is NO CHANGE in assignments
        } else if (evaluatedConstructionMapTriple.getZeroMap().containsKey(key) && !evaluatedConstructionMapTriple.getZeroMap().get(key).hasValidAssignment()) {

            LOGGER.trace("Construction {}: unchanged (invalid)", desc);
            consumer.onUnchangedInvalid(key, desc);

        // This is a legal state: projection was assigned, but it only has weak construction (no strong)
        // We do not need to do anything. But we want to log the message
        // and we do not want the "looney" error below.
        } else if (plusEvaluatedConstructionPack != null && !plusEvaluatedConstructionPack.hasNonWeakConstruction()) {

            // Just ignore it, do not even create projection context
            LOGGER.trace("Construction {} ignoring: assigned (weak only)", desc);

        // This is a legal state: projection is unchanged, but it only has weak construction (no strong)
        // We do not need to do anything. But we want to log the message
        // and we do not want the "looney" error below.
        } else if (zeroEvaluatedConstructionPack != null && !zeroEvaluatedConstructionPack.hasNonWeakConstruction()) {

            // Just ignore it, do not even create projection context
            LOGGER.trace("Construction {} ignoring: unchanged (weak only)", desc);

        } else {
            throw new IllegalStateException("Construction " + desc + " went looney");
        }
    }
}

/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.focus.ObjectTemplateProcessor;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Responsible for consolidation of delta set triple (plus, minus, zero sets) to item delta.
 *
 * TODO Consider making this an inner class of DeltaSetTripleMapConsolidation.
 *   But it is (still) possible we would need to use this class directly from some clients.
 *   Or should we merge its functionality with IVwO consolidator?
 */
@Experimental
class DeltaSetTripleConsolidation<V extends PrismValue, D extends ItemDefinition, I extends ItemValueWithOrigin<V,D>> {

    // The logger name is intentionally different because of the backward compatibility.
    private static final Trace LOGGER = TraceManager.getTrace(ObjectTemplateProcessor.class);

    /**
     * Path to item that is to be consolidated.
     */
    private final UniformItemPath itemPath;

    /**
     * Delta set triple that is to be consolidated.
     */
    private final DeltaSetTriple<I> deltaSetTriple;

    /**
     * Existing (apriori) delta that was specified by the caller and/or computed upstream.
     */
    private final ItemDelta<V, D> aprioriItemDelta;

    /**
     * Existing item values. Note: this is the state AFTER apriori item delta is applied!
     */
    private final Item<V, D> existingItem;

    /**
     * Definition of item to be consolidated.
     */
    private final D itemDefinition;

    /**
     * Should the values from zero set be transformed to delta ADD section?
     * This is the case when the whole object is being added.
     */
    private final boolean addUnchangedValues;

    /**
     * Mapping evaluation environment (context description, now, task).
     */
    private final MappingEvaluationEnvironment env;

    private ItemDelta<V, D> itemDelta;

    DeltaSetTripleConsolidation(UniformItemPath itemPath, DeltaSetTriple<I> deltaSetTriple, ItemDelta<V, D> aprioriItemDelta, PrismObject<?> targetObject, D itemDefinition,
            boolean addUnchangedValues, MappingEvaluationEnvironment env) {
        this.itemPath = itemPath;
        this.deltaSetTriple = deltaSetTriple;
        this.aprioriItemDelta = aprioriItemDelta;
        this.existingItem = targetObject != null ? targetObject.findItem(itemPath) : null;
        this.itemDefinition = itemDefinition;
        this.addUnchangedValues = addUnchangedValues;
        this.env = env;
    }

    ItemDelta<?, ?> consolidateItem() throws ExpressionEvaluationException, PolicyViolationException, SchemaException {
        LOGGER.trace("Computing delta for {} with the delta set triple:\n{}", itemPath, deltaSetTriple.debugDumpLazily());

        computeItemDelta();
        cleanupItemDelta();

        return itemDelta;
    }

    private void computeItemDelta() throws ExpressionEvaluationException, PolicyViolationException, SchemaException {
        IvwoConsolidator<V, D, I> consolidator = new IvwoConsolidatorBuilder<V, D, I>()
                .itemPath(itemPath)
                .ivwoTriple(deltaSetTriple)
                .itemDefinition(itemDefinition)
                .aprioriItemDelta(aprioriItemDelta)
                .existingItem(existingItem)
                .valueMatcher(null)
                .comparator(null)
                .addUnchangedValues(addUnchangedValues)
                .addUnchangedValuesExceptForNormalMappings(true)
                .hasExistingItem(true)
                .contextDescription(env.contextDescription)
                .strengthSelector(StrengthSelector.ALL)
                .build();

        itemDelta = consolidator.consolidateToDelta();
    }

    private void cleanupItemDelta() throws SchemaException {
        itemDelta.simplify();
        itemDelta.validate(env.contextDescription);
        LOGGER.trace("Computed delta:\n{}", itemDelta.debugDumpLazily());
    }

}

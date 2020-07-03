/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.focus.ObjectTemplateProcessor;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateItemDefinitionType;

import java.util.Collection;

/**
 * Responsible for consolidation of delta set triple (plus, minus, zero sets) to item delta.
 *
 * TODO Consider making this an inner class of DeltaSetTripleMapConsolidation.
 *   But it is (still) possible we would need to use this class directly from some clients.
 *   Or should we merge its functionality with IVwO consolidator?
 */
@Experimental
class DeltaSetTripleConsolidation<T extends AssignmentHolderType> {

    // The logger name is intentionally different because of the backward compatibility.
    private static final Trace LOGGER = TraceManager.getTrace(ObjectTemplateProcessor.class);

    /**
     * Mapping evaluation environment (context description, now, task).
     */
    private final MappingEvaluationEnvironment env;

    private final UniformItemPath itemPath;
    private final ItemDefinition<?> prismItemDefinition;
    private final ObjectTemplateItemDefinitionType templateItemDefinition;
    private final ItemDelta<?, ?> aprioriItemDelta;
    private final DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>> deltaSetTriple;

    /**
     * Target object, for which deltas are to be produced.
     * TODO remove the need to have item container in IVwO consolidator
     *  and delete this field.
     */
    private final PrismObject<T> targetObject;

    /**
     * Should the values from zero set be transformed to delta ADD section?
     * This is the case when the whole object is being added.
     */
    private final boolean addUnchangedValues;

    private ItemDelta itemDelta;

    DeltaSetTripleConsolidation(
            UniformItemPath itemPath, ItemDefinition<?> prismItemDefinition, ObjectTemplateItemDefinitionType templateItemDefinition,
            ItemDelta<?, ?> aprioriItemDelta, DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>> deltaSetTriple,
            PrismObject<T> targetObject, boolean addUnchangedValues,
            MappingEvaluationEnvironment env) {
        this.itemPath = itemPath;
        this.prismItemDefinition = prismItemDefinition;
        this.aprioriItemDelta = aprioriItemDelta;
        this.deltaSetTriple = deltaSetTriple;
        this.templateItemDefinition = templateItemDefinition;
        this.targetObject = targetObject;
        this.addUnchangedValues = addUnchangedValues;
        this.env = env;
    }

    ItemDelta<?, ?> consolidateItem() throws ExpressionEvaluationException, PolicyViolationException, SchemaException {
        LOGGER.trace("Computing delta for {} with the delta set triple:\n{}", itemPath, deltaSetTriple.debugDumpLazily());

        computeItemDelta();
        reconcileItemDelta();

        return itemDelta;
    }

    private void computeItemDelta() throws ExpressionEvaluationException,
            PolicyViolationException, SchemaException {
        //noinspection unchecked
        IvwoConsolidator consolidator = new IvwoConsolidatorBuilder()
                .itemPath(itemPath)
                .ivwoTriple(deltaSetTriple)
                .itemDefinition(prismItemDefinition)
                .aprioriItemDelta(aprioriItemDelta)
                .itemContainer(targetObject)
                .valueMatcher(null)
                .comparator(null)
                .addUnchangedValues(addUnchangedValues)
                .filterExistingValues(true)
                .isExclusiveStrong(false)
                .contextDescription(env.contextDescription)
                .strengthSelector(StrengthSelector.ALL)
                .build();

        itemDelta = consolidator.consolidateToDelta();
    }

    /**
     * Do a quick version of reconciliation. There is not much to reconcile as both the source and the target
     * is focus. But there are few cases to handle, such as strong mappings, and sourceless normal mappings.
     *
     * TODO Why don't we do this as part of IVwO consolidation?
     */
    private void reconcileItemDelta() throws SchemaException {
        boolean isAssignment = SchemaConstants.PATH_ASSIGNMENT.equivalent(itemPath);

        Collection<? extends ItemValueWithOrigin<?,?>> zeroSet = deltaSetTriple.getZeroSet();
        Item<PrismValue, ItemDefinition> itemNew = null;
        if (targetObject != null) {
            itemNew = targetObject.findItem(itemPath);
        }
        for (ItemValueWithOrigin<?,?> zeroSetIvwo: zeroSet) {

            PrismValueDeltaSetTripleProducer<?, ?> mapping = zeroSetIvwo.getMapping();
            if (mapping.getStrength() == null || mapping.getStrength() == MappingStrengthType.NORMAL) {
                if (aprioriItemDelta != null && !aprioriItemDelta.isEmpty()) {
                    continue;
                }
                if (!mapping.isSourceless()) {
                    continue;
                }
                LOGGER.trace("Adding zero values from normal mapping {}, a-priori delta: {}, isSourceless: {}",
                        mapping, aprioriItemDelta, mapping.isSourceless());
            } else if (mapping.getStrength() == MappingStrengthType.WEAK) {
                if (itemNew != null && !itemNew.isEmpty() || itemDelta.addsAnyValue()) {
                    continue;
                }
                LOGGER.trace("Adding zero values from weak mapping {}, itemNew: {}, itemDelta: {}",
                        mapping, itemNew, itemDelta);
            } else {
                LOGGER.trace("Adding zero values from strong mapping {}", mapping);
            }

            PrismValue valueFromZeroSet = zeroSetIvwo.getItemValue();
            if (itemNew == null || !itemNew.contains(valueFromZeroSet, EquivalenceStrategy.REAL_VALUE)) {
                LOGGER.trace("Reconciliation will add value {} for item {}. Existing item: {}", valueFromZeroSet, itemPath, itemNew);
                //noinspection unchecked
                itemDelta.addValuesToAdd(LensUtil.cloneAndApplyMetadata(valueFromZeroSet, isAssignment, mapping));
            }
        }

        if (templateItemDefinition != null && Boolean.FALSE.equals(templateItemDefinition.isTolerant())) {
            throw new UnsupportedOperationException("The 'tolerant=false' setting on template items is no longer supported."
                    + " Please use mapping range instead. In '" + itemPath + "' consolidation in " + env.contextDescription);
        }

        itemDelta.simplify();
        itemDelta.validate(env.contextDescription);
        LOGGER.trace("Computed delta:\n{}", itemDelta.debugDumpLazily());
    }
}

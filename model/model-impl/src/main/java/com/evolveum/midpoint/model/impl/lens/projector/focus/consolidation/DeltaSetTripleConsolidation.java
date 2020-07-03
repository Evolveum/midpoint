/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation;

import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.focus.ObjectTemplateProcessor;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.util.PrismUtil;
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

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * Responsible for consolidation of delta set triple (plus, minus, zero sets) to item delta.
 */
@Experimental
class DeltaSetTripleConsolidation<T extends AssignmentHolderType> {

    // The logger name is intentionally different because of the backward compatibility.
    private static final Trace LOGGER = TraceManager.getTrace(ObjectTemplateProcessor.class);

    private final DeltaSetTripleMapConsolidation<T> wholeConsolidation;
    private final UniformItemPath itemPath;
    private final ItemDefinition<?> prismItemDefinition;
    private final ObjectTemplateItemDefinitionType templateItemDefinition;
    private final ItemDelta<?, ?> aprioriItemDelta;
    private final DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>> deltaSetTriple;

    private ItemDelta itemDelta;

    DeltaSetTripleConsolidation(DeltaSetTripleMapConsolidation<T> wholeConsolidation,
            UniformItemPath itemPath, ItemDefinition<?> prismItemDefinition, ObjectTemplateItemDefinitionType templateItemDefinition,
            ItemDelta<?, ?> aprioriItemDelta, DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>> deltaSetTriple) {
        this.wholeConsolidation = wholeConsolidation;
        this.itemPath = itemPath;
        this.prismItemDefinition = prismItemDefinition;
        this.aprioriItemDelta = aprioriItemDelta;
        this.deltaSetTriple = deltaSetTriple;
        this.templateItemDefinition = templateItemDefinition;
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
                .itemContainer(wholeConsolidation.targetObject)
                .valueMatcher(null)
                .comparator(null)
                .addUnchangedValues(wholeConsolidation.addUnchangedValues)
                .filterExistingValues(true)
                .isExclusiveStrong(false)
                .contextDescription(wholeConsolidation.contextDescription)
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
        if (wholeConsolidation.targetObject != null) {
            itemNew = wholeConsolidation.targetObject.findItem(itemPath);
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
                itemDelta.addValuesToAdd(LensUtil.cloneAndApplyMetadata(valueFromZeroSet, isAssignment, mapping));
            }
        }

        if (templateItemDefinition != null && Boolean.FALSE.equals(templateItemDefinition.isTolerant())) {
            throw new UnsupportedOperationException("The 'tolerant=false' setting on template items is no longer supported."
                    + " Please use mapping range instead. In '" + itemPath + "' consolidation in " + wholeConsolidation.contextDescription);
        }

        itemDelta.simplify();
        itemDelta.validate(wholeConsolidation.contextDescription);
        LOGGER.trace("Computed delta:\n{}", itemDelta.debugDumpLazily());
    }

    // TODO this should be maybe moved into LensUtil.consolidateTripleToDelta e.g.
    //  under a special option "createReplaceDelta", but for the time being, let's keep it here
    private boolean isDeltaRedundant(PrismObject<T> targetObject,
            ObjectTemplateItemDefinitionType templateItemDefinition, @NotNull ItemDelta<?, ?> itemDelta)
            throws SchemaException {
        if (itemDelta instanceof PropertyDelta) {
            QName matchingRuleName = templateItemDefinition != null ? templateItemDefinition.getMatchingRule() : null;
            return isPropertyDeltaRedundant(targetObject, matchingRuleName, (PropertyDelta<?>) itemDelta);
        } else {
            return itemDelta.isRedundant(targetObject, false);
        }
    }

    private <AH extends AssignmentHolderType, X> boolean isPropertyDeltaRedundant(PrismObject<AH> targetObject,
            QName matchingRuleName, @NotNull PropertyDelta<X> propertyDelta)
            throws SchemaException {
        MatchingRule<X> matchingRule = wholeConsolidation.beans.matchingRuleRegistry.getMatchingRule(matchingRuleName, null);
        return propertyDelta.isRedundant(targetObject, EquivalenceStrategy.IGNORE_METADATA, matchingRule, false);
    }
}

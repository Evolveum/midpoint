/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.IvwoConsolidator;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.StrengthSelector;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
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

/**
 * Responsible for consolidation of delta set triple map (plus, minus, zero sets for individual items) to item deltas.
 */
@Experimental
class DeltaSetTripleMapConsolidation<T extends AssignmentHolderType>  {

    // The logger name is intentionally different because of the backward compatibility.
    private static final Trace LOGGER = TraceManager.getTrace(ObjectTemplateProcessor.class);

    private final Map<UniformItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> outputTripleMap;
    private final Map<UniformItemPath, ObjectTemplateItemDefinitionType> itemDefinitionsMap;
    private final PrismObject<T> targetObject;
    private final ObjectDelta<T> targetAPrioriDelta;
    private final PrismObjectDefinition<T> targetDefinition;
    private final String contextDescription;
    private final ModelBeans beans;

    private Collection<ItemDelta<?,?>> itemDeltas;

    DeltaSetTripleMapConsolidation(Map<UniformItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> outputTripleMap,
            Map<UniformItemPath, ObjectTemplateItemDefinitionType> itemDefinitionsMap, PrismObject<T> targetObject,
            ObjectDelta<T> targetAPrioriDelta, PrismObjectDefinition<T> targetDefinition, String contextDescription,
            ModelBeans beans) {
        this.outputTripleMap = outputTripleMap;
        this.itemDefinitionsMap = itemDefinitionsMap;
        this.targetObject = targetObject;
        this.targetAPrioriDelta = targetAPrioriDelta;
        this.targetDefinition = targetDefinition;
        this.contextDescription = contextDescription;
        this.beans = beans;
    }

    @SuppressWarnings("unchecked")
    void computeItemDeltas() throws ExpressionEvaluationException, PolicyViolationException, SchemaException {

        Collection<ItemDelta<?,?>> itemDeltas = new ArrayList<>();

        LOGGER.trace("Computing deltas in {}, focusDelta:\n{}", contextDescription, targetAPrioriDelta);

        boolean addUnchangedValues = false;
        if (targetAPrioriDelta != null && targetAPrioriDelta.isAdd()) {
            addUnchangedValues = true;
        }

        for (Map.Entry<UniformItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?,?>>> entry: outputTripleMap.entrySet()) {
            UniformItemPath itemPath = entry.getKey();
            boolean isAssignment = SchemaConstants.PATH_ASSIGNMENT.equivalent(itemPath);
            DeltaSetTriple<? extends ItemValueWithOrigin<?,?>> outputTriple = entry.getValue();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Computed triple for {}:\n{}", itemPath, outputTriple.debugDump());
            }
            final ObjectTemplateItemDefinitionType templateItemDefinition;
            if (itemDefinitionsMap != null) {
                templateItemDefinition = ItemPathCollectionsUtil.getFromMap(itemDefinitionsMap, itemPath);
            } else {
                templateItemDefinition = null;
            }
            boolean isNonTolerant = templateItemDefinition != null && Boolean.FALSE.equals(templateItemDefinition.isTolerant());

            ItemDelta aprioriItemDelta = LensUtil.getAprioriItemDelta(targetAPrioriDelta, itemPath);

            IvwoConsolidator consolidator = new IvwoConsolidator<>();
            consolidator.setItemPath(itemPath);
            consolidator.setIvwoTriple(outputTriple);
            consolidator.setItemDefinition(targetDefinition.findItemDefinition(itemPath));
            consolidator.setAprioriItemDelta(aprioriItemDelta);
            consolidator.setItemContainer(targetObject);
            consolidator.setValueMatcher(null);
            consolidator.setComparator(null);
            consolidator.setAddUnchangedValues(addUnchangedValues);
            consolidator.setFilterExistingValues(!isNonTolerant); // if non-tolerant, we want to gather ZERO & PLUS sets
            consolidator.setExclusiveStrong(false);
            consolidator.setContextDescription(contextDescription);
            consolidator.setStrengthSelector(StrengthSelector.ALL);

            @NotNull ItemDelta itemDelta = consolidator.consolidateToDelta();

            // Do a quick version of reconciliation. There is not much to reconcile as both the source and the target
            // is focus. But there are few cases to handle, such as strong mappings, and sourceless normal mappings.
            Collection<? extends ItemValueWithOrigin<?,?>> zeroSet = outputTriple.getZeroSet();
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
                    itemDelta.addValuesToAdd(LensUtil.cloneAndApplyMetadata(valueFromZeroSet, isAssignment, mapping));
                }
            }

            if (isNonTolerant) {
                if (itemDelta.isDelete()) {
                    LOGGER.trace("Non-tolerant item with values to DELETE => removing them");
                    itemDelta.resetValuesToDelete();
                }
                if (itemDelta.isReplace()) {
                    LOGGER.trace("Non-tolerant item with resulting REPLACE delta => doing nothing");
                } else {
                    for (ItemValueWithOrigin<?,?> zeroSetIvwo: zeroSet) {
                        // TODO aren't values added twice (regarding addValuesToAdd called ~10 lines above)?
                        itemDelta.addValuesToAdd(LensUtil.cloneAndApplyMetadata(zeroSetIvwo.getItemValue(), isAssignment, zeroSetIvwo.getMapping()));
                    }
                    itemDelta.addToReplaceDelta();
                    LOGGER.trace("Non-tolerant item with resulting ADD delta => converted ADD to REPLACE values: {}", itemDelta.getValuesToReplace());
                }
                // To avoid phantom changes, compare with existing values (MID-2499).
                // TODO why we do this check only for non-tolerant items?
                if (isDeltaRedundant(targetObject, templateItemDefinition, itemDelta)) {
                    LOGGER.trace("Computed item delta is redundant => skipping it. Delta = \n{}", itemDelta.debugDumpLazily());
                    continue;
                }
                PrismUtil.setDeltaOldValue(targetObject, itemDelta);
            }

            itemDelta.simplify();
            itemDelta.validate(contextDescription);
            itemDeltas.add(itemDelta);
            LOGGER.trace("Computed delta:\n{}", itemDelta.debugDumpLazily());
        }
        this.itemDeltas = itemDeltas;
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
        MatchingRule<X> matchingRule = beans.matchingRuleRegistry.getMatchingRule(matchingRuleName, null);
        return propertyDelta.isRedundant(targetObject, EquivalenceStrategy.IGNORE_METADATA, matchingRule, false);
    }

    public Collection<ItemDelta<?, ?>> getItemDeltas() {
        return itemDeltas;
    }
}

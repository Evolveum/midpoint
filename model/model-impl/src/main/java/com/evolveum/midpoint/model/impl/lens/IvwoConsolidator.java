/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import static java.util.Collections.singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.util.Holder;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.projector.ValueMatcher;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;

import org.jetbrains.annotations.Nullable;

/**
 * Consolidate the mappings of a single item to a delta. It takes the convenient structure of ItemValueWithOrigin triple.
 * It produces the delta considering the mapping exclusion, authoritativeness and strength.
 *
 * filterExistingValues: if true, then values that already exist in the item are not added (and those that don't exist are not removed)
 *
 * @author semancik
 */
public class IvwoConsolidator<V extends PrismValue, D extends ItemDefinition, I extends ItemValueWithOrigin<V,D>> {

    private static final Trace LOGGER = TraceManager.getTrace(IvwoConsolidator.class);

    /**
     * Path of item being consolidated.
     */
    private final ItemPath itemPath;

    /**
     * Is the item an assignment (i.e. is path = c:assignment)?
     * This information is needed when preparing an item to be added into the delta.
     */
    private final boolean isAssignment;

    /**
     * Delta set triple of values-with-origin. This is the primary input for the consolidation.
     */
    private final DeltaSetTriple<I> ivwoTriple;

    /**
     * Existing value(s) of the item. This is the secondary input for the consolidation.
     */
    private final Item<V,D> existingItem;

    /**
     * Is existing item empty?
     */
    private final boolean existingItemIsEmpty;

    /**
     * A priori (i.e. explicitly specified or previously computed) delta for the item. This is the tertiary
     * input for the consolidation.
     */
    private final ItemDelta<V,D> aprioriItemDelta;

    /**
     * Apriori delta is not empty.
     */
    private final boolean aprioriItemDeltaIsEmpty;

    /**
     * Value matcher used to compare values (for the purpose of consolidation).
     */
    private final ValueMatcher valueMatcher;

    /**
     * Comparator used to compare values. Used if valueMatcher is null or cannot be used (because item is not a property).
     */
    private final Comparator<V> comparator;

    /**
     * Whether we want to consider values from zero set as being added. (Typically during object ADD operations.)
     */
    private final boolean addUnchangedValues;

    /**
     * Whether we want to consider values from zero set as being added, except for normal, source-ful mappings.
     */
    private final boolean addUnchangedValuesExceptForNormalMappings;

    /**
     * If true, we skip adding values that are already present in the item
     * as well as removing values that are not present in the item.
     *
     * (Setting this to false is important e.g. when we do not have full projection information.
     * Therefore we do not want to skip removing something just because we falsely think it's not there.)
     */
    private final boolean hasExistingItem;

    /**
     * What mappings are to be considered?
     */
    private final StrengthSelector strengthSelector;

    /**
     * This is a property of resource item:
     *
     * When set to false then both strong and normal mapping values are merged to produce the final set of values.
     * When set to true only strong values are used if there is at least one strong mapping. Normal values are
     * used if there is no strong mapping.
     */
    private final boolean itemIsExclusiveStrong;

    /**
     * Whether we should ignore values produced by normal-strength mappings.
     * This is the case if exclusiveStrong is true and there is at least one value produced by strong mapping.
     * TODO Currently checked for ADD values. Should we use the same approach for DELETE or unchanged values?
     */
    private final boolean ignoreNormalMappings;

    /**
     * Description of the context.
     */
    private final String contextDescription;

    /**
     * The output.
     */
    @NotNull private final ItemDelta<V,D> itemDelta;

    IvwoConsolidator(IvwoConsolidatorBuilder<V, D, I> builder) {
        itemPath = builder.itemPath;
        isAssignment = FocusType.F_ASSIGNMENT.equivalent(itemPath);

        ivwoTriple = builder.ivwoTriple;

        if (builder.existingItem != null) {
            existingItem = builder.existingItem;
        } else if (builder.itemContainer != null) {
            existingItem = builder.itemContainer.findItem(itemPath);
        } else {
            existingItem = null;
        }

        existingItemIsEmpty = existingItem == null || existingItem.isEmpty(); // should be hasNoValues, perhaps!

        aprioriItemDelta = builder.aprioriItemDelta;
        aprioriItemDeltaIsEmpty = aprioriItemDelta == null || aprioriItemDelta.isEmpty();

        valueMatcher = builder.valueMatcher;
        comparator = builder.comparator;

        addUnchangedValues = builder.addUnchangedValues;
        addUnchangedValuesExceptForNormalMappings = builder.addUnchangedValuesExceptForNormalMappings;

        hasExistingItem = builder.hasExistingItem;

        strengthSelector = builder.strengthSelector;
        itemIsExclusiveStrong = builder.isExclusiveStrong;
        ignoreNormalMappings = computeIgnoreNormalMappings();

        contextDescription = builder.contextDescription;

        //noinspection unchecked
        itemDelta = builder.itemDefinition.createEmptyDelta(itemPath);
    }

    @NotNull
    public ItemDelta<V,D> consolidateToDelta() throws ExpressionEvaluationException, PolicyViolationException, SchemaException {

        logStart();
        if (strengthSelector.isNone()) {
            LOGGER.trace("Consolidation of {} skipped as strength selector is 'none'", itemPath);
        } else {
            // We will process each value individually. I really mean each value. This whole method deals with
            // a single item (e.g. attribute). But this loop iterates over every potential value of that item.
            Collection<V> allValues = collectAllValues();
            for (V value : allValues) {
                consolidate(value);
            }

            if (!newItemWillHaveAnyValue()) {
                // The application of computed delta results in no value, apply weak mappings
                applyWeakMappings();
            } else {
                LOGGER.trace("Item {} will have some values in {}, weak mapping processing skipped", itemPath, contextDescription);
            }
        }
        setEstimatedOldValues();
        logEnd();

        return itemDelta;
    }

    private void consolidate(V value) throws PolicyViolationException, ExpressionEvaluationException, SchemaException {
        new ValueConsolidation(value).consolidate();
    }

    private void setEstimatedOldValues() {
        if (existingItem != null) {
            List<V> existingValues = existingItem.getValues();
            itemDelta.setEstimatedOldValues(PrismValueCollectionsUtil.cloneCollection(existingValues));
        }
    }

    private void applyWeakMappings() throws SchemaException {
        Collection<ItemValueWithOrigin<V,D>> valuesToAdd = selectWeakNonNegativeValues();
        LOGGER.trace("No value for item {} in {}, weak mapping processing yielded values: {}",
                itemPath, contextDescription, valuesToAdd);
        for (ItemValueWithOrigin<V, D> valueWithOrigin : valuesToAdd) {
            itemDelta.addValueToAdd(LensUtil.cloneAndApplyMetadata(valueWithOrigin.getItemValue(), isAssignment, singleton(valueWithOrigin)));
        }
    }

    private Collection<ItemValueWithOrigin<V,D>> selectWeakNonNegativeValues() {
        Collection<I> nonNegativeIvwos = ivwoTriple.getNonNegativeValues();

        // The distinction here is necessary when dealing with outbound mappings; these are reconciled
        // at the end, where all mappings were processed. So, if a value was given by assignment-based construction
        // then we should use that one. If not, we need to check resource-based outbound mappings.

        Collection<ItemValueWithOrigin<V,D>> weakFromAssignments = selectWeakValues(nonNegativeIvwos, OriginType.ASSIGNMENTS);
        if (!weakFromAssignments.isEmpty()) {
            return weakFromAssignments;
        }
        Collection<ItemValueWithOrigin<V,D>> weakFromOutbounds = selectWeakValues(nonNegativeIvwos, OriginType.OUTBOUND);
        if (!weakFromOutbounds.isEmpty()) {
            return weakFromOutbounds;
        }
        return selectWeakValues(nonNegativeIvwos, null);
    }

    private Collection<ItemValueWithOrigin<V,D>> selectWeakValues(Collection<? extends ItemValueWithOrigin<V,D>> ivwos, OriginType origin) {
        Collection<ItemValueWithOrigin<V,D>> values = new ArrayList<>();
        if (strengthSelector.isWeak()) {
            for (ItemValueWithOrigin<V, D> ivwo : ivwos) {
                if (ivwo.getMapping().getStrength() == MappingStrengthType.WEAK &&
                        (origin == null || origin == ivwo.getItemValue().getOriginType())) {
                    values.add(ivwo);
                }
            }
        }
        return values;
    }

    private class ValueConsolidation {

        /**
         * Value being consolidated.
         */
        private final V value0;

        private final Collection<ItemValueWithOrigin<V,D>> zeroIvwos;
        private final Collection<ItemValueWithOrigin<V,D>> plusIvwos;
        private final Collection<ItemValueWithOrigin<V,D>> minusIvwos;

        private final V existingValue;

        /**
         * Reasons (IVwOs) that indicate the need to add the value to the item.
         * These are IVwOs from plus set, optionally combined with IVwOs from zero set (if addUnchangedValues is true).
         */
        private Collection<ItemValueWithOrigin<V, D>> reasonsToAdd;

        /**
         * In cases we check for exclusiveness we need to store exclusive mapping we have found here.
         * See {@link #classifyMappings(Collection, boolean)}.
         */
        private PrismValueDeltaSetTripleProducer<V, D> exclusiveMapping;

        /**
         * In the set of relevant mappings (can be different for different situations),
         * do we have only weak mappings? See {@link #classifyMappings(Collection, boolean)}.
         */
        private boolean hasOnlyWeakMappings;

        /**
         * In the set of relevant mappings (can be different for different situations),
         * do we have at least one strong mapping? See {@link #classifyMappings(Collection, boolean)}.
         */
        private boolean hasAtLeastOneStrongMapping;

        /**
         * Should we skip metadata computation for this value?
         */
        private boolean skipMetadataComputation;

        private ValueConsolidation(V value0) throws SchemaException {
            this.value0 = value0;

            LOGGER.trace("  consolidating value: {}", value0);
            // Check what to do with the value using the usual "triple routine". It means that if a value is
            // in zero set than we need no delta, plus set means add delta and minus set means delete delta.
            // The first set that the value is present determines the result.
            //
            // We ignore values from invalid constructions, except for a special case of valid->invalid values in minus set.
            zeroIvwos = collectIvwosFromSet(value0, ivwoTriple.getZeroSet(), false);
            plusIvwos = collectIvwosFromSet(value0, ivwoTriple.getPlusSet(), false);
            minusIvwos = collectIvwosFromSet(value0, ivwoTriple.getMinusSet(), true);

            existingValue = findValueInExistingItem();

            LOGGER.trace("PVWOs for value {}:\nzero = {}\nplus = {}\nminus = {}", value0, zeroIvwos, plusIvwos, minusIvwos);
        }

        private void consolidate() throws PolicyViolationException, ExpressionEvaluationException, SchemaException {

            checkDeletionOfStrongValue();

            findReasonsToAdd();
            if (!reasonsToAdd.isEmpty()) {
                new AddConsolidation().consolidate();
            } else {
                new DeleteConsolidation().consolidate();
            }
        }

        private void findReasonsToAdd() {
            reasonsToAdd = new HashSet<>(plusIvwos);
            if (addUnchangedValues) {
                reasonsToAdd.addAll(zeroIvwos);
            } else if (addUnchangedValuesExceptForNormalMappings) {
                for (ItemValueWithOrigin<V, D> zeroIvwo : zeroIvwos) {
                    if (zeroIvwo.isStrong() || zeroIvwo.isNormal() && zeroIvwo.isSourceless() || zeroIvwo.isWeak()) {
                        reasonsToAdd.add(zeroIvwo);
                    }
                }
            } else {
                reasonsToAdd = plusIvwos;
            }
        }

        private void checkMappingExclusiveness(PrismValueDeltaSetTripleProducer<V, D> mapping) throws ExpressionEvaluationException {
            if (exclusiveMapping == null) {
                exclusiveMapping = mapping;
            } else {
                String message = "Exclusion conflict in " + contextDescription + ", item " + itemPath +
                        ", conflicting constructions: " + exclusiveMapping + " and " + mapping;
                LOGGER.error(message);
                throw new ExpressionEvaluationException(message);
            }
        }

        private void checkDeletionOfStrongValue() throws PolicyViolationException {
            if (aprioriItemDelta != null && aprioriItemDelta.isValueToDelete(value0, true)) {
                checkIfStrong(zeroIvwos);
                checkIfStrong(plusIvwos);
            }
        }

        private void checkIfStrong(Collection<ItemValueWithOrigin<V, D>> ivwos) throws PolicyViolationException {
            PrismValueDeltaSetTripleProducer<V, D> strongMapping = findStrongMapping(ivwos);
            if (strongMapping != null) {
                throw new PolicyViolationException("Attempt to delete value " + value0 + " from item " + itemPath
                        + " but that value is mandated by a strong " + strongMapping.toHumanReadableDescription()
                        + " (for " + contextDescription + ")");
            }
        }

        @Nullable
        private PrismValueDeltaSetTripleProducer<V, D> findStrongMapping(Collection<ItemValueWithOrigin<V, D>> ivwos) {
            for (ItemValueWithOrigin<V,D> pvwo : MiscUtil.emptyIfNull(ivwos)) {
                PrismValueDeltaSetTripleProducer<V,D> mapping = pvwo.getMapping();
                if (mapping.getStrength() == MappingStrengthType.STRONG) {
                    return mapping;
                }
            }
            return null;
        }

        private V findValueInExistingItem() {
            if (existingItem == null) {
                return null;
            }
            if (valueMatcher != null && value0 instanceof PrismPropertyValue) {
                //noinspection unchecked
                return (V) valueMatcher.findValue((PrismProperty)existingItem, (PrismPropertyValue) value0);
            } else {
                return existingItem.findValue(value0, EquivalenceStrategy.IGNORE_METADATA, comparator);
            }
        }

        private class AddConsolidation {

            /**
             * We know we have a reason to add the value: either a mapping that returned this value in its plus set,
             * or a mapping that returned it in zero set with "addUnchangedValues" being true. (Typically during object ADD
             * operation.)
             *
             * So let us sort out the mappings to learn about the presence of weak-normal-strong ones and decide
             * on adding/not-adding the value using this information.
             */
            private void consolidate() throws ExpressionEvaluationException, SchemaException {
                classifyMappings(reasonsToAdd, true);
                if (shouldAddValue()) {
                    itemDelta.addValueToAdd(LensUtil.cloneAndApplyMetadata(value0, isAssignment, reasonsToAdd));
                }
            }

            private boolean shouldAddValue() {
                if (hasOnlyWeakMappings) {
                    LOGGER.trace("Value {} mapping is weak in item {}, postponing processing in {}",
                            value0, itemPath, contextDescription);
                    return false;
                }

                if (!hasExistingItem && hasZeroMapping()) {
                    // We use this approximate check only if we don't know current item value
                    LOGGER.trace("Value {} in item {} has zero mapping, so we assume it already exists, skipping processing in {}",
                            value0, itemPath, contextDescription);
                    return false;
                }

                if (!hasAtLeastOneStrongMapping) {
                    if (ignoreNormalMappings) {
                        LOGGER.trace("Value {} mapping is normal in item {} and we have exclusiveStrong, skipping processing in {}",
                                value0, itemPath, contextDescription);
                        return false;
                    }
                    if (!aprioriItemDeltaIsEmpty) {
                        LOGGER.trace("Value {} mapping is not strong and the item {} already has a delta; " +
                                "skipping adding in {} (also skipping value metadata computation)",
                                value0, itemPath, contextDescription);
                        skipMetadataComputation = true;
                        return false;
                    }
                }

                if (hasExistingItem && existingValue != null) {
                    LOGGER.trace("Value {} NOT added to delta for item {} because the item already has that value in {}",
                            value0, itemPath, contextDescription);
                    return false;
                }

                LOGGER.trace("Decided to ADD value {} to delta for item {} in {}", value0, itemPath, contextDescription);
                return true;
            }
        }

        private boolean hasZeroMapping() {
            return zeroIvwos.stream()
                    .anyMatch(zeroIvwo -> !zeroIvwo.isWeak());
        }

        private class DeleteConsolidation {

            /**
             * We have no reason to add the value based on the mappings. Let's check the other options.
             */
            private void consolidate() throws ExpressionEvaluationException {

                // Values that are both in plus and minus are considered to have a reason to be added.
                // This is covered by "AddConsolidation" case above.
                assert plusIvwos.isEmpty();

                if (zeroIvwos.isEmpty() && !minusIvwos.isEmpty()) {

                    LOGGER.trace("Nothing to compute value metadata from (zero and plus set is empty)");
                    skipMetadataComputation = true;

                    classifyMappings(minusIvwos, false);
                    if (shouldDeleteValue()) {
                        //noinspection unchecked
                        itemDelta.addValueToDelete((V) value0.clone());
                    }
                }
            }

            private boolean shouldDeleteValue() {

                if (hasOnlyWeakMappings && !existingItemIsEmpty) {
                    LOGGER.trace("Value {} mapping is weak and the item {} already has a value, skipping deletion in {}",
                            value0, itemPath, contextDescription);
                    return false;
                }

                assert !hasOnlyWeakMappings || strengthSelector.isWeak() :
                        "If we are ordered to skip weak mappings and all mappings in"
                            + " minusIvwos are weak then minusIvwos must be empty set and so we are not here";

                if (!hasAtLeastOneStrongMapping && !aprioriItemDeltaIsEmpty) {
                    LOGGER.trace("Value {} mapping is not strong and the item {} already has a delta, skipping deletion in {}",
                            value0, itemPath, contextDescription);
                    return false;
                }

                if (hasExistingItem && existingValue == null) {
                    LOGGER.trace("Value {} NOT add to delta as DELETE because item {} the item does not have that value in {} (matcher: {})",
                            value0, itemPath, contextDescription, valueMatcher);
                    return false;
                }
                LOGGER.trace("Value {} added to delta as DELETE for item {} in {}", value0, itemPath, contextDescription);
                return true;
            }
        }

        private void classifyMappings(Collection<ItemValueWithOrigin<V, D>> relevantIvwos, boolean checkExclusiveness)
                throws ExpressionEvaluationException {
            hasOnlyWeakMappings = true;
            hasAtLeastOneStrongMapping = false;
            exclusiveMapping = null;
            for (ItemValueWithOrigin<V,D> ivwo : relevantIvwos) {
                PrismValueDeltaSetTripleProducer<V,D> mapping = ivwo.getMapping();
                if (mapping.getStrength() != MappingStrengthType.WEAK) {
                    hasOnlyWeakMappings = false;
                }
                if (mapping.getStrength() == MappingStrengthType.STRONG) {
                    hasAtLeastOneStrongMapping = true;
                }
                if (checkExclusiveness && mapping.isExclusive()) {
                    checkMappingExclusiveness(mapping);
                }
            }
        }
    }

    private boolean computeIgnoreNormalMappings() {
        if (!itemIsExclusiveStrong) {
            return false;
        } else if (strengthSelector.isNone()) {
            return true; // just an optimization
        } else {
            Holder<Boolean> resultHolder = new Holder<>(false);
            SimpleVisitor<I> visitor = pvwo -> {
                if (pvwo.getMapping().getStrength() == MappingStrengthType.STRONG) {
                    resultHolder.setValue(true);
                }
            };
            ivwoTriple.simpleAccept(visitor);
            return resultHolder.getValue();
        }
    }

    private void logStart() {
        LOGGER.trace("Consolidating {} IVwO triple:\n{}\n  Apriori Delta:\n{}\n  Existing item:\n{}\n  Parameters:\n"
                        + "   - addUnchangedValues: {}\n"
                        + "   - hasExistingItem: {}\n"
                        + "   - isExclusiveStrong: {}\n"
                        + "   - strengthSelector: {}\n"
                        + "   - valueMatcher: {}\n"
                        + "   - comparator: {}",
                itemPath, ivwoTriple.debugDumpLazily(1),
                DebugUtil.debugDumpLazily(aprioriItemDelta, 2),
                DebugUtil.debugDumpLazily(existingItem, 2),
                addUnchangedValues, hasExistingItem, itemIsExclusiveStrong, strengthSelector, valueMatcher, comparator);
    }

    private void logEnd() {
        LOGGER.trace("Consolidated {} IVwO triple to delta:\n{}", itemPath, itemDelta.debugDumpLazily(1));
    }

    private Collection<V> collectAllValues() throws SchemaException {
        Collection<V> allValues = new HashSet<>();
        collectAllValuesFromSet(allValues, ivwoTriple.getZeroSet());
        collectAllValuesFromSet(allValues, ivwoTriple.getPlusSet());
        collectAllValuesFromSet(allValues, ivwoTriple.getMinusSet());
        return allValues;
    }

    private void collectAllValuesFromSet(Collection<V> allValues, Collection<? extends ItemValueWithOrigin<V,D>> collection)
            throws SchemaException {
        for (ItemValueWithOrigin<V,D> pvwo : emptyIfNull(collection)) {
            V pval = pvwo.getItemValue();
            if (valueMatcher != null) {
                addWithValueMatcherCheck(allValues, pval);
            } else {
                addWithRealValueCheck(allValues, pval);
            }
        }
    }

    private <T> void addWithValueMatcherCheck(Collection<V> allValues, V pval) throws SchemaException {
        for (V valueFromAllValues: allValues) {
            //noinspection unchecked
            if (valueMatcher.match(((PrismPropertyValue<T>)valueFromAllValues).getValue(),
                    ((PrismPropertyValue<T>)pval).getValue())) {
                return;
            }
        }
        allValues.add(pval);
    }

    private void addWithRealValueCheck(Collection<V> allValues, V pval) {
        if (!PrismValueCollectionsUtil.containsRealValue(allValues, pval)) {
            allValues.add(pval);
        }
    }

    /**
     * @param keepValidInvalid If true, values that originated in constructions that were valid but are invalid now, are kept.
     *                         If false, such values are skipped.
     *
     *                         It is used for collecting data from minus sets. I.e. values being deleted (originating in now-invalid constructions).
     *                         (The reason is not quite clear to me.)
     *
     */
    private Collection<ItemValueWithOrigin<V,D>> collectIvwosFromSet(V pvalue, Collection<? extends ItemValueWithOrigin<V,D>> deltaSet, boolean keepValidInvalid) throws SchemaException {
        Collection<ItemValueWithOrigin<V,D>> ivwos = new ArrayList<>();
        for (ItemValueWithOrigin<V,D> setIvwo : deltaSet) {
            if (shouldSkipMapping(setIvwo.getMapping().getStrength())) {
                continue;
            }
            if (!setIvwo.isValid()) {
                if (!keepValidInvalid) {
                    continue;
                }
                if (!setIvwo.wasValid()) {
                    continue;
                }
                // valid -> invalid change. E.g. disabled assignment. We need to process that
            }
            //noinspection unchecked
            if (setIvwo.equalsRealValue(pvalue, valueMatcher)) {
                ivwos.add(setIvwo);
            }
        }
        return ivwos;
    }

    private boolean shouldSkipMapping(MappingStrengthType mappingStrength) {
        return mappingStrength == MappingStrengthType.STRONG && !strengthSelector.isStrong() ||
                mappingStrength == MappingStrengthType.NORMAL && !strengthSelector.isNormal() ||
                mappingStrength == MappingStrengthType.WEAK && !strengthSelector.isWeak();
    }

    /**
     * After application of the computed delta, will the item have any value?
     */
    private boolean newItemWillHaveAnyValue() throws SchemaException {
        if (existingItem == null || existingItem.isEmpty()) {
            return itemDelta.addsAnyValue();
        } else if (itemDelta.isEmpty()) {
            return true;
        } else {
            //noinspection unchecked
            Item<V,D> clonedItem = existingItem.clone();
            itemDelta.applyToMatchingPath(clonedItem, ParameterizedEquivalenceStrategy.DEFAULT_FOR_DELTA_APPLICATION);
            return !clonedItem.isEmpty();
        }
    }
}

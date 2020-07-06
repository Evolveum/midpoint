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

import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
     * This is the behavior originally present in ObjectTemplateProcessor, then moved to DeltaSetTripleConsolidation,
     * then moved here. We should somehow integrate it with the rest of consolidation processing. (Including
     * finding better name.)
     */
    private final boolean specialZeroSetProcessing;

    /**
     * If true, we skip adding values that are already present in the item
     * as well as removing values that are not present in the item.
     *
     * (Setting this to false is important e.g. when we do not have full projection information.
     * Therefore we do not want to skip removing something just because we falsely think it's not there.)
     */
    private final boolean filterExistingValues;

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
        filterExistingValues = builder.filterExistingValues;
        specialZeroSetProcessing = builder.specialZeroSetProcessing;

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

            if (specialZeroSetProcessing) {
                doSpecialZeroSetProcessing();
            }

            // TODO What about skipped consolidation (resulting in empty delta)?
            //  Shouldn't we set estimated old values also there?
            setEstimatedOldValues();
        }
        logEnd();

        return itemDelta;
    }

    private void doSpecialZeroSetProcessing() throws SchemaException {
        Collection<I> zeroSet = ivwoTriple.getZeroSet();
        for (I zeroSetIvwo: zeroSet) {

            PrismValueDeltaSetTripleProducer<?, ?> mapping = zeroSetIvwo.getMapping();
            if (mapping.getStrength() == null || mapping.getStrength() == MappingStrengthType.NORMAL) {
                if (aprioriItemDeltaIsEmpty && mapping.isSourceless()) {
                    LOGGER.trace("Considering adding zero values from normal mapping {} (sourceless, no apriori delta)", mapping);
                    addValueIfNotThere(zeroSetIvwo);
                }
            } else if (mapping.getStrength() == MappingStrengthType.WEAK) {
                if (existingItemIsEmpty && !itemDelta.addsAnyValue()) {
                    LOGGER.trace("Considering adding zero values from weak mapping {} (existing item empty, itemDelta "
                            + "does not add anything: {})", mapping, itemDelta);
                    addValueIfNotThere(zeroSetIvwo);
                }
            } else {
                LOGGER.trace("Considering adding zero values from strong mapping {}", mapping);
                addValueIfNotThere(zeroSetIvwo);
            }
        }
    }

    private void addValueIfNotThere(I zeroSetIvwo) throws SchemaException {
        V value = zeroSetIvwo.getItemValue();
        if (existingItem == null || !existingItem.contains(value, EquivalenceStrategy.REAL_VALUE)) {
            LOGGER.trace(" -> Reconciliation will add value {} for item {}. Existing item: {}", value, itemPath, existingItem);
            boolean isAssignment = SchemaConstants.PATH_ASSIGNMENT.equivalent(itemPath);
            //noinspection unchecked
            itemDelta.addValuesToAdd(LensUtil.cloneAndApplyMetadata(value, isAssignment, zeroSetIvwo.getMapping()));
        } else {
            LOGGER.trace(" -> ...but the value is already in the delta, not adding it.");
        }
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
        Collection<? extends ItemValueWithOrigin<V,D>> nonNegativeIvwos = ivwoTriple.getNonNegativeValues();

        // TODO why we select values from ASSIGNMENTS, then OUTBOUND, then all?
        Collection<ItemValueWithOrigin<V,D>> valuesToAdd = selectWeakValues(nonNegativeIvwos, OriginType.ASSIGNMENTS);
        if (valuesToAdd.isEmpty()) {
            valuesToAdd = selectWeakValues(nonNegativeIvwos, OriginType.OUTBOUND);
        }
        if (valuesToAdd.isEmpty()) {
            valuesToAdd = selectWeakValues(nonNegativeIvwos, null);
        }
        LOGGER.trace("No value for item {} in {}, weak mapping processing yielded values: {}",
                itemPath, contextDescription, valuesToAdd);
        for (ItemValueWithOrigin<V, D> valueWithOrigin : valuesToAdd) {
            itemDelta.addValueToAdd(LensUtil.cloneAndApplyMetadata(valueWithOrigin.getItemValue(), isAssignment, singleton(valueWithOrigin)));
        }
    }

    private class ValueConsolidation {

        /**
         * Value being consolidated.
         */
        private final V value0;

        private final Collection<ItemValueWithOrigin<V,D>> zeroIvwos;
        private final Collection<ItemValueWithOrigin<V,D>> plusIvwos;
        private final Collection<ItemValueWithOrigin<V,D>> minusIvwos;

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

            LOGGER.trace("PVWOs for value {}:\nzero = {}\nplus = {}\nminus = {}", value0, zeroIvwos, plusIvwos, minusIvwos);
        }

        private void consolidate() throws PolicyViolationException, ExpressionEvaluationException, SchemaException {

            checkDeletionOfZeroStrongValue();

            if (!zeroIvwos.isEmpty() && !addUnchangedValues) {
                LOGGER.trace("Value {} unchanged, doing nothing", value0);
                applyMetadataOnUnchangedValue(); // TODO
                return;
            }

            if (!findReasonsToAdd().isEmpty()) {
                new AddConsolidation().consolidate();
            } else {
                new NoAddConsolidation().consolidate();
            }
        }

        private Collection<ItemValueWithOrigin<V, D>> findReasonsToAdd() {
            if (addUnchangedValues) {
                //noinspection unchecked
                reasonsToAdd = MiscUtil.union(zeroIvwos, plusIvwos);
            } else {
                reasonsToAdd = plusIvwos;
            }
            return reasonsToAdd;
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

        private void checkDeletionOfZeroStrongValue() throws PolicyViolationException {
            PrismValueDeltaSetTripleProducer<V, D> zeroStrongMapping = findStrongMappingAmongZeroIvwos();

            // We perhaps should not rely on current apriori delta, as it contains both primary and secondary changes.
            if (zeroStrongMapping != null && aprioriItemDelta != null && aprioriItemDelta.isValueToDelete(value0, true)) {
                throw new PolicyViolationException("Attempt to delete value " + value0 + " from item " + itemPath
                        + " but that value is mandated by a strong " + zeroStrongMapping.toHumanReadableDescription()
                        + " (for " + contextDescription + ")");
            }
        }

        @Nullable
        private PrismValueDeltaSetTripleProducer<V, D> findStrongMappingAmongZeroIvwos() {
            for (ItemValueWithOrigin<V,D> pvwo : MiscUtil.emptyIfNull(zeroIvwos)) {
                PrismValueDeltaSetTripleProducer<V,D> mapping = pvwo.getMapping();
                if (mapping.getStrength() == MappingStrengthType.STRONG) {
                    return mapping;
                }
            }
            return null;
        }

        // TEMPORARY!
        private void applyMetadataOnUnchangedValue() {
            if (!value0.getValueMetadata().isEmpty()) {
                V existingValue = findValueInExistingItem();
                if (existingValue != null) {
                    applyValueMetadata(existingValue, value0);
                }
            } else {
                LOGGER.info("Huh? Value is not there: {}", value0); // FIXME
            }
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
            private void consolidate() throws ExpressionEvaluationException, PolicyViolationException, SchemaException {
                classifyMappings(reasonsToAdd, true);
                if (shouldAddValue()) {
                    itemDelta.addValueToAdd(LensUtil.cloneAndApplyMetadata(value0, isAssignment, reasonsToAdd));
                }
            }

            private boolean shouldAddValue() throws PolicyViolationException {
                if (hasOnlyWeakMappings) {
                    LOGGER.trace("Value {} mapping is weak in item {}, postponing processing in {}",
                            value0, itemPath, contextDescription);
                    return false;
                }

                // If strength selector for normal + strong is off, we see only weak mappings and so we are not here.
                assert strengthSelector.isNormal() || strengthSelector.isStrong();

                if (hasAtLeastOneStrongMapping) {
                    // We perhaps should not rely on current apriori delta, as it contains both primary and secondary changes.
                    if (aprioriItemDelta != null && aprioriItemDelta.isValueToDelete(value0, true)) {
                        throw new PolicyViolationException("Attempt to delete value "+ value0 +" from item "+itemPath
                                +" but that value is mandated by a strong mapping (in "+contextDescription+")");
                    }
                } else {
                    if (ignoreNormalMappings) {
                        LOGGER.trace("Value {} mapping is normal in item {} and we have exclusiveStrong, skipping processing in {}",
                                value0, itemPath, contextDescription);
                        return false;
                    }
                    if (!aprioriItemDeltaIsEmpty) {
                        LOGGER.trace("Value {} mapping is not strong and the item {} already has a delta; " +
                                "skipping adding in {}", value0, itemPath, contextDescription);
                        return false;
                    }
                }
                if (filterExistingValues) {
                    V existingValue = findValueInExistingItem();
                    if (existingValue != null) {
                        LOGGER.trace("Value {} NOT added to delta for item {} because the item already has that value in {}",
                                value0, itemPath, contextDescription);
                        applyValueMetadata(existingValue, value0);
                        return false;
                    }
                }
                LOGGER.trace("Decided to ADD value {} to delta for item {} in {}", value0, itemPath, contextDescription);
                return true;
            }
        }

        private class NoAddConsolidation {

            /**
             * We have no reason to add the value based on the mappings. Let's check the other options.
             */
            private void consolidate() throws SchemaException, ExpressionEvaluationException {

                // We need to check for empty plus set. Values that are both in plus and minus are considered
                // to have a reason to be added. This is covered by "AddConsolidation" case above.
                assert plusIvwos.isEmpty();

                if (!minusIvwos.isEmpty()) {
                    classifyMappings(minusIvwos, false);
                    if (shouldDeleteValue()) {
                        //noinspection unchecked
                        itemDelta.addValueToDelete((V) value0.clone());
                    }
                }

//                if (!zeroIvwos.isEmpty()) {
//                    classifyMappings(zeroIvwos, false);
//                    if (shouldAddUnchangedValue()) {
//                        itemDelta.addValueToAdd(LensUtil.cloneAndApplyMetadata(value0, isAssignment, zeroIvwos));
//                    }
//                }
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

                if (filterExistingValues && !isValueInExistingItem()) {
                    LOGGER.trace("Value {} NOT add to delta as DELETE because item {} the item does not have that value in {} (matcher: {})",
                            value0, itemPath, contextDescription, valueMatcher);
                    return false;
                }
                LOGGER.trace("Value {} added to delta as DELETE for item {} in {}", value0, itemPath, contextDescription);
                return true;
            }

            private boolean shouldAddUnchangedValue() {
                assert !addUnchangedValues : "This case was treated in AddConsolidation class";

                if (hasAtLeastOneStrongMapping && aprioriItemDelta != null && aprioriItemDelta.isReplace()) {

                    // For focus mappings (template, persona, assignments) we re-add zero values for all strong mappings
                    // so this branch is not relevant. For outbound mappings we force addUnchangedValues if apriori delta
                    // is REPLACE, so this is not relevant either. And inbound mappings do not use this consolidator.
                    // So this code is useless.

                    // Any strong mappings in the zero set needs to be re-applied as otherwise the replace will destroy it
                    LOGGER.trace("Value {} added to delta for item {} in {} because there is strong mapping in the zero set",
                            value0, itemPath, contextDescription);
                    return true;
                } else {
                    return false;
                }
            }

            private boolean isValueInExistingItem() {
                if (existingItem == null) {
                    return false;
                } else if (valueMatcher != null && value0 instanceof PrismPropertyValue) {
                    //noinspection unchecked
                    return valueMatcher.hasRealValue((PrismProperty)existingItem, (PrismPropertyValue) value0);
                } else {
                    return existingItem.contains(value0, EquivalenceStrategy.IGNORE_METADATA, comparator);
                }
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
                        + "   - filterExistingValues: {}\n"
                        + "   - isExclusiveStrong: {}\n"
                        + "   - strengthSelector: {}\n"
                        + "   - valueMatcher: {}\n"
                        + "   - comparator: {}",
                itemPath, ivwoTriple.debugDumpLazily(1),
                DebugUtil.debugDumpLazily(aprioriItemDelta, 2),
                DebugUtil.debugDumpLazily(existingItem, 2),
                addUnchangedValues, filterExistingValues, itemIsExclusiveStrong, strengthSelector, valueMatcher, comparator);
    }

    private void logEnd() {
        LOGGER.trace("Consolidated {} IVwO triple to delta:\n{}", itemPath, itemDelta.debugDumpLazily(1));
    }

    private void applyValueMetadata(V existingValue, V value) {
        if (!value.getValueMetadata().isEmpty()) {
            LOGGER.info("Copying value metadata to value {} for item {} in {}",
                    existingValue, itemPath, contextDescription);
            existingValue.setValueMetadata(value.getValueMetadata().clone()); // TODO merge instead
        }
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

/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.prism.PrismContainerValue.asPrismContainerValues;
import static com.evolveum.midpoint.util.MiscUtil.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;
import com.evolveum.midpoint.util.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ConsolidationValueMetadataComputer;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.metadata.MidpointProvenanceEquivalenceStrategy;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.DeltaSetTripleType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ItemType;

import org.jetbrains.annotations.Nullable;

/**
 * Consolidate the output of mappings for a single item to a delta.
 * It takes the convenient structure of {@link ItemValueWithOrigin} triple and produces the delta considering
 * the mapping exclusion, authoritativeness and strength. See {@link #consolidateTriples()}.
 *
 * @author semancik
 */
public class IvwoConsolidator<V extends PrismValue, D extends ItemDefinition<?>, I extends ItemValueWithOrigin<V,D>> implements AutoCloseable {

    private static final Trace LOGGER = TraceManager.getTrace(IvwoConsolidator.class);

    private static final String OP_CONSOLIDATE_TO_DELTA = IvwoConsolidator.class.getName() + ".consolidateToDelta";

    /**
     * Path of item being consolidated.
     */
    private final ItemPath itemPath;

    /**
     * Categorization of all values (from existing item as well as from delta set triple)
     * into equivalence classes.
     */
    private final List<EquivalenceClass> equivalenceClasses = new ArrayList<>();

    /** This is to check whether there is no conflict for single-valued items, see MID-9621. */
    private final List<EquivalenceClass> equivalenceClassesBeingAddedForSingleValuedItem = new ArrayList<>();

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
     * If true, we know the values of existing item so we can filter values using this knowledge.
     *
     * (Setting this to false is important e.g. when we do not have full projection information.)
     */
    private final boolean existingItemKnown;

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
    private final boolean itemDeltaExists;

    /**
     * Comparator used to compare values.
     * It should be forgivable enough to allow illegal values to be compared.
     */
    private final EqualsChecker<V> equalsChecker;

    /**
     * What mappings are to be considered?
     */
    private final StrengthSelector strengthSelector;

    /**
     * Whether we want to consider values from zero set as being added. (Typically during object ADD operations.)
     */
    private final boolean addUnchangedValues;

    /**
     * Whether we want to consider values from zero set as being added, except for normal, source-ful mappings.
     */
    private final boolean addUnchangedValuesExceptForNormalMappings;

    /**
     * This is a property of resource item:
     *
     * When set to false then both strong and normal mapping values are merged to produce the final set of values.
     * When set to true only strong values are used if there is at least one strong mapping. Normal values are
     * used if there is no strong mapping.
     */
    private final boolean itemIsExclusiveStrong;

    /**
     * If true, deletes all existing values that have no origin. (Non-tolerant behavior.)
     */
    @Experimental
    private final boolean deleteExistingValues;

    /**
     * If true, "mapping is not strong and the item X already has a delta" exclusion rule is skipped.
     * Currently to be used for inbound mappings, because in the original consolidation algorithm
     * for these mappings there was no such rule.
     *
     * (The rule is a bit dubious anyway.)
     */
    @Experimental
    private final boolean skipNormalMappingAPrioriDeltaCheck;

    /**
     * Whether we should ignore values produced by normal-strength mappings.
     * This is the case if exclusiveStrong is true and there is at least one value produced by strong mapping.
     * TODO Currently checked for ADD values. Should we use the same approach for DELETE or unchanged values?
     */
    private final boolean ignoreNormalMappings;

    /**
     * Computer for value metadata.
     */
    private final ConsolidationValueMetadataComputer valueMetadataComputer;

    /**
     * Description of the context.
     */
    private final String contextDescription;

    /** Definition of the item being consolidated. */
    @NotNull private final D itemDefinition;

    /**
     * Operation result (currently needed for value metadata computation).
     * Experimentally we make this consolidator auto-closeable so the result is marked as closed automatically.
     * This works - except for the automatic reporting of exceptions.
     *
     * TODO reconsider if we should not record processing in the caller (e.g. because ConsolidationProcessor
     *  already creates a result for item consolidation).
     */
    private final OperationResult result;

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
        itemDeltaExists = builder.itemDeltaExists;

        equalsChecker = builder.equalsChecker;

        addUnchangedValues = builder.addUnchangedValues;
        addUnchangedValuesExceptForNormalMappings = builder.addUnchangedValuesExceptForNormalMappings;

        existingItemKnown = builder.existingItemKnown;

        strengthSelector = builder.strengthSelector;
        itemIsExclusiveStrong = builder.isExclusiveStrong;
        deleteExistingValues = builder.deleteExistingValues;
        skipNormalMappingAPrioriDeltaCheck = builder.skipNormalMappingAPrioriDeltaCheck;
        ignoreNormalMappings = computeIgnoreNormalMappings();

        valueMetadataComputer = builder.valueMetadataComputer;
        contextDescription = builder.contextDescription;

        this.itemDefinition =
                argNonNull(
                        builder.itemDefinition,
                        "No definition for %s", itemPath);

        //noinspection unchecked
        itemDelta = (ItemDelta<V, D>) itemDefinition.createEmptyDelta(itemPath);

        // Must be the last instruction here (to avoid leaving result open in case of an exception)
        result = builder.result.createMinorSubresult(OP_CONSOLIDATE_TO_DELTA)
                .addArbitraryObjectAsParam("itemPath", itemPath);
    }

    /**
     * Simplified signature (exceptions thrown), assuming no value metadata computation is to be done.
     */
    @NotNull
    public ItemDelta<V,D> consolidateToDeltaNoMetadata() throws ExpressionEvaluationException, SchemaException {
        try {
            assert valueMetadataComputer == null;
            return consolidateTriples();
        } catch (ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException e) {
            throw new IllegalStateException("Unexpected exception: " + e.getMessage(), e);
        }
    }

    @NotNull
    public ItemDelta<V,D> consolidateTriples() throws ExpressionEvaluationException, SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {

        try {
            logStart();
            if (strengthSelector.isNone()) {
                LOGGER.trace("Consolidation of {} skipped as strength selector is 'none'", itemPath);
            } else {
                categorizeValues();
                for (EquivalenceClass equivalenceClass : equivalenceClasses) {
                    new ValueConsolidation(equivalenceClass).consolidate();
                }

                // Experimental. Should be part of consolidate() method.
                if (deleteExistingValues && existingItem != null) {
                    for (EquivalenceClass equivalenceClass : equivalenceClasses) {
                        consolidateExistingValue(equivalenceClass);
                    }
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

            if (equivalenceClassesBeingAddedForSingleValuedItem.size() > 1) {
                throw new SchemaException(
                        "Strong mappings provided more than one value for single-valued item %s: %s".formatted(
                                itemPath,
                                equivalenceClassesBeingAddedForSingleValuedItem.stream()
                                        .map(ec -> ec.getRepresentativeRealValue())
                                        .toList()));
            }

            return itemDelta;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        }
    }

    @Experimental
    private void consolidateExistingValue(EquivalenceClass equivalenceClass) {
        if (equivalenceClass.presentInExistingItem() && equivalenceClass.plusOrigins.isEmpty() &&
                equivalenceClass.zeroOrigins.isEmpty() && equivalenceClass.minusOrigins.isEmpty()) {
            LOGGER.trace("No origins -- removing the value");
            //noinspection unchecked
            itemDelta.addValueToDelete((V) equivalenceClass.getRepresentative().clone());
        }
    }

    private void setEstimatedOldValues() {
        if (existingItem != null) {
            itemDelta.setEstimatedOldValuesWithCloning(
                    existingItem.getValues());
        }
    }

    private void applyWeakMappings() throws SchemaException {
        Collection<I> valuesToAdd = selectWeakNonNegativeValues();
        LOGGER.trace("No value for item {} in {}, weak mapping processing yielded values: {}",
                itemPath, contextDescription, valuesToAdd);
        ValueCategorization categorization = new ValueCategorization(valuesToAdd);
        for (ValueCategorization.EquivalenceClassSimple equivalenceClass : categorization.equivalenceClasses) {
            V value = equivalenceClass.getRepresentative().getItemValue();
            // MD#5 - but metadata for weak mappings are not supported yet
//            computeValueMetadataOnDeltaValue(value, equivalenceClass::getMemberValues);
            itemDelta.addValueToAdd(cloneAndApplyAssignmentOrigin(value, isAssignment, equivalenceClass.members));
        }
    }

    private V cloneAndApplyAssignmentOrigin(
            V value, boolean isAssignment, Collection<? extends ItemValueWithOrigin<V, D>> origins) throws SchemaException {
        return cloneAndApplyAssignmentOrigin(value, isAssignment, () -> getAutoCreationIdentifier(origins));
    }

    private String getAutoCreationIdentifier(Collection<? extends ItemValueWithOrigin<V, D>> origins) {
        // let's ignore conflicts (name1 vs name2, named vs unnamed) for now
        for (ItemValueWithOrigin<V, D> origin : origins) {
            var id = origin.getMappingIdentifier();
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    private V cloneAndApplyAssignmentOrigin(V value, boolean isAssignment,
            Supplier<String> originMappingNameSupplier) throws SchemaException {
        //noinspection unchecked
        V cloned = (V) value.clone();
        if (isAssignment && cloned instanceof PrismContainerValue<?> clonedPcv) {
            clonedPcv.setId(null);
            String originMappingName = originMappingNameSupplier.get();
            LOGGER.trace("cloneAndApplyMetadata: originMappingName = {}", originMappingName);
            if (originMappingName != null) {
                // TODO what about other parts of the mapping specification? (object/resource, object type)
                var metadata = ValueMetadataTypeUtil.getOrCreateMetadata((AssignmentType) clonedPcv.asContainerable());
                var provenance = ValueMetadataTypeUtil.getOrCreateProvenanceMetadata(metadata);
                ValueMetadataTypeUtil.getOrCreateMappingSpecification(provenance).setMappingName(originMappingName);
            }
        }
        return cloned;
    }

    private Collection<I> selectWeakNonNegativeValues() {
        Collection<I> nonNegativeIvwos = ivwoTriple.getNonNegativeValues();

        // The distinction here is necessary when dealing with outbound mappings; these are reconciled
        // at the end, where all mappings were processed. So, if a value was given by assignment-based construction
        // then we should use that one. If not, we need to check resource-based outbound mappings.

        Collection<I> weakFromAssignments = selectWeakValues(nonNegativeIvwos, OriginType.ASSIGNMENTS);
        if (!weakFromAssignments.isEmpty()) {
            return weakFromAssignments;
        }
        Collection<I> weakFromOutbounds = selectWeakValues(nonNegativeIvwos, OriginType.OUTBOUND);
        if (!weakFromOutbounds.isEmpty()) {
            return weakFromOutbounds;
        }
        return selectWeakValues(nonNegativeIvwos, null);
    }

    private Collection<I> selectWeakValues(Collection<I> ivwos, OriginType origin) {
        Collection<I> values = new ArrayList<>();
        if (strengthSelector.isWeak()) {
            for (I ivwo : ivwos) {
                if (ivwo.getProducer().getStrength() == MappingStrengthType.WEAK &&
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
        private final EquivalenceClass equivalenceClass;

        /**
         * IVwOs (origins) that indicate the need to add the value to the item:
         * computed as plusOrigins, optionally combined with (all or selected) zeroOrigins.
         */
        private final Collection<I> addingOrigins;

        /**
         * In cases we check for exclusiveness we need to store exclusive mapping we have found here.
         * See {@link #classifyMappings(Collection, boolean)}.
         */
        private PrismValueDeltaSetTripleProducer<V, D> exclusiveMapping;

        /**
         * In the set of relevant mappings (can be different for different situations),
         * do we have at least one strong mapping? See {@link #classifyMappings(Collection, boolean)}.
         */
        private boolean hasAtLeastOneStrongMapping;

        private ValueConsolidation(EquivalenceClass equivalenceClass) {
            this.equivalenceClass = equivalenceClass;

            LOGGER.trace("Consolidating value equivalence class ({} of {}):\n{}",
                    equivalenceClass.id, equivalenceClasses.size(),
                    equivalenceClass.debugDumpLazily());
            addingOrigins = findAddingOrigins();

            LOGGER.trace("Add-indicating IVwOs = {}", addingOrigins);
        }

        private void consolidate() throws ExpressionEvaluationException, SchemaException,
                ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {

            warnIfDeletingStronglyMandatedValue();

            // This division is quite simplistic in the presence of metadata (yields).
            // But let's keep it for the time being.

            if (!addingOrigins.isEmpty()) {
                consolidateToAddSet();
            } else {
                consolidateToDeleteSet();
            }
        }

        private Collection<I> findAddingOrigins() {
            Collection<I> addingOrigins = new HashSet<>(equivalenceClass.plusOrigins);
            if (addUnchangedValues) {
                addingOrigins.addAll(equivalenceClass.zeroOrigins);
            } else if (addUnchangedValuesExceptForNormalMappings) {
                for (I zeroIvwo : equivalenceClass.zeroOrigins) {
                    if (zeroIvwo.isStrong()
                            || zeroIvwo.isNormal() && (zeroIvwo.isSourceless() || zeroIvwo.isPushChanges())
                            || zeroIvwo.isWeak()) {
                        addingOrigins.add(zeroIvwo);
                    }
                }
            } else {
                // only plus origins cause adding values here
            }
            return addingOrigins;
        }

        /**
         * We know we have a reason to add the value: either a mapping that returned this value in its plus set,
         * or a mapping that returned it in zero set with "addUnchangedValues" being true. (Typically during object ADD
         * operation.) Or addUnchangedValues is false but addUnchangedValuesExceptForNormalMappings is true, and
         * the mapping is strong or weak or sourceless normal.
         *
         * So let us sort out the mappings to learn about the presence of weak-normal-strong ones and decide
         * on adding/not-adding the value using this information.
         */
        private void consolidateToAddSet() throws ExpressionEvaluationException, SchemaException, ConfigurationException,
                ObjectNotFoundException, CommunicationException, SecurityViolationException {
            classifyMappings(addingOrigins, true);
            addValueIfNeeded();
        }

        private void addCurrentValueToDeltaAddSet() throws SchemaException {
            itemDelta.addValueToAdd(
                    cloneAndApplyAssignmentOrigin(equivalenceClass.getRepresentative(), isAssignment, addingOrigins));
        }

        private void addCurrentValueToDeltaDeleteSet() {
            //noinspection unchecked
            itemDelta.addValueToDelete((V) equivalenceClass.getRepresentative().clone());
        }

        private void addValueIfNeeded() throws CommunicationException, ObjectNotFoundException, SchemaException,
                SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

            // Detecting conflicting values provided by strong mappings. Normally, this is checked on the deltas after
            // consolidation. But if one of the conflicting values is stored in the existing item, the delta for it will not
            // be produced. Hence, we have to check for conflicts explicitly. See MID-9621.
            if (hasAtLeastOneStrongMapping && itemDefinition.isSingleValue()) {
                LOGGER.trace("Including in uniqueness checking for single-valued item {}: {}", itemPath, equivalenceClass);
                equivalenceClassesBeingAddedForSingleValuedItem.add(equivalenceClass);
            }

            // Checking for presence of value0 in existing item (real or assumed)

            if (existingItemKnown) {
                if (equivalenceClass.presentInExistingItem()) {
                    LOGGER.trace("Value {} NOT added to delta for item {} because the item already has that value. In: {}",
                            equivalenceClass, itemPath, contextDescription);
                    if (valueMetadataComputer != null) {
                        decideAccordingToMetadata("adding-but-present-in-existing (#2)");
                    }
                    return;
                }
            } else {
                if (hasZeroNonWeakMapping()) {
                    // We use this approximate check only if we don't know current item value - we can only assume it
                    LOGGER.trace("Value {} in item {} has zero mapping, so we assume it already exists, skipping processing in {}",
                            equivalenceClass, itemPath, contextDescription);
                    // We cannot compute metadata because we do not have the target value to compute them for.
                    return;
                }
            }

            // Normal mappings are ignored on some occasions

            if (!hasAtLeastOneStrongMapping) {
                if (ignoreNormalMappings) {
                    LOGGER.trace("Value {} mapping is normal in item {} and we have exclusiveStrong, skipping processing in {}",
                            equivalenceClass, itemPath, contextDescription);
                    // We are not sure what to do with metadata now. But, fortunately, this case occurs only with
                    // resource object items mappings, and we currently do not implement metadata computation for them.
                    return;
                }
                if (itemDeltaExists && !skipNormalMappingAPrioriDeltaCheck) {
                    LOGGER.trace("Value {} mapping is not strong and the item {} already has a delta; " +
                                    "skipping adding in {} (also skipping value metadata computation)",
                            equivalenceClass, itemPath, contextDescription);
                    return;
                }
            }

            LOGGER.trace("Decided to ADD value {} to delta for item {} in {}", equivalenceClass, itemPath, contextDescription);
            assert !existingItemKnown || equivalenceClass.getPresenceInExistingItem().isEmpty();

            if (valueMetadataComputer != null) {
                decideAccordingToMetadata("adding-new (#1)");
            } else {
                addCurrentValueToDeltaAddSet();
            }
        }

        /**
         * We have no reason to add the value based on the mappings. Let's check the other options.
         */
        private void consolidateToDeleteSet() throws ExpressionEvaluationException, ConfigurationException,
                ObjectNotFoundException, SchemaException, CommunicationException, SecurityViolationException {

            assert equivalenceClass.plusOrigins.isEmpty() : "Non-empty plus origin set is treated in consolidateToAddSet";

            if (!equivalenceClass.zeroOrigins.isEmpty() /* && !selectNonWeakValuesFrom(equivalenceClass.zeroOrigins).isEmpty() */) {
                LOGGER.trace("Keeping the value {} for {} because there are some (non-weak) zero origins. In: {}",
                        equivalenceClass, itemPath, contextDescription);
                // This means there are some non-weak mappings that think the value should be kept. These mappings are
                // normal (not strong), because otherwise the value would be in plus set (as addUnchangedValuesExceptForNormalMappings
                // is now universally true). However, although they are normal, we take them into account. Why? In the world of yields
                // it is obvious: they provide yields that are distinct from yield(s) present in the minus origins. So the most natural
                // behavior is to keep the value, and remove only the respective yields (if provenance metadata processing is enabled).

                if (valueMetadataComputer != null) {
                    decideAccordingToMetadata("not-adding-present-in-zero (#3)");
                } else {
                    // just keep the value
                }
            } else if (!equivalenceClass.minusOrigins.isEmpty()) {
                classifyMappings(equivalenceClass.minusOrigins, false);
                deleteValueIfNeeded();
            } else {
                LOGGER.trace("Value being consolidated ({} for {}) has no visible plus, minus, nor zero origins. " +
                                "So we just keep it as is. In: {}",
                        equivalenceClass, itemPath, contextDescription);
            }
        }

        private void deleteValueIfNeeded() throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
            if (existingItemKnown && !equivalenceClass.presentInExistingItem()) {
                LOGGER.trace("Value {} NOT add to delta as DELETE because item {} the item does not have that value in {}",
                        equivalenceClass, itemPath, contextDescription);

                assert equivalenceClass.zeroOrigins.isEmpty();
                // No metadata to be computed here, because no existing value and no zero nor plus origins
                return;
            }

            if (!hasAtLeastOneStrongMapping && itemDeltaExists && !skipNormalMappingAPrioriDeltaCheck) {
                LOGGER.trace("Value {} mapping is not strong and the item {} already has a delta, skipping deletion in {}",
                        equivalenceClass, itemPath, contextDescription);
                // TODO treat metadata somehow here
                return;
            }

            if (valueMetadataComputer != null && existingItemKnown) {
                assert !equivalenceClass.presenceInExistingItem.isEmpty();
                assert !equivalenceClass.minusOrigins.isEmpty();
                assert equivalenceClass.plusOrigins.isEmpty();
                assert equivalenceClass.zeroOrigins.isEmpty();
                decideAccordingToMetadata("deleting (#4)");
            } else {
                LOGGER.trace("Value {} added to delta as DELETE for item {} in {}", equivalenceClass, itemPath, contextDescription);
                addCurrentValueToDeltaDeleteSet();
            }
        }

        /**
         * Metadata support is enabled so we will analyze the situation more precisely.
         */
        private void decideAccordingToMetadata(String situation) throws CommunicationException, ObjectNotFoundException,
                SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
            new MetadataBasedConsolidation(situation)
                    .consolidate();
        }

        private void classifyMappings(Collection<I> origins, boolean checkExclusiveness)
                throws ExpressionEvaluationException {
            hasAtLeastOneStrongMapping = false;
            exclusiveMapping = null;
            for (var origin : origins) {
                var mapping = origin.getProducer();
                if (mapping.isStrong()) {
                    hasAtLeastOneStrongMapping = true;
                }
                if (checkExclusiveness && mapping.isExclusive()) {
                    checkMappingExclusiveness(mapping);
                }
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

        private boolean hasZeroNonWeakMapping() {
            return equivalenceClass.zeroOrigins.stream()
                    .anyMatch(origin -> !origin.isWeak());
        }

//        private List<V> selectNonWeakValuesFrom(Collection<I> origins) {
//            return origins.stream()
//                    .filter(origin -> !origin.isWeak())
//                    .map(ItemValueWithOrigin::getItemValue)
//                    .collect(Collectors.toList());
//        }

        private List<V> valuesIn(Collection<I> origins) {
            return origins.stream()
                    .map(ItemValueWithOrigin::getItemValue)
                    .collect(Collectors.toList());
        }

        /** When a-priori delta requests deleting a value mandated by strong mapping, we issue a warning. */
        private void warnIfDeletingStronglyMandatedValue() {
            if (aprioriItemDelta != null
                    && ItemCollectionsUtil.contains(
                            emptyIfNull(aprioriItemDelta.getValuesToDelete()),
                            equivalenceClass.getRepresentative(),
                            EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS)) {
                checkIfStrong(equivalenceClass.zeroOrigins);
                checkIfStrong(equivalenceClass.plusOrigins);
            }
        }

        private void checkIfStrong(Collection<I> origins) {
            var strongMapping = findStrongMapping(origins);
            if (strongMapping != null) {
                LOGGER.warn("Attempt to delete value {} from item {} but that value is mandated by a strong mapping {} (for {})",
                        equivalenceClass.getRepresentative(), itemPath, strongMapping.toHumanReadableDescription(), contextDescription);
            }
        }

        @Nullable
        private PrismValueDeltaSetTripleProducer<V, D> findStrongMapping(Collection<I> ivwos) {
            for (ItemValueWithOrigin<V,D> pvwo : MiscUtil.emptyIfNull(ivwos)) {
                PrismValueDeltaSetTripleProducer<V,D> mapping = pvwo.getProducer();
                if (mapping.getStrength() == MappingStrengthType.STRONG) {
                    return mapping;
                }
            }
            return null;
        }

        private class MetadataBasedConsolidation {

            private final String situation;
            private final List<YieldPresence> yieldPresences = new ArrayList<>();
            private final List<ValueMetadataType> toAdd = new ArrayList<>();
            private final List<ValueMetadataType> toDelete = new ArrayList<>();

            private MetadataBasedConsolidation(String situation) {
                this.situation = situation;
                createYieldPresences();
            }

            private void createYieldPresences() {
                addYields(equivalenceClass.presenceInExistingItem, YieldPresence::getExisting);
                addYields(valuesIn(equivalenceClass.plusOrigins), YieldPresence::getPlus);
                addYields(valuesIn(equivalenceClass.zeroOrigins), YieldPresence::getZero);
                addYields(valuesIn(equivalenceClass.minusOrigins), YieldPresence::getMinus);
            }

            private void addYields(List<V> values, Function<YieldPresence, List<ValueMetadataType>> collection) {
                for (V value : values) {
                    // Values without metadata (and still somehow we are processing metadata
                    // should end up in their own yield (empty metadata) so they are properly consolidated.
                    if (value.getValueMetadata().isEmpty()) {
                        var yield = new ValueMetadataType();
                        YieldPresence yieldPresence = createOrFindYieldPresence(yield);
                        collection.apply(yieldPresence).add(yield);
                    }
                    for (PrismContainerValue<Containerable> yieldPcv : value.getValueMetadataAsContainer().getValues()) {
                        ValueMetadataType yield = (ValueMetadataType) yieldPcv.asContainerable();
                        YieldPresence yieldPresence = createOrFindYieldPresence(yield);
                        collection.apply(yieldPresence).add(yield);
                    }
                }
            }

            private @NotNull YieldPresence createOrFindYieldPresence(ValueMetadataType yield) {
                for (YieldPresence yieldPresence : yieldPresences) {
                    if (yieldPresence.matches(yield)) {
                        return yieldPresence;
                    }
                }
                YieldPresence newPresence = new YieldPresence(yield);
                yieldPresences.add(newPresence);
                return newPresence;
            }

            void consolidate() throws CommunicationException, ObjectNotFoundException,
                    SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
                logStart();
                for (YieldPresence yieldPresence : yieldPresences) {
                    yieldPresence.consolidate();
                }
                logEnd();

                if (!toAdd.isEmpty()) {
                    PrismValue valueToAdd = equivalenceClass.getRepresentative().clone();
                    PrismContainer<Containerable> valueMetadataAsContainer = valueToAdd.getValueMetadataAsContainer();
                    valueMetadataAsContainer.clear();
                    for (ValueMetadataType metadataToAdd : toAdd) {
                        //noinspection unchecked
                        valueMetadataAsContainer.add(metadataToAdd.clone().asPrismContainerValue());
                    }
                    //noinspection unchecked
                    itemDelta.addValueToAdd((V) valueToAdd);
                }

                if (!toDelete.isEmpty()) {
                    PrismValue valueToDelete = equivalenceClass.getRepresentative().clone();
                    PrismContainer<Containerable> valueMetadataAsContainer = valueToDelete.getValueMetadataAsContainer();
                    valueMetadataAsContainer.clear();
                    for (ValueMetadataType metadataToDelete : toDelete) {
                        //noinspection unchecked
                        valueMetadataAsContainer.add(metadataToDelete.clone().asPrismContainerValue());
                    }
                    //noinspection unchecked
                    itemDelta.addValueToDelete((V) valueToDelete);
                }
            }

            private void logStart() {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Starting metadata-based consolidation for situation: {}\nYield presences:\n{}", situation,
                            yieldPresences.stream()
                                .map(presence -> "  -- Yield presence:\n" + presence.debugDump(2))
                                .collect(Collectors.joining("\n")));
                }
            }

            private void logEnd() {
                LOGGER.trace("Result of metadata-based consolidation:\nTo add:\n{}\nTo delete:\n{}",
                        DebugUtil.debugDumpLazily(toAdd, 1),
                        DebugUtil.debugDumpLazily(toDelete, 1));
            }

            private class YieldPresence implements DebugDumpable {
                @NotNull private final ValueMetadataType yield;
                private final List<ValueMetadataType> existing = new ArrayList<>();
                private final List<ValueMetadataType> plus = new ArrayList<>();
                private final List<ValueMetadataType> zero = new ArrayList<>();
                private final List<ValueMetadataType> minus = new ArrayList<>();

                private YieldPresence(@NotNull ValueMetadataType yield) {
                    this.yield = yield;
                }

                public List<ValueMetadataType> getExisting() {
                    return existing;
                }

                public List<ValueMetadataType> getPlus() {
                    return plus;
                }

                public List<ValueMetadataType> getZero() {
                    return zero;
                }

                public List<ValueMetadataType> getMinus() {
                    return minus;
                }

                public boolean matches(ValueMetadataType yield) {
                    return MidpointProvenanceEquivalenceStrategy.INSTANCE.equals(yield, this.yield);
                }

                @Override
                public String debugDump(int indent) {
                    StringBuilder sb = new StringBuilder();
                    DebugUtil.debugDumpWithLabelLn(sb, "Existing", asPrismContainerValues(existing), indent);
                    DebugUtil.debugDumpWithLabelLn(sb, "Plus", asPrismContainerValues(plus), indent);
                    DebugUtil.debugDumpWithLabelLn(sb, "Zero", asPrismContainerValues(zero), indent);
                    DebugUtil.debugDumpWithLabel(sb, "Minus", asPrismContainerValues(minus), indent);
                    return sb.toString();
                }

                public void consolidate() throws CommunicationException, ObjectNotFoundException, SchemaException,
                        SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

                    if (!zero.isEmpty() || !plus.isEmpty()) {
                        @NotNull ValueMetadataType computedMetadata =
                                valueMetadataComputer.compute(getNonNegativeValues(), existing, result);

                        // This is to avoid phantom adds. It is sufficient to check this on the level
                        // of a yield because we assume that the metadata computer never changes the yield
                        // of the input metadata. (I.e. provenance of the output is equivalent with the provenance
                        // of the inputs. And provenance of all the inputs is equivalent.)
                        //
                        // TODO What if the existing metadata are also in the minus set?
                        //  It is no harm if we ignore this fact but at least we should think about it.
                        //  See e.g. TestValueMetadata.test320ReinforceGivenNameByManualEntry.
                        if (isPresent(computedMetadata)) {
                            LOGGER.trace("Computed metadata is already present, not adding them:\n{}",
                                    DebugUtil.debugDumpLazily(computedMetadata));
                        } else {
                            LOGGER.trace("Adding computed metadata:\n{}",
                                    DebugUtil.debugDumpLazily(computedMetadata));
                            toAdd.add(computedMetadata);
                        }

                    } else {
                        if (!existing.isEmpty() && !minus.isEmpty()) {
                            LOGGER.trace("Yield {} has no plus/zero presence but it is present in existing item and "
                                    + "in minus set -- so we need to delete it", yield);
                            toDelete.add(yield);
                        }
                    }
                }

                private boolean isPresent(ValueMetadataType toBeAdded) {
                    // TODO Consider the strategy. REAL_VALUE (instead of DATA) prevents uninteresting changes to be applied.
                    //  But what about timestamps, channels, actors, etc?
                    return existing.stream()
                            .anyMatch(e -> e.asPrismContainerValue().equals(toBeAdded.asPrismContainerValue(), EquivalenceStrategy.REAL_VALUE));
                }

                private List<ValueMetadataType> getNonNegativeValues() {
                    List<ValueMetadataType> rv = new ArrayList<>(plus);
                    rv.addAll(zero);
                    return rv;
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
                if (pvwo.isMappingStrong()) {
                    resultHolder.setValue(true);
                }
            };
            ivwoTriple.simpleAccept(visitor);
            return resultHolder.getValue();
        }
    }

    private void logStart() {
        LOGGER.trace("""
                        Consolidating {} IVwO triple:
                        {}
                          Apriori Delta (exists: {}):
                        {}
                          Existing item (known: {}, isEmpty: {}):
                        {}
                          Parameters:
                           - equalsChecker: {}
                           - strengthSelector: {}
                           - addUnchangedValues: {}
                           - addUnchangedValuesExceptForNormalMappings: {}
                           - itemIsExclusiveStrong: {}
                           - deleteExistingValues (experimental): {}
                           - skipNormalMappingAPrioriDeltaCheck (experimental): {}
                           - ignoreNormalMappings: {}
                           - valueMetadataComputer: {}
                          Context: {}
                        """,
                itemPath, ivwoTriple.debugDumpLazily(1),
                itemDeltaExists, DebugUtil.debugDumpLazily(aprioriItemDelta, 2),
                existingItemKnown, existingItemIsEmpty, DebugUtil.debugDumpLazily(existingItem, 2),
                equalsChecker, strengthSelector,
                addUnchangedValues, addUnchangedValuesExceptForNormalMappings,
                itemIsExclusiveStrong,
                deleteExistingValues, skipNormalMappingAPrioriDeltaCheck,
                ignoreNormalMappings, valueMetadataComputer,
                contextDescription);
    }

    private void logEnd() {
        LOGGER.trace("Consolidated {} IVwO triple to delta:\n{}", itemPath, itemDelta.debugDumpLazily(1));
        if (result.isTracingNormal(ItemConsolidationTraceType.class)) {
            ItemConsolidationTraceType trace = new ItemConsolidationTraceType();
            trace.setItemPath(new ItemPathType(itemPath));
            PrismContext prismContext = PrismContext.get();
            try {
                if (ivwoTriple != null) {
                    PrismValueDeltaSetTriple<PrismValue> prismValueDeltaSetTriple =
                            prismContext.deltaFactory().createPrismValueDeltaSetTriple();
                    ivwoTriple.transform(prismValueDeltaSetTriple, ItemValueWithOrigin::getItemValue);
                    trace.setDeltaSetTriple(DeltaSetTripleType.fromDeltaSetTriple(prismValueDeltaSetTriple));
                }
                if (existingItem != null) {
                    trace.setExistingItem(ItemType.fromItem(existingItem));
                }
                if (aprioriItemDelta != null) {
                    trace.getAprioriDelta().addAll(DeltaConvertor.toItemDeltaTypes(aprioriItemDelta));
                }
                trace.setEquivalenceClassCount(equivalenceClasses.size());
                trace.getResultingDelta().addAll(DeltaConvertor.toItemDeltaTypes(itemDelta));
            } catch (SchemaException e) {
                LOGGER.warn("Couldn't convert itemDelta to the trace", e);
            }
            result.getTraces().add(trace);
        }
    }

    private void categorizeValues() throws SchemaException {
        categorizeFromTriple(ivwoTriple.getPlusSet(), EquivalenceClass::getPlusOrigins, false);
        categorizeFromTriple(ivwoTriple.getZeroSet(), EquivalenceClass::getZeroOrigins, false);
        categorizeFromTriple(ivwoTriple.getMinusSet(), EquivalenceClass::getMinusOrigins, true);
        if (existingItem != null) {
            categorizeFromCollection(existingItem.getValues(), EquivalenceClass::getPresenceInExistingItem);
        }
        if (aprioriItemDelta != null) {
            categorizeFromCollection(aprioriItemDelta.getValuesToAdd(), EquivalenceClass::getPresenceInAprioriPlus);
            categorizeFromCollection(aprioriItemDelta.getValuesToReplace(), EquivalenceClass::getPresenceInAprioriPlus);
            categorizeFromCollection(aprioriItemDelta.getValuesToDelete(), EquivalenceClass::getPresenceInAprioriMinus);
        }
    }

    private void categorizeFromCollection(Collection<V> values, Function<EquivalenceClass, List<V>> targetSet) throws SchemaException {
        for (V value : emptyIfNull(values)) {
            EquivalenceClass equivalenceClass = findOrCreateEquivalenceClass(value);
            targetSet.apply(equivalenceClass).add(value);
        }
    }

    private void categorizeFromTriple(Collection<I> ivwoSet, Function<EquivalenceClass, List<I>> targetSet,
            boolean takeValidInvalid) throws SchemaException {
        for (I ivwo : ivwoSet) {
            categorizeFromTriple(ivwo, targetSet, takeValidInvalid);
        }
    }

    private void categorizeFromTriple(I ivwo, Function<EquivalenceClass, List<I>> targetSet, boolean takeValidInvalid) throws SchemaException {
        if (shouldCategorize(ivwo, takeValidInvalid)) {
            EquivalenceClass equivalenceClass = findOrCreateEquivalenceClass(ivwo.getItemValue());
            targetSet.apply(equivalenceClass).add(ivwo);
        } else {
            LOGGER.trace("Not categorizing {} (to {})", ivwo, targetSet);
        }
    }

    private boolean shouldCategorize(I ivwo, boolean takeValidInvalid) {
        return !ivwo.isWeak() && // experimental
                !shouldSkipMapping(ivwo.getProducer().getStrength()) &&
                (ivwo.isValid() || takeValidInvalid && ivwo.wasValid());
    }

    private EquivalenceClass findOrCreateEquivalenceClass(V value) throws SchemaException {
        for (EquivalenceClass equivalenceClass : equivalenceClasses) {
            if (equivalenceClass.covers(value)) {
                return equivalenceClass;
            }
        }
        EquivalenceClass equivalenceClass = new EquivalenceClass(value);
        equivalenceClasses.add(equivalenceClass);
        return equivalenceClass;
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
            Item<V,D> clonedItem = existingItem.clone();
            itemDelta.applyToMatchingPath(clonedItem);
            return !clonedItem.isEmpty();
        }
    }

//    private void computeMetadataOnExistingValue(EquivalenceClass equivalenceClass, Supplier<List<V>> additionalSourcesSupplier)
//            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
//            ConfigurationException, ExpressionEvaluationException {
//        if (valueMetadataComputer == null) {
//            LOGGER.trace("Skipping value metadata computation (for existing value) because computer is null");
//        } else if (!equivalenceClass.presentInExistingItem()) {
//            LOGGER.trace("Skipping value metadata computation for existing value because the value is not present in existing item: {}", equivalenceClass);
//        } else {
//            V existingValue = equivalenceClass.presenceInExistingItem.get(0);
//            ValueMetadata metadata = computeValueMetadata(existingValue, additionalSourcesSupplier, equivalenceClass::getPresenceInAprioriPlus); // TODO
//            LOGGER.trace("Computed value metadata for existing value {} ({}):\n{}", existingValue, itemPath,
//                    DebugUtil.debugDumpLazily(metadata));
//            applyMetadataIfChanged(existingValue, metadata);
//        }
//    }

//    @SuppressWarnings("unchecked")
//    private void applyMetadataIfChanged(V existingValue, ValueMetadata metadata) {
//        ValueMetadata existingMetadata = existingValue.getValueMetadata();
//        if (metadata.isEmpty() && existingMetadata.isEmpty()) {
//            // Maybe we should generalize this comparison
//            LOGGER.trace("Both computed and existing value metadata is empty, not doing anything");
//        } else if (metadata.equals(existingMetadata)) {
//            LOGGER.trace("Computed value metadata is the same as existing, not doing anything");
//        } else {
//            LOGGER.trace("Computed value metadata is different from existing, deleting old value and adding updated one");
//
//            V oldValue = (V) existingValue.clone();
//            itemDelta.addValueToDelete(oldValue);
//
//            V newValue = (V) existingValue.clone();
//            newValue.setValueMetadata(metadata);
//            itemDelta.addValuesToAdd(newValue);
//        }
//    }

//    @SafeVarargs
//    private final void computeValueMetadataOnDeltaValue(EquivalenceClass equivalenceClass, Supplier<List<V>>... additionalSourcesSuppliers)
//            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
//            ConfigurationException, ExpressionEvaluationException {
//        if (valueMetadataComputer != null) {
//            ValueMetadata metadata = computeValueMetadata(equivalenceClass.getExistingValue(), additionalSourcesSuppliers);
//            LOGGER.trace("Computed value metadata for delta value {} ({}):\n{}", equivalenceClass, itemPath, DebugUtil.debugDumpLazily(metadata));
//            equivalenceClass.getRepresentative().setValueMetadata(metadata);
//        } else {
//            LOGGER.trace("Skipping value metadata computation (for delta value) because computer is null");
//        }
//    }

//    // TEMPORARY
//    private void computeValueMetadataOnDeltaValue(V deltaValue, Supplier<List<V>> additionalSourcesSupplier)
//            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
//            ConfigurationException, ExpressionEvaluationException {
//        if (valueMetadataComputer != null) {
//            ValueMetadata metadata = computeValueMetadata(null, additionalSourcesSupplier);
//            LOGGER.trace("Computed value metadata for delta value {} ({}):\n{}", deltaValue, itemPath, DebugUtil.debugDumpLazily(metadata));
//            deltaValue.setValueMetadata(metadata);
//        } else {
//            LOGGER.trace("Skipping value metadata computation (for delta value) because computer is null");
//        }
//    }

//    @SafeVarargs
//    private final ValueMetadata computeValueMetadata(V existingValue, Supplier<List<V>>... additionalSourcesSuppliers)
//            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
//            ConfigurationException, ExpressionEvaluationException {
//        List<PrismValue> inputValues = new ArrayList<>();
//        if (existingValue != null) {
//            inputValues.add(existingValue);
//        }
//        for (Supplier<List<V>> additionalSourcesSupplier : additionalSourcesSuppliers) {
//            inputValues.addAll(additionalSourcesSupplier.get());
//        }
//        return valueMetadataComputer.compute(inputValues, result);
//    }

    private class ValueCategorization {

        // TEMPORARY: TODO rework!
        private class EquivalenceClassSimple {
            private final List<I> members = new ArrayList<>();

            private EquivalenceClassSimple(I head) {
                members.add(head);
            }

            private I getRepresentative() {
                return members.get(0);
            }

            private void addMember(I member) {
                members.add(member);
            }

            private List<V> getMemberValues() {
                return members.stream()
                        .map(ItemValueWithOrigin::getItemValue)
                        .collect(Collectors.toList());
            }
        }

        @NotNull private final Set<EquivalenceClassSimple> equivalenceClasses = new HashSet<>();

        private ValueCategorization(Collection<I> allValues) throws SchemaException {
            categorizeValues(new LinkedList<>(allValues));
        }

        private void categorizeValues(LinkedList<I> valuesToCategorize) throws SchemaException {
            while (!valuesToCategorize.isEmpty()) {
                categorizeFirstValue(valuesToCategorize);
            }
        }

        private void categorizeFirstValue(LinkedList<I> valuesToCategorize) throws SchemaException {
            Iterator<I> iterator = valuesToCategorize.iterator();
            I head = iterator.next();
            iterator.remove();
            EquivalenceClassSimple equivalenceClass = new EquivalenceClassSimple(head);
            equivalenceClasses.add(equivalenceClass);

            while (iterator.hasNext()) {
                I current = iterator.next();
                if (areEquivalent(head.getItemValue(), current.getItemValue())) {
                    equivalenceClass.addMember(current);
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void close() {
        result.computeStatusIfUnknown();
    }

    private class EquivalenceClass implements DebugDumpable {

        // Just for reporting. Ugly hack but should work.
        final int id = equivalenceClasses.size() + 1;

        @NotNull private final List<V> members = new ArrayList<>();

        /**
         * IVwOs (origins) that have value0 in the plus set.
         * 1. Filtered according to strength selector, so some mappings may be omitted.
         * 2. Mappings from invalid constructions are also filtered out.
         */
        @NotNull private final List<I> plusOrigins = new ArrayList<>();

        /**
         * IVwOs (origins) that have value0 in the zero set.
         * 1. Filtered according to strength selector, so some mappings may be omitted.
         * 2. Mappings from invalid constructions are also filtered out.
         */
        @NotNull private final List<I> zeroOrigins = new ArrayList<>();

        /**
         * IVwOs (origins) that have value0 in the minus set.
         * 1. Filtered according to strength selector, so some mappings may be omitted.
         * 2. However, mappings from invalid constructions ARE here, but only if they were valid
         * and became invalid currently.
         *
         * TODO This valid-invalid behavior should be explained more thoroughly. Also, why the validity
         *  of constructions is considered but validity of the mappings is not? We should check this.
         *
         * TODO Maybe we could employ value categorization when dealing with plus/zero/minus origins.
         */
        @NotNull private final List<I> minusOrigins = new ArrayList<>();

        @NotNull private final List<V> presenceInExistingItem = new ArrayList<>();

        @NotNull private final List<V> presenceInAprioriPlus = new ArrayList<>();

        @NotNull private final List<V> presenceInAprioriMinus = new ArrayList<>();

        private EquivalenceClass(V head) {
            members.add(head);
        }

        private V getRepresentative() {
            return members.get(0);
        }

        private Object getRepresentativeRealValue() {
            // Actually not sure whether the representative can be null
            var representative = getRepresentative();
            return representative != null ? representative.getRealValue() : null;
        }

        private boolean covers(V value) throws SchemaException {
            for (V member : members) {
                if (areEquivalent(member, value)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String debugDump(int indent) {
            StringBuilder sb = new StringBuilder();
            DebugUtil.debugDumpWithLabelLn(sb, "Equivalence class members", members, indent);
            DebugUtil.debugDumpWithLabelLn(sb, "Plus origins", plusOrigins, indent);
            DebugUtil.debugDumpWithLabelLn(sb, "Zero origins", zeroOrigins, indent);
            DebugUtil.debugDumpWithLabelLn(sb, "Minus origins", minusOrigins, indent);
            DebugUtil.debugDumpWithLabelLn(sb, "Presence in existing item", presenceInExistingItem, indent);
            DebugUtil.debugDumpWithLabelLn(sb, "Presence in apriori plus", presenceInAprioriPlus, indent);
            DebugUtil.debugDumpWithLabelLn(sb, "Presence in apriori minus", presenceInAprioriMinus, indent);
            return sb.toString();
        }

        private boolean presentInExistingItem() {
            return !presenceInExistingItem.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("%s (triple: %d+/%dz/%d-, existing: %d, apriori: %d/%d)",
                    members, plusOrigins.size(), zeroOrigins.size(), minusOrigins.size(), presenceInExistingItem.size(),
                    presenceInAprioriPlus.size(), presenceInAprioriMinus.size());
        }

        private @NotNull List<I> getPlusOrigins() {
            return plusOrigins;
        }

        private @NotNull List<I> getZeroOrigins() {
            return zeroOrigins;
        }

        private @NotNull List<I> getMinusOrigins() {
            return minusOrigins;
        }

        private @NotNull List<V> getPresenceInExistingItem() {
            return presenceInExistingItem;
        }

        private @NotNull List<V> getPresenceInAprioriPlus() {
            return presenceInAprioriPlus;
        }

        private @NotNull List<V> getPresenceInAprioriMinus() {
            return presenceInAprioriMinus;
        }

        // TEMPORARY
        private V getExistingValue() {
            return presenceInExistingItem.isEmpty() ? null : presenceInExistingItem.get(0);
        }
    }

    // TODO the relation defined here is not transitive:
    //  Having container values V1, V2, V3 like this:
    //   V1 (id=42) has content C
    //   V2 (id=42) is empty (or has content C')
    //   V3 (no id) has content C
    //  Then V1 ~ V2, V1 ~ V3 but not V2 ~ V3.
    //
    // So, after all, is the concept of using equivalence classes OK? :)
    private boolean areEquivalent(V value1, V value2) throws SchemaException {
        if (containerIdentifiersPresentAndEqual(value1, value2)) {
            return true;
        } else if (equalsChecker != null) {
            return equalsChecker.test(value1, value2);
        } else if (value1 != null) {
            return value1.equals(value2, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);
        } else {
            return value2 == null;
        }
    }

    private boolean containerIdentifiersPresentAndEqual(V value1, V value2) {
        if (value1 instanceof PrismContainerValue<?> pcv1 && value2 instanceof PrismContainerValue<?> pcv2) {
            Long id1 = pcv1.getId();
            Long id2 = pcv2.getId();
            return id1 != null && id1.equals(id2);
        } else {
            return false;
        }
    }

}

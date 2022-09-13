/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.indexing;

import com.evolveum.midpoint.model.api.indexing.IndexingConfiguration;
import com.evolveum.midpoint.model.api.indexing.IndexingItemConfiguration;
import com.evolveum.midpoint.model.api.indexing.IndexedItemValueNormalizer;
import com.evolveum.midpoint.model.api.indexing.ValueNormalizer;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Helps with searching through model-indexed values (currently, in identities/identity[X]/items/normalized container).
 *
 * Only `String` and `PolyString` types are supported.
 */
@Component
public class IndexingManager {

    private static final Trace LOGGER = TraceManager.getTrace(IndexingManager.class);

    @Autowired private PrismContext prismContext;

    /** Updates normalized (indexed) identity data on focus add. */
    public <O extends ObjectType> void updateIndexDataOnElementAdd(
            @NotNull O objectToAdd,
            @NotNull LensElementContext<O> elementContext,
            @NotNull Task task,
            @NotNull OperationResult result) throws ConfigurationException, SchemaException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ObjectNotFoundException {
        IndexingConfiguration configuration = getIndexingConfiguration(elementContext);
        if (configuration == null || configuration.hasNoItems()) {
            LOGGER.trace("No indexing configuration for {}: index data will not be updated", elementContext);
            return;
        }
        if (!(objectToAdd instanceof FocusType)) {
            LOGGER.trace("Not a FocusType: {}", objectToAdd);
            return;
        }
        FocusType focusToAdd = (FocusType) objectToAdd;
        FocusTypeUtil.addOrReplaceIdentity(focusToAdd,
                createIdentityForIndexData(
                        computeIndexData(focusToAdd, configuration, task, result)));
    }

    /**
     * Updates normalized (indexed) identity data on focus modification
     * (by adding necessary changes to the `delta` parameter.)
     */
    public <O extends ObjectType> void updateIndexDataOnElementModify(
            O current, // we accept null values here but only for non-essential cases (i.e. no index data to be updated)
            @NotNull ObjectDelta<O> delta,
            @NotNull Class<O> objectClass,
            @NotNull LensElementContext<O> elementContext,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException, ConfigurationException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ObjectNotFoundException {
        IndexingConfiguration configuration = getIndexingConfiguration(elementContext);
        if (configuration == null || configuration.hasNoItems()) {
            LOGGER.trace("No indexing configuration for {}: index data will not be updated", elementContext);
            return;
        }
        if (!FocusType.class.isAssignableFrom(objectClass)
                || current != null && !(current instanceof FocusType)) {
            LOGGER.trace("Not a FocusType: {}, {}", objectClass, current);
            return;
        }
        if (current == null) {
            throw new IllegalStateException("Current focal object is null: " + elementContext);
        }
        FocusType expectedNew =
                (FocusType) Objects.requireNonNull(
                                elementContext.getObjectNew(),
                                () -> String.format("Expected 'new' focal object is null: %s "
                                                + "(it shouldn't be, as we are modifying it)",
                                        elementContext))
                        .asObjectable();

        delta.addModifications(
                computeIndexingDeltas(
                        expectedNew,
                        computeIndexData(expectedNew, configuration, task, result)));
    }


    @Nullable
    private IndexingConfiguration getIndexingConfiguration(
            @NotNull LensElementContext<?> elementContext) throws ConfigurationException {
        if (elementContext instanceof LensFocusContext<?>) {
            return ((LensFocusContext<?>) elementContext).getIndexingConfiguration();
        } else {
            LOGGER.trace("Not a LensFocusContext: {}", elementContext);
            return null;
        }
    }

    /**
     * Computes "own" {@link FocusIdentityType} that contains all the normalized (index) data.
     * (They are grouped together from the focal object and all identities.)
     *
     * This is the preliminary behavior. To be confirmed or changed later.
     */
    private IdentityItemsType computeIndexData(
            @NotNull FocusType focus,
            @NotNull IndexingConfiguration configuration,
            @NotNull Task task,
            @NotNull OperationResult result) throws ConfigurationException, SchemaException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ObjectNotFoundException {
        IdentityItemsType normalized = new IdentityItemsType();
        for (IndexingItemConfiguration itemConfig : configuration.getItems()) {
            ItemPath originalItemPath = itemConfig.getPath();
            Collection<PrismValue> allValues = collectAllValues(focus, originalItemPath);
            ItemDefinition<?> originalItemDef =
                    MiscUtil.requireNonNull(
                            focus.asPrismObject().getDefinition().findItemDefinition(originalItemPath),
                            () -> String.format("No prism definition of indexed item '%s' in %s", originalItemPath, focus));
            //noinspection unchecked
            normalized.asPrismContainerValue().addAll(
                    normalizeItemValues(originalItemDef, allValues, itemConfig, task, result));
        }
        LOGGER.trace("Computed normalized identity:\n{}", normalized.debugDumpLazily(1));
        return normalized;
    }

    private FocusIdentityType createIdentityForIndexData(IdentityItemsType normalized) {
        return
                new FocusIdentityType()
                        .items(new FocusIdentityItemsType()
                                .normalized(normalized));
    }

    private Collection<PrismValue> collectAllValues(@NotNull FocusType focus, @NotNull ItemPath path) {
        List<PrismValue> allRealValues = new ArrayList<>();
        allRealValues.addAll(
                focus.asPrismContainerValue().getAllValues(path));
        allRealValues.addAll(
                focus.asPrismContainerValue().getAllValues(SchemaConstants.PATH_IDENTITY.append(FocusIdentityType.F_DATA, path)));
        return allRealValues;
    }

    private Collection<? extends Item<?, ?>> normalizeItemValues(
            @NotNull ItemDefinition<?> originalItemDef,
            @NotNull Collection<PrismValue> originalValues,
            @NotNull IndexingItemConfiguration config,
            @NotNull Task task,
            @NotNull OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        if (originalValues.isEmpty()) {
            return List.of();
        }
        Collection<Item<?, ?>> normalizedItems = new ArrayList<>();
        for (IndexedItemValueNormalizer normalizer : config.getNormalizers()) {
            ItemNormalizer itemNormalizer = new ItemNormalizer(normalizer);
            normalizedItems.add(
                    itemNormalizer.createNormalizedItem(originalItemDef, originalValues, task, result));
        }
        return normalizedItems;
    }

    public static @NotNull ValueNormalizer getDefaultNormalizer() {
        return (input, task, result) ->
                PrismContext.get().getDefaultPolyStringNormalizer().normalize(input);
    }

    public static @NotNull String normalizeValue(
            @NotNull Object originalRealValue,
            @NotNull ValueNormalizer normalizer,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        String stringValue;
        if (originalRealValue instanceof PolyString) {
            stringValue = ((PolyString) originalRealValue).getOrig();
        } else if (originalRealValue instanceof String) {
            stringValue = (String) originalRealValue;
        } else {
            throw new UnsupportedOperationException(
                    String.format("Only string or polystring identity items are supported yet: '%s' is %s",
                            originalRealValue, originalRealValue.getClass()));
        }
        return normalizer.normalize(stringValue, task, result);
    }

    private Collection<? extends ItemDelta<?, ?>> computeIndexingDeltas(
            @NotNull FocusType expectedNewFocus,
            IdentityItemsType indexData)
            throws SchemaException {

        FocusIdentityType matching = FocusTypeUtil.getMatchingIdentity(expectedNewFocus, null);
        if (matching == null) {
            LOGGER.trace("No matching identity in focus object -> adding the value 'as is'");
            return prismContext.deltaFor(FocusType.class)
                    .item(SchemaConstants.PATH_IDENTITY)
                    .add(createIdentityForIndexData(indexData))
                    .asItemDeltas();
        } else {
            FocusIdentityItemsType existingItems = matching.getItems();
            IdentityItemsType existingIndexData = existingItems != null ? existingItems.getNormalized() : null;
            // We clone the identity to remove path information from it (the root will be the identity PCV itself).
            //noinspection unchecked
            PrismContainerValue<IdentityItemsType> existingIndexPcvCloned =
                    (existingIndexData != null ? existingIndexData.clone() : new IdentityItemsType()).asPrismContainerValue();
            LOGGER.trace("Matching identity bean found -> computing a delta; matching old value is:\n{}",
                    existingIndexPcvCloned.debugDumpLazily(1));
            //noinspection rawtypes
            Collection<? extends ItemDelta> differences =
                    existingIndexPcvCloned.diff(
                            indexData.asPrismContainerValue(),
                            EquivalenceStrategy.DATA);
            // Now we re-add the path information to the item deltas
            differences.forEach(
                    delta ->
                            delta.setParentPath(
                                    ItemPath.create(
                                            SchemaConstants.PATH_IDENTITY,
                                            matching.asPrismContainerValue().getId(),
                                            FocusIdentityType.F_ITEMS,
                                            FocusIdentityItemsType.F_NORMALIZED,
                                            delta.getParentPath())));
            LOGGER.trace("Computed identity deltas:\n{}", DebugUtil.debugDumpLazily(differences, 1));
            //noinspection CastCanBeRemovedNarrowingVariableType,unchecked
            return (Collection<? extends ItemDelta<?, ?>>) differences;
        }
    }
}

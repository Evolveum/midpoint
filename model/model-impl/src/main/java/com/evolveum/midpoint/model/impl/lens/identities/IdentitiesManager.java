/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.identities;

import static com.evolveum.midpoint.prism.PrismContainerValue.getId;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.util.FocusIdentitiesTypeUtil;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Manages `identities` container in focal objects.
 *
 * . For "own" identity it tries to update that information according to "primary" focus data.
 * . For shadow-based identity it updates the data based on what has been provided by the caller.
 *
 * PRELIMINARY/LIMITED IMPLEMENTATION.
 *
 * . Only single-valued items.
 * . Only `String` and `PolyString` types are supported.
 * . All correlation item names are in `common-3` namespace. This may be good or bad; reconsider eventually.
 * Maybe we should use original (extension) namespaces for items from extensions?
 */
@Component
public class IdentitiesManager {

    private static final Trace LOGGER = TraceManager.getTrace(IdentitiesManager.class);

    @Autowired private PrismContext prismContext;

    /**
     * Updates "own identity" information in object being added.
     */
    public <O extends ObjectType> void applyOnElementAdd(
            @NotNull O objectToAdd,
            @NotNull LensElementContext<O> elementContext) throws ConfigurationException, SchemaException {
        IdentityManagementConfiguration configuration = getIdentityManagementConfiguration(elementContext);
        if (configuration == null) {
            LOGGER.trace("No identity management configuration for {}: identity data will not be updated", elementContext);
            return;
        }
        if (!(objectToAdd instanceof FocusType)) {
            LOGGER.trace("Not a FocusType: {}", objectToAdd);
            return;
        }
        FocusType focusToAdd = (FocusType) objectToAdd;
        FocusTypeUtil.addOrReplaceIdentity(focusToAdd,
                computeOwnIdentity(focusToAdd, configuration));
    }

    /**
     * Adds necessary changes to "own identity" information to `delta` parameter.
     */
    public <O extends ObjectType> void applyOnElementModify(
            O current, // we accept null values here but only for non-essential cases (i.e. no identity data to be updated)
            @NotNull ObjectDelta<O> delta,
            @NotNull Class<O> objectClass,
            @NotNull LensElementContext<O> elementContext) throws SchemaException, ConfigurationException {
        IdentityManagementConfiguration configuration = getIdentityManagementConfiguration(elementContext);
        if (configuration == null) {
            LOGGER.trace("No identity management configuration for {}: identity data will not be updated", elementContext);
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
        FocusType expectedNewFocus = (FocusType) computeExpectedNewFocus(current, delta);
        FocusIdentityType expectedOwnIdentity = computeOwnIdentity(expectedNewFocus, configuration);
        delta.addModifications(
                computeIdentityDeltas(expectedNewFocus, List.of(expectedOwnIdentity)));
    }

    @Nullable public IdentityManagementConfiguration getIdentityManagementConfiguration(@NotNull LensContext<?> context) {
        LensFocusContext<?> focusContext = context.getFocusContext();
        if (focusContext == null) {
            LOGGER.trace("No focus context");
            return null;
        } else {
            return focusContext.getIdentityManagementConfiguration();
        }
    }

    @Nullable private IdentityManagementConfiguration getIdentityManagementConfiguration(
            @NotNull LensElementContext<?> elementContext) {
        if (elementContext instanceof LensFocusContext<?>) {
            return ((LensFocusContext<?>) elementContext).getIdentityManagementConfiguration();
        } else {
            LOGGER.trace("Not a LensFocusContext: {}", elementContext);
            return null;
        }
    }

    /** If we were brave enough, we could probably use 'object new' from element context. But this is perhaps safer. */
    private <O extends ObjectType> O computeExpectedNewFocus(O current, ObjectDelta<O> delta) throws SchemaException {
        if (delta.isEmpty()) {
            return current;
        }
        //noinspection unchecked
        O clone = (O) current.clone();
        //noinspection unchecked
        delta.applyTo(
                (PrismObject<O>) clone.asPrismObject());
        return clone;
    }

    /** Computes "own" identity from scratch. */
    private @NotNull FocusIdentityType computeOwnIdentity(
            @NotNull FocusType focus,
            @NotNull IdentityManagementConfiguration configuration) throws ConfigurationException, SchemaException {
        IdentityItemsType original = new IdentityItemsType();
        for (IdentityItemConfiguration itemConfig : configuration.getItems()) {
            ItemPath path = itemConfig.getPath();
            Item<?, ?> item = focus.asPrismObject().findItem(path);
            if (item != null) {
                Item<?, ?> clone = item.clone();
                ItemDefinition<?> definition = clone.getDefinition();
                if (definition != null && !definition.isDynamic()) {
                    MutableItemDefinition<?> definitionClone = definition.clone().toMutable();
                    definitionClone.setDynamic(true);
                    //noinspection unchecked,rawtypes
                    ((Item) clone).setDefinition(definitionClone);
                }
                //noinspection unchecked
                original.asPrismContainerValue().add(clone);
            }
        }
        IdentityItemsType normalized = normalize(original, configuration);

        FocusIdentityType identity = new FocusIdentityType()
                .items(new FocusIdentityItemsType()
                        .original(original)
                        .normalized(normalized));
        LOGGER.trace("Computed own identity:\n{}", identity.debugDumpLazily(1));
        return identity;
    }

    private @NotNull IdentityItemsType normalize(
            @NotNull IdentityItemsType original,
            @NotNull IdentityManagementConfiguration configuration) throws SchemaException {

        IdentityItemsType normalized = new IdentityItemsType();
        for (Item<?, ?> originalItem : ((PrismContainerValue<?>) original.asPrismContainerValue()).getItems()) {
            //noinspection unchecked
            normalized.asPrismContainerValue().addAll(
                    normalizeItem(originalItem, configuration));
        }
        return normalized;
    }

    // TODO take configuration into account
    private Collection<? extends Item<?, ?>> normalizeItem(
            Item<?, ?> originalItem, IdentityManagementConfiguration configuration) {
        if (originalItem.isEmpty()) {
            return List.of();
        }
        ItemName itemName = originalItem.getElementName();
        MutablePrismPropertyDefinition<String> normalizedItemDef =
                prismContext.definitionFactory().createPropertyDefinition(itemName, DOMUtil.XSD_STRING);
        normalizedItemDef.setDynamic(true);
        PrismProperty<String> normalizedItem =
                prismContext.itemFactory().createProperty(
                        itemName,
                        normalizedItemDef);
        for (PrismValue originalValue : originalItem.getValues()) {
            Object originalRealValue = originalValue.getRealValue();
            if (originalRealValue != null) {
                normalizedItem.addRealValue(
                        normalizeValue(originalRealValue, null));
            } else {
                LOGGER.warn("No real value in {} in {}", originalValue, originalItem);
            }
        }
        return List.of(normalizedItem);
    }

    private @NotNull String normalizeValue(
            @NotNull Object originalRealValue,
            @Nullable IdentityItemConfiguration itemConfiguration) {
        String stringValue;
        if (originalRealValue instanceof PolyString) {
            stringValue = ((PolyString) originalRealValue).getOrig();
        } else if (originalRealValue instanceof String) {
            stringValue = (String) originalRealValue;
        } else {
            throw new UnsupportedOperationException(
                    String.format("Only string or polystring identity items are supported yet: '%s' of %s is %s",
                            originalRealValue, itemConfiguration, originalRealValue.getClass()));
        }
        // TODO normalize according to the configuration
        return prismContext.getDefaultPolyStringNormalizer().normalize(stringValue);
    }

    private Collection<? extends ItemDelta<?, ?>> computeIdentityDeltas(
            @NotNull FocusType expectedNewFocus,
            @NotNull Collection<FocusIdentityType> identities)
            throws SchemaException {

        FocusIdentitiesType identitiesInNewFocus = expectedNewFocus.getIdentities();
        if (identitiesInNewFocus == null) {
            LOGGER.trace("No identities in focus object -> adding values 'as they are':\n{}",
                    DebugUtil.debugDumpLazily(identities, 1));
            return prismContext.deltaFor(FocusType.class)
                    .item(FocusType.F_IDENTITIES, FocusIdentitiesType.F_IDENTITY)
                    .addRealValues(identities)
                    .asItemDeltas();
        } else {
            List<ItemDelta<?, ?>> itemDeltas = new ArrayList<>();
            for (FocusIdentityType identityBean : identities) {
                addOrReplaceIdentityBean(identityBean, identitiesInNewFocus, itemDeltas);
            }
            return itemDeltas;
        }
    }

    private void addOrReplaceIdentityBean(
            @NotNull FocusIdentityType identityBean,
            @NotNull FocusIdentitiesType currentIdentities,
            @NotNull List<ItemDelta<?, ?>> itemDeltas) throws SchemaException {
        LOGGER.trace("Adding or replacing identity bean: {}", identityBean);
        FocusIdentityType matching = FocusIdentitiesTypeUtil.getMatchingIdentity(currentIdentities, identityBean.getSource());
        if (matching == null) {
            LOGGER.trace("No matching identity bean -> adding this one:\n{}", identityBean.debugDumpLazily(1));
            itemDeltas.add(
                    prismContext.deltaFor(FocusType.class)
                            .item(FocusType.F_IDENTITIES, FocusIdentitiesType.F_IDENTITY)
                            .add(identityBean)
                            .asItemDelta());
        } else {
            LOGGER.trace("Matching identity bean found -> computing a delta");
            //noinspection rawtypes
            Collection<? extends ItemDelta> differences =
                    matching.asPrismContainerValue().diff(
                            identityBean.asPrismContainerValue(),
                            EquivalenceStrategy.DATA);
            LOGGER.trace("Computed identity deltas:\n{}", DebugUtil.debugDumpLazily(differences, 1));
            //noinspection CastCanBeRemovedNarrowingVariableType,unchecked
            itemDeltas.addAll((Collection<? extends ItemDelta<?, ?>>) differences);
        }
    }

    /**
     * Computes deltas that synchronize `original` and `normalized` sections of identity data.
     * To spare processing time, looks at `secondaryDelta` to know what has been changed.
     */
    public @NotNull Collection<? extends ItemDelta<?, ?>> computeNormalizationDeltas(
            @Nullable FocusType objectNew,
            @Nullable ObjectDelta<?> secondaryDelta,
            @NotNull IdentityManagementConfiguration configuration) throws SchemaException {

        LOGGER.trace("Normalizing focus identity data from inbound mapping output(s)");
        if (objectNew == null) {
            LOGGER.trace("No 'object new' -> nothing to normalize");
            return List.of();
        }
        FocusIdentitiesType identitiesBean = objectNew.getIdentities();
        if (identitiesBean == null) {
            LOGGER.trace("No identities -> nothing to normalize");
            return List.of();
        }
        if (ObjectDelta.isEmpty(secondaryDelta)) {
            LOGGER.trace("No secondary delta -> nothing to normalize");
            return List.of();
        }

        List<ItemDelta<?, ?>> normalizationDeltas = new ArrayList<>();

        Set<Long> changedIds = getChangedIdentityIds(secondaryDelta);
        boolean all = changedIds.contains(null); // just for sure

        List<FocusIdentityType> identityList = identitiesBean.getIdentity();
        for (FocusIdentityType identityBean : identityList) {
            Long id = identityBean.getId();
            if (id == null) {
                LOGGER.warn("No-ID identity bean in {}: {}", objectNew, identitiesBean);
                continue;
            }
            if (all || changedIds.contains(id)) {
                LOGGER.trace("Going to normalize identity bean with ID {}", id);
                computeNormalizationDeltas(identityBean, configuration, normalizationDeltas);
            } else {
                LOGGER.trace("No need to normalize identity bean with ID {}", id);
            }
        }

        return normalizationDeltas;
    }

    private void computeNormalizationDeltas(
            FocusIdentityType identityBean,
            IdentityManagementConfiguration configuration,
            List<ItemDelta<?, ?>> normalizationDeltas) throws SchemaException {

        // We go through all items in given identity. (Not only those that have been changed.)

        FocusIdentityItemsType bothItems = identityBean.getItems();
        if (bothItems == null) {
            LOGGER.trace("No items");
            return;
        }

        IdentityItemsType original = bothItems.getOriginal();
        IdentityItemsType oldNormalized = bothItems.getNormalized();
        IdentityItemsType newNormalized = original != null ? normalize(original, configuration) : null;
        ItemPath normalizedItemPath = bothItems.asPrismContainerValue().getPath().append(FocusIdentityItemsType.F_NORMALIZED);
        Collection<? extends ItemDelta<?, ?>> deltas = computeDeltas(normalizedItemPath, oldNormalized, newNormalized);

        LOGGER.trace("Computed identity deltas:\n{}", DebugUtil.debugDumpLazily(deltas, 1));
        normalizationDeltas.addAll(deltas);
    }

    private Collection<? extends ItemDelta<?, ?>> computeDeltas(
            ItemPath normalizedItemPath, IdentityItemsType oldNormalized, IdentityItemsType newNormalized)
            throws SchemaException {
        if (newNormalized == null) {
            if (oldNormalized != null) {
                return prismContext.deltaFor(FocusType.class)
                        .item(normalizedItemPath)
                        .replace()
                        .asItemDeltas();
            } else {
                return List.of();
            }
        } else {
            if (oldNormalized == null) {
                return prismContext.deltaFor(FocusType.class)
                        .item(normalizedItemPath)
                        .replace(newNormalized)
                        .asItemDeltas();
            } else {
                //noinspection unchecked
                return (Collection<? extends ItemDelta<?, ?>>) oldNormalized.asPrismContainerValue().diff(
                        newNormalized.asPrismContainerValue(),
                        EquivalenceStrategy.DATA);
            }
        }
    }

    @NotNull private Set<Long> getChangedIdentityIds(ObjectDelta<?> secondaryDelta) {
        ItemPath identityPrefix = ItemPath.create(FocusType.F_IDENTITIES, FocusIdentitiesType.F_IDENTITY);
        stateCheck(secondaryDelta.isModify(), "Secondary delta is not a modify delta: %s", secondaryDelta);
        Set<Long> changedIds = new HashSet<>();
        for (ItemDelta<?, ?> modification : secondaryDelta.getModifications()) {
            ItemPath modifiedItemPath = modification.getPath();
            if (modifiedItemPath.startsWith(identityPrefix)) {
                ItemPath rest = modifiedItemPath.rest(2);
                if (rest.startsWithId()) {
                    changedIds.add(rest.firstToId());
                } else if (rest.isEmpty()) {
                    for (PrismValue value : emptyIfNull(modification.getValuesToAdd())) {
                        changedIds.add(getId(value));
                    }
                    for (PrismValue value : emptyIfNull(modification.getValuesToReplace())) {
                        changedIds.add(getId(value));
                    }
                }
            }
        }
        LOGGER.trace("Changed identity beans: {}", changedIds);
        return changedIds;
    }
}

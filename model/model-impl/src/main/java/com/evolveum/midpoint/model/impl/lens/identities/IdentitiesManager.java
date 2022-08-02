/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.identities;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.FocusIdentityTypeUtil;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
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
 * . Only `String` and `PolyString` types are supported.
 */
@Component
public class IdentitiesManager {

    private static final Trace LOGGER = TraceManager.getTrace(IdentitiesManager.class);

    @Autowired private PrismContext prismContext;

    /**
     * Updates normalized identity data on focus add.
     */
    public <O extends ObjectType> void applyOnElementAdd(
            @NotNull O objectToAdd,
            @NotNull LensElementContext<O> elementContext) throws ConfigurationException, SchemaException {
        IdentityManagementConfiguration configuration = getIdentityManagementConfiguration(elementContext);
        if (configuration == null || configuration.hasNoItems()) {
            LOGGER.trace("No identity management configuration for {}: identity data will not be updated", elementContext);
            return;
        }
        if (!(objectToAdd instanceof FocusType)) {
            LOGGER.trace("Not a FocusType: {}", objectToAdd);
            return;
        }
        FocusType focusToAdd = (FocusType) objectToAdd;
        FocusTypeUtil.addOrReplaceIdentity(focusToAdd,
                computeNormalizedIdentityData(focusToAdd, configuration));
    }

    /**
     * Updates normalized identity data on focus add (by adding necessary changes to the `delta` parameter.)
     */
    public <O extends ObjectType> void applyOnElementModify(
            O current, // we accept null values here but only for non-essential cases (i.e. no identity data to be updated)
            @NotNull ObjectDelta<O> delta,
            @NotNull Class<O> objectClass,
            @NotNull LensElementContext<O> elementContext) throws SchemaException, ConfigurationException {
        IdentityManagementConfiguration configuration = getIdentityManagementConfiguration(elementContext);
        if (configuration == null || configuration.hasNoItems()) {
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
        FocusType expectedNew =
                (FocusType) Objects.requireNonNull(
                        elementContext.getObjectNew(),
                        () -> String.format("Expected 'new' focal object is null: %s (it shouldn't be, as we are modifying it)",
                                elementContext))
                        .asObjectable();

        delta.addModifications(
                computeIdentityDeltas(
                        expectedNew,
                        computeNormalizedIdentityData(expectedNew, configuration)));
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

    /**
     * Computes "own" identity that contains all the normalized data.
     * (Grouped together from the focal object and all identities.)
     *
     * This is the preliminary behavior. To be confirmed or changed later.
     */
    private @NotNull FocusIdentityType computeNormalizedIdentityData(
            @NotNull FocusType focus,
            @NotNull IdentityManagementConfiguration configuration) throws ConfigurationException, SchemaException {
        IdentityItemsType normalized = new IdentityItemsType();
        for (IdentityItemConfiguration itemConfig : configuration.getItems()) {
            ItemPath path = itemConfig.getPath();
            ItemDefinition<?> itemDef =
                    MiscUtil.requireNonNull(
                            focus.asPrismObject().getDefinition().findItemDefinition(path),
                            () -> String.format("No definition of identity item '%s' in %s", path, focus));
            Collection<PrismValue> allValues = collectAllValues(focus, path);
            //noinspection unchecked
            normalized.asPrismContainerValue().addAll(
                    normalizeItemValues(itemDef, allValues, itemConfig));
        }
        FocusIdentityType identity = new FocusIdentityType()
                .items(new FocusIdentityItemsType()
                        .normalized(normalized));
        LOGGER.trace("Computed normalized identity:\n{}", identity.debugDumpLazily(1));
        return identity;
    }

    private Collection<PrismValue> collectAllValues(@NotNull FocusType focus, @NotNull ItemPath path) {
        List<PrismValue> allRealValues = new ArrayList<>();
        allRealValues.addAll(
                focus.asPrismContainerValue().getAllValues(path));
        allRealValues.addAll(
                focus.asPrismContainerValue().getAllValues(SchemaConstants.PATH_IDENTITY.append(FocusIdentityType.F_DATA, path)));
        return allRealValues;
    }

    // TODO take configuration into account
    private Collection<? extends Item<?, ?>> normalizeItemValues(
            ItemDefinition<?> itemDef, Collection<PrismValue> prismValues, IdentityItemConfiguration itemConfig) {
        if (prismValues.isEmpty()) {
            return List.of();
        }
        ItemName itemName = itemDef.getItemName();
        MutablePrismPropertyDefinition<String> normalizedItemDef =
                prismContext.definitionFactory().createPropertyDefinition(itemName, DOMUtil.XSD_STRING); // TODO generalize
        normalizedItemDef.setDynamic(true);
        normalizedItemDef.setMinOccurs(0);
        normalizedItemDef.setMaxOccurs(-1);
        PrismProperty<String> normalizedItem =
                prismContext.itemFactory().createProperty(
                        itemName,
                        normalizedItemDef);
        for (PrismValue originalValue : prismValues) {
            Object originalRealValue = originalValue.getRealValue();
            if (originalRealValue != null) {
                normalizedItem.addRealValue(
                        normalizeValue(originalRealValue, itemConfig));
            } else {
                LOGGER.warn("No real value in {} in {}", originalValue, itemDef);
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
            @NotNull FocusIdentityType newIdentity)
            throws SchemaException {

        FocusIdentityType matching = FocusTypeUtil.getMatchingIdentity(expectedNewFocus, newIdentity.getSource());
        if (matching == null) {
            LOGGER.trace("No matching identity in focus object -> adding the value 'as is'");
            return prismContext.deltaFor(FocusType.class)
                    .item(SchemaConstants.PATH_IDENTITY)
                    .add(newIdentity)
                    .asItemDeltas();
        } else {
            LOGGER.trace("Matching identity bean found -> computing a delta");
            //noinspection rawtypes
            Collection<? extends ItemDelta> differences =
                    matching.asPrismContainerValue().diff(
                            newIdentity.asPrismContainerValue(),
                            EquivalenceStrategy.DATA);
            LOGGER.trace("Computed identity deltas:\n{}", DebugUtil.debugDumpLazily(differences, 1));
            //noinspection CastCanBeRemovedNarrowingVariableType,unchecked
            return (Collection<? extends ItemDelta<?, ?>>) differences;
        }
    }

    /** See {@link MidpointFunctions#selectIdentityItemValues(Collection, FocusIdentitySourceType, ItemPath)}. */
    public @NotNull Collection<PrismValue> selectIdentityItemValue(
            @Nullable Collection<FocusIdentityType> identities,
            @Nullable FocusIdentitySourceType source,
            @NotNull ItemPath itemPath) {

        Set<PrismValue> selected = new HashSet<>();
        for (FocusIdentityType identityBean : emptyIfNull(identities)) {
            if (source != null) {
                if (!FocusIdentityTypeUtil.matches(identityBean, source)) {
                    continue;
                }
            } else {
                // null source means "any non-own"
                if (FocusIdentityTypeUtil.isOwn(identityBean)) {
                    continue;
                }
            }
            PrismProperty<?> property = identityBean.asPrismContainerValue().findProperty(
                    ItemPath.create(FocusIdentityType.F_DATA, itemPath));
            if (property != null) {
                selected.addAll(property.getClonedValues());
            }
        }
        return selected;
    }
}

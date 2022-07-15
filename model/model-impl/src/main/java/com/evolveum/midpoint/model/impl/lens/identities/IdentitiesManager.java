/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.identities;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_C;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Manages `identities` container in focal objects.
 *
 * For "own" identity it tries to update that information according to "primary" focus data.
 *
 * "Resource object" identities are not implemented yet.
 *
 * PRELIMINARY/LIMITED IMPLEMENTATION. See {@link #computeOwnIdentity(FocusType, IdentityManagementConfiguration)}
 * for details.
 */
@Component
public class IdentitiesManager {

    private static final Trace LOGGER = TraceManager.getTrace(IdentitiesManager.class);

    /**
     * Updates "own identity" information in object being added.
     */
    public <O extends ObjectType> void applyOnAdd(
            @NotNull O objectToAdd,
            @NotNull LensElementContext<O> elementContext) throws ConfigurationException, SchemaException {
        IdentityManagementConfiguration configuration = getIdentityManagementConfiguration(elementContext);
        if (configuration == null) {
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
    public <O extends ObjectType> void applyOnModify(
            O current, // should not be null as well
            @NotNull ObjectDelta<O> delta,
            @NotNull Class<O> objectClass,
            @NotNull LensElementContext<O> elementContext) throws SchemaException, ConfigurationException {
        IdentityManagementConfiguration configuration = getIdentityManagementConfiguration(elementContext);
        if (configuration == null) {
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
                computeOwnIdentityDeltas(expectedNewFocus, expectedOwnIdentity));
    }

    @Nullable private <E extends ObjectType> IdentityManagementConfiguration getIdentityManagementConfiguration(
            @NotNull LensElementContext<E> elementContext) {
        if (!(elementContext instanceof LensFocusContext<?>)) {
            LOGGER.trace("Not a LensFocusContext: {}, identity data will not be updated", elementContext);
            return null;
        }
        LensFocusContext<E> focusContext = (LensFocusContext<E>) elementContext;
        IdentityManagementConfiguration configuration = focusContext.getIdentityManagementConfiguration();
        if (configuration == null) {
            LOGGER.trace("No identity management configuration; identity data will not be updated for {}", focusContext);
            return null;
        }
        return configuration;
    }

    /** If we were brave enough, we could probably use 'object new' from element context. But this is safer. */
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

    /**
     * Computes "own" identity from scratch. This is perhaps the most safe way: compute and then compare.
     *
     * LIMITED IMPLEMENTATION.
     *
     * . Only single-valued items.
     * . Only `String` and `PolyString` types are supported.
     * . All correlation item names are in `common-3` namespace. This may be good or bad; reconsider eventually.
     * Maybe we should use original (extension) namespaces for items from extensions?
     */
    private @NotNull FocusIdentityType computeOwnIdentity(
            @NotNull FocusType focus,
            @NotNull IdentityManagementConfiguration configuration) throws ConfigurationException, SchemaException {
        FocusIdentityType identity =
                new FocusIdentityType()
                        .source(new OwnFocusIdentitySourceType());
        for (IdentityItemConfiguration itemConfig : configuration.getItems()) {
            ItemPath path = itemConfig.getPath();
            Item<?, ?> item = focus.asPrismObject().findItem(path);
            List<? extends PrismValue> values = item.getValues();
            if (values.size() > 1) {
                throw new UnsupportedOperationException(
                        String.format("Couldn't use multi-valued item '%s' (%s) as identity item in %s",
                                path, values, focus));
            } else if (values.size() == 1) {
                addIdentityItem(identity, itemConfig, values.get(0));
            }
        }
        LOGGER.trace("Computed own identity:\n{}", identity.debugDumpLazily(1));
        return identity;
    }

    private void addIdentityItem(FocusIdentityType identity, IdentityItemConfiguration itemConfig, PrismValue value)
            throws ConfigurationException, SchemaException {
        Object realValue = value.getRealValue();
        if (realValue == null) {
            return; // should not occur
        }
        String original;
        if (realValue instanceof PolyString) {
            original = ((PolyString) realValue).getOrig();
        } else if (realValue instanceof String) {
            original = (String) realValue;
        } else {
            throw new UnsupportedOperationException(
                    String.format("Only string or polystring identity items are supported yet: '%s' of %s is %s",
                            realValue, itemConfig, realValue.getClass()));
        }
        String normalized = PrismContext.get().getDefaultPolyStringNormalizer().normalize(original);
        addIdentityItemValues(identity, itemConfig.getName(), original, normalized);
    }

    private void addIdentityItemValues(FocusIdentityType identity, String name, String originalValue, String normalizedValue)
            throws ConfigurationException, SchemaException {
        FocusIdentityItemsType itemsBean = identity.getItems();
        if (itemsBean == null) {
            identity.setItems(itemsBean = new FocusIdentityItemsType());
        }
        IdentityItemsType originalBean = itemsBean.getOriginal();
        if (originalBean == null) {
            itemsBean.setOriginal(originalBean = new IdentityItemsType());
        }
        IdentityItemsType normalizedBean = itemsBean.getNormalized();
        if (normalizedBean == null) {
            itemsBean.setNormalized(normalizedBean = new IdentityItemsType());
        }
        addIdentityItemValue(originalBean, name, originalValue);
        addIdentityItemValue(normalizedBean, name, normalizedValue);
    }

    private void addIdentityItemValue(IdentityItemsType itemsBean, String name, String value)
            throws ConfigurationException, SchemaException {
        PrismContainerValue<?> itemsPcv = itemsBean.asPrismContainerValue();
        ItemName qName = new ItemName(NS_C, name);
        Item<?, ?> existingItem = itemsPcv.findItem(qName);
        configCheck(existingItem == null, "Multiple uses of identity item name: %s", name);

        PrismContext prismContext = PrismContext.get();
        MutablePrismPropertyDefinition<String> propertyDefinition =
                prismContext.definitionFactory().createPropertyDefinition(qName, DOMUtil.XSD_STRING);
        propertyDefinition.setDynamic(true);
        PrismProperty<String> property = prismContext.itemFactory().createProperty(qName, propertyDefinition);
        property.addRealValue(value);
        itemsPcv.add(property);
    }

    private Collection<? extends ItemDelta<?, ?>> computeOwnIdentityDeltas(
            FocusType expectedNewFocus, FocusIdentityType expectedOwn)
            throws SchemaException {
        // Note that existing own identity may contain changes induced by the deltas. (If explicitly requested.)
        FocusIdentityType existingOwnIdentity = FocusTypeUtil.getOwnIdentity(expectedNewFocus);
        if (existingOwnIdentity == null) {
            return PrismContext.get().deltaFor(FocusType.class)
                    .item(FocusType.F_IDENTITIES, FocusIdentitiesType.F_IDENTITY)
                    .add(expectedOwn)
                    .asItemDeltas();
        }
        Long id = existingOwnIdentity.getId();
        stateCheck(id != null,
                "PCV ID of own identity data is not null. Are you trying to add identity data explicitly?"
                        + "This is currently not supported. In: %s", expectedNewFocus);
        //noinspection rawtypes
        PrismContainerValue existingOwnPcv = existingOwnIdentity.asPrismContainerValue();
        //noinspection rawtypes
        Collection<? extends ItemDelta> differences =
                existingOwnPcv.diff(
                        expectedOwn.asPrismContainerValue(),
                        EquivalenceStrategy.DATA); // reconsider the strategy if needed
        LOGGER.trace("Computed identity deltas:\n{}", DebugUtil.debugDumpLazily(differences, 1));
        //noinspection CastCanBeRemovedNarrowingVariableType,unchecked
        return (Collection<? extends ItemDelta<?, ?>>) differences;
    }
}

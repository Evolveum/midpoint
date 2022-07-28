/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.identities;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.delta.ItemDelta;
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

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_C;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;

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

    /**
     * Adds necessary changes to shadow-based identity information.
     * Calls appropriate delta-manipulation methods on lens focus context.
     *
     * Unlike "own" identities, here the caller computes the identity beans (with the help of methods in this class).
     * The reason is that computation of identity beans from inbound mappings is strongly bound to inbound processing itself.
     */
    public <F extends FocusType> void applyOnInbounds(
            @NotNull Collection<FocusIdentityType> identities,
            @NotNull LensContext<F> context) throws SchemaException {
        LOGGER.trace("applyOnInbounds starting");
        LensFocusContext<F> focusContext = context.getFocusContextRequired();
        F expectedNewFocus = asObjectable(focusContext.getObjectNew());
        if (expectedNewFocus == null) {
            LOGGER.trace("The focus is going to be deleted, no point in computing identity deltas");
        } else {
            focusContext.swallowToSecondaryDelta(
                    computeIdentityDeltas(expectedNewFocus, identities));
        }
        LOGGER.trace("applyOnInbounds done");
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
        FocusIdentityType identity = new FocusIdentityType();
        for (IdentityItemConfiguration itemConfig : configuration.getItems()) {
            ItemPath path = itemConfig.getPath();
            Item<?, ?> item = focus.asPrismObject().findItem(path);
            if (item != null) {
                List<? extends PrismValue> values = item.getValues();
                if (values.size() > 1) {
                    throw new UnsupportedOperationException(
                            String.format("Couldn't use multi-valued item '%s' (%s) as identity item in %s",
                                    path, values, focus));
                } else if (values.size() == 1) {
                    addIdentityItem(identity, itemConfig, values.get(0));
                }
            }
        }
        LOGGER.trace("Computed own identity:\n{}", identity.debugDumpLazily(1));
        return identity;
    }

    public void addIdentityItem(
            @NotNull FocusIdentityType identity,
            @NotNull IdentityItemConfiguration itemConfig,
            @NotNull PrismValue value)
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

    public @NotNull Collection<? extends ItemDelta<?, ?>> computeNormalizationDeltas(
            @NotNull FocusType objectNew,
            @NotNull Set<Long> changedIds,
            @NotNull IdentityManagementConfiguration configuration) {

        List<ItemDelta<?, ?>> normalizationDeltas = new ArrayList<>();

        boolean all = changedIds.contains(null);
        FocusIdentitiesType identitiesBean = objectNew.getIdentities();
        if (identitiesBean == null) {
            return normalizationDeltas;
        }
        List<FocusIdentityType> identityList = identitiesBean.getIdentity();
        for (FocusIdentityType identityBean : identityList) {
            Long id = identityBean.getId();
            if (id == null) {
                LOGGER.warn("No-ID identity bean in {}: {}", objectNew, identitiesBean);
                continue;
            }
            if (all || changedIds.contains(id)) {
                computeNormalizationDeltas(identityBean, configuration, normalizationDeltas);
            }
        }

        return normalizationDeltas;
    }

    private void computeNormalizationDeltas(
            FocusIdentityType identityBean,
            IdentityManagementConfiguration configuration,
            List<ItemDelta<?, ?>> normalizationDeltas) {

        // TODO

    }
}

/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsNormalizationUtil;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManagerMiscUtil.determinePrimaryIdentifierValue;

/**
 * Computes deltas to be applied to repository shadows.
 *
 * Works in two situations:
 *
 * . When an object (optionally with the delta) was obtained from the resource. Here the computation uses the absolute state
 * of the resource object as the information source (helped with the observed delta from the resource).
 * . When an object was created (or attempted to be created) on the resource, and the result is to be written to an existing
 * shadow. So we have to compute deltas for that shadow.
 *
 * These two situations are discriminated by {@link DeltaComputation#fromResource} flag.
 *
 * @see ShadowDeltaComputerRelative
 * @see ShadowUpdater#updateShadowInRepository(ProvisioningContext, ShadowType, ObjectDelta, ShadowType,
 * ShadowLifecycleStateType, OperationResult)
 */
@Component
class ShadowDeltaComputerAbsolute {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowDeltaComputerAbsolute.class);

    @Autowired private Clock clock;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private PrismContext prismContext;

    @NotNull ObjectDelta<ShadowType> computeShadowDelta(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType repoShadow,
            @NotNull ShadowType resourceObject,
            @Nullable ObjectDelta<ShadowType> resourceObjectDelta,
            ShadowLifecycleStateType shadowState, // TODO ensure this is filled-in
            boolean fromResource)
            throws SchemaException, ConfigurationException {
        return new DeltaComputation(ctx, repoShadow, resourceObject, resourceObjectDelta, shadowState, fromResource)
                .execute();
    }

    /**
     * Objects are NOT updated. Only {@link #computedShadowDelta} is created.
     */
    private class DeltaComputation {

        @NotNull private final ProvisioningContext ctx;
        @NotNull private final ShadowType repoShadow;
        @NotNull private final ShadowType resourceObject;
        @Nullable private final ObjectDelta<ShadowType> resourceObjectDelta;
        private final ShadowLifecycleStateType shadowState;
        @NotNull private final ObjectDelta<ShadowType> computedShadowDelta;
        @NotNull private final CachingStrategyType cachingStrategy;
        /**
         * True if the information we deal with (resource object, resource object delta) comes from the resource.
         * False if the shadow was sent to the resource, and the operation might or might not succeeded.
         */
        private final boolean fromResource;

        private DeltaComputation(
                @NotNull ProvisioningContext ctx,
                @NotNull ShadowType repoShadow,
                @NotNull ShadowType resourceObject,
                @Nullable ObjectDelta<ShadowType> resourceObjectDelta,
                ShadowLifecycleStateType shadowState,
                boolean fromResource) {
            this.ctx = ctx;
            this.repoShadow = repoShadow;
            this.resourceObject = resourceObject;
            this.resourceObjectDelta = resourceObjectDelta;
            this.shadowState = shadowState;
            this.computedShadowDelta = repoShadow.asPrismObject().createModifyDelta();
            this.cachingStrategy = ctx.getCachingStrategy();
            this.fromResource = fromResource;
        }

        private @NotNull ObjectDelta<ShadowType> execute()
                throws SchemaException, ConfigurationException {

            Collection<QName> incompleteCacheableItems = new HashSet<>();

            // Note: these updateXXX method work by adding respective deltas (if needed) to the computedShadowDelta
            // They do not change repoShadow nor resourceObject.

            updateAttributes(incompleteCacheableItems);
            updateShadowName();
            updateAuxiliaryObjectClasses();

            if (fromResource) {
                updateExistsFlag();
            } else {
                updatePrimaryIdentifierValue();
            }

            if (fromResource) { // TODO reconsider this
                if (cachingStrategy == CachingStrategyType.NONE) {
                    clearCachingMetadata();
                } else if (cachingStrategy == CachingStrategyType.PASSIVE) {
                    updateCachedActivation();
                    updateCachingMetadata(incompleteCacheableItems);
                } else {
                    throw new ConfigurationException("Unknown caching strategy " + cachingStrategy);
                }
            }
            return computedShadowDelta;
        }

        private void updateShadowName() throws SchemaException {
            PolyString resourceObjectName = ShadowUtil.determineShadowName(resourceObject);
            PolyString repoShadowName = PolyString.toPolyString(repoShadow.getName());
            if (resourceObjectName != null && !resourceObjectName.equalsOriginalValue(repoShadowName)) {
                PropertyDelta<?> shadowNameDelta = prismContext.deltaFactory().property()
                        .createModificationReplaceProperty(
                                ShadowType.F_NAME, repoShadow.asPrismObject().getDefinition(), resourceObjectName);
                computedShadowDelta.addModification(shadowNameDelta);
            }
        }

        private void updateAuxiliaryObjectClasses() {
            PropertyDelta<QName> auxOcDelta = ItemUtil.diff(
                    repoShadow.asPrismObject().findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS),
                    resourceObject.asPrismObject().findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS));
            if (auxOcDelta != null) {
                computedShadowDelta.addModification(auxOcDelta);
            }
        }

        private void updateExistsFlag() {
            // Resource object obviously exists in this case. However, we do not want to mess with isExists flag in some
            // situations (e.g. in CORPSE state) as this existence may be just a quantum illusion.
            if (shadowState == ShadowLifecycleStateType.CONCEIVED || shadowState == ShadowLifecycleStateType.GESTATING) {
                PropertyDelta<Boolean> existsDelta = computedShadowDelta.createPropertyModification(ShadowType.F_EXISTS);
                existsDelta.setRealValuesToReplace(true);
                computedShadowDelta.addModification(existsDelta);
            }
        }

        private void updatePrimaryIdentifierValue() throws SchemaException {
            String newPrimaryIdentifierValue = determinePrimaryIdentifierValue(ctx, resourceObject);
            String existingPrimaryIdentifierValue = repoShadow.getPrimaryIdentifierValue();
            if (!Objects.equals(existingPrimaryIdentifierValue, newPrimaryIdentifierValue)) {
                LOGGER.trace("Existing primary identifier value: {}, new: {}",
                        existingPrimaryIdentifierValue, newPrimaryIdentifierValue);
                computedShadowDelta.addModification(
                        prismContext.deltaFor(ShadowType.class)
                                .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace(newPrimaryIdentifierValue)
                                .asItemDelta()
                );
            }
        }

        private void clearCachingMetadata() {
            if (repoShadow.getCachingMetadata() != null) {
                computedShadowDelta.addModificationReplaceProperty(ShadowType.F_CACHING_METADATA);
            }
        }

        private void updateCachingMetadata(Collection<QName> incompleteCacheableItems) {
            if (incompleteCacheableItems.isEmpty()) {
                CachingMetadataType cachingMetadata = new CachingMetadataType();
                cachingMetadata.setRetrievalTimestamp(clock.currentTimeXMLGregorianCalendar());
                computedShadowDelta.addModificationReplaceProperty(ShadowType.F_CACHING_METADATA, cachingMetadata);
            } else {
                LOGGER.trace("Shadow has incomplete cacheable items; will not update caching timestamp: {}",
                        incompleteCacheableItems);
            }
        }

        private void updateCachedActivation() {
            updatePropertyIfNeeded(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
            updatePropertyIfNeeded(SchemaConstants.PATH_ACTIVATION_VALID_FROM);
            updatePropertyIfNeeded(SchemaConstants.PATH_ACTIVATION_VALID_TO);
            updatePropertyIfNeeded(SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS);
        }

        private <T> void updatePropertyIfNeeded(ItemPath itemPath) {
            PrismProperty<T> currentProperty = resourceObject.asPrismObject().findProperty(itemPath);
            PrismProperty<T> oldProperty = repoShadow.asPrismObject().findProperty(itemPath);
            PropertyDelta<T> itemDelta = ItemUtil.diff(oldProperty, currentProperty);
            if (itemDelta != null && !itemDelta.isEmpty()) {
                computedShadowDelta.addModification(itemDelta);
            }
        }

        private void updateAttributes(Collection<QName> incompleteCacheableAttributes)
                throws SchemaException, ConfigurationException {

            PrismContainer<Containerable> resourceObjectAttributes =
                    resourceObject.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
            PrismContainer<Containerable> repoShadowAttributes =
                    repoShadow.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
            ResourceObjectDefinition ocDef = ctx.computeCompositeObjectDefinition(resourceObject);

            // For complete attributes we can proceed as before: take resourceObjectAttributes as authoritative.
            // If not obtained from the resource, they were created from object delta anyway.
            // However, for incomplete (e.g. index-only) attributes we have to rely on object delta, if present.
            // TODO clean this up! MID-5834

            for (Item<?, ?> currentResourceAttrItem : resourceObjectAttributes.getValue().getItems()) {
                if (currentResourceAttrItem instanceof PrismProperty<?>) {
                    //noinspection unchecked
                    PrismProperty<Object> currentResourceAttrProperty = (PrismProperty<Object>) currentResourceAttrItem;
                    ResourceAttributeDefinition<?> attrDef =
                            ocDef.findAttributeDefinitionRequired(currentResourceAttrProperty.getElementName());
                    if (ProvisioningUtil.shouldStoreAttributeInShadow(ocDef, attrDef.getItemName(), cachingStrategy)) {
                        if (!currentResourceAttrItem.isIncomplete()) {
                            updateAttribute(repoShadowAttributes, currentResourceAttrProperty, attrDef);
                        } else {
                            incompleteCacheableAttributes.add(attrDef.getItemName());
                            if (resourceObjectDelta != null) {
                                LOGGER.trace(
                                        "Resource attribute {} is incomplete but a delta does exist: we'll update the shadow "
                                                + "using the delta", attrDef.getItemName());
                            } else {
                                LOGGER.trace(
                                        "Resource attribute {} is incomplete and object delta is not present: will not update the"
                                                + " shadow with its content", attrDef.getItemName());
                            }
                        }
                    } else {
                        LOGGER.trace("Skipping resource attribute because it's not going to be stored in shadow: {}",
                                attrDef.getItemName());
                    }
                } else {
                    LOGGER.warn("Skipping resource attribute because it's not a PrismProperty (huh?): {}", currentResourceAttrItem);
                }
            }

            for (Item<?, ?> oldRepoItem : repoShadowAttributes.getValue().getItems()) {
                if (oldRepoItem instanceof PrismProperty<?>) {
                    //noinspection unchecked
                    PrismProperty<Object> oldRepoAttrProperty = (PrismProperty<Object>) oldRepoItem;
                    ResourceAttributeDefinition<?> attrDef = ocDef.findAttributeDefinition(oldRepoAttrProperty.getElementName());
                    PrismProperty<Object> currentAttribute =
                            resourceObjectAttributes.findProperty(oldRepoAttrProperty.getElementName());
                    // note: incomplete attributes with no values are not here: they are found in resourceObjectAttributes container
                    if (attrDef == null
                            || !ProvisioningUtil.shouldStoreAttributeInShadow(ocDef, attrDef.getItemName(), cachingStrategy)
                            || currentAttribute == null) {
                        // No definition for this property it should not be there or no current value: remove it from the shadow
                        PropertyDelta<Object> oldRepoAttrPropDelta = oldRepoAttrProperty.createDelta();
                        oldRepoAttrPropDelta.addValuesToDelete(
                                PrismValueCollectionsUtil.cloneCollection(
                                        oldRepoAttrProperty.getValues()));
                        computedShadowDelta.addModification(oldRepoAttrPropDelta);
                    }
                } else {
                    LOGGER.warn("Skipping repo shadow attribute because it's not a PrismProperty (huh?): {}", oldRepoItem);
                }
            }

            if (resourceObjectDelta != null && !incompleteCacheableAttributes.isEmpty()) {
                LOGGER.trace("Found incomplete cacheable attributes: {} while resource object delta is known. "
                        + "We'll update them using the delta.", incompleteCacheableAttributes);
                for (ItemDelta<?, ?> modification : resourceObjectDelta.getModifications()) {
                    if (modification.getPath().startsWith(ShadowType.F_ATTRIBUTES)) {
                        if (QNameUtil.contains(incompleteCacheableAttributes, modification.getElementName())) {
                            LOGGER.trace(" - using: {}", modification);
                            computedShadowDelta.addModification(modification.clone());
                        }
                    }
                }
                incompleteCacheableAttributes.clear(); // So we are OK regarding this. We can update caching timestamp.
            }
        }

        private void updateAttribute(
                PrismContainer<Containerable> oldRepoAttributes,
                PrismProperty<Object> currentResourceAttrProperty,
                ResourceAttributeDefinition<?> attrDef)
                throws SchemaException {
            MatchingRule<Object> matchingRule =
                    matchingRuleRegistry.getMatchingRule(attrDef.getMatchingRuleQName(), attrDef.getTypeName());
            PrismProperty<Object> oldRepoAttributeProperty = oldRepoAttributes.findProperty(attrDef.getItemName());
            if (oldRepoAttributeProperty == null) {
                PropertyDelta<Object> attrAddDelta = currentResourceAttrProperty.createDelta();
                List<PrismPropertyValue<Object>> valuesOnResource = currentResourceAttrProperty.getValues();
                if (attrDef.isIndexOnly()) {
                    // We don't know what is in the repository. We simply want to replace everything with the current values.
                    setNormalizedValuesToReplace(attrAddDelta, valuesOnResource, matchingRule);
                } else {
                    // This is a brutal hack: For extension attributes the ADD operation is slow when using large # of
                    // values to add. So let's do REPLACE instead (this is OK if there are no existing values).
                    // TODO Move this logic to repository. Here it is only for PoC purposes.
                    if (valuesOnResource.size() >= 100) {
                        setNormalizedValuesToReplace(attrAddDelta, valuesOnResource, matchingRule);
                    } else {
                        for (PrismPropertyValue<?> pVal : valuesOnResource) {
                            attrAddDelta.addRealValuesToAdd(matchingRule.normalize(pVal.getValue()));
                        }
                    }
                }
                computedShadowDelta.addModification(attrAddDelta);
            } else {
                if (attrDef.isSingleValue()) {
                    Object currentResourceRealValue = currentResourceAttrProperty.getRealValue();
                    Object currentResourceNormalizedRealValue = matchingRule.normalize(currentResourceRealValue);
                    if (!Objects.equals(currentResourceNormalizedRealValue, oldRepoAttributeProperty.getRealValue())) {
                        PropertyDelta<Object> delta;
                        if (currentResourceNormalizedRealValue != null) {
                            delta = computedShadowDelta.addModificationReplaceProperty(
                                    currentResourceAttrProperty.getPath(), currentResourceNormalizedRealValue);
                        } else {
                            delta = computedShadowDelta.addModificationReplaceProperty(currentResourceAttrProperty.getPath());
                        }
                        delta.setDefinition(currentResourceAttrProperty.getDefinition());
                    }
                } else {
                    PrismProperty<Object> normalizedCurrentResourceAttrProperty = currentResourceAttrProperty.clone();
                    for (PrismPropertyValue<Object> pVal : normalizedCurrentResourceAttrProperty.getValues()) {
                        pVal.setValue(matchingRule.normalize(pVal.getValue()));
                    }
                    PropertyDelta<Object> attrDiff = oldRepoAttributeProperty.diff(normalizedCurrentResourceAttrProperty);
                    if (attrDiff != null && !attrDiff.isEmpty()) {
                        attrDiff.setParentPath(ShadowType.F_ATTRIBUTES);
                        computedShadowDelta.addModification(attrDiff);
                    }
                }
            }
        }

        /** See also {@link ShadowsNormalizationUtil}. */
        private void setNormalizedValuesToReplace(
                PropertyDelta<Object> attrAddDelta,
                List<PrismPropertyValue<Object>> currentValues,
                MatchingRule<Object> matchingRule) throws SchemaException {
            Object[] currentValuesNormalized = new Object[currentValues.size()];
            for (int i = 0; i < currentValues.size(); i++) {
                currentValuesNormalized[i] = matchingRule.normalize(currentValues.get(i).getValue());
            }
            attrAddDelta.setRealValuesToReplace(currentValuesNormalized);
        }
    }
}

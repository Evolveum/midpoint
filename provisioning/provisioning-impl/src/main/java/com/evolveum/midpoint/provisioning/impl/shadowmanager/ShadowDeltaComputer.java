/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadowmanager;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.api.ShadowState;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingStategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 *  Computes deltas to be applied to repository shadows.
 *  This functionality grew too large to deserve special implementation class.
 *
 *  In the future we might move more functionality here and rename this class.
 */
@Component
public class ShadowDeltaComputer {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowDeltaComputer.class);

    @Autowired private Clock clock;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private PrismContext prismContext;

    @NotNull
    ObjectDelta<ShadowType> computeShadowDelta(@NotNull ProvisioningContext ctx,
            @NotNull PrismObject<ShadowType> repoShadowOld, PrismObject<ShadowType> resourceShadowNew,
            ObjectDelta<ShadowType> resourceObjectDelta, ShadowState shadowState)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {

        ObjectDelta<ShadowType> computedShadowDelta = repoShadowOld.createModifyDelta();

        RefinedObjectClassDefinition ocDef = ctx.computeCompositeObjectClassDefinition(resourceShadowNew);
        PrismContainer<Containerable> currentResourceAttributes = resourceShadowNew.findContainer(ShadowType.F_ATTRIBUTES);
        PrismContainer<Containerable> oldRepoAttributes = repoShadowOld.findContainer(ShadowType.F_ATTRIBUTES);
        ShadowType oldRepoShadowType = repoShadowOld.asObjectable();

        CachingStategyType cachingStrategy = ProvisioningUtil.getCachingStrategy(ctx);
        Collection<QName> incompleteCacheableItems = new HashSet<>();

        processAttributes(computedShadowDelta, incompleteCacheableItems, oldRepoAttributes, currentResourceAttributes,
                resourceObjectDelta, ocDef, cachingStrategy);

        PolyString currentShadowName = ShadowUtil.determineShadowName(resourceShadowNew);
        PolyString oldRepoShadowName = repoShadowOld.getName();
        if (!currentShadowName.equalsOriginalValue(oldRepoShadowName)) {
            PropertyDelta<?> shadowNameDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(ShadowType.F_NAME,
                    repoShadowOld.getDefinition(),currentShadowName);
            computedShadowDelta.addModification(shadowNameDelta);
        }

        PropertyDelta<QName> auxOcDelta = ItemUtil.diff(
                repoShadowOld.findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS),
                resourceShadowNew.findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS));
        if (auxOcDelta != null) {
            computedShadowDelta.addModification(auxOcDelta);
        }

        // Resource object obviously exists in this case. However, we do not want to mess with isExists flag in some
        // situations (e.g. in CORPSE state) as this existence may be just a quantum illusion.
        if (shadowState == ShadowState.CONCEPTION || shadowState == ShadowState.GESTATION) {
            PropertyDelta<Boolean> existsDelta = computedShadowDelta.createPropertyModification(ShadowType.F_EXISTS);
            existsDelta.setRealValuesToReplace(true);
            computedShadowDelta.addModification(existsDelta);
        }

        if (cachingStrategy == CachingStategyType.NONE) {
            if (oldRepoShadowType.getCachingMetadata() != null) {
                computedShadowDelta.addModificationReplaceProperty(ShadowType.F_CACHING_METADATA);
            }

        } else if (cachingStrategy == CachingStategyType.PASSIVE) {

            compareUpdateProperty(computedShadowDelta, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, resourceShadowNew, repoShadowOld);
            compareUpdateProperty(computedShadowDelta, SchemaConstants.PATH_ACTIVATION_VALID_FROM, resourceShadowNew, repoShadowOld);
            compareUpdateProperty(computedShadowDelta, SchemaConstants.PATH_ACTIVATION_VALID_TO, resourceShadowNew, repoShadowOld);
            compareUpdateProperty(computedShadowDelta, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, resourceShadowNew, repoShadowOld);

            if (incompleteCacheableItems.isEmpty()) {
                CachingMetadataType cachingMetadata = new CachingMetadataType();
                cachingMetadata.setRetrievalTimestamp(clock.currentTimeXMLGregorianCalendar());
                computedShadowDelta.addModificationReplaceProperty(ShadowType.F_CACHING_METADATA, cachingMetadata);
            } else {
                LOGGER.trace("Shadow has incomplete cacheable items; will not update caching timestamp: {}", incompleteCacheableItems);
            }
        } else {
            throw new ConfigurationException("Unknown caching strategy "+cachingStrategy);
        }
        return computedShadowDelta;
    }

    private void processAttributes(ObjectDelta<ShadowType> computedShadowDelta, Collection<QName> incompleteCacheableAttributes,
            PrismContainer<Containerable> oldRepoAttributes, PrismContainer<Containerable> currentResourceAttributes,
            ObjectDelta<ShadowType> resourceObjectDelta, RefinedObjectClassDefinition ocDef, CachingStategyType cachingStrategy)
            throws SchemaException, ConfigurationException {

        // For complete attributes we can proceed as before: take currentResourceAttributes as authoritative.
        // If not obtained from the resource, they were created from object delta anyway. :)
        // However, for incomplete (e.g. index-only) attributes we have to rely on object delta, if present.
        // TODO clean this up! MID-5834

        for (Item<?, ?> currentResourceAttrItem: currentResourceAttributes.getValue().getItems()) {
            if (currentResourceAttrItem instanceof PrismProperty<?>) {
                //noinspection unchecked
                PrismProperty<Object> currentResourceAttrProperty = (PrismProperty<Object>) currentResourceAttrItem;
                RefinedAttributeDefinition<Object> attrDef = ocDef.findAttributeDefinition(currentResourceAttrProperty.getElementName());
                if (attrDef == null) {
                    throw new SchemaException("No definition of " + currentResourceAttrProperty.getElementName() + " in " + ocDef);
                }
                if (ProvisioningUtil.shouldStoreAttributeInShadow(ocDef, attrDef.getItemName(), cachingStrategy)) {
                    if (!currentResourceAttrItem.isIncomplete()) {
                        processResourceAttribute(computedShadowDelta, oldRepoAttributes, currentResourceAttrProperty, attrDef);
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

        for (Item<?, ?> oldRepoItem: oldRepoAttributes.getValue().getItems()) {
            if (oldRepoItem instanceof PrismProperty<?>) {
                PrismProperty<?> oldRepoAttrProperty = (PrismProperty<?>) oldRepoItem;
                RefinedAttributeDefinition<Object> attrDef = ocDef.findAttributeDefinition(oldRepoAttrProperty.getElementName());
                PrismProperty<Object> currentAttribute = currentResourceAttributes.findProperty(oldRepoAttrProperty.getElementName());
                // note: incomplete attributes with no values are not here: they are found in currentResourceAttributes container
                if (attrDef == null || !ProvisioningUtil.shouldStoreAttributeInShadow(ocDef, attrDef.getItemName(), cachingStrategy) ||
                        currentAttribute == null) {
                    // No definition for this property it should not be there or no current value: remove it from the shadow
                    PropertyDelta<?> oldRepoAttrPropDelta = oldRepoAttrProperty.createDelta();
                    if (oldRepoAttrPropDelta.getDefinition().getTypeName() == null) {
                        throw new SchemaException("No definition in "+oldRepoAttrPropDelta);
                    }
                    //noinspection unchecked
                    oldRepoAttrPropDelta.addValuesToDelete((Collection) PrismValueCollectionsUtil.cloneCollection(oldRepoAttrProperty.getValues()));
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
            incompleteCacheableAttributes.clear();          // So we are OK regarding this. We can update caching timestamp.
        }
    }

    private void processResourceAttribute(ObjectDelta<ShadowType> computedShadowDelta,
            PrismContainer<Containerable> oldRepoAttributes, PrismProperty<Object> currentResourceAttrProperty,
            RefinedAttributeDefinition<Object> attrDef)
            throws SchemaException {
        MatchingRule<Object> matchingRule = matchingRuleRegistry.getMatchingRule(attrDef.getMatchingRuleQName(), attrDef.getTypeName());
        PrismProperty<Object> oldRepoAttributeProperty = oldRepoAttributes.findProperty(attrDef.getItemName());
        if (oldRepoAttributeProperty == null) {
            PropertyDelta<Object> attrAddDelta = currentResourceAttrProperty.createDelta();
            List<PrismPropertyValue<Object>> currentValues = currentResourceAttrProperty.getValues();
            // This is a brutal hack: For extension attributes the ADD operation is slow when using large # of
            // values to add. So let's do REPLACE instead (this is OK if there are no existing values).
            // TODO Move this logic to repository. Here it is only for PoC purposes.
            if (currentValues.size() >= 100) {
                Object[] currentValuesNormalized = new Object[currentValues.size()];
                for (int i = 0; i < currentValues.size(); i++) {
                    currentValuesNormalized[i] = matchingRule.normalize(currentValues.get(i).getValue());
                }
                attrAddDelta.setRealValuesToReplace(currentValuesNormalized);
            } else {
                for (PrismPropertyValue<?> pval : currentValues) {
                    attrAddDelta.addRealValuesToAdd(matchingRule.normalize(pval.getValue()));
                }
            }
            if (attrAddDelta.getDefinition().getTypeName() == null) {
                throw new SchemaException("No definition in " + attrAddDelta);
            }
            computedShadowDelta.addModification(attrAddDelta);
        } else {
            if (attrDef.isSingleValue()) {
                Object currentResourceRealValue = currentResourceAttrProperty.getRealValue();
                Object currentResourceNormalizedRealValue = matchingRule.normalize(currentResourceRealValue);
                if (!Objects.equals(currentResourceNormalizedRealValue, oldRepoAttributeProperty.getRealValue())) {
                    PropertyDelta delta;
                    if (currentResourceNormalizedRealValue != null) {
                        delta = computedShadowDelta.addModificationReplaceProperty(currentResourceAttrProperty.getPath(),
                                currentResourceNormalizedRealValue);
                    } else {
                        delta = computedShadowDelta.addModificationReplaceProperty(currentResourceAttrProperty.getPath());
                    }
                    //noinspection unchecked
                    delta.setDefinition(currentResourceAttrProperty.getDefinition());
                    if (delta.getDefinition().getTypeName() == null) {
                        throw new SchemaException("No definition in " + delta);
                    }
                }
            } else {
                PrismProperty<Object> normalizedCurrentResourceAttrProperty = currentResourceAttrProperty.clone();
                for (PrismPropertyValue pval : normalizedCurrentResourceAttrProperty.getValues()) {
                    Object normalizedRealValue = matchingRule.normalize(pval.getValue());
                    //noinspection unchecked
                    pval.setValue(normalizedRealValue);
                }
                PropertyDelta<Object> attrDiff = oldRepoAttributeProperty.diff(normalizedCurrentResourceAttrProperty);
                //                                LOGGER.trace("DIFF:\n{}\n-\n{}\n=:\n{}",
                //                                        oldRepoAttributeProperty==null?null:oldRepoAttributeProperty.debugDump(1),
                //                                        normalizedCurrentResourceAttrProperty==null?null:normalizedCurrentResourceAttrProperty.debugDump(1),
                //                                        attrDiff==null?null:attrDiff.debugDump(1));
                if (attrDiff != null && !attrDiff.isEmpty()) {
                    attrDiff.setParentPath(ShadowType.F_ATTRIBUTES);
                    if (attrDiff.getDefinition().getTypeName() == null) {
                        throw new SchemaException("No definition in " + attrDiff);
                    }
                    computedShadowDelta.addModification(attrDiff);
                }
            }
        }
    }

    private <T> void compareUpdateProperty(ObjectDelta<ShadowType> shadowDelta,
            ItemPath itemPath, PrismObject<ShadowType> currentResourceShadow, PrismObject<ShadowType> oldRepoShadow) {
        PrismProperty<T> currentProperty = currentResourceShadow.findProperty(itemPath);
        PrismProperty<T> oldProperty = oldRepoShadow.findProperty(itemPath);
        PropertyDelta<T> itemDelta = ItemUtil.diff(oldProperty, currentProperty);
        if (itemDelta != null && !itemDelta.isEmpty()) {
            shadowDelta.addModification(itemDelta);
        }
    }
}

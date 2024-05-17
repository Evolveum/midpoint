/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManagerMiscUtil.determinePrimaryIdentifierValue;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.impl.RepoShadowModifications;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.api.ResourceObjectClassification;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObject;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsLocalBeans;
import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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
 * These two situations are discriminated by {@link #fromResource} flag.
 *
 * @see ShadowDeltaComputerRelative
 * @see ShadowUpdater#updateShadowInRepository(ProvisioningContext, RepoShadow, ResourceObject, ObjectDelta,
 * ResourceObjectClassification, OperationResult)
 */
class ShadowDeltaComputerAbsolute {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowDeltaComputerAbsolute.class);

    @NotNull private final ProvisioningContext ctx;
    @NotNull private final RepoShadow repoShadow;
    @NotNull private final RawRepoShadow rawRepoShadow;
    @NotNull private final ResourceObject resourceObject;
    @Nullable private final ObjectDelta<ShadowType> resourceObjectDelta;
    @NotNull private final RepoShadowModifications computedModifications = new RepoShadowModifications();
    private final boolean cachingEnabled; // FIXME partial caching?!

    /**
     * True if the information we deal with (resource object, resource object delta) comes from the resource.
     * False if the shadow was sent to the resource, and the operation might or might not succeeded.
     */
    private final boolean fromResource;

    private final ShadowsLocalBeans b = ShadowsLocalBeans.get();

    private ShadowDeltaComputerAbsolute(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull ResourceObject resourceObject,
            @Nullable ObjectDelta<ShadowType> resourceObjectDelta,
            boolean fromResource) {
        this.ctx = ctx;
        this.repoShadow = repoShadow;
        this.rawRepoShadow = Preconditions.checkNotNull(repoShadow.getRawRepoShadow(), "no raw repo shadow");
        this.resourceObject = resourceObject;
        this.resourceObjectDelta = resourceObjectDelta;
        this.cachingEnabled = ctx.isCachingEnabled();
        this.fromResource = fromResource;
    }

    static @NotNull RepoShadowModifications computeShadowModifications(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull ResourceObject resourceObject,
            @Nullable ObjectDelta<ShadowType> resourceObjectDelta,
            boolean fromResource)
            throws SchemaException, ConfigurationException {
        return new ShadowDeltaComputerAbsolute(ctx, repoShadow, resourceObject, resourceObjectDelta, fromResource)
                .execute();
    }

    /**
     * Objects are NOT updated. Only {@link #computedModifications} is created.
     */
    private @NotNull RepoShadowModifications execute()
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
            updateEffectiveMarks();
            if (cachingEnabled) {
                updateCachedActivation();
                updateCachingMetadata(incompleteCacheableItems);
            } else {
                clearCachingMetadata();
            }
        }
        return computedModifications;
    }

    private void updateEffectiveMarks() throws SchemaException {
        // protected status of resourceShadow was computed without exclusions present in
        // original shadow
        List<ObjectReferenceType> effectiveMarkRef = resourceObject.getBean().getEffectiveMarkRef();
        if (!effectiveMarkRef.isEmpty()) {
            // We should check if marks computed on resourceObject without exclusions should
            // be excluded and not propagated to repository layer.
            computedModifications.add(
                    ObjectOperationPolicyHelper.get().computeEffectiveMarkDelta(
                            repoShadow.getBean(), effectiveMarkRef));
        }
    }

    private void updateShadowName() throws SchemaException {
        PolyString resourceObjectName = resourceObject.determineShadowName();
        PolyString repoShadowName = PolyString.toPolyString(repoShadow.getName());
        if (resourceObjectName != null && !resourceObjectName.equalsOriginalValue(repoShadowName)) {
            computedModifications.add(
                    PrismContext.get().deltaFactory().property()
                            .createModificationReplaceProperty(
                                    ShadowType.F_NAME, repoShadow.getPrismDefinition(), resourceObjectName));
        }
    }

    private void updateAuxiliaryObjectClasses() {
        PropertyDelta<QName> auxOcDelta = ItemUtil.diff(
                repoShadow.getPrismObject().findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS),
                resourceObject.getPrismObject().findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS));
        computedModifications.add(auxOcDelta);
    }

    private void updateExistsFlag() throws SchemaException {
        // Resource object obviously exists in this case. However, we do not want to mess with isExists flag in
        // GESTATING nor CORPSE state, as this existence may be just a quantum illusion.
        if (!repoShadow.isInQuantumState()) {
            computedModifications.add(
                    PrismContext.get().deltaFor(ShadowType.class)
                            .item(ShadowType.F_EXISTS).replace(resourceObject.doesExist())
                            .asItemDelta());
        }
    }

    private void updatePrimaryIdentifierValue() throws SchemaException {
        String newPrimaryIdentifierValue = determinePrimaryIdentifierValue(ctx, resourceObject);
        String existingPrimaryIdentifierValue = repoShadow.getBean().getPrimaryIdentifierValue();
        if (!Objects.equals(existingPrimaryIdentifierValue, newPrimaryIdentifierValue)) {
            LOGGER.trace("Existing primary identifier value: {}, new: {}",
                    existingPrimaryIdentifierValue, newPrimaryIdentifierValue);
            computedModifications.add(
                    PrismContext.get().deltaFor(ShadowType.class)
                            .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace(newPrimaryIdentifierValue)
                            .asItemDelta()
            );
        }
    }

    private void clearCachingMetadata() throws SchemaException {
        if (repoShadow.getBean().getCachingMetadata() != null) {
            computedModifications.add(
                    PrismContext.get().deltaFor(ShadowType.class)
                            .item(ShadowType.F_CACHING_METADATA).replace()
                            .asItemDelta());
        }
    }

    private void updateCachingMetadata(Collection<QName> incompleteCacheableItems) throws SchemaException {
        if (incompleteCacheableItems.isEmpty()) {
            CachingMetadataType cachingMetadata = new CachingMetadataType();
            cachingMetadata.setRetrievalTimestamp(b.clock.currentTimeXMLGregorianCalendar());
            computedModifications.add(
                    PrismContext.get().deltaFor(ShadowType.class)
                            .item(ShadowType.F_CACHING_METADATA).replace(cachingMetadata)
                            .asItemDelta());
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
        PrismProperty<T> currentProperty = resourceObject.getPrismObject().findProperty(itemPath);
        PrismProperty<T> oldProperty = repoShadow.getPrismObject().findProperty(itemPath);
        PropertyDelta<T> itemDelta = ItemUtil.diff(oldProperty, currentProperty);
        if (itemDelta != null && !itemDelta.isEmpty()) {
            computedModifications.add(itemDelta);
        }
    }

    private void updateAttributes(Collection<QName> incompleteCacheableAttributes)
            throws SchemaException, ConfigurationException {

        ShadowAttributesContainer resourceObjectAttributesContainer = resourceObject.getAttributesContainer();
        PrismContainerValue<?> rawRepoShadowAttributesPcv = rawRepoShadow.getAttributesContainerValue();

        // TODO the object should have the composite definition by now!
        ResourceObjectDefinition ocDef = ctx.computeCompositeObjectDefinition(resourceObject.getBean());

        // For complete attributes we can proceed as before: take the resource object as authoritative.
        // If not obtained from the resource, they were created from object delta anyway.
        // However, for incomplete (e.g. index-only) attributes we have to rely on object delta, if present.
        // TODO clean this up! MID-5834

        Collection<QName> expectedRepoAttributes = new ArrayList<>();

        for (ShadowSimpleAttribute<?> resourceObjectAttribute : resourceObjectAttributesContainer.getAttributes()) {
            ShadowSimpleAttributeDefinition<?> attrDef = resourceObjectAttribute.getDefinitionRequired();
            ItemName attrName = attrDef.getItemName();
            if (ctx.shouldStoreAttributeInShadow(ocDef, attrDef)) {
                expectedRepoAttributes.add(attrName);
                if (!resourceObjectAttribute.isIncomplete()) {
                    updateAttributeIfNeeded(rawRepoShadowAttributesPcv, resourceObjectAttribute);
                } else {
                    incompleteCacheableAttributes.add(attrName);
                }
            } else {
                LOGGER.trace("Skipping resource attribute because it's not going to be stored in shadow: {}", attrName);
            }
        }

        for (Item<?, ?> oldRepoItem : rawRepoShadowAttributesPcv.getItems()) {
            ItemName oldRepoItemName = oldRepoItem.getElementName();
            if (!expectedRepoAttributes.contains(oldRepoItemName)) {
                removeAttribute(oldRepoItem, resourceObjectAttributesContainer.findAttribute(oldRepoItemName));
            }
        }

        if (!incompleteCacheableAttributes.isEmpty()) {
            if (resourceObjectDelta != null) {
                LOGGER.trace("Found incomplete cacheable attributes: {} while resource object delta is known. "
                        + "We'll update them using the delta.", incompleteCacheableAttributes);
                throw new UnsupportedOperationException("Please implement incomplete attributes handling"); // MID-2119
//                for (ItemDelta<?, ?> modification : resourceObjectDelta.getModifications()) {
//                    if (modification.getPath().startsWith(ShadowType.F_ATTRIBUTES)) {
//                        if (QNameUtil.contains(incompleteCacheableAttributes, modification.getElementName())) {
//                            LOGGER.trace(" - using: {}", modification);
//                            computedShadowDelta.addModification(modification.clone());
//                        }
//                    }
//                }
//                incompleteCacheableAttributes.clear(); // So we are OK regarding this. We can update caching timestamp.
            } else {
                LOGGER.trace("Found incomplete cacheable attributes: {} while resource object delta is not known. "
                        + "We will not update them in the repo shadow.", incompleteCacheableAttributes);
            }
        }
    }

    private void removeAttribute(@NotNull Item<?, ?> oldRepoItem, @Nullable ShadowSimpleAttribute<?> correspondingAttribute) {
        LOGGER.trace("Removing old repo shadow attribute {} because it should not be cached", oldRepoItem.getElementName());
        ItemDelta<?, ?> rawEraseDelta = oldRepoItem.createDelta();
        rawEraseDelta.setValuesToReplace();
        if (correspondingAttribute != null) {
            ItemDelta<?, ?> eraseDelta = correspondingAttribute.createDelta();
            eraseDelta.setValuesToReplace();
            computedModifications.add(eraseDelta, rawEraseDelta);
        } else {
            computedModifications.addRawOnly(rawEraseDelta);
        }
    }

    private <T, N> void updateAttributeIfNeeded(
            @NotNull PrismContainerValue<?> rawRepoShadowAttributesPcv,
            @NotNull ShadowSimpleAttribute<T> simpleAttribute)
            throws SchemaException {
        ShadowSimpleAttributeDefinition<T> attrDef = simpleAttribute.getDefinitionRequired();
        NormalizationAwareResourceAttributeDefinition<N> expectedRepoAttrDef = attrDef.toNormalizationAware();
        List<N> expectedRepoRealValues = expectedRepoAttrDef.adoptRealValues(simpleAttribute.getRealValues());

        PrismProperty<?> oldRepoAttr = rawRepoShadowAttributesPcv.findProperty(attrDef.getItemName());
        if (oldRepoAttr == null) {
            replaceRepoAttribute(simpleAttribute, "the attribute in repo is missing");
            return;
        }

        PrismPropertyDefinition<?> oldRepoAttrDef = oldRepoAttr.getDefinition();
        if (oldRepoAttrDef == null) {
            replaceRepoAttribute(simpleAttribute, "it has no definition in repo");
            return;
        }

        if (!oldRepoAttrDef.getTypeName().equals(expectedRepoAttrDef.getTypeName())) {
            replaceRepoAttribute(simpleAttribute, "it has a different definition in repo");
            return;
        }

        if (MiscUtil.unorderedCollectionEquals(oldRepoAttr.getRealValues(), expectedRepoRealValues)) {
            LOGGER.trace("Not updating attribute {} because it is up-to-date in repo", attrDef.getItemName());
            return;
        }

        if (attrDef.isSingleValue()) {
            replaceRepoAttribute(simpleAttribute, "the attribute value is outdated");
        } else {
            updateRepoAttribute(oldRepoAttr, simpleAttribute, expectedRepoAttrDef, expectedRepoRealValues);
        }
    }

    private <T> void replaceRepoAttribute(
            @NotNull ShadowSimpleAttribute<T> simpleAttribute,
            @NotNull String reason) throws SchemaException {
        ShadowSimpleAttributeDefinition<T> attrDef = simpleAttribute.getDefinitionRequired();
        LOGGER.trace("Going to set new attribute {} to repo shadow, because {}", attrDef.getItemName(), reason);
        computedModifications.add(simpleAttribute.createReplaceDelta(), attrDef);
    }

    private <T, N> void updateRepoAttribute(
            @NotNull PrismProperty<?> oldRepoAttr,
            @NotNull ShadowSimpleAttribute<T> simpleAttribute,
            @NotNull NormalizationAwareResourceAttributeDefinition<N> repoAttrDef,
            @NotNull List<N> expectedRepoRealValues) {
        PrismProperty<N> expectedRepoAttr = repoAttrDef.instantiateFromUniqueRealValues(expectedRepoRealValues);
        //noinspection unchecked
        PropertyDelta<N> repoAttrDelta = ((PrismProperty<N>) oldRepoAttr).diff(expectedRepoAttr);
        if (repoAttrDelta != null && !repoAttrDelta.isEmpty()) {
            repoAttrDelta.setParentPath(ShadowType.F_ATTRIBUTES);
            LOGGER.trace("Going to update the new attribute {} in repo shadow because it's outdated", repoAttrDef.getItemName());
            // The repo is update with a nice, relative delta. We need not bother with computing such delta
            // for the in-memory update, as it is efficient enough also for "replace" version (hopefully)
            PropertyDelta<T> attrDelta = simpleAttribute.createReplaceDelta();
            computedModifications.add(attrDelta, repoAttrDelta);
        } else {
            throw new IllegalStateException("Different content but non-empty delta?");
        }
    }
}

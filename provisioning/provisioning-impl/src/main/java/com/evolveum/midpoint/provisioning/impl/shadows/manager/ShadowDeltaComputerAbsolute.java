/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowComputerUtil.*;
import static com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManagerMiscUtil.determinePrimaryIdentifierValue;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_PASSWORD;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_PASSWORD_VALUE;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.impl.RepoShadowModifications;

import com.evolveum.midpoint.provisioning.impl.shadows.RepoShadowWithState;
import com.evolveum.midpoint.provisioning.util.ShadowItemsToReturnProvider;
import com.evolveum.midpoint.repo.common.ObjectMarkHelper;
import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper.EffectiveMarksAndPolicies;

import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectShadow;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsLocalBeans;
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
 * @see ShadowUpdater#updateShadowInRepositoryAndInMemory(ProvisioningContext, RepoShadowWithState, ResourceObjectShadow,
 * ObjectDelta, ResourceObjectClassification, EffectiveMarksAndPolicies, OperationResult)
 * @see ShadowObjectComputer
 */
class ShadowDeltaComputerAbsolute {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowDeltaComputerAbsolute.class);

    @NotNull private final ProvisioningContext ctx;

    /** Current repo shadow. Not directly updated, used only as a source for creating {@link #computedModifications}. */
    @NotNull private final RepoShadow repoShadow;

    /** "Raw" component of {@link #repoShadow}. */
    @NotNull private final RawRepoShadow rawRepoShadow;

    /** The current resource object (or presumed resource object) that should be reflected in the repository shadow. */
    @NotNull private final ResourceObjectShadow resourceObject;

    /** The delta reflecting what happened with the object on the resource. Always `null` if {@link #fromResource} is false. */
    @Nullable private final ObjectDelta<ShadowType> resourceObjectDelta;

    /** Effective marks and policies for the shadow or resource object. Provided externally. */
    @NotNull private final EffectiveMarksAndPolicies effectiveMarksAndPolicies;

    /**
     * True if the information we deal with ({@link #resourceObject}, {@link #resourceObjectDelta}) comes from the resource.
     *
     * False if the shadow is not from the resource: it is an object that was tried to be created on the resource,
     * and the operation might or might not succeeded.
     */
    private final boolean fromResource;

    /** Here we collect modifications that will be (presumably) applied to the repo shadow by the caller. */
    @NotNull private final RepoShadowModifications computedModifications = new RepoShadowModifications();

    private final ShadowsLocalBeans b = ShadowsLocalBeans.get();

    private ShadowDeltaComputerAbsolute(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull ResourceObjectShadow resourceObject,
            @Nullable ObjectDelta<ShadowType> resourceObjectDelta,
            @NotNull EffectiveMarksAndPolicies effectiveMarksAndPolicies,
            boolean fromResource) {
        this.effectiveMarksAndPolicies = effectiveMarksAndPolicies;
        argCheck(fromResource || resourceObjectDelta == null,
                "Non-null delta with object not coming from resource?");
        this.ctx = ctx;
        this.repoShadow = repoShadow;
        this.rawRepoShadow = Preconditions.checkNotNull(repoShadow.getRawRepoShadow(), "no raw repo shadow");
        this.resourceObject = resourceObject;
        this.resourceObjectDelta = resourceObjectDelta;
        this.fromResource = fromResource;
    }

    static @NotNull RepoShadowModifications computeShadowModifications(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull ResourceObjectShadow resourceObject,
            @Nullable ObjectDelta<ShadowType> resourceObjectDelta,
            @NotNull EffectiveMarksAndPolicies effectiveMarksAndPolicies,
            boolean fromResource, OperationResult result)
            throws SchemaException, ConfigurationException, EncryptionException {
        var computer = new ShadowDeltaComputerAbsolute(
                ctx, repoShadow, resourceObject, resourceObjectDelta, effectiveMarksAndPolicies, fromResource);
        return computer.execute(result);
    }

    /**
     * Objects are NOT updated. Only {@link #computedModifications} is created.
     */
    private @NotNull RepoShadowModifications execute(OperationResult result)
            throws SchemaException, ConfigurationException, EncryptionException {

        // Note: these updateXXX method work by adding respective deltas (if needed) to the computedShadowDelta
        // They do not change repoShadow nor resourceObject.

        var incompleteCacheableItems = updateAttributes();
        updateShadowName();

        // TODO should we take "caching aux OCs" into account here? (the information was always updated in the repo shadow)
        updateAuxiliaryObjectClasses();

        if (fromResource) {
            updateExistsFlag();
        } else {
            updatePrimaryIdentifierValue();
        }

        if (fromResource) { // TODO reconsider this
            var definition = ctx.getObjectDefinitionRequired();
            updateCachedActivation(definition.isActivationCached());
            updateCachedCredentials(definition.areCredentialsCached(), definition.areCredentialsCachedLegacy(), result);
            if (definition.isCachingEnabled()) {
                if (definition.shouldUpdateCachingMetadata()) {
                    updateCachingMetadata(incompleteCacheableItems);
                } else {
                    // doing nothing, as the caching metadata should not be updated
                }
            } else {
                clearCachingMetadata();
            }
        }
        updateEffectiveMarks();
        return computedModifications;
    }

    private void updateEffectiveMarks() throws SchemaException {
        computedModifications.add(
                ObjectMarkHelper.get().computeEffectiveMarkDelta(
                        repoShadow.getBean().getEffectiveMarkRef(),
                        effectiveMarksAndPolicies.productionModeEffectiveMarkRefs()));
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

    /**
     * The `exists` flag is set in the resource object. We want to update it in the repo shadow - if needed.
     *
     * However, we want to avoid updating the shadow in quantum (GESTATING, CORPSE) states, as this existence may be just
     * a quantum illusion. For these cases, the EXISTS flag is updated in `ShadowRefreshOperation#refreshShadowAsyncStatus method`
     * (for ADD operation) and at various other places, see e.g. `ShadowUpdater#markShadowTombstone` & `#markShadowExists`.
     */
    private void updateExistsFlag() throws SchemaException {
        var existingValue = repoShadow.getBean().isExists();
        var desiredValue = resourceObject.doesExist();
        if (!Objects.equals(existingValue, desiredValue) && !repoShadow.isInQuantumState()) {
            computedModifications.add(
                    PrismContext.get().deltaFor(ShadowType.class)
                            .item(ShadowType.F_EXISTS).replace(desiredValue)
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

    /**
     * Limited to passwords for now.
     *
     * See https://docs.evolveum.com/midpoint/devel/design/password-caching-4.9.1/.
     *
     * Currently, we expect the password is always returned (if it's readable).
     * See {@link ShadowItemsToReturnProvider} and MID-10160.
     */
    private void updateCachedCredentials(boolean cached, boolean cachedLegacy, OperationResult result)
            throws SchemaException, EncryptionException {

        if (cachedLegacy) {
            return; // This is the legacy behavior: NOT updating the shadow regarding credentials
        }

        var currentProperty = ShadowUtil.getPasswordValueProperty(resourceObject.getBean());

        // Case 0: not cached
        if (!cached) {
            deleteCachedPassword();
            return;
        }

        // Case 1: absolutely empty
        if (currentProperty == null
                || currentProperty.hasNoValues() && !currentProperty.isIncomplete()) {
            if (ctx.isPasswordReadable()) {
                deleteCachedPassword();
            } else {
                // Password is not readable. We will cache it only on writing (when written by midPoint);
                // we will NOT erase it on reading (when we don't get it back from the resource).
            }
            return;
        }

        // Case 2: incomplete
        if (currentProperty.hasNoValues()) {
            assert currentProperty.isIncomplete();
            var oldProperty = ShadowUtil.getPasswordValueProperty(repoShadow.getBean());
            if (oldProperty != null && oldProperty.hasAnyValue()) {
                // Any value is as good as "incomplete=true". We cannot determine the real value (as it is hashed).
                // The only difference is that the value may be outdated, if it was changed on the resource,
                // meaning that the comparison (regarding prohibited values) may provide wrong results.
                // But this situation is exactly the same as before 4.9 (when cached passwords were not updated
                // on resource read at all), so we are not making it worse.
            } else if (oldProperty == null || !oldProperty.isIncomplete()) {
                // We need to replace the property in repo with zero-values, incomplete one.
                // Unfortunately, this cannot be done by a simple delta. We have to replace the whole password container.
                // BEWARE: Make sure we don't update other parts of this container elsewhere; deltas would get overlapping.
                var passwordClone = resourceObject.getBean().getCredentials().getPassword().clone();
                passwordClone.asPrismContainerValue().removeProperty(PasswordType.F_VALUE);
                ShadowUtil.setPasswordIncomplete(passwordClone);
                computedModifications.add(
                        PrismContext.get().deltaFor(ShadowType.class)
                                .item(PATH_PASSWORD).replace(passwordClone)
                                .asItemDelta());
            } else {
                // Everything is correct (zero-values, incomplete property exists), no need to issue any deltas.
            }
            return;
        }

        // Case 3: regular (known) value
        var value = currentProperty.getRealValue(ProtectedStringType.class); // fails if there are multiple values
        if (value != null && value.canGetCleartext()) {
            var credentialsPolicy =
                    b.securityPolicyFinder.locateResourceObjectCredentialsPolicy(ctx.getObjectDefinitionRequired(), result);
            var oldValue = ShadowUtil.getPasswordValue(repoShadow.getBean());
            computedModifications.add(
                    b.credentialsStorageManager.createShadowPasswordDelta(credentialsPolicy, oldValue, value));
        } else {
            LOGGER.warn("Empty or non-clear-retrievable password in {}, ignoring: {} (context: {})", resourceObject, value, ctx);
        }
    }

    private void deleteCachedPassword() throws SchemaException {
        var oldProperty = ShadowUtil.getPasswordValueProperty(repoShadow.getBean());
        if (oldProperty != null) {
            computedModifications.add(
                    PrismContext.get().deltaFor(ShadowType.class)
                            .item(PATH_PASSWORD_VALUE).replace()
                            .asItemDelta());
        }
    }

    private void updateCachedActivation(boolean cached) {
        updatePropertyIfNeeded(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, cached);
        updatePropertyIfNeeded(SchemaConstants.PATH_ACTIVATION_VALID_FROM, cached);
        updatePropertyIfNeeded(SchemaConstants.PATH_ACTIVATION_VALID_TO, cached);
        updatePropertyIfNeeded(SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, cached);
    }

    private <T> void updatePropertyIfNeeded(ItemPath itemPath, boolean cached) {
        PrismProperty<T> toBe = cached ? resourceObject.getPrismObject().findProperty(itemPath) : null;
        PrismProperty<T> asIs = repoShadow.getPrismObject().findProperty(itemPath);
        PropertyDelta<T> itemDelta = ItemUtil.diff(asIs, toBe);
        if (itemDelta != null && !itemDelta.isEmpty()) {
            computedModifications.add(itemDelta);
        }
    }

    private Collection<QName> updateAttributes()
            throws SchemaException, ConfigurationException {

        Collection<QName> incompleteCacheableAttributes = new HashSet<>();

        var resourceObjectAttributesContainer = resourceObject.getAttributesContainer();

        // TODO the object should have the composite definition by now!
        var ocDef = ctx.computeCompositeObjectDefinition(resourceObject.getBean());

        // For complete attributes we can proceed as before: take the resource object as authoritative.
        // If not obtained from the resource, they were created from object delta anyway.
        // However, for incomplete (e.g. index-only) attributes we have to rely on object delta, if present.
        // TODO clean this up! MID-5834

        var expectedRepoSimpleAttributes = new HashSet<QName>();
        var expectedRepoReferenceAttributes = new HashSet<QName>();

        // Let's update repo shadow according to attributes currently present in the resource object

        for (var resourceObjectAttribute : resourceObjectAttributesContainer.getAttributes()) {
            if (resourceObjectAttribute instanceof ShadowSimpleAttribute<?> simpleAttribute) {
                var attrDef = simpleAttribute.getDefinitionRequired();
                var attrName = attrDef.getItemName();
                if (shouldStoreSimpleAttributeInShadow(ocDef, attrDef)) {
                    expectedRepoSimpleAttributes.add(attrName);
                    if (!resourceObjectAttribute.isIncomplete()) {
                        updateSimpleAttributeIfNeeded(simpleAttribute);
                    } else {
                        incompleteCacheableAttributes.add(attrName);
                    }
                } else {
                    LOGGER.trace("Skipping simple attribute because it's not going to be stored in repo: {}", attrName);
                }
            } else if (resourceObjectAttribute instanceof ShadowReferenceAttribute referenceAttribute) {
                var attrDef = referenceAttribute.getDefinitionRequired();
                var attrName = attrDef.getItemName();
                if (shouldStoreReferenceAttributeInShadow(ocDef, attrDef)) {
                    expectedRepoReferenceAttributes.add(attrName);
                    if (!resourceObjectAttribute.isIncomplete()) {
                        updateReferenceAttributeIfNeeded(referenceAttribute);
                    } else {
                        incompleteCacheableAttributes.add(attrName);
                    }
                } else {
                    LOGGER.trace("Skipping reference attribute because it's not going to be stored in repo: {}", attrName);
                }
            } else {
                throw new AssertionError(resourceObjectAttribute);
            }
        }

        // Now let's remove the items that are present in the repo shadow but should not be there

        for (Item<?, ?> oldRepoItem : rawRepoShadow.getSimpleAttributes()) {
            ItemName oldRepoItemName = oldRepoItem.getElementName();
            if (!expectedRepoSimpleAttributes.contains(oldRepoItemName)) {
                removeRepoAttribute(
                        oldRepoItem,
                        resourceObjectAttributesContainer.findSimpleAttribute(oldRepoItemName),
                        ocDef.findSimpleAttributeDefinition(oldRepoItemName));
            }
        }

        for (Item<?, ?> oldRepoItem : rawRepoShadow.getReferenceAttributes()) {
            ItemName oldRepoItemName = oldRepoItem.getElementName();
            if (!expectedRepoReferenceAttributes.contains(oldRepoItemName)) {
                removeRepoAttribute(
                        oldRepoItem,
                        resourceObjectAttributesContainer.findReferenceAttribute(oldRepoItemName),
                        ocDef.findReferenceAttributeDefinition(oldRepoItemName));
            }
        }

        if (!incompleteCacheableAttributes.isEmpty()) {
            if (resourceObjectDelta != null) {
                LOGGER.trace("Found incomplete cacheable attributes: {} while resource object delta is known. "
                        + "We'll update them using the delta.", incompleteCacheableAttributes);
                for (ItemDelta<?, ?> modification : resourceObjectDelta.getModifications()) {
                    if (modification.getPath().startsWith(ShadowType.F_ATTRIBUTES)) {
                        var attrName = modification.getElementName();
                        if (QNameUtil.contains(incompleteCacheableAttributes, attrName)) {
                            LOGGER.trace(" - using: {}", modification);
                            // assuming that the attribute is not a reference one
                            computedModifications.add(
                                    modification.clone(),
                                    ocDef.findSimpleAttributeDefinitionRequired(attrName));
                        }
                    }
                }
                incompleteCacheableAttributes.clear(); // So we are OK regarding this. We can update caching timestamp.
            } else {
                LOGGER.trace("Found incomplete cacheable attributes: {} while resource object delta is not known. "
                        + "We will not update them in the repo shadow.", incompleteCacheableAttributes);
            }
        }

        return incompleteCacheableAttributes;
    }


    //region Updating simple attributes

    /** Generates modifications of the cached version of a simple attribute (represented by a prism property). */
    private <T, N> void updateSimpleAttributeIfNeeded(@NotNull ShadowSimpleAttribute<T> resourceObjectAttribute)
            throws SchemaException {
        ShadowSimpleAttributeDefinition<T> attrDef = resourceObjectAttribute.getDefinitionRequired();
        NormalizationAwareResourceAttributeDefinition<N> expectedRepoPropDef = attrDef.toNormalizationAware();
        List<N> expectedRepoPropRealValues = expectedRepoPropDef.adoptRealValues(resourceObjectAttribute.getRealValues());

        var oldRepoProp = rawRepoShadow.getPrismObject().findProperty(ShadowType.F_ATTRIBUTES.append(attrDef.getItemName()));
        if (oldRepoProp == null) {
            replaceRepoAttribute(
                    resourceObjectAttribute, expectedRepoPropRealValues, "the property in repo is missing");
            return;
        }

        PrismPropertyDefinition<?> oldRepoPropDef = oldRepoProp.getDefinition();
        if (oldRepoPropDef == null) {
            replaceRepoAttribute(
                    resourceObjectAttribute, expectedRepoPropRealValues, "the property in repo has no definition");
            return;
        }

        if (!oldRepoPropDef.getTypeName().equals(expectedRepoPropDef.getTypeName())) {
            replaceRepoAttribute(
                    resourceObjectAttribute, expectedRepoPropRealValues, "the property in repo has a wrong definition");
            return;
        }

        PrismProperty<N> expectedRepoAttr = expectedRepoPropDef.instantiateFromUniqueRealValues(expectedRepoPropRealValues);
        //noinspection unchecked
        PropertyDelta<N> repoAttrDelta = ((PrismProperty<N>) oldRepoProp).diff(expectedRepoAttr);
        if (repoAttrDelta == null || repoAttrDelta.isEmpty()) {
            LOGGER.trace("Not updating property {} because it is up-to-date in repo", attrDef.getItemName());
        } else if (attrDef.isSingleValue()) {
            replaceRepoAttribute(
                    resourceObjectAttribute, expectedRepoPropRealValues, "the (single) property value is outdated");
        } else {
            updateMultiValuedSimpleRepoAttribute(repoAttrDelta, resourceObjectAttribute, expectedRepoPropDef);
        }
    }

    private <T, N> void updateMultiValuedSimpleRepoAttribute(
            @NotNull PropertyDelta<N> repoAttrDelta,
            @NotNull ShadowSimpleAttribute<T> resourceObjectAttribute,
            @NotNull NormalizationAwareResourceAttributeDefinition<N> repoAttrDef) throws SchemaException {
        repoAttrDelta.applyDefinition(repoAttrDef, true); // e.g. the indexed flag should be updated there
        repoAttrDelta.setParentPath(ShadowType.F_ATTRIBUTES);
        LOGGER.trace("Going to update the new attribute {} in repo shadow because it's outdated", repoAttrDef.getItemName());
        // The repo is update with a nice, relative delta. We need not bother with computing such delta
        // for the in-memory update, as it is efficient enough also for "replace" version (hopefully)
        PropertyDelta<T> attrDelta = resourceObjectAttribute.createReplaceDelta();
        computedModifications.add(attrDelta, repoAttrDelta);
    }
    //endregion

    /** Generates modifications of the cached version of a reference attribute (represented by a prism reference). */
    private void updateReferenceAttributeIfNeeded(@NotNull ShadowReferenceAttribute referenceAttribute)
            throws SchemaException {
        ShadowReferenceAttributeDefinition attrDef = referenceAttribute.getDefinitionRequired();
        List<ObjectReferenceType> expectedRepoRefRealValues =
                ShadowComputerUtil.toRepoFormat(ctx, referenceAttribute.getAttributeValues());

        var oldRepoRef = rawRepoShadow.getPrismObject().findReference(
                ShadowType.F_REFERENCE_ATTRIBUTES.append(attrDef.getItemName()));
        if (oldRepoRef == null) {
            replaceRepoAttribute(referenceAttribute, expectedRepoRefRealValues, "the attribute in repo is missing");
            return;
        }

        if (MiscUtil.unorderedCollectionEquals(oldRepoRef.getRealValues(), expectedRepoRefRealValues)) {
            LOGGER.trace("Not updating attribute {} because it is up-to-date in repo", attrDef.getItemName());
            return;
        }

        // FIXME currently we'll simply create REPLACE deltas for both repo and in-memory version,
        //  please improve this for multi-valued attributes later
        replaceRepoAttribute(referenceAttribute, expectedRepoRefRealValues, "the attribute value is outdated");
    }

    //region Common support
    private void removeRepoAttribute(
            @NotNull Item<?, ?> oldRepoItem,
            @Nullable ShadowAttribute<?, ?, ?, ?> correspondingResourceObjectAttribute,
            @Nullable ItemDefinition<?> estimatedAttrDefinition) {

        LOGGER.trace("Removing old repo shadow attribute {} because it should not be cached", oldRepoItem.getElementName());
        ItemDelta<?, ?> rawEraseDelta = oldRepoItem.createDelta();

        if (rawEraseDelta.getDefinition() == null) {
            var estimatedRepoDefinition = estimateRepoDefinition(oldRepoItem, estimatedAttrDefinition);
            if (estimatedRepoDefinition != null) {
                //noinspection rawtypes,unchecked
                ((ItemDelta) rawEraseDelta).setDefinition(estimatedRepoDefinition);
            }
        }

        rawEraseDelta.setValuesToReplace();

        if (correspondingResourceObjectAttribute != null) {
            ItemDelta<?, ?> eraseDelta = correspondingResourceObjectAttribute.createDelta();
            eraseDelta.setValuesToReplace();
            computedModifications.add(eraseDelta, rawEraseDelta);
        } else {
            computedModifications.addRawOnly(rawEraseDelta);
        }
    }

    /** Repo (especially generic one) cannot handle deltas without definition. So, we have to try to provide one. */
    private static ItemDefinition<?> estimateRepoDefinition(
            @NotNull Item<?, ?> oldRepoItem, @Nullable ItemDefinition<?> estimatedDefinition) {
        if (estimatedDefinition != null) {
            return estimatedDefinition instanceof ShadowSimpleAttributeDefinition<?> simpleAttrDef ?
                    simpleAttrDef.toNormalizationAware() : estimatedDefinition;
        } else {
            LOGGER.debug("No definition for {}", oldRepoItem);
            return null;
        }
    }

    private void replaceRepoAttribute(
            @NotNull ShadowAttribute<?, ?, ?, ?> resourceObjectAttribute,
            @NotNull Collection<?> newRealValues,
            @NotNull String reason) throws SchemaException {

        var attrDef = resourceObjectAttribute.getDefinitionRequired();
        var attrName = attrDef.getItemName();
        LOGGER.trace("Going to set set/replace attribute {} ({} values) to repo shadow, because {}",
                attrName, newRealValues.size(), reason);

        ItemDelta<?, ?> nonRawDelta = resourceObjectAttribute.createReplaceDelta();
        ItemDelta<?, ?> rawDelta;
        if (attrDef instanceof ShadowSimpleAttributeDefinition<?> simpleAttrDef) {
            var repoAttrDef = simpleAttrDef.toNormalizationAware();
            rawDelta = repoAttrDef.createEmptyDelta();
            //noinspection unchecked,rawtypes
            ((ItemDelta) rawDelta).setValuesToReplace(
                    PrismContext.get().itemFactory().createPropertyValues(
                            repoAttrDef.adoptRealValues(newRealValues)));
        } else if (attrDef instanceof ShadowReferenceAttributeDefinition refAttrDef) {
            var repoAttrDef = createRepoRefAttrDef(refAttrDef);
            rawDelta = repoAttrDef.createEmptyDelta(ShadowType.F_REFERENCE_ATTRIBUTES.append(attrName));
            //noinspection unchecked,rawtypes
            ((ItemDelta) rawDelta).setValuesToReplace(
                    PrismContext.get().itemFactory().createReferenceValues((Collection<Referencable>) newRealValues));
        } else {
            throw new AssertionError(attrDef);
        }
        computedModifications.add(nonRawDelta, rawDelta);
    }
    //endregion
}

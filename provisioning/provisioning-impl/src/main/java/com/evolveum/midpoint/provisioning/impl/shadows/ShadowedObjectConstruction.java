/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObject;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.CompleteResourceObject;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.ucf.api.UcfResourceObject;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Represents a shadowed object construction process for an object that is being returned from the shadows facade to a client.
 *
 * Data in the resulting object come from two sources:
 *
 * 1. resource object; TODO - can be "fake" i.e. coming from the repository??
 * 2. repository shadow (potentially updated by previous processing).
 *
 * TODO the algorithm:
 *
 * 1. All the mandatory fields are filled (e.g name, resourceRef, ...)
 * 2. Transforms the shadow with respect to simulated capabilities. (???)
 * 3. Adds shadowRefs to associations. TODO
 * 4. TODO
 *
 * Instantiated separately for each shadowing operation.
 */
class ShadowedObjectConstruction {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowedObjectConstruction.class);

    /**
     * Existing repository shadow. Usually contains only a subset of attributes.
     * OTOH it is the only source of some information like password, activation metadata, or shadow state,
     * and a more reliable source for others: like exists and dead.
     */
    @NotNull private final RepoShadow repoShadow;

    /**
     * Object that was fetched from the resource.
     */
    @NotNull private final ExistingResourceObject resourceObject;

    /** Attributes of the resource object. */
    @NotNull private final ResourceAttributeContainer resourceObjectAttributes;

    /** Associations of the resource object. */
    @Nullable private final PrismContainer<ShadowAssociationType> resourceObjectAssociations;

    /**
     * Provisioning context related to the object fetched from the resource.
     *
     * TODO what exact requirements we have for this context? It looks like it is sometimes derived from real
     *  resource object but in some other cases (when calling from {@link ShadowedChange}) the context is derived from
     *  the repo shadow if the object is being deleted.
     */
    @NotNull private final ProvisioningContext ctx;

    /**
     * Result shadow that is being constructed. It starts with the repo shadow, with selected information
     * transferred from the resource object.
     */
    @NotNull private final ShadowType resultingShadowedBean;

    @NotNull private final ShadowsLocalBeans b = ShadowsLocalBeans.get();

    private ShadowedObjectConstruction(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull ExistingResourceObject resourceObject) {
        this.ctx = ctx;
        this.resourceObject = resourceObject;
        this.resourceObjectAttributes = resourceObject.getAttributesContainer();
        this.resourceObjectAssociations = resourceObject.getAssociationsContainer();
        this.repoShadow = repoShadow;
        this.resultingShadowedBean = repoShadow.getBean().clone();
    }

    /** Combines the repo shadow and resource object, as described in the class-level docs. */
    @NotNull static ExistingResourceObject construct(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull ExistingResourceObject resourceObject,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

        return new ShadowedObjectConstruction(ctx, repoShadow, resourceObject)
                .construct(result);
    }

    private @NotNull ExistingResourceObject construct(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

        // The naming convention:
        //  - "copy" means we take the information from the resource object.
        //  - "merge" means we take from both sources (resource and repo).

        applyDefinition();

        setName();
        copyObjectClassIfMissing();
        copyAuxiliaryObjectClasses();

        copyAttributes(result);

        copyIgnored();
        mergeCredentials();

        // exists, dead
        // This may seem strange, but always take exists and dead flags from the repository.
        // Repository is wiser in this case. It may seem that the shadow exists if it is returned
        // by the resource. But that may be just a quantum illusion (gestation and corpse shadow states).
        // (The repository shadow was updated in this respect by ShadowDeltaComputerAbsolute, just before
        // calling this method.)

        mergeActivation();
        copyAndAdoptAssociations(result);
        copyCachingMetadata();

        checkConsistence();

        var updatedObject = resourceObject.withNewContent(resultingShadowedBean);

        // Called here, as we need AbstractShadow to be present.
        ProvisioningUtil.setEffectiveProvisioningPolicy(ctx, updatedObject, result);

        LOGGER.trace("Shadowed resource object:\n{}", updatedObject.debugDumpLazily(1));

        return updatedObject;
    }

    private void checkConsistence() {
        // Sanity asserts to catch some exotic bugs
        PolyStringType resultName = resultingShadowedBean.getName();
        assert resultName != null : "No name generated in " + resultingShadowedBean;
        assert !StringUtils.isEmpty(resultName.getOrig()) : "No name (orig) in " + resultingShadowedBean;
        assert !StringUtils.isEmpty(resultName.getNorm()) : "No name (norm) in " + resultingShadowedBean;
    }

    private void copyCachingMetadata() {
        resultingShadowedBean.setCachingMetadata(resourceObject.getBean().getCachingMetadata());
    }

    private void copyAndAdoptAssociations(OperationResult result) throws SchemaException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, SecurityViolationException,
            EncryptionException {

        if (resourceObjectAssociations == null) {
            return;
        }

        LOGGER.trace("Start adopting associations: {} value(s)", resourceObjectAssociations.size());

        PrismContainer<ShadowAssociationType> associationsCloned = resourceObjectAssociations.clone();
        resultingShadowedBean.asPrismObject().addReplaceExisting(associationsCloned);
        Iterator<PrismContainerValue<ShadowAssociationType>> associationIterator = associationsCloned.getValues().iterator();
        while (associationIterator.hasNext()) {
            if (!adoptAssociationValue(associationIterator.next(), result)) {
                associationIterator.remove();
            }
        }
    }

    /**
     * We take activation from the resource object, but metadata is taken from the repo!
     */
    private void mergeActivation() {
        resultingShadowedBean.setActivation(resourceObject.getBean().getActivation());
        transplantActivationMetadata();
    }

    private void transplantActivationMetadata() {
        ActivationType repoActivation = repoShadow.getBean().getActivation();
        if (repoActivation == null) {
            return;
        }

        ActivationType resultActivation = resultingShadowedBean.getActivation();
        if (resultActivation == null) {
            resultActivation = new ActivationType();
            resultingShadowedBean.setActivation(resultActivation);
        }
        resultActivation.setId(repoActivation.getId());
        resultActivation.setDisableReason(repoActivation.getDisableReason());
        resultActivation.setEnableTimestamp(repoActivation.getEnableTimestamp());
        resultActivation.setDisableTimestamp(repoActivation.getDisableTimestamp());
        resultActivation.setArchiveTimestamp(repoActivation.getArchiveTimestamp());
        resultActivation.setValidityChangeTimestamp(repoActivation.getValidityChangeTimestamp());
    }

    private void copyIgnored() {
        resultingShadowedBean.setIgnored(resourceObject.getBean().isIgnored());
    }

    private void mergeCredentials() {
        resultingShadowedBean.setCredentials(resourceObject.getBean().getCredentials());
        transplantRepoPasswordMetadataIfMissing();
    }

    private void transplantRepoPasswordMetadataIfMissing() {

        MetadataType repoPasswordMetadata = getRepoPasswordMetadata();
        if (repoPasswordMetadata == null) {
            return;
        }

        PasswordType resultPassword = ShadowUtil.getOrCreateShadowPassword(resultingShadowedBean);

        MetadataType resultMetadata = resultPassword.getMetadata();
        if (resultMetadata == null) {
            resultPassword.setMetadata(repoPasswordMetadata.clone());
        }
    }

    @Nullable
    private MetadataType getRepoPasswordMetadata() {
        CredentialsType repoCredentials = repoShadow.getBean().getCredentials();
        if (repoCredentials == null) {
            return null;
        }
        PasswordType repoPassword = repoCredentials.getPassword();
        if (repoPassword == null) {
            return null;
        }
        return repoPassword.getMetadata();
    }

    private void copyObjectClassIfMissing() {
        // TODO shouldn't we always use the object class of the resource object?
        if (resultingShadowedBean.getObjectClass() == null) {
            resultingShadowedBean.setObjectClass(resourceObjectAttributes.getDefinition().getTypeName());
        }
    }

    /**
     * Always take auxiliary object classes from the resource. Unlike structural object classes
     * the auxiliary object classes may change.
     */
    private void copyAuxiliaryObjectClasses() {
        List<QName> targetAuxObjectClassList = resultingShadowedBean.getAuxiliaryObjectClass();
        targetAuxObjectClassList.clear();
        targetAuxObjectClassList.addAll(resourceObject.getBean().getAuxiliaryObjectClass());
    }

    private void setName() throws SchemaException {
        PolyString newName = resourceObject.determineShadowName();
        if (newName != null) {
            resultingShadowedBean.setName(PolyString.toPolyStringType(newName));
        } else {
            // TODO emergency name
            throw new SchemaException("Name could not be determined for " + resourceObject);
        }
    }

    /** The real definition may be different than that of repo shadow (e.g. because of different auxiliary object classes). */
    private void applyDefinition() throws SchemaException {
        resultingShadowedBean.asPrismObject().applyDefinition(
                ctx.getObjectDefinitionRequired().getPrismObjectDefinition());
    }

    private void copyAttributes(OperationResult result) throws SchemaException, ConfigurationException {

        resultingShadowedBean.asPrismObject().removeContainer(ShadowType.F_ATTRIBUTES);
        ResourceAttributeContainer resultAttributes = resourceObjectAttributes.clone();

        ResourceObjectDefinition compositeObjectClassDef = computeCompositeObjectClassDefinition();
        b.accessChecker.filterGetAttributes(resultAttributes, compositeObjectClassDef, result);

        resultingShadowedBean.asPrismObject().add(resultAttributes);
    }

    // TODO the composite definition should be known at this moment, please remove this method
    private ResourceObjectDefinition computeCompositeObjectClassDefinition() throws SchemaException, ConfigurationException {
        return ctx.computeCompositeObjectDefinition(
                ctx.getObjectDefinitionRequired(),
                resourceObject.getBean().getAuxiliaryObjectClass());
    }

    /**
     * Tries to acquire (find/create) shadow for given association value and fill-in its reference.
     *
     * Also, provides all identifier values from the shadow (if it's classified). Normally, the name is present among
     * identifiers, and it is sufficient. However, there may be cases when UID is there only. But the name is sometimes needed,
     * e.g. when evaluating tolerant/intolerant patterns on associations. See also MID-8815.
     *
     * @return false if the association value does not fit and should be removed
     */
    private boolean adoptAssociationValue(PrismContainerValue<ShadowAssociationType> associationValue, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException, EncryptionException {

        LOGGER.trace("Determining shadowRef for {}", associationValue);

        ResourceAttributeContainer identifierContainer = ShadowUtil.getIdentifiersContainerRequired(associationValue);

        ShadowAssociationType associationValueBean = associationValue.asContainerable();
        QName associationName = associationValueBean.getName();
        ResourceAssociationDefinition rAssociationDef = getAssociationDefinition(associationName);
        ShadowKindType entitlementKind = rAssociationDef.getKind();

        // TODO what if we find a shadow given one of the intents? Shouldn't we stop there? Overall, the process seems
        //  to be inefficient for multiple intents.
        for (String entitlementIntent : rAssociationDef.getIntents()) {
            LOGGER.trace("Processing kind={}, intent={} (from the definition)", entitlementKind, entitlementIntent);
            ProvisioningContext ctxEntitlement = ctx.spawnForKindIntent(entitlementKind, entitlementIntent);

            RepoShadow entitlementRepoShadow = acquireEntitlementRepoShadow(
                    associationValue, identifierContainer, ctxEntitlement, result);
            if (entitlementRepoShadow == null) {
                continue; // maybe we should try another intent
            }
            if (doesAssociationMatch(rAssociationDef, entitlementRepoShadow)) {
                LOGGER.trace("Association value matches. Repo shadow is: {}", entitlementRepoShadow);
                associationValueBean.setShadowRef(entitlementRepoShadow.objectRef());
                if (entitlementRepoShadow.isClassified()) {
                    addMissingIdentifiers(identifierContainer, ctxEntitlement, entitlementRepoShadow);
                } else {
                    // We are not sure we have the right shadow. Hence let us be careful and not copy any identifiers.
                }
            } else {
                LOGGER.trace("Association value does not match. Repo shadow is: {}", entitlementRepoShadow);
                // We have association value that does not match its definition. This may happen because the association attribute
                // may be shared among several associations. The EntitlementConverter code has no way to tell them apart.
                // We can do that only if we have shadow or full resource object. And that is available at this point only.
                // Therefore just silently filter out the association values that do not belong here.
                // See MID-5790
                return false;
            }
        }
        return true;
    }

    /** Copies missing identifiers from entitlement repo shadow to the association value; does not overwrite anything! */
    private void addMissingIdentifiers(
            ResourceAttributeContainer identifiersContainer,
            ProvisioningContext ctxEntitlement,
            RepoShadow shadow)
            throws SchemaException {
        var identifierDefinitions = ctxEntitlement.getObjectDefinitionRequired().getAllIdentifiers();
        for (ResourceAttributeDefinition<?> identifierDef : identifierDefinitions) {
            ItemName identifierName = identifierDef.getItemName();
            if (!identifiersContainer.containsAttribute(identifierName)) {
                var shadowIdentifier = shadow.findAttribute(identifierName);
                if (shadowIdentifier != null) {
                    identifiersContainer.add(shadowIdentifier.clone());
                }
            }
        }
    }

    private @Nullable RepoShadow acquireEntitlementRepoShadow(
            PrismContainerValue<ShadowAssociationType> associationValue,
            ResourceAttributeContainer identifierContainer,
            ProvisioningContext ctxEntitlement,
            OperationResult result)
            throws ConfigurationException, CommunicationException, ExpressionEvaluationException, SecurityViolationException,
            EncryptionException, SchemaException {

        // TODO should we fully cache the entitlement shadow (~ attribute/shadow caching)?
        //  (If yes, maybe we should retrieve also the associations below?)

        UcfResourceObject providedResourceObject = identifierContainer.getUserData(ResourceObjectConverter.ENTITLEMENT_OBJECT_KEY);
        if (providedResourceObject != null) {
            var resourceObject = ctxEntitlement.adoptUcfResourceObject(providedResourceObject);
            return ShadowAcquisition.acquireRepoShadow(
                    ctxEntitlement, resourceObject, result); // TODO not doing classification here?
        }

        try {
            ResourceObjectDefinition entitlementObjDef = ctxEntitlement.getObjectDefinitionRequired();

            List<ResourceAttribute<?>> identifyingAttributes = new ArrayList<>();
            // TODO most probably these definitions should be already applied, reconsider this code
            for (ResourceAttribute<?> rawIdentifyingAttribute : identifierContainer.getAttributes()) {
                identifyingAttributes.add(
                        rawIdentifyingAttribute.clone().applyDefinitionFrom(entitlementObjDef));
            }

            // it looks like here should be exactly one attribute

            var existingLiveRepoShadow =
                    b.shadowFinder.lookupLiveShadowByAllAttributes(ctxEntitlement, identifyingAttributes, result);
            if (existingLiveRepoShadow != null) {
                return existingLiveRepoShadow;
            }

            // Nothing found in repo, let's do the search on the resource.

            // TODO the following code requires that all attributes in shadow association value are identifiers
            //  (primary or secondary). However, at other places in the code we also allow non-identifiers in these situations.
            //  What is the correct approach?
            var entitlementIdentification = ResourceObjectIdentification.fromAssociationValue(entitlementObjDef, associationValue);
            CompleteResourceObject fetchedResourceObject =
                    b.resourceObjectConverter.locateResourceObject(
                            ctxEntitlement, entitlementIdentification, false, result);

            // Try to look up repo shadow again, this time with full resource shadow. When we
            // have searched before we might have only some identifiers. The shadow
            // might still be there, but it may be renamed
            return ShadowAcquisition.acquireRepoShadow( // TODO not doing classification?
                    ctxEntitlement, fetchedResourceObject.resourceObject(), result);

        } catch (ObjectNotFoundException e) {
            // The entitlement to which we point is not there. Simply ignore this association value.
            result.muteLastSubresultError();
            LOGGER.warn("The entitlement identified by {} referenced from {} does not exist. Skipping.",
                    associationValue, resourceObject);
            return null;
        } catch (SchemaException e) {
            // The entitlement to which we point is bad. Simply ignore this association value.
            result.muteLastSubresultError();
            LOGGER.warn("The entitlement identified by {} referenced from {} violates the schema. Skipping. Original error: {}",
                    associationValue, resourceObject, e.getMessage(), e);
            return null;
        }
    }

    @NotNull
    private ResourceAssociationDefinition getAssociationDefinition(QName associationName) throws SchemaException {
        ResourceObjectDefinition objectDefinition = ctx.getObjectDefinitionRequired();
        ResourceAssociationDefinition rEntitlementAssociationDef = objectDefinition.findAssociationDefinition(associationName);
        if (rEntitlementAssociationDef == null) {
            LOGGER.trace("Entitlement association with name {} couldn't be found in {} {}\nresource shadow:\n{}\nrepo shadow:\n{}",
                    associationName, objectDefinition, ctx.getDesc(),
                    resourceObject.debugDumpLazily(1), repoShadow.debugDumpLazily(1));
            LOGGER.trace("Full [refined] definition: {}", objectDefinition.debugDumpLazily());
            throw new SchemaException("Entitlement association with name " + associationName
                    + " couldn't be found in " + ctx);
        }
        return rEntitlementAssociationDef;
    }

    private boolean doesAssociationMatch(
            ResourceAssociationDefinition rEntitlementAssociationDef, @NotNull RepoShadow entitlementRepoShadow) {

        ShadowKindType shadowKind = entitlementRepoShadow.getKind();
        String shadowIntent = entitlementRepoShadow.getIntent();
        if (ShadowUtil.isNotKnown(shadowKind) || ShadowUtil.isNotKnown(shadowIntent)) {
            // We have unclassified shadow here. This should not happen in a well-configured system. But the world is a tough place.
            // In case that this happens let's just keep all such shadows in all associations. This is how midPoint worked before,
            // therefore we will get better compatibility. But it is also better for visibility. MidPoint will show data that are
            // wrong. But it will at least show something. The alternative would be to show nothing, which is not really friendly
            // for debugging.
            return true;
        }
        return rEntitlementAssociationDef.getKind() == shadowKind
                && rEntitlementAssociationDef.getIntents().contains(shadowIntent);
    }
}

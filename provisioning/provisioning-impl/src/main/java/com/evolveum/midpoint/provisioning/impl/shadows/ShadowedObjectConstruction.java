/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ValueMetadata;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObjectShadow;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
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
 * 1. resource object (in some times retrieved from the repository - TODO this is to be reviewed!)
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
 *
 * The resource object is *UNUSABLE* after the operation.
 */
class ShadowedObjectConstruction {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowedObjectConstruction.class);

    /** The most up-to-date definition. Primarily retrieved from the repo shadow, enriched with aux OCs. */
    @NotNull private final ResourceObjectDefinition authoritativeDefinition;

    /**
     * Existing repository shadow. Usually contains only a subset of attributes.
     * OTOH it is the only source of some information like password, activation metadata, or shadow state,
     * and a more reliable source for others: like exists and dead.
     */
    @NotNull private final RepoShadow repoShadow;

    /**
     * Object that was fetched from the resource.
     * Its attributes are *DESTROYED* during the shadowization.
     */
    @NotNull private final ExistingResourceObjectShadow resourceObject;

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
            @NotNull ExistingResourceObjectShadow resourceObject) throws SchemaException, ConfigurationException {
        this.ctx = ctx;
        this.resourceObject = resourceObject;
        this.repoShadow = repoShadow;
        this.resultingShadowedBean = repoShadow.getBean().clone();
        this.authoritativeDefinition = ctx.computeCompositeObjectDefinition(
                repoShadow.getObjectDefinition(),
                resourceObject.getBean().getAuxiliaryObjectClass());
    }

    /** Combines the repo shadow and resource object, as described in the class-level docs. */
    @NotNull static ExistingResourceObjectShadow construct(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull ExistingResourceObjectShadow resourceObject,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, GenericConnectorException {

        return new ShadowedObjectConstruction(ctx, repoShadow, resourceObject)
                .construct(result);
    }

    private @NotNull ExistingResourceObjectShadow construct(OperationResult result)
            throws SchemaException, GenericConnectorException {

        // The naming convention:
        //  - "copy" means we take the information from the resource object.
        //  - "merge" means we take from both sources (resource and repo).

        applyAuthoritativeDefinition();

        setName();
        copyObjectClassIfMissing();
        copyAuxiliaryObjectClasses();

        moveAndShadowizeAttributes();

        copyIgnored();
        mergeCredentials();

        // exists, dead
        // This may seem strange, but always take exists and dead flags from the repository.
        // Repository is wiser in this case. It may seem that the shadow exists if it is returned
        // by the resource. But that may be just a quantum illusion (gestation and corpse shadow states).
        // (The repository shadow was updated in this respect by ShadowDeltaComputerAbsolute, just before
        // calling this method.)

        mergeActivation();

        mergeBehavior();

        b.associationsHelper.convertReferenceAttributesToAssociations(
                ctx, resultingShadowedBean, authoritativeDefinition, result);

        copyCachingMetadata(); // these should not be present in the resource object, so that this should effectively clear them
        checkConsistence();

        var updatedObject = resourceObject.withNewContent(resultingShadowedBean);

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

    /**
     * We take activation from the resource object, but metadata is taken from the repo!
     */
    private void mergeActivation() {
        resultingShadowedBean.setActivation(resourceObject.getBean().getActivation());
        transplantActivationMetadata();
    }

    private void mergeBehavior() {
        resultingShadowedBean.setBehavior(resourceObject.getBean().getBehavior());
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

        var repoPasswordMetadata = getRepoPasswordMetadata();
        if (repoPasswordMetadata == null) {
            return;
        }

        var resultPasswordPcv = ShadowUtil.getOrCreateShadowPassword(resultingShadowedBean).asPrismContainerValue();
        if (resultPasswordPcv.hasValueMetadata()) {
            return; // would be unexpected, but - in theory - possible, if the connector provides password metadata
        }

        resultPasswordPcv.setValueMetadata(repoPasswordMetadata);
    }

    private @Nullable ValueMetadata getRepoPasswordMetadata() {
        CredentialsType repoCredentials = repoShadow.getBean().getCredentials();
        if (repoCredentials == null) {
            return null;
        }
        PasswordType repoPassword = repoCredentials.getPassword();
        if (repoPassword == null) {
            return null;
        }
        return repoPassword.asPrismContainerValue().getValueMetadataIfExists();
    }

    private void copyObjectClassIfMissing() throws SchemaException {
        // TODO shouldn't we always use the object class of the resource object?
        if (resultingShadowedBean.getObjectClass() == null) {
            resultingShadowedBean.setObjectClass(resourceObject.getObjectClassName());
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
    private void applyAuthoritativeDefinition() throws SchemaException {
        resultingShadowedBean.asPrismObject().applyDefinition(
                authoritativeDefinition.getPrismObjectDefinition());
    }

    private void moveAndShadowizeAttributes() throws SchemaException {

        resultingShadowedBean.asPrismObject().removeContainer(ShadowType.F_ATTRIBUTES);

        var sourceAttributesContainer = resourceObject.getAttributesContainer();
        var targetAttributesContainer = authoritativeDefinition.toShadowAttributesContainerDefinition().instantiate();

        // Here we move the attributes from the resource object to the resulting shadow.
        // We could move the whole attributes container, but we have to provide the correct definition.
        // So let's do it this way - for now.
        // Note: Removing attributes one-by-one is way too slow - O(n^2).

        var allAttributes = List.copyOf(sourceAttributesContainer.getAttributes());
        sourceAttributesContainer.clear();

        var definitionChanged = authoritativeDefinition != repoShadow.getObjectDefinition();

        // TODO if the definition was not changed, we could move the whole container, and avoid re-adding the attributes
        //  (we would just need to remove non-applicable ones) ... but that would be ~2% from the "no-op" processing
        //  for "1s-200m-0t-0m-0a-10ku" TestSystemPerformance scenario.

        for (var attribute : allAttributes) {
            attribute.clearParent(); // it has still reference to the source container

            if (ProvisioningUtil.isExtraLegacyReferenceAttribute(attribute, authoritativeDefinition)) {
                LOGGER.trace("Ignoring extra legacy reference attribute {}", attribute.getElementName());
                continue;
            }
            if (definitionChanged) {
                // Even if the "apply definition" itself avoids applying the attribute definition, we can spare even the
                // attribute definition lookup in the authoritative definition by this check.
                attribute.applyDefinitionFrom(authoritativeDefinition);
            }
            if (attribute.getDefinitionRequired().getLimitations(LayerType.MODEL).canRead()) {
                targetAttributesContainer.addAttribute(attribute);
            } else {
                LOGGER.trace("Ignoring non-readable attribute {}", attribute);
            }
        }

        resultingShadowedBean.asPrismObject().add(targetAttributesContainer);
    }
}

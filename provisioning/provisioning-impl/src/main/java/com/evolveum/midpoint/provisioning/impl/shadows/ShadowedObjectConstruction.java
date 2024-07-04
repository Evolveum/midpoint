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

import com.evolveum.midpoint.schema.util.AbstractShadow;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.CompleteResourceObject;
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
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

        return new ShadowedObjectConstruction(ctx, repoShadow, resourceObject)
                .construct(result);
    }

    private @NotNull ExistingResourceObjectShadow construct(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

        // The naming convention:
        //  - "copy" means we take the information from the resource object.
        //  - "merge" means we take from both sources (resource and repo).

        applyAuthoritativeDefinition();

        setName();
        copyObjectClassIfMissing();
        copyAuxiliaryObjectClasses();

        copyAndShadowizeAttributes(result);

        copyIgnored();
        mergeCredentials();

        // exists, dead
        // This may seem strange, but always take exists and dead flags from the repository.
        // Repository is wiser in this case. It may seem that the shadow exists if it is returned
        // by the resource. But that may be just a quantum illusion (gestation and corpse shadow states).
        // (The repository shadow was updated in this respect by ShadowDeltaComputerAbsolute, just before
        // calling this method.)

        mergeActivation();
        convertReferenceAttributesToAssociations();
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

    private void copyAndShadowizeAttributes(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, EncryptionException {

        resultingShadowedBean.asPrismObject().removeContainer(ShadowType.F_ATTRIBUTES);

        var resultingAttributesContainer = authoritativeDefinition.toShadowAttributesContainerDefinition().instantiate();
        for (var attribute : resourceObject.getAttributes()) {
            if (ProvisioningUtil.isExtraLegacyReferenceAttribute(attribute, authoritativeDefinition)) {
                LOGGER.trace("Ignoring extra legacy reference attribute {}", attribute.getElementName());
                continue;
            }
            var clone = attribute.clone();
            clone.applyDefinitionFrom(authoritativeDefinition);
            resultingAttributesContainer.addAttribute(clone);
        }

        b.accessChecker.filterGetAttributes(resultingAttributesContainer, authoritativeDefinition, result);

        resultingShadowedBean.asPrismObject().add(resultingAttributesContainer);
    }

    /**
     * Converts reference attributes to associations, including some processing:
     *
     * - acquires target shadows for related references and stores them into those references
     * - sorts the reference attributes into associations based on the definitions (kind/intent)
     */
    private void convertReferenceAttributesToAssociations()
            throws SchemaException, ConfigurationException {

        var attributesContainer = ShadowUtil.getAttributesContainerRequired(resultingShadowedBean);
        var associationDefinitions = authoritativeDefinition.getAssociationDefinitions();

        LOGGER.trace("Start converting {} reference attributes to {} associations",
                attributesContainer.getReferenceAttributes().size(), associationDefinitions.size());

        // TODO optimize this by iterating through the ref attr values, not the associations!
        for (var assocDef : associationDefinitions) {
            var refAttrDef = assocDef.getReferenceAttributeDefinition();
            var refAttr = attributesContainer.findReferenceAttribute(refAttrDef.getItemName());
            var refAttrValues = refAttr != null ? refAttr.getReferenceValues() : List.<ShadowReferenceAttributeValue>of();
            LOGGER.trace("Processing association {} sourced by {} ({} values)",
                    assocDef.getItemName(), refAttrDef.getItemName(), refAttrValues.size());
            for (var refAttrValue : refAttrValues) {
                convertReferenceAttributeValueToAssociationValueIfPossible(refAttrValue, assocDef);
            }
        }
    }

    /**
     * Assumes that the reference attribute value has all shadows processed.
     *
     * TODO UPDATE DOCS
     *
     * Tries to acquire (find/create) shadow for given association value and fill-in its reference.
     *
     * Also, provides all identifier values from the shadow (if it's classified). Normally, the name is present among
     * identifiers, and it is sufficient. However, there may be cases when UID is there only. But the name is sometimes needed,
     * e.g. when evaluating tolerant/intolerant patterns on associations. See also MID-8815.
     */
    private void convertReferenceAttributeValueToAssociationValueIfPossible(
            @NotNull ShadowReferenceAttributeValue refAttrValue, @NotNull ShadowAssociationDefinition assocDef)
            throws ConfigurationException, SchemaException {

        LOGGER.trace("Considering conversion of reference attribute value {} into {}", refAttrValue, assocDef);

        var refAttrShadow = refAttrValue.getShadowRequired();

        var participantsMap = assocDef.getObjectParticipants(ctx.getResourceSchema());
        for (QName objectName : participantsMap.keySet()) {
            var expectedObjectTypes = participantsMap.get(objectName);
            LOGGER.trace("Checking participating object {}; expecting: {}", objectName, expectedObjectTypes);
            AbstractShadow objectShadow;
            if (assocDef.hasAssociationObject()) {
                var objectRefAttrValue = refAttrShadow.getReferenceAttributeSingleValue(objectName);
                if (objectRefAttrValue == null) {
                    LOGGER.trace("Reference attribute {} not found, skipping the check", objectName);
                    continue;
                }
                objectShadow = objectRefAttrValue.getShadowRequired();
            } else {
                objectShadow = refAttrShadow;
            }
            if (expectedObjectTypes.stream().anyMatch(
                    type -> type.matches(objectShadow.getBean()))) {
                LOGGER.trace("Shadow {} accepted for participating object {}", objectShadow, objectName);
            } else {
                LOGGER.trace("Shadow {} NOT accepted for participating object {}, rejecting the whole value",
                        objectShadow, objectName);
                return;
            }
        }

        ShadowUtil
                .getOrCreateAssociationsContainer(resultingShadowedBean)
                .findOrCreateAssociation(assocDef.getItemName())
                .createNewValue()
                .fillFromReferenceAttributeValue(refAttrValue);
    }
}

/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.google.common.base.MoreObjects.firstNonNull;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.refinery.CompositeRefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Construction of an object that is being returned from the shadows facade to a client.
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
public class ShadowedObjectConstruction {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowedObjectConstruction.class);

    /**
     * Existing repository shadow. Usually contains only a subset of attributes.
     * OTOH it is the only source of some information like password, activation metadata, or shadow state,
     * and a more reliable source for others: like exists and dead.
     */
    @NotNull private final PrismObject<ShadowType> repoShadow;

    /**
     * Object that was fetched from the resource.
     */
    @NotNull private final PrismObject<ShadowType> resourceObject;

    /** Attributes of the resource object. */
    private final ResourceAttributeContainer resourceObjectAttributes;

    /** Associations of the resource object. */
    private final PrismContainer<ShadowAssociationType> resourceObjectAssociations;

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
    @NotNull private final PrismObject<ShadowType> resultingShadowedObject;

    @NotNull private final CommonBeans beans;

    @NotNull private final ShadowsLocalBeans localBeans;

    private ShadowedObjectConstruction(@NotNull ProvisioningContext ctx, @NotNull PrismObject<ShadowType> repoShadow,
            @NotNull PrismObject<ShadowType> resourceObject, @NotNull CommonBeans beans) {
        this.ctx = ctx;
        this.resourceObject = resourceObject;
        this.resourceObjectAttributes = ShadowUtil.getAttributesContainer(resourceObject);
        this.resourceObjectAssociations = resourceObject.findContainer(ShadowType.F_ASSOCIATION);
        this.repoShadow = repoShadow;
        this.resultingShadowedObject = repoShadow.clone();
        this.beans = beans;
        this.localBeans = beans.shadowsFacade.getLocalBeans();
    }

    public static ShadowedObjectConstruction create(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow,
            PrismObject<ShadowType> resourceObject, CommonBeans commonBeans) {
        return new ShadowedObjectConstruction(ctx, repoShadow, resourceObject, commonBeans);
    }

    @NotNull
    public PrismObject<ShadowType> construct(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

        // The naming convention:
        //  - "copy" means we take the information from the resource object.
        //  - "merge" means we take from both sources (resource and repo).

        applyDefinition();
        assertPrismContext();

        setName();
        copyObjectClassIfMissing();

        copyAttributes(result);

        copyIgnored();
        mergeCredentials();
        setProtectedFlag(result);

        // exists, dead
        // This may seem strange, but always take exists and dead flags from the repository.
        // Repository is wiser in this case. It may seem that the shadow exists if it is returned
        // by the resource. But that may be just a quantum illusion (gestation and corpse shadow states).

        mergeActivation();
        copyAndAdoptAssociations(result);
        copyCachingMetadata();

        checkConsistence();

        return resultingShadowedObject;
    }

    private void checkConsistence() {
        // Sanity asserts to catch some exotic bugs
        PolyStringType resultName = resultingShadowedObject.asObjectable().getName();
        assert resultName != null : "No name generated in " + resultingShadowedObject;
        assert !StringUtils.isEmpty(resultName.getOrig()) : "No name (orig) in " + resultingShadowedObject;
        assert !StringUtils.isEmpty(resultName.getNorm()) : "No name (norm) in " + resultingShadowedObject;
    }

    private void copyCachingMetadata() {
        resultingShadowedObject.asObjectable().setCachingMetadata(resourceObject.asObjectable().getCachingMetadata());
    }

    private void copyAndAdoptAssociations(OperationResult result) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException, SecurityViolationException,
            EncryptionException {

        if (resourceObjectAssociations == null) {
            return;
        }

        LOGGER.trace("Start adopting associations: {}", resourceObjectAssociations.size());

        PrismContainer<ShadowAssociationType> associationsCloned = resourceObjectAssociations.clone();
        resultingShadowedObject.addReplaceExisting(associationsCloned);
        Iterator<PrismContainerValue<ShadowAssociationType>> associationIterator = associationsCloned.getValues().iterator();
        while (associationIterator.hasNext()) {
            if (!setAssociationValueShadowRef(associationIterator.next(), result)) {
                associationIterator.remove();
            }
        }
    }

    private void setProtectedFlag(OperationResult result) throws SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, SecurityViolationException {
        ProvisioningUtil.setProtectedFlag(ctx, resultingShadowedObject, beans.matchingRuleRegistry, beans.relationRegistry,
                beans.expressionFactory, result);
    }

    /**
     * We take activation from the resource object, but metadata is taken from the repo!
     */
    private void mergeActivation() {
        resultingShadowedObject.asObjectable().setActivation(resourceObject.asObjectable().getActivation());
        transplantActivationMetadata();
    }

    private void transplantActivationMetadata() {
        ActivationType repoActivation = repoShadow.asObjectable().getActivation();
        if (repoActivation == null) {
            return;
        }

        ActivationType resultActivation = resultingShadowedObject.asObjectable().getActivation();
        if (resultActivation == null) {
            resultActivation = new ActivationType(beans.prismContext);
            resultingShadowedObject.asObjectable().setActivation(resultActivation);
        }
        resultActivation.setId(repoActivation.getId());
        resultActivation.setDisableReason(repoActivation.getDisableReason());
        resultActivation.setEnableTimestamp(repoActivation.getEnableTimestamp());
        resultActivation.setDisableTimestamp(repoActivation.getDisableTimestamp());
        resultActivation.setArchiveTimestamp(repoActivation.getArchiveTimestamp());
        resultActivation.setValidityChangeTimestamp(repoActivation.getValidityChangeTimestamp());
    }

    private void copyIgnored() {
        resultingShadowedObject.asObjectable().setIgnored(resourceObject.asObjectable().isIgnored());
    }

    private void mergeCredentials() {
        resultingShadowedObject.asObjectable().setCredentials(resourceObject.asObjectable().getCredentials());
        transplantRepoPasswordMetadataIfMissing();
    }

    private void transplantRepoPasswordMetadataIfMissing() {

        MetadataType repoPasswordMetadata = getRepoPasswordMetadata();
        if (repoPasswordMetadata == null) {
            return;
        }

        PasswordType resultPassword = getOrCreateResultPassword();

        MetadataType resultMetadata = resultPassword.getMetadata();
        if (resultMetadata == null) {
            resultPassword.setMetadata(repoPasswordMetadata.clone());
        }
    }

    @NotNull
    private PasswordType getOrCreateResultPassword() {
        ShadowType resultShadowBean = resultingShadowedObject.asObjectable();
        CredentialsType resultCredentials = resultShadowBean.getCredentials();
        if (resultCredentials == null) {
            resultCredentials = new CredentialsType(beans.prismContext);
            resultShadowBean.setCredentials(resultCredentials);
        }
        PasswordType resultPassword = resultCredentials.getPassword();
        if (resultPassword == null) {
            resultPassword = new PasswordType(beans.prismContext);
            resultCredentials.setPassword(resultPassword);
        }
        return resultPassword;
    }

    @Nullable
    private MetadataType getRepoPasswordMetadata() {
        CredentialsType repoCredentials = repoShadow.asObjectable().getCredentials();
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
        if (resultingShadowedObject.asObjectable().getObjectClass() == null) {
            resultingShadowedObject.asObjectable().setObjectClass(resourceObjectAttributes.getDefinition().getTypeName());
        }
    }

    private void setName() throws SchemaException {
        PolyString newName = ShadowUtil.determineShadowName(resourceObject);
        if (newName != null) {
            resultingShadowedObject.asObjectable().setName(PolyString.toPolyStringType(newName));
        } else {
            // TODO emergency name
            throw new SchemaException("Name could not be determined for " + resourceObject);
        }
    }

    /** The real definition may be different than that of repo shadow (e.g. because of different auxiliary object classes). */
    private void applyDefinition() throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException {
        resultingShadowedObject.applyDefinition(ctx.getObjectClassDefinition().getObjectDefinition(), true);
    }

    private void assertPrismContext() {
        assert resultingShadowedObject.getPrismContext() != null : "No prism context in resultShadow";
    }

    private void copyAttributes(OperationResult result) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {

        resultingShadowedObject.removeContainer(ShadowType.F_ATTRIBUTES);
        ResourceAttributeContainer resultAttributes = resourceObjectAttributes.clone();

        CompositeRefinedObjectClassDefinition compositeObjectClassDef = computeCompositeObjectClassDefinition();
        localBeans.accessChecker.filterGetAttributes(resultAttributes, compositeObjectClassDef, result);

        resultingShadowedObject.add(resultAttributes);
    }

    private CompositeRefinedObjectClassDefinition computeCompositeObjectClassDefinition() throws SchemaException,
            ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        Collection<QName> auxObjectClassQNames = getAuxiliaryObjectClasses();
        return ctx.computeCompositeObjectClassDefinition(auxObjectClassQNames);
    }

    /**
     * Always take auxiliary object classes from the resource. Unlike structural object classes
     * the auxiliary object classes may change.
     */
    private Collection<QName> getAuxiliaryObjectClasses() throws SchemaException {
        Collection<QName> auxObjectClassQNames = new ArrayList<>();
        resultingShadowedObject.removeProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
        PrismProperty<QName> resourceAuxOcProp = resourceObject.findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
        if (resourceAuxOcProp != null) {
            PrismProperty<QName> resultAuxOcProp = resultingShadowedObject.findOrCreateProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
            resultAuxOcProp.addAll(PrismValueCollectionsUtil.cloneCollection(resourceAuxOcProp.getValues()));
            auxObjectClassQNames.addAll(resultAuxOcProp.getRealValues());
        }
        return auxObjectClassQNames;
    }

    /**
     * Tries to acquire (find/create) shadow for given association value and fill-in its reference.
     *
     * @return false if the association value does not fit and should be removed
     */
    private boolean setAssociationValueShadowRef(PrismContainerValue<ShadowAssociationType> associationValue, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException, EncryptionException {

        LOGGER.trace("Determining shadowRef for {}", associationValue);

        ResourceAttributeContainer identifierContainer = ShadowUtil.getAttributesContainer(associationValue,
                ShadowAssociationType.F_IDENTIFIERS);

        ShadowAssociationType associationValueBean = associationValue.asContainerable();
        QName associationName = associationValueBean.getName();
        var rAssociationDef = ctx.getObjectClassDefinition().findAssociationDefinition(associationName);
        if (rAssociationDef == null) {
            // This is quite legal. Imagine that we are looking for account/type1 type that has an association defined,
            // but the shadow is classified (after being fetched) as account/type2 that does not have the association.
            // We should simply ignore such association.
            LOGGER.trace("Entitlement association with name {} does not exist in {}", associationName, ctx);
            return false;
        }
        ShadowKindType entitlementKind = firstNonNull(rAssociationDef.getKind(), ShadowKindType.ENTITLEMENT);

        for (String entitlementIntent : rAssociationDef.getIntents()) {
            LOGGER.trace("Processing kind={}, intent={} (from the definition)", entitlementKind, entitlementIntent);
            ProvisioningContext ctxEntitlement = ctx.spawn(entitlementKind, entitlementIntent);

            PrismObject<ShadowType> entitlementRepoShadow = acquireEntitlementRepoShadow(associationValue, identifierContainer,
                    ctxEntitlement, result);
            if (entitlementRepoShadow == null) {
                continue; // maybe we should try another intent
            }
            if (doesAssociationMatch(rAssociationDef, entitlementRepoShadow)) {
                LOGGER.trace("Association value matches. Repo shadow is: {}", entitlementRepoShadow);
                associationValueBean.setShadowRef(createObjectRef(entitlementRepoShadow, beans.prismContext));
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

    @Nullable
    private PrismObject<ShadowType> acquireEntitlementRepoShadow(PrismContainerValue<ShadowAssociationType> associationValue,
            ResourceAttributeContainer identifierContainer, ProvisioningContext ctxEntitlement, OperationResult result)
            throws ConfigurationException, CommunicationException, ExpressionEvaluationException, SecurityViolationException,
            EncryptionException, SchemaException, ObjectNotFoundException {

        Collection<ResourceAttribute<?>> entitlementIdentifiers = getEntitlementIdentifiers(associationValue, identifierContainer);
        PrismObject<ShadowType> providedResourceObject = identifierContainer.getUserData(ResourceObjectConverter.FULL_SHADOW_KEY);
        if (providedResourceObject != null) {
            return localBeans.shadowAcquisitionHelper.acquireRepoShadow(ctxEntitlement, providedResourceObject, false, result);
        }

        try {
            PrismObject<ShadowType> existingRepoShadow = beans.shadowManager.lookupLiveShadowByAllIds(ctxEntitlement,
                    identifierContainer, result);
            if (existingRepoShadow != null) {
                return existingRepoShadow;
            }

            PrismObject<ShadowType> fetchedResourceObject = beans.resourceObjectConverter
                    .locateResourceObject(ctxEntitlement, entitlementIdentifiers, result);

            // Try to look up repo shadow again, this time with full resource shadow. When we
            // have searched before we might have only some identifiers. The shadow
            // might still be there, but it may be renamed
            return localBeans.shadowAcquisitionHelper
                    .acquireRepoShadow(ctxEntitlement, fetchedResourceObject, false, result);

        } catch (ObjectNotFoundException e) {
            // The entitlement to which we point is not there. Simply ignore this association value.
            result.muteLastSubresultError();
            LOGGER.warn("The entitlement identified by {} referenced from {} does not exist. Skipping.",
                    associationValue, resourceObject);
            return null;
        } catch (SchemaException e) {
            // The entitlement to which we point is bad. Simply ignore this association value.
            result.muteLastSubresultError();
            LOGGER.warn("The entitlement identified by {} referenced from {} violates the schema. Skipping. Original error: {}-{}",
                    associationValue, resourceObject, e.getMessage(), e);
            return null;
        }
    }

    @Contract("_, null -> fail")
    private @NotNull Collection<ResourceAttribute<?>> getEntitlementIdentifiers(
            PrismContainerValue<ShadowAssociationType> associationValue, ResourceAttributeContainer identifierContainer) {
        Collection<ResourceAttribute<?>> entitlementIdentifiers = identifierContainer != null ?
                identifierContainer.getAttributes() : null;
        if (entitlementIdentifiers == null || entitlementIdentifiers.isEmpty()) {
            throw new IllegalStateException("No entitlement identifiers present for association " + associationValue + " " + ctx.getDesc());
        }
        return entitlementIdentifiers;
    }

    private boolean doesAssociationMatch(RefinedAssociationDefinition rEntitlementAssociationDef,
            @NotNull PrismObject<ShadowType> entitlementRepoShadow) {

        ShadowKindType shadowKind = ShadowUtil.getKind(entitlementRepoShadow.asObjectable());
        String shadowIntent = ShadowUtil.getIntent(entitlementRepoShadow.asObjectable());
        if (ShadowUtil.isNotKnown(shadowKind) || ShadowUtil.isNotKnown(shadowIntent)) {
            // We have unclassified shadow here. This should not happen in a well-configured system. But the world is a tough place.
            // In case that this happens let's just keep all such shadows in all associations. This is how midPoint worked before,
            // therefore we will get better compatibility. But it is also better for visibility. MidPoint will show data that are
            // wrong. But it will at least show something. The alternative would be to show nothing, which is not really friendly
            // for debugging.
            return true;
        }
        ShadowKindType defKind = firstNonNull(rEntitlementAssociationDef.getKind(), ShadowKindType.ENTITLEMENT);
        return defKind.equals(shadowKind) &&
                rEntitlementAssociationDef.getIntents().contains(shadowIntent);
    }

}

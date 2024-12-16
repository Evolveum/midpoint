/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectClassification;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.CompleteResourceObject;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObjectShadow;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeValue;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.ShadowReferenceAttributesCollection;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Responsible for the usual shadow/resource-object "post-processing":
 *
 * - classification, after the resource object is available, and the current shadow is not classified or needs re-classification
 * - updating the shadow in the repository
 * - creating the "combined object" (shadow + resource object)
 *
 * TODO better name for this class
 */
class ShadowPostProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowPostProcessor.class);

    @NotNull private ProvisioningContext ctx;
    @NotNull private RepoShadowWithState repoShadow;
    @NotNull private final ExistingResourceObjectShadow resourceObject;
    @Nullable private final ObjectDelta<ShadowType> resourceObjectDelta;

    /** The new classification (if applicable). */
    private ResourceObjectClassification newClassification;

    private ExistingResourceObjectShadow combinedObject;

    @NotNull private final ShadowsLocalBeans b = ShadowsLocalBeans.get();

    ShadowPostProcessor(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadowWithState repoShadow,
            @NotNull ExistingResourceObjectShadow resourceObject,
            @Nullable ObjectDelta<ShadowType> resourceObjectDelta) {
        // We force the resource object definition into the context - just to relieve the caller from this responsibility.
        this.ctx = ctx.spawnForDefinition(resourceObject.getObjectDefinition());
        this.repoShadow = repoShadow;
        this.resourceObject = resourceObject;
        this.resourceObjectDelta = resourceObjectDelta;
    }

    ExistingResourceObjectShadow execute(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, EncryptionException {

        // Classifies the object if needed. Applies the current definition (in all cases).
        classifyIfNeededAndApplyTheDefinition(result);

        // Acquires/updates/combines shadow(s) embedded in the reference values, in both object and object delta.
        postProcessShadowsInReferenceValues(result);

        // Computes current marks and policies, taking into account both repo shadow (current marks, statements)
        // and the current object (data). We must NOT apply policies to the shadow, in order to correctly determine the delta.
        var marksAndPolicies = ctx.computeEffectiveMarksAndPolicies(repoShadow, resourceObject, result);

        // Updates the shadow in repository (and in memory), based on the information obtained from the resource.
        repoShadow = b.shadowUpdater.updateShadowInRepositoryAndInMemory(
                ctx, repoShadow, resourceObject, resourceObjectDelta, newClassification, marksAndPolicies, result);

        // Completes the shadow by adding attributes from the resource object.
        combinedObject = ShadowedObjectConstruction.construct(ctx, repoShadow.shadow(), resourceObject, result);

        return combinedObject;
    }

    private void classifyIfNeededAndApplyTheDefinition(OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        var oldClassification = ResourceObjectClassification.of(repoShadow.shadow());

        if (b.classificationHelper.shouldClassify(ctx, repoShadow.getBean())) {
            // TODO is it OK that the classification helper updates the repository (and determines the tag)?
            //  Shouldn't we do that only during shadow update?
            //  Probably that's ok, because that's the place where we can redirect the delta to the simulation result.
            //  As part of general shadow update it is hidden among other deltas.
            newClassification = b.classificationHelper.classify(ctx, repoShadow.shadow(), resourceObject, result);
            if (newClassification.isKnown()) {
                ResourceObjectTypeDefinition newTypeDefinition = newClassification.getDefinitionRequired();
                LOGGER.debug("Classified {} as {}", repoShadow, newTypeDefinition);
                var compositeDefinition =
                        ctx.computeCompositeObjectDefinition(
                                newTypeDefinition, resourceObject.getBean().getAuxiliaryObjectClass());
                ctx = ctx.spawnForDefinition(compositeDefinition);

                ProvisioningUtil.removeExtraLegacyReferenceAttributes(resourceObject, compositeDefinition);
                resourceObject.applyDefinition(compositeDefinition);

                ProvisioningUtil.removeExtraLegacyReferenceAttributes(repoShadow.shadow(), compositeDefinition);
                repoShadow.shadow().applyDefinition(compositeDefinition);

                if (!newClassification.equivalent(oldClassification)
                        || repoShadow.state() == RepoShadowWithState.ShadowState.DISCOVERED) {
                    // It is possible that the shadow is newly discovered, and its classification is already determined, e.g.,
                    // if the type is known because it's the default type for the object class.
                    //
                    // For such cases, we must treat the shadow as classified as well, even if the classification was technically
                    // not changed.
                    repoShadow = repoShadow.classified();
                }
            }
        } else {
            // The classification was not changed; but we still should apply the correct definition to the resource object.
            var compositeDefinition =
                    ctx.computeCompositeObjectDefinition(
                            repoShadow.shadow().getObjectDefinition(), resourceObject.getBean().getAuxiliaryObjectClass());
            ctx = ctx.spawnForDefinition(compositeDefinition);

            ProvisioningUtil.removeExtraLegacyReferenceAttributes(resourceObject, compositeDefinition);
            resourceObject.applyDefinition(compositeDefinition);
        }
    }

    private void postProcessShadowsInReferenceValues(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, EncryptionException {
        for (var refAttrValue : ShadowReferenceAttributesCollection.ofShadow(resourceObject.getBean()).getAllReferenceValues()) {
            postProcessEmbeddedShadow(refAttrValue, result);
        }
        for (var refAttrValue : ShadowReferenceAttributesCollection.ofObjectDelta(resourceObjectDelta).getAllReferenceValues()) {
            postProcessEmbeddedShadow(refAttrValue, result);
        }
    }

    private void postProcessEmbeddedShadow(@NotNull ShadowReferenceAttributeValue refAttrValue, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, EncryptionException, ObjectNotFoundException {

        var shadow = refAttrValue.getShadowIfPresent();
        if (shadow == null) {
            return;
        }

        var shadowCtx = ctx.spawnForShadow(shadow.getBean());
        var updatedShadow = acquireAndPostProcessEmbeddedShadow(shadow, shadowCtx, result);
        if (updatedShadow != null) {
            refAttrValue.setObject(updatedShadow.getPrismObject());
            refAttrValue.setOid(updatedShadow.getOid());
        }
    }

    /**
     * Returns either {@link RepoShadow} or combined {@link ExistingResourceObjectShadow}.
     *
     * FIXME the second case is wrong, should be something different (that denotes we have a shadow connected)
     */
    private @Nullable AbstractShadow acquireAndPostProcessEmbeddedShadow(
            @NotNull AbstractShadow shadow,
            @NotNull ProvisioningContext shadowCtx,
            @NotNull OperationResult result)
            throws ConfigurationException, CommunicationException, ExpressionEvaluationException, SecurityViolationException,
            EncryptionException, ObjectNotFoundException, SchemaException {

        // TODO should we fully cache the entitlement shadow (~ attribute/shadow caching)?
        //  (If yes, maybe we should retrieve also the associations below?)

        if (!shadow.isIdentificationOnly()) {
            // The conversion from shadow to an ExistingResourceObjectShadow looks strange but actually has a point:
            // the shadow really came from the resource.
            var existingResourceObject = ExistingResourceObjectShadow.fromShadow(shadow);
            return acquireAndPostProcessShadow(shadowCtx, existingResourceObject, result);
        }

        var attributesContainer = shadow.getAttributesContainer();
        var identifiers = attributesContainer.getAllIdentifiers();

        // for simulated references, here should be exactly one attribute; for native ones, it can vary
        var existingLiveRepoShadow = b.shadowFinder.lookupLiveShadowByAllAttributes(shadowCtx, identifiers, result);
        if (existingLiveRepoShadow != null) {
            // no post-processing (updating shadow, combining with the resource object) is needed
            // except for object marks!
            var effectiveShadowCtx = shadowCtx.spawnForShadow(existingLiveRepoShadow.getBean());
            effectiveShadowCtx.computeAndUpdateEffectiveMarksAndPolicies(
                    existingLiveRepoShadow, RepoShadowWithState.ShadowState.EXISTING, result);
            return existingLiveRepoShadow;
        }

        // Nothing found in repo, let's do the search on the resource.
        var identification = shadow.getIdentificationRequired();

        CompleteResourceObject fetchedResourceObject;
        try {
            fetchedResourceObject =
                    b.resourceObjectConverter.locateResourceObject(
                            shadowCtx, identification, false, result);

        } catch (ObjectNotFoundException e) {
            // The entitlement to which we point is not there. Simply ignore this association value.
            result.muteLastSubresultError();
            LOGGER.warn("The entitlement identified by {} referenced from {} does not exist. Skipping.",
                    identification, resourceObject);
            return null;
        } catch (SchemaException e) {
            // The entitlement to which we point is bad. Simply ignore this association value.
            result.muteLastSubresultError();
            LOGGER.warn("The entitlement identified by {} referenced from {} violates the schema. Skipping. Original error: {}",
                    identification, resourceObject, e.getMessage(), e);
            return null;
        }

        // Try to look up repo shadow again, this time with full resource shadow. When we
        // have searched before we might have only some identifiers. The shadow
        // might still be there, but it may be renamed
        return acquireAndPostProcessShadow(shadowCtx, fetchedResourceObject.resourceObject(), result);
    }

    private static @NotNull ExistingResourceObjectShadow acquireAndPostProcessShadow(
            ProvisioningContext ctxEntitlement, ExistingResourceObjectShadow existingResourceObject, OperationResult result)
            throws SchemaException, ConfigurationException, EncryptionException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ObjectNotFoundException {
        var repoShadow = ShadowAcquisition.acquireRepoShadow(ctxEntitlement, existingResourceObject, result);
        var shadowPostProcessor = new ShadowPostProcessor(
                ctxEntitlement, repoShadow, existingResourceObject, null);
        return shadowPostProcessor.execute(result);
    }

    ExistingResourceObjectShadow getCombinedObject() {
        return combinedObject;
    }

    @NotNull ProvisioningContext getCurrentProvisioningContext() {
        return Objects.requireNonNull(ctx);
    }
}

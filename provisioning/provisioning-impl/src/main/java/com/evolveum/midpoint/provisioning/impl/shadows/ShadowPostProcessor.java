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
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObjectShadow;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;

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
    @NotNull private RepoShadow repoShadow;
    @NotNull private final ExistingResourceObjectShadow resourceObject;
    @Nullable private final ObjectDelta<ShadowType> resourceObjectDelta;

    /** The new classification (if applicable). */
    private ResourceObjectClassification newClassification;

    private ExistingResourceObjectShadow combinedObject;

    @NotNull private final ShadowsLocalBeans b = ShadowsLocalBeans.get();

    ShadowPostProcessor(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
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

        classifyIfNeeded(result);
        updateShadowInRepository(result);
        createCombinedObject(result);

        return combinedObject;
    }

    /** Classifies the object if needed. */
    private void classifyIfNeeded(OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (!b.classificationHelper.shouldClassify(ctx, repoShadow.getBean())) {
            return;
        }

        // TODO is it OK that the classification helper updates the repository (and determines the tag)?
        //  Shouldn't we do that only during shadow update?
        //  Probably that's ok, because that's the place where we can redirect the delta to the simulation result.
        //  As part of general shadow update it is hidden among other deltas.
        newClassification = b.classificationHelper.classify(ctx, repoShadow, resourceObject, result);
        if (newClassification.isKnown()) {
            ResourceObjectTypeDefinition newTypeDefinition = newClassification.getDefinitionRequired();
            LOGGER.debug("Classified {} as {}", repoShadow, newTypeDefinition);
            var compositeDefinition =
                    ctx.computeCompositeObjectDefinition(newTypeDefinition, resourceObject.getBean().getAuxiliaryObjectClass());
            ctx = ctx.spawnForDefinition(compositeDefinition);
            resourceObject.applyDefinition(compositeDefinition);
            repoShadow.applyDefinition(compositeDefinition);
        }
    }

    /**
     * Updates the shadow in repository, based on the information obtained from the resource.
     */
    private void updateShadowInRepository(@NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        repoShadow = b.shadowUpdater.updateShadowInRepository(
                ctx, repoShadow, resourceObject, resourceObjectDelta, newClassification, result);
    }

    /**
     * Completes the shadow by adding attributes from the resource object.
     * This also completes the associations by adding shadowRefs.
     */
    private void createCombinedObject(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException, EncryptionException {
        combinedObject = ShadowedObjectConstruction.construct(ctx, repoShadow, resourceObject, result);
    }

    ExistingResourceObjectShadow getCombinedObject() {
        return combinedObject;
    }

    @NotNull ProvisioningContext getCurrentProvisioningContext() {
        return Objects.requireNonNull(ctx);
    }
}

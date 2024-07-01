/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.provisioning.impl.RepoShadowModifications;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowClassificationModeType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectClassification;
import com.evolveum.midpoint.provisioning.api.ShadowSimulationData;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObjectShadow;
import com.evolveum.midpoint.provisioning.impl.shadows.classification.ResourceObjectClassifier;
import com.evolveum.midpoint.provisioning.impl.shadows.classification.ShadowTagGenerator;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowUpdater;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Helps with resource object classification, i.e. determining their kind, intent, and tag.
 * (Is tag determination a part of the classification? Maybe not.)
 */
@Component
@Experimental
class ClassificationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ClassificationHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired private ResourceObjectClassifier classifier;
    @Autowired private ShadowTagGenerator shadowTagGenerator;
    @Autowired private ShadowUpdater shadowUpdater;

    /**
     * Classifies the current repoShadow, based on information from the resource object.
     * As a result, the repository is updated.
     */
    ResourceObjectClassification classify(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull ExistingResourceObjectShadow resourceObject,
            @NotNull OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

        // The classifier code works with the "combined" version of resource object and its repoShadow.
        // This is NOT a full shadowization. Just good enough for the classifier to work.
        ShadowType combinedObject = combine(resourceObject, repoShadow);

        return classifyInternal(ctx, repoShadow, combinedObject, result);
    }

    private ResourceObjectClassification classifyInternal(
            ProvisioningContext ctx,
            RepoShadow repoShadow,
            ShadowType combinedObject,
            OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ResourceObjectClassification classification = classifier.classify(
                combinedObject,
                ctx.getResource(),
                null,
                ctx.getTask(),
                result);

        if (isDifferent(classification, combinedObject)) {
            LOGGER.trace("New/updated classification of {} found: {}", combinedObject, classification);
            updateShadowClassificationAndTag(repoShadow, combinedObject, classification, ctx, result);
        } else {
            LOGGER.trace("No change in classification of {}: {}", combinedObject, classification);
        }
        return classification;
    }

    /**
     * The combination simply takes attributes from the resource object, and the rest from the shadow.
     * It is much simplified version of what is done in {@link ShadowedObjectConstruction}. We hope it will suffice for now.
     * In particular, we hope that the object class is roughly OK, and things like entitlement, credentials, and so on
     * are not needed.
     */
    private ShadowType combine(ExistingResourceObjectShadow resourceObject, RepoShadow shadow)
            throws SchemaException {
        ShadowType combined = shadow.getBean().clone();
        combined.asPrismObject().removeContainer(ShadowType.F_ATTRIBUTES);
        combined.asPrismObject().add(
                resourceObject.getAttributesContainer().clone());
        LOGGER.trace("Combined object:\n{}", combined.debugDumpLazily(1));
        return combined;
    }

    /**
     * We update the tag as well, because it may depend on the object type.
     *
     * (We intentionally set the value of intent to "unknown" if the classification is not known!)
     */
    private void updateShadowClassificationAndTag(
            @NotNull RepoShadow repoShadow,
            @NotNull ShadowType combinedObject,
            @NotNull ResourceObjectClassification classification,
            @NotNull ProvisioningContext ctx,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException {
        String tag = classification.isKnown() ?
                shadowTagGenerator.generateTag(
                        combinedObject, ctx.getResource(), classification.getDefinitionRequired(), ctx.getTask(), result) :
                null;
        ShadowKindType oldKind = combinedObject.getKind();
        String oldIntent = combinedObject.getIntent();
        String oldTag = combinedObject.getTag();
        ShadowKindType kindToSet = classification.isKnown() ?
                classification.getKind() :
                Objects.requireNonNullElse( // We don't want to lose last-known kind even if classification is not known
                        oldKind, ShadowKindType.UNKNOWN);
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(ShadowType.class)
                .optimizing()
                .item(ShadowType.F_KIND).old(oldKind).replace(kindToSet)
                .item(ShadowType.F_INTENT).old(oldIntent).replace(classification.getIntent())
                .item(ShadowType.F_TAG).old(oldTag).replace(tag)
                .asItemDeltas();

        if (itemDeltas.isEmpty()) {
            // Strange but possible. If the (new) classification is unknown but the current classification in only partially
            // unknown (e.g. account/unknown), this method is called, but - in fact - it does not update anything.
            assert !classification.isKnown();
            LOGGER.trace("Classification stays unchanged in {}", combinedObject);
            return;
        }

        if (ctx.getTask().areShadowChangesSimulated()) {
            sendSimulationData(repoShadow, itemDeltas, ctx.getTask(), result);
            repoShadow.updateWith(itemDeltas);
        } else {
            var modifications = RepoShadowModifications.of(itemDeltas);
            shadowUpdater.executeRepoShadowModifications(ctx, repoShadow, modifications, result);
        }
    }

    private void sendSimulationData(RepoShadow shadow, List<ItemDelta<?, ?>> itemDeltas, Task task, OperationResult result) {
        SimulationTransaction transaction = task.getSimulationTransaction();
        if (transaction == null) {
            LOGGER.debug("Ignoring simulation data because there is no simulation transaction: {}: {}", shadow, itemDeltas);
        } else {
            transaction.writeSimulationData(
                    ShadowSimulationData.of(shadow.getBean(), itemDeltas), task, result);
        }
    }

    private boolean isDifferent(ResourceObjectClassification classification, ShadowType shadow) {
        return classification.getKind() != shadow.getKind()
                || !Objects.equals(classification.getIntent(), shadow.getIntent());
    }

    /**
     * In the future, here can be a complex algorithm that determines whether a particular shadow should be classified
     * (reclassified) or not.
     *
     * But for now, let us keep it simple.
     *
     * See https://docs.evolveum.com/midpoint/devel/design/simulations/simulated-shadows/#shadow-classification.
     */
    boolean shouldClassify(ProvisioningContext ctx, ShadowType repoShadow) throws SchemaException, ConfigurationException {
        if (ctx.getClassificationMode() == ShadowClassificationModeType.FORCED) {
            LOGGER.trace("Classification is forced -> will do it now");
            return true;
        }

        ResourceObjectTypeIdentification declaredType = ShadowUtil.getTypeIdentification(repoShadow);
        if (declaredType == null) {
            LOGGER.trace("Shadow is not classified -> we will do that");
            return true;
        }

        ProvisioningContext subCtx = ctx.spawnForShadow(repoShadow);
        ResourceObjectDefinition resolvedDefinition = subCtx.getObjectDefinition();
        if (resolvedDefinition == null) {
            LOGGER.trace("Shadow is classified as {} but no definition (currently) exists -> will re-classify it", declaredType);
            return true;
        }
        @Nullable ResourceObjectTypeIdentification resolvedType = resolvedDefinition.getTypeIdentification();
        if (!declaredType.equals(resolvedType)) {
            // For example, if the type no longer exists in the definition, we may end up either with class definition, or
            // (even worse) with default type definition for given class. Hence, the type equality check.
            // See also 2nd part of MID-8613.
            LOGGER.trace("Shadow is classified as {} but the definition is resolved to {} ({}) -> will re-classify it",
                    declaredType, resolvedType, resolvedDefinition);
            return true;
        }

        if (subCtx.isObjectDefinitionInProduction()) {
            LOGGER.trace("Current object definition is in production and the shadow is already classified "
                    + "-> will NOT re-classify the shadow");
            return false;
        } else if (subCtx.isProductionConfigurationTask()) {
            LOGGER.trace("Current object definition is NOT in production but the task is using production configuration "
                    + "-> will NOT re-classify the shadow");
            return false;
        } else {
            LOGGER.trace("Current object definition is NOT in production and the task is using development configuration "
                    + "-> will re-classify the shadow");
            return true;
        }
    }
}

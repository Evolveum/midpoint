/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.impl.shadows.classification.ResourceObjectClassifier;
import com.evolveum.midpoint.provisioning.impl.shadows.classification.ShadowTagGenerator;
import com.evolveum.midpoint.provisioning.impl.simulation.ShadowSimulationDataImpl;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectClassification;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private ResourceObjectClassifier classifier;
    @Autowired private ShadowTagGenerator shadowTagGenerator;

    /**
     * Classifies the current shadow, based on information from the resource object.
     * As a result, the repository is updated.
     */
    ResourceObjectClassification classify(
            ProvisioningContext ctx,
            ShadowType shadow,
            ShadowType resourceObject,
            OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

        argCheck(shadow.getOid() != null, "Shadow has no OID");

        // The classifier code works with the "combined" version of resource object and its shadow.
        // This is NOT a full shadowization. Just good enough for the classifier to work.
        ShadowType combinedObject = combine(resourceObject, shadow);

        return classifyInternal(ctx, combinedObject, result);
    }

    private ResourceObjectClassification classifyInternal(
            ProvisioningContext ctx,
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
            updateShadowClassificationAndTag(combinedObject, classification, ctx, result);
        } else {
            LOGGER.trace("No change in classification of {}: {}", combinedObject, classification);
        }
        return classification;
    }

    /**
     * The combination simply takes attributes from the resource object, and the rest from the shadow.
     * It is much simplified version of what is done in {@link ShadowedObjectConstruction}. We hope if will suffice for now.
     * In particular, we hope that the object class is roughly OK, and things like entitlement, credentials, and so on
     * are not needed.
     */
    private ShadowType combine(ShadowType resourceObject, ShadowType shadow)
            throws SchemaException {
        ShadowType combined = shadow.clone();
        ResourceAttributeContainer fullAttributes = ShadowUtil.getAttributesContainer(resourceObject);
        if (fullAttributes != null) {
            combined.asPrismObject().removeContainer(ShadowType.F_ATTRIBUTES);
            combined.asPrismObject().add(fullAttributes.clone());
        }
        LOGGER.trace("Combined object:\n{}", combined.debugDumpLazily(1));
        return combined;
    }

    /**
     * We update the tag as well, because it may depend on the object type.
     *
     * (We intentionally set the value of intent to "unknown" if the classification is not known!)
     */
    private void updateShadowClassificationAndTag(
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

        assert !itemDeltas.isEmpty();
        if (ctx.getTask().areShadowChangesSimulated()) {
            sendSimulationData(combinedObject, itemDeltas, ctx.getTask(), result);
        } else {
            try {
                repositoryService.modifyObject(ShadowType.class, combinedObject.getOid(), itemDeltas, result);
            } catch (ObjectAlreadyExistsException e) {
                throw SystemException.unexpected(e, "when updating classification and tag");
            }
        }
    }

    private void sendSimulationData(ShadowType shadow, List<ItemDelta<?, ?>> itemDeltas, Task task, OperationResult result) {
        SimulationTransaction transaction = task.getSimulationTransaction();
        if (transaction == null) {
            LOGGER.debug("Ignoring simulation data because there is no simulation transaction: {}: {}", shadow, itemDeltas);
            return;
        }
        ObjectDelta<ShadowType> delta = shadow.asPrismObject().createModifyDelta();
        delta.addModifications(itemDeltas);
        transaction.writeSimulationData(
                ShadowSimulationDataImpl.of(shadow, delta, List.of(SystemObjectsType.MARK_SHADOW_CLASSIFICATION_CHANGED.value())),
                task, result);
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
        if (!ShadowUtil.isClassified(repoShadow)) {
            LOGGER.trace("Shadow is not classified -> we will do that");
            return true;
        }

        ProvisioningContext subCtx = ctx.spawnForShadow(repoShadow);
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

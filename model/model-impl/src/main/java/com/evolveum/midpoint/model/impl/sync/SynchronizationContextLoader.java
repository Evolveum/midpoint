/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import static com.evolveum.midpoint.model.impl.sync.SynchronizationServiceUtils.isPolicyFullyApplicable;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.processor.RefinedDefinitionUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeSynchronizationPolicy;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Responsible for creating ("loading") the synchronization context.
 *
 * This includes:
 *
 * - evaluating synchronization sorter (if present),
 * - finding applicable synchronization policy - using pre-determined shadow kind/intent if present (or not using if it's not!),
 * - determining the tag.
 */
@Component
class SynchronizationContextLoader {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationContextLoader.class);

    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private ModelBeans beans;
    @Autowired private SynchronizationExpressionsEvaluator synchronizationExpressionsEvaluator;

    SynchronizationContext<FocusType> loadSynchronizationContextFromChange(
            @NotNull ResourceObjectShadowChangeDescription change, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        PrismObject<ResourceType> resource =
                MiscUtil.requireNonNull(
                        change.getResource(),
                        () -> new IllegalStateException("No resource in change description: " + change));

        SynchronizationContext<FocusType> syncCtx = loadSynchronizationContext(
                change.getShadowedResourceObject(),
                change.getObjectDelta(),
                resource,
                change.getSourceChannel(),
                change.getItemProcessingIdentifier(),
                null,
                task,
                result);
        if (Boolean.FALSE.equals(change.getShadowExistsInRepo())) {
            syncCtx.setShadowExistsInRepo(false);
            // TODO shadowExistsInRepo in syncCtx perhaps should be tri-state as well
        }
        LOGGER.trace("SYNCHRONIZATION context created: {}", syncCtx);
        return syncCtx;
    }

    <F extends FocusType> SynchronizationContext<F> loadSynchronizationContext(
            @NotNull PrismObject<ShadowType> shadowedResourceObject,
            ObjectDelta<ShadowType> resourceObjectDelta,
            @NotNull PrismObject<ResourceType> resource,
            String sourceChanel,
            String itemProcessingIdentifier,
            PrismObject<SystemConfigurationType> explicitSystemConfiguration,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        SynchronizationContext<F> syncCtx = new SynchronizationContext<>(
                shadowedResourceObject,
                resourceObjectDelta,
                resource,
                sourceChanel,
                beans,
                task,
                itemProcessingIdentifier);
        setSystemConfiguration(syncCtx, explicitSystemConfiguration, result);

        ObjectSynchronizationDiscriminatorType sorterResult =
                new SynchronizationSorterEvaluation<>(syncCtx, beans)
                        .evaluateAndApply(result);

        for (ResourceObjectTypeSynchronizationPolicy policy : syncCtx.getAllSynchronizationPolicies()) {
            if (isPolicyFullyApplicable(policy, sorterResult, syncCtx, result)) {
                syncCtx.setObjectSynchronizationPolicy(policy);
                break;
            }
        }

        generateTagIfNotPresent(syncCtx, result);

        return syncCtx;
    }

    private <F extends FocusType> void setSystemConfiguration(
            SynchronizationContext<F> syncCtx,
            PrismObject<SystemConfigurationType> explicitSystemConfiguration,
            OperationResult result) throws SchemaException {
        if (explicitSystemConfiguration != null) {
            syncCtx.setSystemConfiguration(explicitSystemConfiguration);
        } else {
            syncCtx.setSystemConfiguration(systemObjectCache.getSystemConfiguration(result));
        }
    }

    private <F extends FocusType> void generateTagIfNotPresent(SynchronizationContext<F> syncCtx, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        PrismObject<ShadowType> applicableShadow = syncCtx.getShadowedResourceObject();
        if (applicableShadow.asObjectable().getTag() != null) {
            return;
        }
        ResourceObjectTypeDefinition objectTypeDef = syncCtx.findObjectTypeDefinition();
        if (objectTypeDef == null) {
            // We probably do not have kind/intent yet.
            return;
        }
        ResourceObjectMultiplicityType multiplicity = objectTypeDef.getObjectMultiplicity();
        if (!RefinedDefinitionUtil.isMultiaccount(multiplicity)) {
            return;
        }
        String tag = synchronizationExpressionsEvaluator.generateTag(
                multiplicity,
                applicableShadow,
                syncCtx.getResource(),
                syncCtx.getSystemConfiguration(),
                "tag expression for " + applicableShadow,
                syncCtx.getTask(),
                result);
        LOGGER.debug("SYNCHRONIZATION: TAG generated: {}", tag);
        syncCtx.setTag(tag);
    }
}

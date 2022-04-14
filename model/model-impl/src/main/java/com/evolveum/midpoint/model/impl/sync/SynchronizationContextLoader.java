/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import static com.evolveum.midpoint.model.impl.ResourceObjectProcessingContextImpl.ResourceObjectProcessingContextBuilder.aResourceObjectProcessingContext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.ResourceObjectProcessingContextImpl;
import com.evolveum.midpoint.model.impl.classification.SynchronizationSorterEvaluation;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeSynchronizationPolicy;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
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

    SynchronizationContext<FocusType> loadSynchronizationContextFromChange(
            @NotNull ResourceObjectShadowChangeDescription change,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        PrismObject<ResourceType> resource =
                MiscUtil.requireNonNull(
                        change.getResource(),
                        () -> new IllegalStateException("No resource in change description: " + change));

        SynchronizationContext<FocusType> syncCtx = loadSynchronizationContext(
                change.getShadowedResourceObject().asObjectable(),
                change.getObjectDelta(),
                resource.asObjectable(),
                change.getSourceChannel(),
                change.getItemProcessingIdentifier(),
                null,
                SynchronizationContext.isSkipMaintenanceCheck(),
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
            @NotNull ShadowType shadow,
            ObjectDelta<ShadowType> resourceObjectDelta,
            @NotNull ResourceType originalResource,
            String sourceChanel,
            String itemProcessingIdentifier,
            SystemConfigurationType explicitSystemConfiguration,
            boolean skipMaintenanceCheck,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        ResourceType updatedResource;
        if (skipMaintenanceCheck) {
            updatedResource = originalResource;
        } else {
            updatedResource = checkNotInMaintenance(originalResource, task, result);
        }

        SystemConfigurationType systemConfiguration = explicitSystemConfiguration != null ?
                explicitSystemConfiguration :
                systemObjectCache.getSystemConfigurationBean(result);

        @Nullable ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(updatedResource);

        ResourceObjectProcessingContextImpl processingContext =
                aResourceObjectProcessingContext(shadow, updatedResource, task, beans)
                        .withResourceObjectDelta(resourceObjectDelta)
                        .withExplicitChannel(sourceChanel)
                        .withSystemConfiguration(systemConfiguration)
                        .build();

        ObjectSynchronizationDiscriminatorType sorterResult =
                new SynchronizationSorterEvaluation(processingContext)
                        .evaluate(result);

        ResourceObjectTypeDefinition definition = determineTypeDefinition(processingContext, schema, sorterResult, result);

        String tag = getOrGenerateTag(processingContext, definition, result);

        @Nullable ResourceObjectTypeSynchronizationPolicy policy =
                definition != null ?
                        ResourceObjectTypeSynchronizationPolicy.forTypeDefinition(definition, updatedResource) :
                        null;

        return new SynchronizationContext<>(
                processingContext,
                definition,
                policy,
                sorterResult,
                tag,
                itemProcessingIdentifier);
    }

    /**
     * Checks whether the source resource is not in maintenance mode.
     * (Throws an exception if it is.)
     *
     * Side-effect: updates the resource prism object (if it was changed).
     */
    private @NotNull ResourceType checkNotInMaintenance(ResourceType resource, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        resource = beans.provisioningService
                .getObject(ResourceType.class, resource.getOid(), null, task, result)
                .asObjectable();
        ResourceTypeUtil.checkNotInMaintenance(resource);
        return resource;
    }

    private @Nullable ResourceObjectTypeDefinition determineTypeDefinition(
            @NotNull ResourceObjectProcessingContextImpl processingContext,
            @Nullable ResourceSchema schema,
            @Nullable ObjectSynchronizationDiscriminatorType sorterResult,
            @NotNull OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        ShadowType shadow = processingContext.getShadowedResourceObject();
        ShadowKindType kind = shadow.getKind();
        String intent = shadow.getIntent();
        if (ShadowUtil.isNotKnown(kind) || ShadowUtil.isNotKnown(intent)) {
            return beans.resourceObjectClassifier
                    .classify(processingContext, sorterResult, result)
                    .getDefinition();
        } else {
            return schema != null ? schema.findObjectTypeDefinition(kind, intent) : null;
        }
    }

    private @Nullable String getOrGenerateTag(
            @NotNull ResourceObjectProcessingContextImpl processingContext,
            @Nullable ResourceObjectTypeDefinition definition,
            @NotNull OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ShadowType shadow = processingContext.getShadowedResourceObject();
        if (shadow.getTag() == null) {
            if (definition != null) {
                return beans.shadowTagGenerator.generateTag(processingContext, definition, result);
            } else {
                return null;
            }
        } else {
            return shadow.getTag();
        }
    }
}

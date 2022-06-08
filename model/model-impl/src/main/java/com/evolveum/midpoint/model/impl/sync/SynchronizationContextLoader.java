/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import static com.evolveum.midpoint.model.impl.ResourceObjectProcessingContextImpl.ResourceObjectProcessingContextBuilder.aResourceObjectProcessingContext;

import com.evolveum.midpoint.provisioning.api.ResourceObjectClassification;
import com.evolveum.midpoint.schema.processor.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.ResourceObjectProcessingContextImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

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

    private <F extends FocusType> SynchronizationContext<F> loadSynchronizationContext(
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
                beans.synchronizationSorterEvaluator.evaluate(
                        shadow,
                        processingContext.getResource(),
                        task,
                        result);

        TypeAndDefinition typeAndDefinition = determineObjectTypeAndDefinition(processingContext, schema, sorterResult, result);

        String tag = getOrGenerateTag(processingContext, typeAndDefinition.definition, result);

        @Nullable SynchronizationPolicy policy =
                typeAndDefinition.typeIdentification != null ?
                        SynchronizationPolicyFactory.forKindAndIntent(
                                typeAndDefinition.typeIdentification.getKind(),
                                typeAndDefinition.typeIdentification.getIntent(),
                                updatedResource) :
                        null;

        return new SynchronizationContext<>(
                processingContext,
                typeAndDefinition.typeIdentification,
                typeAndDefinition.definition,
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

    private @NotNull TypeAndDefinition determineObjectTypeAndDefinition(
            @NotNull ResourceObjectProcessingContextImpl processingContext,
            @Nullable ResourceSchema schema,
            @Nullable ObjectSynchronizationDiscriminatorType sorterResult,
            @NotNull OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        ShadowType shadow = processingContext.getShadowedResourceObject();
        ShadowKindType kind = shadow.getKind();
        String intent = shadow.getIntent();
        if (ShadowUtil.isNotKnown(kind) || ShadowUtil.isNotKnown(intent)) {
            ResourceObjectClassification classification = beans.provisioningService
                    .classifyResourceObject(
                            processingContext.getShadowedResourceObject(),
                            processingContext.getResource(),
                            sorterResult,
                            processingContext.getTask(),
                            result);
            ResourceObjectTypeDefinition typeDefinition = classification.getDefinition();
            if (typeDefinition != null) {
                return TypeAndDefinition.of(typeDefinition);
            } else {
                return TypeAndDefinition.of(schema, shadow.getObjectClass());
            }
        } else {
            return TypeAndDefinition.of(schema, kind, intent);
        }
    }

    private @Nullable String getOrGenerateTag(
            @NotNull ResourceObjectProcessingContextImpl processingContext,
            @Nullable ResourceObjectDefinition definition,
            @NotNull OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ShadowType shadow = processingContext.getShadowedResourceObject();
        if (shadow.getTag() == null) {
            if (definition != null) {
                return beans.provisioningService.generateShadowTag(
                        processingContext.getShadowedResourceObject(),
                        processingContext.getResource(),
                        definition,
                        processingContext.getTask(),
                        result);
            } else {
                return null;
            }
        } else {
            return shadow.getTag();
        }
    }

    /**
     * Why do we need both type and definition? The object may be classified but only {@link ResourceObjectClassDefinition}
     * may be available - in the case of `account/default` hack. The reverse is also possible: the object may be unclassified,
     * but default type definition may apply.
     */
    private static class TypeAndDefinition {
        @Nullable private final ResourceObjectTypeIdentification typeIdentification;
        @Nullable private final ResourceObjectDefinition definition;

        private TypeAndDefinition(
                @Nullable ResourceObjectTypeIdentification typeIdentification,
                @Nullable ResourceObjectDefinition definition) {
            this.typeIdentification = typeIdentification;
            this.definition = definition;
        }

        public static @NotNull TypeAndDefinition of(@NotNull ResourceObjectTypeDefinition typeDefinition) {
            return new TypeAndDefinition(typeDefinition.getTypeIdentification(), typeDefinition);
        }

        public static @NotNull TypeAndDefinition of(ResourceSchema schema, ShadowKindType knownKind, String knownIntent) {
            return new TypeAndDefinition(
                    ResourceObjectTypeIdentification.of(knownKind, knownIntent),
                    schema != null ? schema.findObjectDefinition(knownKind, knownIntent) : null);
        }

        public static @NotNull TypeAndDefinition of(ResourceSchema schema, QName objectClassName) {
            if (schema != null && objectClassName != null) {
                ResourceObjectDefinition definition = schema.findDefinitionForObjectClass(objectClassName);
                if (definition != null && definition.getObjectClassDefinition().isDefaultAccountDefinition()) {
                    // A kind of "emergency classification" - we hope it will not cause any problems.
                    return new TypeAndDefinition(ResourceObjectTypeIdentification.defaultAccount(), definition);
                } else {
                    return new TypeAndDefinition(null, definition);
                }
            } else {
                return new TypeAndDefinition(null, null);
            }
        }
    }
}

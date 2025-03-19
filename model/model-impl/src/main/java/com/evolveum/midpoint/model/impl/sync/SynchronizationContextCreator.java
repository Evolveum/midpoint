/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import static com.evolveum.midpoint.model.impl.ResourceObjectProcessingContextImpl.ResourceObjectProcessingContextBuilder.aResourceObjectProcessingContext;
import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;

import com.evolveum.midpoint.provisioning.api.ResourceObjectClassification;
import com.evolveum.midpoint.schema.processor.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.ResourceObjectProcessingContextImpl;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
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
class SynchronizationContextCreator {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationContextCreator.class);

    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private ModelBeans beans;

    SynchronizationContext<FocusType> createSynchronizationContext(
            @NotNull ResourceObjectShadowChangeDescription change,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        ShadowType shadow = change.getShadowedResourceObject().asObjectable();
        ResourceType updatedResource = checkNotInMaintenance(change.getResource().asObjectable(), task, result);

        ResourceObjectProcessingContextImpl processingContext =
                aResourceObjectProcessingContext(shadow, updatedResource, task)
                        .withResourceObjectDelta(change.getObjectDelta())
                        .withExplicitChannel(change.getSourceChannel())
                        .withSystemConfiguration(
                                systemObjectCache.getSystemConfigurationBean(result))
                        .build();

        ObjectSynchronizationDiscriminatorType sorterResult =
                beans.synchronizationSorterEvaluator.evaluate(
                        shadow, processingContext.getResource(), task, result);

        // Note this may update shadow kind/intent.
        TypeAndDefinition typeAndDefinition = determineObjectTypeAndDefinition(processingContext, shadow, sorterResult, result);
        LOGGER.trace("Type and definition: {}", typeAndDefinition);

        String tag = getOrGenerateTag(processingContext, shadow, typeAndDefinition.definition, result);

        @Nullable SynchronizationPolicy policy =
                typeAndDefinition.typeIdentification != null ?
                        SynchronizationPolicyFactory.forKindAndIntent(
                                typeAndDefinition.typeIdentification.getKind(),
                                typeAndDefinition.typeIdentification.getIntent(),
                                updatedResource) :
                        null;
        LOGGER.trace("Synchronization policy: {}", policy);

        // i.e. type identification == null => policy == null

        SynchronizationContext<FocusType> syncCtx;
        if (policy != null && typeAndDefinition.definition != null) {
            syncCtx = new SynchronizationContext.Complete<>(
                    change,
                    processingContext,
                    typeAndDefinition.typeIdentification,
                    typeAndDefinition.definition,
                    policy,
                    sorterResult,
                    tag);
        } else {
            syncCtx = new SynchronizationContext.Incomplete<>(
                    change,
                    processingContext,
                    typeAndDefinition.typeIdentification,
                    typeAndDefinition.definition,
                    sorterResult,
                    tag);
        }

        if (Boolean.FALSE.equals(change.getShadowExistsInRepo())) {
            syncCtx.setShadowExistsInRepo(false);
            // TODO shadowExistsInRepo in syncCtx perhaps should be tri-state as well
        }
        LOGGER.trace("SYNCHRONIZATION context created: {}", syncCtx);
        return syncCtx;
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
        if (!SynchronizationContext.isSkipMaintenanceCheck()) {
            resource = beans.provisioningService
                    .getObject(ResourceType.class, resource.getOid(), readOnly(), task, result)
                    .asObjectable();
            ResourceTypeUtil.checkNotInMaintenance(resource);
        }
        return resource;
    }

    private @NotNull TypeAndDefinition determineObjectTypeAndDefinition(
            @NotNull ResourceObjectProcessingContextImpl processingContext,
            @NotNull ShadowType shadow,
            @Nullable ObjectSynchronizationDiscriminatorType sorterResult,
            @NotNull OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        @Nullable ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(processingContext.getResource());
        if (ShadowUtil.isClassified(shadow)) {
            if (isClassificationInSorterResult(sorterResult)) {
                // Sorter result overrides any classification information stored in the shadow
                // (and it is also applied to the shadow by SynchronizationContext#forceClassificationUpdate)
                return TypeAndDefinition.of(schema, sorterResult.getKind(), sorterResult.getIntent());
            } else {
                return TypeAndDefinition.of(schema, shadow.getKind(), shadow.getIntent());
            }
        } else {
            LOGGER.debug("Attempting to classify {}", shadow);
            // Most probably the classification attempt has been already done for the shadow: If it came through
            // get or search or live sync operation, the classification attempt is there. But what if there are more
            // exotic channels, like "external changes"? Let us try the classification once more.
            //
            // Note that the sorter result is used here (if it contains the classification)
            ResourceObjectClassification classification =
                    beans.provisioningService.classifyResourceObject(
                            shadow,
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
        }
    }

    private boolean isClassificationInSorterResult(@Nullable ObjectSynchronizationDiscriminatorType sorterResult) {
        return sorterResult != null
                && ShadowUtil.isKnown(sorterResult.getKind())
                && ShadowUtil.isKnown(sorterResult.getIntent());
    }

    private @Nullable String getOrGenerateTag(
            @NotNull ResourceObjectProcessingContextImpl processingContext,
            @NotNull ShadowType shadow,
            @Nullable ResourceObjectDefinition definition,
            @NotNull OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (shadow.getTag() == null) {
            if (definition != null) {
                return beans.provisioningService.generateShadowTag(
                        shadow,
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

        /**
         * Creates type spec based on object class name only (if present).
         *
         * We intentionally do not use "account/default" hack based on
         * {@link ResourceObjectClassDefinition#isDefaultAccountDefinition()} here, as it gives wrong results for accounts
         * that are intentionally excluded from `account/default` type (if such type is defined). After all, if we have
         * no `schemaHandling` present, there is no synchronization to be done. See MID-8516.
         */
        public static @NotNull TypeAndDefinition of(ResourceSchema schema, QName objectClassName) {
            if (schema != null && objectClassName != null) {
                @Nullable ResourceObjectDefinition definition = schema.findDefinitionForObjectClass(objectClassName);
                return new TypeAndDefinition(null, definition);
            } else {
                return new TypeAndDefinition(null, null);
            }
        }

        @Override
        public String toString() {
            return "TypeAndDefinition{" +
                    "typeIdentification=" + typeIdentification +
                    ", definition=" + definition +
                    '}';
        }
    }
}

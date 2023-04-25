/*
 * Copyright (c) 2015-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.impl.resources.ResourceManager;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceOperationCoordinates;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.processor.ResourceSchemaUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Creates instances of {@link ProvisioningContext}, either from scratch or spawning from existing one.
 *
 * Deals mainly with resolution of {@link ResourceObjectDefinition} objects. The hard part is delegated
 * to {@link ResourceSchemaUtil}.
 *
 * Note about the "unknown" values for kind/intent: They should come _only_ when determining
 * a definition for given shadow. They should never be requested by the client of provisioning API.
 */
@Component
public class ProvisioningContextFactory {

    @Autowired private ResourceManager resourceManager;
    @Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;
    @Autowired private CommonBeans commonBeans;

    /**
     * Creates the context when exact resource + object type is known. This is the most direct approach;
     * almost no extra activities have to be done.
     */
    @NotNull ProvisioningContext createForDefinition(
            @NotNull ResourceType resource,
            @NotNull ResourceObjectDefinition objectDefinition,
            @SuppressWarnings("SameParameterValue") Boolean wholeClass,
            @NotNull Task task) {
        return new ProvisioningContext(task, resource, objectDefinition, wholeClass, this);
    }

    /**
     * Creates the context when exact resource + coordinates are known.
     *
     * "Unknown" values for kind/intent are not supported here.
     *
     * Note: We set the `wholeClass` flag to `null`, because we are not expecting bulk operation here.
     * We are simply trying to create the context for a single shadow.
     */
    public @NotNull ProvisioningContext createForShadowCoordinates(
            @NotNull ResourceShadowCoordinates coords,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException {
        ResourceType resource = getResource(coords.getResourceOid(), task, result);
        return new ProvisioningContext(
                task,
                resource,
                ResourceSchemaUtil.findObjectDefinitionPrecisely(
                        resource,
                        coords.getKind(),
                        coords.getIntent(),
                        coords.getObjectClass()),
                null,
                this);
    }

    /**
     * Creates the context for "bulk operation", like search, live sync, or async update.
     * It is important to preserve the intention of the caller here, so e.g. if it specified
     * only the object class, we have to set the {@link ProvisioningContext#wholeClass}
     * appropriately.
     *
     * "Unknown" values for kind/intent are not supported here.
     */
    public @NotNull ProvisioningContext createForBulkOperation(
            @NotNull ResourceOperationCoordinates coords,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException {
        ResourceType resource = getResource(coords.getResourceOid(), task, result);
        ScopedDefinition scopedDefinition = createScopedDefinitionForBulkOperation(coords, resource);
        return new ProvisioningContext(
                task,
                resource,
                scopedDefinition.definition,
                scopedDefinition.wholeClass,
                this);
    }

    private ScopedDefinition createScopedDefinitionForBulkOperation(ResourceOperationCoordinates coords, ResourceType resource)
            throws SchemaException, ConfigurationException {

        coords.checkNotUnknown(); // This is also checked when looking for definition, but let's be explicit

        ShadowKindType kind = coords.getKind();
        String intent = coords.getIntent();
        QName objectClassName = coords.getObjectClassName();

        ResourceObjectDefinition definition =
                ResourceSchemaUtil.findDefinitionForBulkOperation(resource, kind, intent, objectClassName);

        Boolean wholeClass;
        if (coords.areObjectTypeScoped()) {
            wholeClass = false;
        } else if (coords.areObjectClassScoped()) {
            wholeClass = true; // definition may be of class (if we are lucky) or of type (in legacy situation)
        } else {
            wholeClass = null; // not important
        }

        return new ScopedDefinition(definition, wholeClass);
    }

    /**
     * Spawns the context for a potentially different kind/intent on the same resource.
     *
     * "Unknown" kind/intent is not supported.
     */
    @NotNull ProvisioningContext spawnForKindIntent(
            @NotNull ProvisioningContext originalCtx,
            @NotNull ShadowKindType kind,
            @NotNull String intent) throws SchemaException, ConfigurationException {
        ResourceSchema schema = originalCtx.getResourceSchema();
        return new ProvisioningContext(
                originalCtx,
                originalCtx.getTask(),
                getDefinition(schema, kind, intent),
                false); // The client has explicitly requested kind/intent, so it wants the type, not the class.
    }

    @NotNull
    private ResourceObjectDefinition getDefinition(
            @NotNull ResourceSchema schema, @NotNull ShadowKindType kind, @NotNull String intent) {
        ResourceObjectDefinition definition = schema.findObjectDefinitionRequired(kind, intent);
        return ResourceSchemaUtil.addOwnAuxiliaryObjectClasses(definition, schema);
    }

    /**
     * Spawns the context for an object class on the same resource.
     *
     * @param useRawDefinition If true, we want to get "raw" object class definition, not a refined (object class or type) one.
     */
    ProvisioningContext spawnForObjectClass(
            @NotNull ProvisioningContext originalCtx,
            @NotNull Task task,
            @NotNull QName objectClassName,
            boolean useRawDefinition) throws SchemaException, ConfigurationException {
        ResourceSchema resourceSchema = originalCtx.getResourceSchema();
        ResourceObjectDefinition definition = resourceSchema.findDefinitionForObjectClassRequired(objectClassName);
        ResourceObjectDefinition augmented = ResourceSchemaUtil.addOwnAuxiliaryObjectClasses(definition, resourceSchema);
        return new ProvisioningContext(
                originalCtx,
                task,
                useRawDefinition ? augmented.getRawObjectClassDefinition() : augmented,
                true);
    }

    /** A convenience variant of {@link #createForShadow(ShadowType, Task, OperationResult)}. */
    public ProvisioningContext createForShadow(
            @NotNull PrismObject<ShadowType> shadow,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return createForShadow(shadow.asObjectable(), task, result);
    }

    /**
     * Creates the context for a given shadow (pointing to resource, kind, and intent).
     *
     * Assuming there is no pre-resolved resource.
     */
    public ProvisioningContext createForShadow(
            @NotNull ShadowType shadow,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ResourceType resource = getResource(shadow, task, result);
        return new ProvisioningContext(
                task,
                resource,
                getObjectDefinition(resource, shadow, List.of()),
                null, // we don't expect any searches nor other bulk actions
                this);
    }

    /**
     * Creates the context for a given shadow (pointing to resource, kind, and intent).
     *
     * Assuming there is no pre-resolved resource.
     *
     * Additional auxiliary object class names are to be put into the object type definition.
     */
    public ProvisioningContext createForShadow(
            @NotNull ShadowType shadow,
            @NotNull Collection<QName> additionalAuxiliaryObjectClassNames,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ResourceType resource = getResource(shadow, task, result);
        return new ProvisioningContext(
                task,
                resource,
                getObjectDefinition(resource, shadow, additionalAuxiliaryObjectClassNames),
                null, // we don't expect any searches nor other bulk actions
                this);
    }

    /**
     * Creates the context for a given pre-resolved resource, and a shadow (pointing to kind, and intent).
     */
    public ProvisioningContext createForShadow(
            @NotNull ShadowType shadow,
            @NotNull ResourceType resource,
            @NotNull Task task)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
        return new ProvisioningContext(
                task,
                resource,
                getObjectDefinition(resource, shadow, List.of()),
                null, // we don't expect any searches nor other bulk actions
                this);
    }

    /**
     * Spawns the context for given shadow.
     *
     * Currently assumes that the resource OID is the same.
     *
     * TODO what if the shadow is "less-classified" (no kind/intent) than the original context?
     */
    ProvisioningContext spawnForShadow(
            @NotNull ProvisioningContext originalCtx,
            @NotNull ShadowType shadow) throws SchemaException, ConfigurationException {
        assertSameResource(originalCtx, shadow);
        return new ProvisioningContext(
                originalCtx,
                originalCtx.getTask(),
                getObjectDefinition(originalCtx.getResource(), shadow, List.of()),
                null // we don't expect any searches nor other bulk actions
        );
    }

    private void assertSameResource(@NotNull ProvisioningContext ctx, @NotNull ShadowType shadow) {
        String oidInShadow = ShadowUtil.getResourceOid(shadow);
        stateCheck(oidInShadow == null || oidInShadow.equals(ctx.getResourceOid()),
                "Not allowed to change resource OID in provisioning context (from %s to %s): %s",
                ctx.getResourceOid(), oidInShadow, ctx);
    }

    public @NotNull ResourceType getResource(ShadowType shadow, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException {
        return getResource(
                ShadowUtil.getResourceOidRequired(shadow),
                task, result);
    }

    public @NotNull ResourceType getResource(String resourceOid, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException {
        return resourceManager.getCompletedResource(resourceOid, GetOperationOptions.createReadOnly(), task, result);
    }

    private ResourceObjectDefinition getObjectDefinition(
            @NotNull ResourceType resource,
            @NotNull ShadowType shadow,
            @NotNull Collection<QName> additionalAuxiliaryObjectClassNames) throws SchemaException, ConfigurationException {
        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchemaRequired(resource);
        return schema.findDefinitionForShadow(shadow, additionalAuxiliaryObjectClassNames);
    }

    @NotNull ResourceManager getResourceManager() {
        return resourceManager;
    }

    @NotNull public LightweightIdentifierGenerator getLightweightIdentifierGenerator() {
        return lightweightIdentifierGenerator;
    }

    /** Object type/class definition with `wholeClass` option. */
    private static class ScopedDefinition {
        @Nullable private final ResourceObjectDefinition definition;
        @Nullable private final Boolean wholeClass;

        private ScopedDefinition(@Nullable ResourceObjectDefinition definition, @Nullable Boolean wholeClass) {
            this.definition = definition;
            this.wholeClass = wholeClass;
        }
    }

    public @NotNull CommonBeans getCommonBeans() {
        return commonBeans;
    }
}

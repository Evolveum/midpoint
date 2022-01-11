/*
 * Copyright (c) 2015-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import static com.evolveum.midpoint.util.MiscUtil.*;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinitionResolver;
import com.evolveum.midpoint.schema.processor.ResourceSchema;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Creates instances of {@link ProvisioningContext}, either from scratch or spawning from existing one.
 *
 * Deals mainly with resolution of {@link ResourceObjectDefinition} objects. The hard part is delegated
 * to {@link ResourceObjectDefinitionResolver}.
 *
 * Note about the "unknown" values for kind/intent: They should come _only_ when determining
 * a definition for given shadow. They should never be requested by the client of provisioning API.
 *
 * TODO implement MID-7470
 */
@Component
public class ProvisioningContextFactory {

    @Autowired private ResourceManager resourceManager;
    @Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;

    /**
     * Creates the context when exact resource + object type is known. This is the most direct approach;
     * almost no extra activities have to be done.
     */
    public ProvisioningContext createForDefinition(
            @NotNull ResourceType resource,
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull Task task)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
        return new ProvisioningContext(task, resource, objectDefinition, this);
    }

    /**
     * Creates the context when exact resource + coordinates are known.
     *
     * The coordinates may point to a specific object type, or to a whole object class.
     *
     * "Unknown" values for kind/intent are not supported here.
     */
    public ProvisioningContext createForCoordinates(
            @NotNull ResourceShadowCoordinates coords,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ResourceType resource = getResource(coords.getResourceOid(), task, result);
        return new ProvisioningContext(
                task,
                resource,
                ResourceObjectDefinitionResolver.getObjectDefinitionPrecisely(
                        resource,
                        coords.getKind(),
                        coords.getIntent(),
                        coords.getObjectClass(),
                        List.of(),
                        false),
                this);
    }

    /**
     * Spawns the context for a potentially different kind/intent on the same resource.
     *
     * "Unknown" kind/intent is not supported.
     */
    ProvisioningContext spawnForKindIntent(
            @NotNull ProvisioningContext originalCtx,
            @NotNull ShadowKindType kind,
            @NotNull String intent) throws SchemaException, ConfigurationException {
        return new ProvisioningContext(
                originalCtx,
                originalCtx.getTask(),
                ResourceObjectDefinitionResolver.getObjectDefinitionPrecisely(
                        originalCtx.getResource(),
                        kind,
                        intent,
                        null,
                        List.of(),
                        false));
    }

    /**
     * Spawns the context for an object class on the same resource.
     *
     * @param useRawDefinition If true, we want to get "raw" object class definition, not a refined (object type) one.
     */
    ProvisioningContext spawnForObjectClass(
            @NotNull ProvisioningContext originalCtx,
            @NotNull Task task,
            @NotNull QName objectClassName,
            boolean useRawDefinition) throws SchemaException, ConfigurationException {
        @NotNull ResourceObjectDefinition definition = ResourceObjectDefinitionResolver.getObjectDefinitionPrecisely(
                originalCtx.getResource(),
                null,
                null,
                objectClassName,
                List.of(),
                false);
        return new ProvisioningContext(
                originalCtx,
                task,
                useRawDefinition ? definition.getObjectClassDefinition() : definition);
    }

    /**
     * Creates the context for a given shadow (pointing to resource, kind, and intent).
     *
     * Assuming there is no pre-resolved resource.
     */
    public ProvisioningContext createForShadow(
            @NotNull PrismObject<ShadowType> shadow,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ResourceType resource = getResource(shadow, task, result);
        return new ProvisioningContext(
                task,
                resource,
                getObjectDefinition(resource, shadow, List.of()),
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
            @NotNull PrismObject<ShadowType> shadow,
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
                this);
    }

    /**
     * Creates the context for a given pre-resolved resource, and a shadow (pointing to kind, and intent).
     */
    public ProvisioningContext createForShadow(
            @NotNull PrismObject<ShadowType> shadow,
            @NotNull ResourceType resource,
            @NotNull Task task)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
        return new ProvisioningContext(
                task,
                resource,
                getObjectDefinition(resource, shadow, List.of()),
                this);
    }

    /**
     * Spawns the context for given shadow.
     *
     * Currently assumes that the resource OID is the same.
     */
    ProvisioningContext spawnForShadow(
            @NotNull ProvisioningContext originalCtx,
            @NotNull PrismObject<ShadowType> shadow) throws SchemaException, ConfigurationException {
        assertSameResource(originalCtx, shadow);
        return new ProvisioningContext(
                originalCtx,
                originalCtx.getTask(),
                getObjectDefinition(originalCtx.getResource(), shadow, List.of()));
    }

    private void assertSameResource(@NotNull ProvisioningContext ctx, @NotNull PrismObject<ShadowType> shadow) {
        String oidInShadow = ShadowUtil.getResourceOid(shadow);
        stateCheck(oidInShadow == null || oidInShadow.equals(ctx.getResourceOid()),
                "Not allowed to change resource OID in provisioning context (from %s to %s): %s",
                ctx.getResourceOid(), oidInShadow, ctx);
    }

    public @NotNull ResourceType getResource(PrismObject<ShadowType> shadow, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
        return getResource(
                ShadowUtil.getResourceOidRequired(shadow.asObjectable()),
                task, result);
    }

    public @NotNull ResourceType getResource(String resourceOid, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
        return resourceManager.getResource(resourceOid, GetOperationOptions.createReadOnly(), task, result)
                .asObjectable();
    }

    private ResourceObjectDefinition getObjectDefinition(
            @NotNull ResourceType resource,
            @NotNull PrismObject<ShadowType> shadow,
            @NotNull Collection<QName> additionalAuxiliaryObjectClassNames) throws SchemaException, ConfigurationException {

        return ResourceObjectDefinitionResolver.getObjectDefinitionPrecisely(
                resource,
                shadow.asObjectable().getKind(),
                shadow.asObjectable().getIntent(),
                shadow.asObjectable().getObjectClass(),
                MiscUtil.union(
                        shadow.asObjectable().getAuxiliaryObjectClass(),
                        additionalAuxiliaryObjectClassNames),
                true);
    }

    @NotNull ResourceManager getResourceManager() {
        return resourceManager;
    }

    @NotNull public LightweightIdentifierGenerator getLightweightIdentifierGenerator() {
        return lightweightIdentifierGenerator;
    }
}

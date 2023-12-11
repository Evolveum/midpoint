/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.impl.LazilyInitializableMixin;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.AbstractLazilyInitializableShadowedEntity;
import com.evolveum.midpoint.provisioning.util.InitializationState;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;

/**
 * *Lazily-initializable* representation of retrieved resource objects and resource object changes.
 *
 * Main responsibilities:
 *
 * - shadow acquisition in emergency and ultra-emergency modes
 * - shadow management (update, creation of shadowed object)
 * - support for initialization and provisioning context information
 *
 * Note that resource object, repo shadow, and shadowed objects are not held here, as their semantics are different
 * for objects and changes. They are accessed by abstract ("SPI") methods.
 *
 * @see ResourceObjectFound
 * @see ResourceObjectChange
 *
 * @see AbstractLazilyInitializableShadowedEntity
 */
public abstract class AbstractLazilyInitializableResourceEntity implements LazilyInitializableMixin {

    /**
     * Context of the processing that was known when the search or sync operation was invoked.
     * It contains the original (caller) task and only approximate (if any) class/kind/intent specification.
     */
    @NotNull final ProvisioningContext originalCtx;

    /**
     * The {@link #originalCtx} with the worker task applied. Available since initialization start.
     */
    ProvisioningContext globalCtx;

    /** The {@link #globalCtx} refined as needed during initialization. */
    ProvisioningContext effectiveCtx;

    /** State of the processing. */
    @NotNull final InitializationState initializationState = InitializationState.created();

    /** Useful beans local to the Resource objects package. */
    @NotNull final ResourceObjectsBeans b = ResourceObjectsBeans.get();

    AbstractLazilyInitializableResourceEntity(@NotNull ProvisioningContext originalCtx) {
        this.originalCtx = originalCtx;
    }

    @Override
    public void initializeInternalCommon(Task task, OperationResult result) throws SchemaException, ConfigurationException {
        globalCtx = originalCtx.spawn(task);
    }

    public @NotNull InitializationState getInitializationState() {
        return initializationState;
    }

    public @NotNull ProvisioningContext getEffectiveCtx() {
        return MiscUtil.getFirstNonNullRequired(effectiveCtx, globalCtx, originalCtx);
    }

    public abstract Object getPrimaryIdentifierValue();

    /** May be null in exceptional cases (e.g. delete no-class event in wildcard LS). */
    public abstract ResourceObjectDefinition getResourceObjectDefinition();

    /** "Minimalistic" resource object e.g. for ultra-emergency shadow creation. */
    @NotNull ExistingResourceObject getMinimalResourceObject() throws SchemaException {
        var definition = getResourceObjectDefinition();
        if (definition == null) {
            throw new IllegalStateException(
                    "No object definition in %s, cannot create minimal resource object".formatted(this));
        }
        var primaryIdentifierValue = getPrimaryIdentifierValue();
        if (primaryIdentifierValue == null) {
            throw new IllegalStateException(
                    "No primary identifier value in %s, cannot create minimal resource object".formatted(this));
        }
        return ExistingResourceObject.minimal(
                definition,
                primaryIdentifierValue,
                objectDoesExist(),
                getEffectiveCtx().getResourceOid());
    }

    /** TODO */
    abstract boolean objectDoesExist();

    /**
     * Fills-in provisioning policy, simulated activation, associations, and so on.
     * Modifies provided {@link ResourceObject} instance.
     */
    void completeResourceObject(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObject resourceObject,
            boolean fetchAssociations,
            @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        ProvisioningUtil.setEffectiveProvisioningPolicy(ctx, resourceObject, result);

        new ActivationConverter(ctx)
                .completeActivation(resourceObject, result);

        if (fetchAssociations) {
            EntitlementReader.read(resourceObject, ctx, result);
        }
    }
}

/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.util.ErrorState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

/**
 * Makes resource objects "complete" by resolving simulated activation, associations, and so on.
 */
class ResourceObjectCompleter {

    /**
     * Fills-in simulated activation, associations, and so on.
     * Modifies provided {@link ResourceObjectShadow} instance.
     *
     * This is the core of the processing on objects coming from the resources.
     *
     * Error handling:
     *
     * . Errors in input object: skips the processing
     * . Errors during the processing: just throws them out. They will be handled in {@link ResourceObjectFound}, if
     * the processing occurs in that context.
     */
    static @NotNull CompleteResourceObject completeResourceObject(
            @NotNull ProvisioningContext ctx,
            @NotNull ExistingResourceObjectShadow resourceObject,
            boolean fetchAssociations,
            @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        if (resourceObject.isError()) {
            // The idea is that we do not want to process objects which are not guaranteed to be "complete".
            return CompleteResourceObject.of(resourceObject, resourceObject.getErrorState());
        }

        new ActivationConverter(ctx)
                .completeActivation(resourceObject, result);

        new BehaviorConverter(ctx)
                .completeBehavior(resourceObject, result);

        if (fetchAssociations) {
            EntitlementReader.read(resourceObject, ctx, result);
        }

        return CompleteResourceObject.of(resourceObject, ErrorState.ok());
    }
}

/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fetches resource objects by their primary identifiers, handling the following methods:
 *
 * - {@link ResourceObjectConverter#fetchResourceObject(ProvisioningContext, ResourceObjectIdentification.WithPrimary,
 * ShadowItemsToReturn, boolean, OperationResult)}
 * - plus "fetch raw" called from various places, mainly related to entitlements
 */
class ResourceObjectFetchOperation extends AbstractResourceObjectRetrievalOperation {

    @NotNull private final ResourceObjectIdentification.WithPrimary primaryIdentification;

    private ResourceObjectFetchOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification.WithPrimary primaryIdentification,
            boolean fetchAssociations) {
        super(ctx, fetchAssociations, null);
        this.primaryIdentification = primaryIdentification;
    }

    /** Fetches the resource by the primary identifier. */
    static @NotNull CompleteResourceObject execute(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification.WithPrimary identification,
            boolean fetchAssociations,
            @Nullable ShadowItemsToReturn shadowItemsToReturn,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return new ResourceObjectFetchOperation(ctx, identification, fetchAssociations)
                .execute(shadowItemsToReturn, result);
    }

    /**
     * As {@link #execute(ProvisioningContext, ResourceObjectIdentification.WithPrimary, boolean,
     * ShadowItemsToReturn, OperationResult)} but without the "post-processing" contained in
     * {@link ResourceObjectCompleter}.
     *
     * This means no simulated activation, associations, and so on.
     */
    static @NotNull ExistingResourceObjectShadow executeRaw(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification.WithPrimary identification,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        return new ResourceObjectFetchOperation(ctx, identification, false)
                .executeRaw(null, result);
    }

    private @NotNull CompleteResourceObject execute(
            ShadowItemsToReturn shadowItemsToReturn, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var resourceObject = executeRaw(shadowItemsToReturn, result);
        return complete(resourceObject, result);
    }

    /**
     * Note that the identifiers here can be either primary or secondary.
     * In the latter case, the repository is used to find the primary identifier.
     */
    private @NotNull ExistingResourceObjectShadow executeRaw(
            @Nullable ShadowItemsToReturn shadowItemsToReturn,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        checkFullReadCapability();

        ConnectorInstance connector = ctx.getConnector(ReadCapabilityType.class, result);
        try {
            var ucfObject =
                    connector.fetchObject(primaryIdentification, shadowItemsToReturn, ctx.getUcfExecutionContext(), result);
            return ExistingResourceObjectShadow.fromUcf(ucfObject, ctx.getResourceRef());
        } catch (ObjectNotFoundException e) {
            // Not finishing the result because we did not create it! (The same for other catch clauses.)
            // We do not use simple "e.wrap" because there is a lot of things to be filled-in here.
            ObjectNotFoundException objectNotFoundException = new ObjectNotFoundException(
                    "Object not found. identifiers=%s, objectclass=%s: %s".formatted(
                            primaryIdentification, PrettyPrinter.prettyPrint(ctx.getObjectClassNameRequired()), e.getMessage()),
                    e,
                    ShadowType.class, null,
                    ctx.isAllowNotFound());
            result.recordExceptionNotFinish(objectNotFoundException);
            throw objectNotFoundException;
        } catch (CommunicationException e) {
            result.setFatalError(
                    "Error communication with the connector " + connector + ": " + e.getMessage(), e);
            throw e;
        } catch (GenericFrameworkException e) {
            result.setFatalError(
                    "Generic error in the connector " + connector + ". Reason: " + e.getMessage(), e);
            throw new GenericConnectorException(
                    "Generic error in the connector " + connector + ". Reason: " + e.getMessage(), e);
        } catch (SchemaException ex) {
            result.setFatalError("Can't get resource object, schema error: " + ex.getMessage(), ex);
            throw ex;
        } catch (ConfigurationException e) {
            result.setFatalError(e);
            throw e;
        }
    }

    private void checkFullReadCapability() {
        ReadCapabilityType readCapability = ctx.getEnabledCapability(ReadCapabilityType.class);
        if (readCapability == null) {
            throw new UnsupportedOperationException(
                    "Resource does not support 'read' operation: " + ctx.toHumanReadableDescription());
        } else if (Boolean.TRUE.equals(readCapability.isCachingOnly())) {
            throw new UnsupportedOperationException(
                    "Resource does not support 'read' operation (only 'caching only'): " + ctx.toHumanReadableDescription());
        }
    }
}

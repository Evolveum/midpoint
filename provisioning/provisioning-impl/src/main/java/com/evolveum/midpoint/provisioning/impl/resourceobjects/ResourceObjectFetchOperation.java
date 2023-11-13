/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManagerMiscUtil;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles fetch` method calls:
 *
 * - {@link ResourceObjectConverter#fetchResourceObject(ProvisioningContext, ResourceObjectIdentification.WithPrimary,
 * AttributesToReturn, ShadowType, boolean, OperationResult)}
 * - plus "fetch raw" called from various places, mainly related to entitlements
 *
 * @see ResourceObjectSearchOperation
 */
class ResourceObjectFetchOperation extends AbstractResourceObjectRetrievalOperation {

    @NotNull private final ResourceObjectIdentification.WithPrimary primaryIdentification;

    private ResourceObjectFetchOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification.WithPrimary primaryIdentification,
            boolean fetchAssociations,
            @Nullable FetchErrorReportingMethodType errorReportingMethod) {
        super(ctx, fetchAssociations, errorReportingMethod);
        this.primaryIdentification = primaryIdentification;
    }

    /**
     * Fetches the resource object either by primary identifier(s) or by secondary identifier(s).
     * In the latter case, the primary identifier is resolved (from the secondary ones) by the repository.
     */
    static @Nullable CompleteResourceObject execute(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification.WithPrimary identification,
            boolean fetchAssociations,
            @Nullable AttributesToReturn attributesToReturn,
            @Nullable ShadowType repoShadow,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return new ResourceObjectFetchOperation(ctx, identification, fetchAssociations, null)
                .execute(attributesToReturn, repoShadow, result);
    }

    /**
     * As {@link #execute(ProvisioningContext, ResourceObjectIdentification.WithPrimary, boolean, AttributesToReturn,
     * ShadowType, OperationResult)} but without the "post-processing" contained in
     * {@link ResourceObjectFound#completeResourceObject(ProvisioningContext, ResourceObject, boolean, OperationResult)}.
     *
     * This means no simulated activation, associations, and so on.
     */
    static ResourceObject executeRaw(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification.WithPrimary identification,
            @Nullable ShadowType repoShadow,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        return new ResourceObjectFetchOperation(ctx, identification, false, null)
                .executeRaw(null, repoShadow, result);
    }

    private @Nullable CompleteResourceObject execute(
            AttributesToReturn attributesToReturn, ShadowType repoShadow, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        ResourceObject resourceObject = executeRaw(attributesToReturn, repoShadow, result);
        if (resourceObject != null) {
            return complete(resourceObject, result);
        } else {
            return null;
        }
    }

    /**
     * Note that the identifiers here can be either primary or secondary.
     * In the latter case, the repository is used to find the primary identifier.
     *
     * Can return `null` only if the resource is caching only, and no `repoShadow` is provided.
     * (Probably not quite right approach!)
     *
     * @param repoShadow Used when read capability is "caching only"
     */
    @Nullable private ResourceObject executeRaw(
            @Nullable AttributesToReturn attributesToReturn,
            @Nullable ShadowType repoShadow,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ConnectorInstance connector = ctx.getConnector(ReadCapabilityType.class, result);
        ResourceObjectDefinition objectDefinition = ctx.getObjectDefinitionRequired();

        try {

            ReadCapabilityType readCapability = ctx.getEnabledCapability(ReadCapabilityType.class);
            if (readCapability == null) {
                throw new UnsupportedOperationException(
                        "Resource does not support 'read' operation: " + ctx.toHumanReadableDescription());
            }

            if (Boolean.TRUE.equals(readCapability.isCachingOnly())) {
                return repoShadow != null ?
                        ResourceObject.fromRepoShadow(
                                repoShadow,
                                ShadowManagerMiscUtil.determinePrimaryIdentifierValue(ctx, repoShadow)) :
                        null;
            }

            var object = connector.fetchObject(primaryIdentification, attributesToReturn, ctx.getUcfExecutionContext(), result);
            return ResourceObject.from(object);
        } catch (ObjectNotFoundException e) {
            // Not finishing the result because we did not create it! (The same for other catch clauses.)
            // We do not use simple "e.wrap" because there is a lot of things to be filled-in here.
            ObjectNotFoundException objectNotFoundException = new ObjectNotFoundException(
                    "Object not found. identifiers=%s, objectclass=%s: %s".formatted(
                            primaryIdentification, PrettyPrinter.prettyPrint(objectDefinition.getTypeName()), e.getMessage()),
                    e,
                    ShadowType.class,
                    repoShadow != null ? repoShadow.getOid() : null,
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
}

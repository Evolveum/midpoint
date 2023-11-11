/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManagerMiscUtil;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

/**
 * Handles `locate` and `fetch` method calls:
 *
 * - {@link ResourceObjectConverter#locateResourceObject(ProvisioningContext, ResourceObjectIdentification, boolean, OperationResult)}
 * - {@link ResourceObjectConverter#fetchResourceObject(ProvisioningContext, ResourceObjectIdentification.WithPrimary,
 * AttributesToReturn, ShadowType, boolean, OperationResult)}
 * - plus "fetch raw" called from various places, mainly related to entitlements
 *
 * @see ResourceObjectSearchOperation
 */
class ResourceObjectLocateOrFetchOperation extends AbstractResourceObjectSearchOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectLocateOrFetchOperation.class);

    @NotNull private final ResourceObjectIdentification<?> identification;

    private ResourceObjectLocateOrFetchOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification<?> identification,
            boolean fetchAssociations,
            @Nullable FetchErrorReportingMethodType errorReportingMethod) {
        super(ctx, fetchAssociations, errorReportingMethod);
        this.identification = identification;
    }

    /**
     * Locates the resource object either by primary identifier(s) or by secondary identifier(s).
     * Both cases are handled by the resource, i.e. not by the repository.
     */
    static CompleteResourceObject executeLocate(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification<?> identification,
            boolean fetchAssociations,
            @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return new ResourceObjectLocateOrFetchOperation(ctx, identification, fetchAssociations, null)
                .locate(result);
    }

    /**
     * Fetches the resource object either by primary identifier(s) or by secondary identifier(s).
     * In the latter case, the primary identifier is resolved (from the secondary ones) by the repository.
     */
    static @Nullable CompleteResourceObject executeFetch(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification.WithPrimary identification,
            boolean fetchAssociations,
            @Nullable AttributesToReturn attributesToReturn,
            @Nullable ShadowType repoShadow,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return new ResourceObjectLocateOrFetchOperation(ctx, identification, fetchAssociations, null)
                .fetch(attributesToReturn, repoShadow, result);
    }

    /**
     * As {@link #executeFetch(ProvisioningContext, ResourceObjectIdentification.WithPrimary, boolean, AttributesToReturn,
     * ShadowType, OperationResult)} but without the "post-processing" contained in
     * {@link ResourceObjectFound#completeResourceObject(ProvisioningContext, ResourceObject, boolean, OperationResult)}.
     *
     * This means no simulated activation, associations, and so on.
     */
    static ResourceObject executeFetchRaw(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification<?> identification,
            @Nullable ShadowType repoShadow,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        return new ResourceObjectLocateOrFetchOperation(ctx, identification, false, null)
                .fetchRaw(null, repoShadow, result);
    }

    private CompleteResourceObject locate(OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        LOGGER.trace("Locating resource object {}", identification);

        if (identification.hasPrimaryIdentifier()) {
            return fetch(
                    ctx.createAttributesToReturn(),
                    null,
                    result);
        } else {
            return searchBySecondaryIdentifiers(result);
        }
    }

    private @Nullable CompleteResourceObject fetch(
            AttributesToReturn attributesToReturn, ShadowType repoShadow, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        ResourceObject resourceObject = fetchRaw(attributesToReturn, repoShadow, result);
        if (resourceObject == null) {
            return null;
        }
        ResourceObjectFound objectFound = new ResourceObjectFound(resourceObject, ctx, fetchAssociations);
        objectFound.initialize(ctx.getTask(), result);
        return objectFound.asCompleteResourceObject();
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
    @Nullable private ResourceObject fetchRaw(
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

            var primaryIdentification =
                    b.resourceObjectReferenceResolver.resolvePrimaryIdentifier(ctx, identification, result);
            var object = connector.fetchObject(primaryIdentification, attributesToReturn, ctx.getUcfExecutionContext(), result);
            return ResourceObject.from(object);
        } catch (ObjectNotFoundException e) {
            // Not finishing the result because we did not create it! (The same for other catch clauses.)
            // We do not use simple "e.wrap" because there is a lot of things to be filled-in here.
            ObjectNotFoundException objectNotFoundException = new ObjectNotFoundException(
                    "Object not found. identifiers=%s, objectclass=%s: %s".formatted(
                            identification, PrettyPrinter.prettyPrint(objectDefinition.getTypeName()), e.getMessage()),
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
        } catch (ExpressionEvaluationException ex) {
            result.setFatalError("Can't get resource object, expression error: " + ex.getMessage(), ex);
            throw ex;
        } catch (ConfigurationException e) {
            result.setFatalError(e);
            throw e;
        }
    }

    private @NotNull CompleteResourceObject searchBySecondaryIdentifiers(OperationResult result)
            throws SchemaException, CommunicationException, SecurityViolationException, ObjectNotFoundException,
            ConfigurationException, ExpressionEvaluationException {

        ConnectorInstance connector = ctx.getConnector(ReadCapabilityType.class, result);

        var secondaryIdentifiers = identification.getSecondaryIdentifiers();

        LOGGER.trace("Searching by {}", secondaryIdentifiers);
        for (var secondaryIdentifier: secondaryIdentifiers) {
            ObjectQuery query = PrismContext.get().queryFor(ShadowType.class)
                    .item(secondaryIdentifier.getSearchPath(), secondaryIdentifier.getDefinition())
                    .eq(secondaryIdentifier.getValue())
                    .build();
            Holder<ResourceObject> objectHolder = new Holder<>();
            UcfObjectHandler handler = (ucfObject, lResult) -> {
                if (!objectHolder.isEmpty()) {
                    throw new IllegalStateException(
                            String.format("More than one object found for %s (%s)",
                                    secondaryIdentifier, ctx.getExceptionDescription()));
                }
                objectHolder.setValue(ResourceObject.from(ucfObject));
                return true;
            };
            try {
                // TODO constraints? scope?
                connector.search(
                        ctx.getObjectDefinitionRequired(),
                        query,
                        handler,
                        ctx.createAttributesToReturn(),
                        null,
                        null,
                        UcfFetchErrorReportingMethod.EXCEPTION,
                        ctx.getUcfExecutionContext(),
                        result);
                if (objectHolder.isEmpty()) {
                    // We could consider continuing with another secondary identifier, but let us keep the original behavior.
                    throw new ObjectNotFoundException(
                            String.format("No object found for %s (%s)",
                                    secondaryIdentifier, ctx.getExceptionDescription(connector)),
                            ShadowType.class,
                            null,
                            ctx.isAllowNotFound());
                }
                ResourceObject resourceObjectBefore = objectHolder.getValue();
                ResourceObjectFound objectFound = new ResourceObjectFound(resourceObjectBefore, ctx, fetchAssociations);
                objectFound.initialize(ctx.getTask(), result);
                LOGGER.trace("Located resource object {}", objectFound);
                return objectFound.asCompleteResourceObject();
            } catch (GenericFrameworkException e) {
                throw new GenericConnectorException(
                        String.format("Generic exception in connector while searching for object (%s): %s",
                                ctx.getExceptionDescription(connector), e.getMessage()),
                        e);
            }
        }
        throw new SchemaException( // Shouldn't be other kind of exception?
                String.format("No suitable secondary identifier(s) defined, cannot search for secondary identifiers among %s (%s)",
                        identification, ctx.getExceptionDescription()));
    }
}

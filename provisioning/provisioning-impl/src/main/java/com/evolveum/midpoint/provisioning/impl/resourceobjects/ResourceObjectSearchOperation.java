/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.provisioning.ucf.api.ShadowItemsToReturn;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.UcfResourceObject;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

/**
 * Searches for the resource objects, implementing this method:
 *
 * - {@link ResourceObjectConverter#searchResourceObjects(ProvisioningContext, ResourceObjectHandler, ObjectQuery,
 * boolean, FetchErrorReportingMethodType, OperationResult)}
 */
class ResourceObjectSearchOperation extends AbstractResourceObjectRetrievalOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectSearchOperation.class);

    @NotNull private final ResourceObjectHandler resultHandler;

    /** Query with delineation applied. */
    @NotNull private final QueryWithConstraints queryWithConstraints;

    @Nullable private final ShadowItemsToReturn shadowItemsToReturn;

    /** Just for numbering the objects for diagnostics purposes (for now). */
    private final AtomicInteger objectCounter = new AtomicInteger(0);

    private ResourceObjectSearchOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectHandler resultHandler,
            @NotNull QueryWithConstraints queryWithConstraints,
            @Nullable ShadowItemsToReturn shadowItemsToReturn,
            boolean fetchAssociations,
            @Nullable FetchErrorReportingMethodType errorReportingMethod) {
        super(ctx, fetchAssociations, errorReportingMethod);
        this.resultHandler = resultHandler;
        this.queryWithConstraints = queryWithConstraints;
        this.shadowItemsToReturn = shadowItemsToReturn;
    }

    /** The standard case: definition and limitations are taken from the context. */
    public static SearchResultMetadata execute(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectHandler resultHandler,
            @Nullable ObjectQuery query,
            boolean fetchAssociations,
            @Nullable FetchErrorReportingMethodType errorReportingMethod,
            @NotNull OperationResult parentResult)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(ResourceObjectConverter.OP_SEARCH_RESOURCE_OBJECTS);
        try {
            var queryWithConstraints = DelineationProcessor.determineQueryWithConstraints(ctx, query, result);
            var operation = new ResourceObjectSearchOperation(
                    ctx, resultHandler, queryWithConstraints,
                    ctx.createItemsToReturn(), fetchAssociations,
                    errorReportingMethod);
            return operation.execute(result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /** The non-standard case: the context is wildcard, and the definition and explicit query is provided independently. */
    @SuppressWarnings("UnusedReturnValue")
    static SearchResultMetadata execute(
            @NotNull ProvisioningContext wildcardCtx,
            @NotNull ResourceObjectDefinition definition,
            @NotNull ResourceObjectHandler resultHandler,
            @NotNull QueryWithConstraints queryWithConstraints,
            @Nullable ShadowItemsToReturn shadowItemsToReturn,
            @SuppressWarnings("SameParameterValue") boolean fetchAssociations,
            @NotNull OperationResult parentResult)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(ResourceObjectConverter.OP_SEARCH_RESOURCE_OBJECTS);
        try {
            wildcardCtx.assertWildcard();
            var ctx = wildcardCtx.spawnForDefinition(definition);
            var operation = new ResourceObjectSearchOperation(
                    ctx, resultHandler, queryWithConstraints, shadowItemsToReturn, fetchAssociations,
                    FetchErrorReportingMethodType.EXCEPTION);
            return operation.execute(result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    public SearchResultMetadata execute(OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        ResourceObjectDefinition objectDefinition = ctx.getObjectDefinitionRequired();

        LOGGER.trace("Searching resource objects, query: {}, object definition: {}", queryWithConstraints, objectDefinition);

        ObjectQuery query = queryWithConstraints.query();

        if (InternalsConfig.consistencyChecks && query != null && query.getFilter() != null) {
            query.getFilter().checkConsistence(true);
        }

        ConnectorInstance connector = ctx.getConnector(ReadCapabilityType.class, result);

        SearchResultMetadata metadata;
        try {

            // Note that although both search hierarchy constraints and custom filters are part of object type delineation,
            // they are treated differently. The former are handled by the UCF/ConnId connector, whereas the latter ones
            // are handled here.
            metadata = connector.search(
                    objectDefinition,
                    query,
                    this::handleObjectFound,
                    shadowItemsToReturn,
                    ctx.getEnabledCapability(PagedSearchCapabilityType.class),
                    queryWithConstraints.constraints(),
                    getUcfErrorReportingMethod(),
                    ctx.getUcfExecutionContext(),
                    result);

        } catch (GenericFrameworkException e) {
            throw new SystemException("Generic error in the connector: " + e.getMessage(), e);
        } catch (CommunicationException ex) {
            throw new CommunicationException(
                    "Error communicating with the connector " + connector + ": " + ex.getMessage(), ex);
        } catch (SecurityViolationException ex) {
            throw new SecurityViolationException(
                    "Security violation communicating with the connector " + connector + ": " + ex.getMessage(), ex);
        } catch (TunnelException e) {
            Throwable cause = e.getCause();
            String message = "Problem while executing the search using connector " + connector + ": " + cause.getMessage();
            Throwable enriched = MiscUtil.createSame(cause, message);
            if (cause instanceof SchemaException) {
                throw (SchemaException) enriched;
            } else if (cause instanceof CommunicationException) {
                throw (CommunicationException) enriched;
            } else if (cause instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException) enriched;
            } else if (cause instanceof ConfigurationException) {
                throw (ConfigurationException) enriched;
            } else if (cause instanceof SecurityViolationException) {
                throw (SecurityViolationException) enriched;
            } else if (cause instanceof ExpressionEvaluationException) {
                throw (ExpressionEvaluationException) enriched;
            } else if (cause instanceof GenericFrameworkException) {
                throw new GenericConnectorException(message, cause);
            } else {
                throw new SystemException(cause.getMessage(), cause);
            }
        }

        ResourceObjectConverter.computeResultStatusAndAsyncOpReference(result);

        LOGGER.trace("Searching resource objects done: {}", result.getStatus());

        return metadata;
    }

    private boolean handleObjectFound(UcfResourceObject ucfObject, OperationResult parentResult) {
        ucfObject.checkConsistence();

        ResourceObjectFound objectFound = ResourceObjectFound.fromUcf(ucfObject, ctx, fetchAssociations);

        // In order to utilize the cache right from the beginning.
        RepositoryCache.enterLocalCaches(b.cacheConfigurationManager);
        try {

            int objectNumber = objectCounter.getAndIncrement();

            try {
                OperationResult objResult = parentResult
                        .subresult(ResourceObjectConverter.OP_HANDLE_OBJECT_FOUND)
                        .setMinor()
                        .addParam("number", objectNumber)
                        .addArbitraryObjectAsParam("primaryIdentifierValue", ucfObject.getPrimaryIdentifierValue())
                        .addArbitraryObjectAsParam("errorState", ucfObject.getErrorState())
                        .build();
                try {
                    // Intentionally not initializing the object here. Let us be flexible and let the ultimate caller decide.
                    return resultHandler.handle(objectFound, objResult);
                } catch (Throwable t) {
                    objResult.recordException(t);
                    throw t;
                } finally {
                    objResult.close();
                }
            } finally {
                RepositoryCache.exitLocalCaches();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new TunnelException(t);
        }
    }
}

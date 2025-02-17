/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.UcfFetchErrorReportingMethod;
import com.evolveum.midpoint.provisioning.ucf.api.UcfObjectFound;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

import static com.evolveum.midpoint.schema.result.OperationResult.HANDLE_OBJECT_FOUND;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles {@link ResourceObjectConverter#searchResourceObjects(ProvisioningContext, ResourceObjectHandler, ObjectQuery,
 * boolean, FetchErrorReportingMethodType, OperationResult)} method call.
 */
class ResourceObjectSearchOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectSearchOperation.class);

    @NotNull private final ProvisioningContext ctx;

    private static final String OP_HANDLE_OBJECT_FOUND = ResourceObjectSearchOperation.class.getName() + "." + HANDLE_OBJECT_FOUND;

    @NotNull private final ResourceObjectHandler resultHandler;

    /** Query as requested by the client. */
    @Nullable private final ObjectQuery clientQuery;

    /** Whether associations should be fetched for the object found. */
    private final boolean fetchAssociations;

    @Nullable private final FetchErrorReportingMethodType errorReportingMethod;

    @NotNull private final ResourceObjectsBeans beans;

    private final AtomicInteger objectCounter = new AtomicInteger(0);

    ResourceObjectSearchOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectHandler resultHandler,
            @Nullable ObjectQuery clientQuery,
            boolean fetchAssociations,
            @Nullable FetchErrorReportingMethodType errorReportingMethod,
            @NotNull ResourceObjectsBeans beans) {
        this.ctx = ctx;
        this.resultHandler = resultHandler;
        this.clientQuery = clientQuery;
        this.fetchAssociations = fetchAssociations;
        this.errorReportingMethod = errorReportingMethod;
        this.beans = beans;
    }

    public SearchResultMetadata execute(OperationResult parentResult)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(ResourceObjectConverter.OP_SEARCH_RESOURCE_OBJECTS);
        try {

            ResourceObjectDefinition objectDefinition = ctx.getObjectDefinitionRequired();

            LOGGER.trace("Searching resource objects, query: {}, OC: {}", clientQuery, objectDefinition);

            QueryWithConstraints queryWithConstraints =
                    beans.delineationProcessor.determineQueryWithConstraints(ctx, clientQuery, result);

            if (InternalsConfig.consistencyChecks && clientQuery != null && clientQuery.getFilter() != null) {
                clientQuery.getFilter().checkConsistence(true);
            }

            ConnectorInstance connector = ctx.getConnector(ReadCapabilityType.class, result);

            SearchResultMetadata metadata;
            try {

                // Note that although both search hierarchy constraints and custom filters are part of object type delineation,
                // they are treated differently. The former are handled by the UCF/ConnId connector, whereas the latter ones
                // are handled here.
                metadata = connector.search(
                        objectDefinition,
                        queryWithConstraints.query,
                        this::handleObjectFound,
                        ctx.createAttributesToReturn(),
                        ctx.getEnabledCapability(PagedSearchCapabilityType.class),
                        queryWithConstraints.constraints,
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
                String message = "Problem while communicating with the connector " + connector + ": " + cause.getMessage();
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

            ResourceObjectConverter.computeResultStatus(result);

            LOGGER.trace("Searching resource objects done: {}", result.getStatus());

            return metadata;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private UcfFetchErrorReportingMethod getUcfErrorReportingMethod() {
        if (errorReportingMethod == FetchErrorReportingMethodType.FETCH_RESULT) {
            return UcfFetchErrorReportingMethod.UCF_OBJECT;
        } else {
            return UcfFetchErrorReportingMethod.EXCEPTION;
        }
    }

    private boolean handleObjectFound(UcfObjectFound ucfObject, OperationResult parentResult) {
        ResourceObjectFound objectFound =
                new ResourceObjectFound(ucfObject, beans.resourceObjectConverter, ctx, fetchAssociations);

        // in order to utilize the cache right from the beginning...
        RepositoryCache.enterLocalCaches(beans.cacheConfigurationManager);
        try {

            int objectNumber = objectCounter.getAndIncrement();

            Task task = ctx.getTask();
            try {
                OperationResult objResult = parentResult
                        .subresult(OP_HANDLE_OBJECT_FOUND)
                        .setMinor()
                        .addParam("number", objectNumber)
                        .addArbitraryObjectAsParam("primaryIdentifierValue", ucfObject.getPrimaryIdentifierValue())
                        .addArbitraryObjectAsParam("errorState", ucfObject.getErrorState()).build();
                try {
                    objectFound.initialize(task, objResult);
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

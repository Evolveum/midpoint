/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsSimulateType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

/**
 * Handles {@link ResourceObjectConverter#countResourceObjects(ProvisioningContext, ObjectQuery, OperationResult)} method call:
 * either natively, or by using one of the simulation approaches.
 */
class ResourceObjectCountOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectCountOperation.class);

    @NotNull private final ProvisioningContext ctx;

    /** Query as requested by the client. */
    @Nullable private final ObjectQuery clientQuery;

    @NotNull private final ResourceObjectsBeans beans;

    ResourceObjectCountOperation(
            @NotNull ProvisioningContext ctx,
            @Nullable ObjectQuery clientQuery,
            @NotNull ResourceObjectsBeans beans) {
        this.ctx = ctx;
        this.clientQuery = clientQuery;
        this.beans = beans;
    }

    public Integer execute(OperationResult parentResult)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(ResourceObjectConverter.OP_COUNT_RESOURCE_OBJECTS);
        try {
            CountObjectsCapabilityType countCapability = ctx.getEnabledCapability(CountObjectsCapabilityType.class);
            if (countCapability == null) {
                LOGGER.trace("countObjects: cannot count (no counting capability)");
                result.recordNotApplicable("no counting capability");
                return null; // This means "I do not know".
            }

            CountObjectsSimulateType simulate = countCapability.getSimulate();
            if (simulate == null) {
                return executeNative(result);
            } else if (simulate == CountObjectsSimulateType.PAGED_SEARCH_ESTIMATE) {
                return executeUsingPagedSearchEstimate(result);
            } else if (simulate == CountObjectsSimulateType.SEQUENTIAL_SEARCH) {
                return executeUsingSequentialSearch(result);
            } else {
                throw new IllegalArgumentException("Unknown count capability simulation type: " + simulate);
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
            result.cleanup();
        }
    }

    private int executeNative(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException {
        LOGGER.trace("countObjects: counting with native count capability");
        ConnectorInstance connector = ctx.getConnector(ReadCapabilityType.class, result);

        ResourceObjectDefinition objectTypeDef = ctx.getObjectDefinitionRequired();
        try {
            return connector.count(
                    objectTypeDef,
                    clientQuery,
                    objectTypeDef.getPagedSearches(ctx.getResource()),
                    ctx.getUcfExecutionContext(),
                    result);
        } catch (GenericFrameworkException e) {
            throw new SystemException(e.getMessage(), e);
        }
    }

    private Integer executeUsingPagedSearchEstimate(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        LOGGER.trace("countObjects: simulating counting with paged search estimate");
        if (!ctx.hasCapability(PagedSearchCapabilityType.class)) {
            throw new ConfigurationException(
                    "Configured count object capability to be simulated using a paged search"
                            + " but paged search capability is not present");
        }

        ObjectPaging paging = PrismContext.get().queryFactory().createPaging();
        // Explicitly set offset. This makes a difference for some resources.
        // E.g. LDAP connector will detect presence of an offset and it will initiate VLV search which
        // can estimate number of results. If no offset is specified then continuous/linear search is
        // assumed (e.g. Simple Paged Results search). Such search does not have ability to estimate
        // number of results.
        paging.setOffset(0);
        paging.setMaxSize(1);
        return executeCountingSearch(CountMethod.METADATA, paging, result);
    }

    private Integer executeUsingSequentialSearch(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        LOGGER.trace("countObjects: simulating counting with sequential search (likely performance impact)");
        return executeCountingSearch(CountMethod.COUNTING, null, result);
    }

    private Integer executeCountingSearch(CountMethod countMethod, ObjectPaging paging, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        AtomicInteger counter = new AtomicInteger(0);
        ResourceObjectHandler countingHandler = (ResourceObjectFound object, OperationResult objResult) -> {
            counter.incrementAndGet();
            return true;
        };

        ObjectQuery actualQuery = clientQuery != null ? clientQuery.clone() : PrismContext.get().queryFactory().createQuery();
        actualQuery.setPaging(paging);

        var metadata = beans.resourceObjectConverter.searchResourceObjects(
                ctx,
                countingHandler,
                actualQuery,
                false,
                FetchErrorReportingMethodType.FETCH_RESULT,
                result);
        if (countMethod == CountMethod.METADATA) {
            return metadata != null ? metadata.getApproxNumberOfAllResults() : null;
        } else if (countMethod == CountMethod.COUNTING) {
            return counter.get();
        } else {
            throw new AssertionError("Unknown counting method: " + countMethod);
        }
    }

    private enum CountMethod {
        METADATA, COUNTING
    }
}

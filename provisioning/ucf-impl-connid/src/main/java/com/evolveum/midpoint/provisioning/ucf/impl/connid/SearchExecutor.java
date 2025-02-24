/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper.ucfAttributeNameToConnId;
import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdUtil.processConnIdException;

import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.reporting.ConnIdOperation;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.Validate;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ProvisioningOperation;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;

/**
 * Executes `search` operation. (Offloads {@link ConnectorInstanceConnIdImpl} from this task.)
 */
class SearchExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(SearchExecutor.class);

    @NotNull private final ResourceObjectDefinition resourceObjectDefinition;
    @NotNull private final ObjectClass icfObjectClass;
    private final ObjectQuery query;
    private final Filter connIdFilter;
    @NotNull private final UcfObjectHandler handler;
    private final ShadowItemsToReturn shadowItemsToReturn;
    private final PagedSearchCapabilityType pagedSearchConfiguration;
    private final SearchHierarchyConstraints searchHierarchyConstraints;
    private final UcfFetchErrorReportingMethod errorReportingMethod;
    @NotNull private final ConnectorOperationContext operationContext;
    @NotNull private final ConnectorInstanceConnIdImpl connectorInstance;

    /**
     * Increases on each object fetched. Used for simulated paging and overall result construction.
     * We can assume we work in a single thread, but let's play it safe.
     */
    private final AtomicInteger objectsFetched = new AtomicInteger(0);

    SearchExecutor(
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            ObjectQuery query,
            @NotNull UcfObjectHandler handler,
            ShadowItemsToReturn shadowItemsToReturn,
            PagedSearchCapabilityType pagedSearchConfiguration,
            SearchHierarchyConstraints searchHierarchyConstraints,
            UcfFetchErrorReportingMethod errorReportingMethod,
            @NotNull ConnectorOperationContext operationContext,
            @NotNull ConnectorInstanceConnIdImpl connectorInstance) throws SchemaException {

        this.resourceObjectDefinition = resourceObjectDefinition;
        this.icfObjectClass = connectorInstance.objectClassToConnId(resourceObjectDefinition);
        this.query = query;
        this.connIdFilter = connectorInstance.convertFilterToIcf(query, resourceObjectDefinition);
        this.handler = handler;
        this.shadowItemsToReturn = shadowItemsToReturn;
        this.pagedSearchConfiguration = pagedSearchConfiguration;
        this.searchHierarchyConstraints = searchHierarchyConstraints;
        this.errorReportingMethod = errorReportingMethod;
        this.operationContext = operationContext;
        this.connectorInstance = connectorInstance;
    }

    public SearchResultMetadata execute(OperationResult result)
            throws CommunicationException, ObjectNotFoundException,
            GenericFrameworkException, SchemaException, SecurityViolationException {

        if (isNoConnectorPaging() && query != null && query.getPaging() != null &&
                (query.getPaging().getOffset() != null || query.getPaging().getMaxSize() != null)) {
            InternalMonitor.recordCount(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);
        }

        OperationOptions connIdOptions = createOperationOptions();

        SearchResult connIdSearchResult = executeConnIdSearch(connIdOptions, result);

        return createResultMetadata(connIdSearchResult, connIdOptions);
    }

    @NotNull
    private OperationOptions createOperationOptions() throws SchemaException {
        OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();

        setupAttributesToGet(optionsBuilder);
        setupPagingAndSorting(optionsBuilder);
        setupSearchHierarchyScope(optionsBuilder);

        setupPartialResults(optionsBuilder);

        // Relax completeness requirements. This is a search, not get. So it is OK to
        // return incomplete member lists and similar attributes.
        optionsBuilder.setAllowPartialAttributeValues(true);

        return optionsBuilder.build();
    }

    private void setupAttributesToGet(OperationOptionsBuilder optionsBuilder) throws SchemaException {
        connectorInstance.convertToIcfAttrsToGet(resourceObjectDefinition, shadowItemsToReturn, optionsBuilder);
    }

    private void setupPagingAndSorting(OperationOptionsBuilder optionsBuilder) throws SchemaException {
        if (isNoConnectorPaging() || query == null || query.getPaging() == null) {
            return;
        }

        ObjectPaging paging = query.getPaging();
        if (paging.getOffset() != null) {
            optionsBuilder.setPagedResultsOffset(paging.getOffset() + 1); // ConnId API says the numbering starts at 1
        }
        if (paging.getMaxSize() != null) {
            optionsBuilder.setPageSize(paging.getMaxSize());
        }
        QName orderByAttributeName;
        boolean isAscending;
        ItemPath orderByPath = paging.getPrimaryOrderingPath();
        String desc;
        if (ItemPath.isNotEmpty(orderByPath)) {
            orderByAttributeName = ShadowUtil.getAttributeName(orderByPath, "OrderBy path");
            if (SchemaConstants.C_NAME.equals(orderByAttributeName)) {
                orderByAttributeName = SchemaConstants.ICFS_NAME; // What a hack...
            }
            isAscending = paging.getPrimaryOrderingDirection() != OrderDirection.DESCENDING;
            desc = "(explicitly specified orderBy attribute)";
        } else {
            orderByAttributeName = pagedSearchConfiguration.getDefaultSortField();
            isAscending = pagedSearchConfiguration.getDefaultSortDirection() != OrderDirectionType.DESCENDING;
            desc = "(default orderBy attribute from capability definition)";
        }
        if (orderByAttributeName != null) {
            String orderByIcfName = ucfAttributeNameToConnId(orderByAttributeName, resourceObjectDefinition, desc);
            optionsBuilder.setSortKeys(new SortKey(orderByIcfName, isAscending));
        }
    }

    private void setupPartialResults(OperationOptionsBuilder optionsBuilder) {
        if (query != null && query.isAllowPartialResults()) {
            optionsBuilder.setAllowPartialResults(query.isAllowPartialResults());
        }
    }

    private void setupSearchHierarchyScope(OperationOptionsBuilder optionsBuilder) throws SchemaException {
        if (searchHierarchyConstraints != null) {
            ResourceObjectIdentification.WithPrimary baseContextIdentification = searchHierarchyConstraints.getBaseContext();
            if (baseContextIdentification != null) {
                // Only LDAP connector really supports base context. And this one will work better with
                // DN. And DN is secondary identifier (__NAME__). This is ugly, but practical. It works around ConnId problems.
                //
                // Limitations/assumptions: there is exactly one identifier - either secondary (and it's DN in that case)
                // or primary which is the same as secondary (for the particular resource).
                ResourceObjectIdentifier<?> identifierToUse;
                var secIdentifiers = baseContextIdentification.getSecondaryIdentifiers();
                if (secIdentifiers.size() == 1) {
                    // the standard case
                    identifierToUse = secIdentifiers.iterator().next();
                } else if (secIdentifiers.isEmpty()) {
                    if (resourceObjectDefinition.getSecondaryIdentifiers().isEmpty()) {
                        // This object class obviously has __NAME__ and __UID__ the same. Primary identifier will work here.
                        identifierToUse = baseContextIdentification.getPrimaryIdentifier();
                    } else {
                        throw new SchemaException(
                                "No secondary identifier in base context identification " + baseContextIdentification);
                    }
                } else {
                    throw new SchemaException(
                            "More than one secondary identifier in base context identification is not supported: %s".formatted(
                                    baseContextIdentification));
                }
                ObjectClass baseContextIcfObjectClass = connectorInstance.objectClassToConnId(
                        baseContextIdentification.getResourceObjectDefinition());
                optionsBuilder.setContainer(
                        new QualifiedUid(baseContextIcfObjectClass, new Uid(identifierToUse.getStringOrigValue())));
            }
            SearchHierarchyScope scope = searchHierarchyConstraints.getScope();
            if (scope != null) {
                optionsBuilder.setScope(scope.getString());
            }
        }
    }

    private SearchResult executeConnIdSearch(OperationOptions connIdOptions, OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, GenericFrameworkException, SchemaException,
            SecurityViolationException {

        // Connector operation cannot create result for itself, so we need to create result for it
        OperationResult result = parentResult.createSubresult(ConnectorInstanceConnIdImpl.FACADE_OP_SEARCH);
        result.addArbitraryObjectAsParam("objectClass", icfObjectClass);

        SearchResult connIdSearchResult;
        InternalMonitor.recordConnectorOperation("search");
        ConnIdOperation operation = recordIcfOperationStart();

        try {
            LOGGER.trace("Executing ConnId search operation: {}", operation);
            connIdSearchResult = connectorInstance.getConnIdConnectorFacadeRequired()
                    .search(
                            icfObjectClass,
                            connIdFilter,
                            new SearchResultsHandler(operation, result),
                            connIdOptions);
            recordIcfOperationEnd(operation, null);

            result.recordSuccess();
        } catch (IntermediateSchemaException inEx) {
            SchemaException ex = inEx.getSchemaException();
            recordIcfOperationEnd(operation, ex);
            result.recordFatalError(ex);
            throw ex;
        } catch (Throwable ex) {
            recordIcfOperationEnd(operation, ex);
            Throwable midpointEx = processConnIdException(ex, connectorInstance, result);
            throwProperException(midpointEx, ex);
            throw new AssertionError("should not get here");
        } finally {
            result.close();
        }
        return connIdSearchResult;
    }

    /** Do some kind of acrobatics to do proper throwing of checked exception */
    private void throwProperException(Throwable transformed, Throwable original)
            throws CommunicationException, ObjectNotFoundException, GenericFrameworkException, SchemaException,
            SecurityViolationException {
        if (transformed instanceof CommunicationException communicationException) {
            throw communicationException;
        } else if (transformed instanceof ObjectNotFoundException objectNotFoundException) {
            throw objectNotFoundException;
        } else if (transformed instanceof GenericFrameworkException genericFrameworkException) {
            throw genericFrameworkException;
        } else if (transformed instanceof SchemaException schemaException) {
            throw schemaException;
        } else if (transformed instanceof SecurityViolationException securityViolationException) {
            throw securityViolationException;
        } else if (transformed instanceof RuntimeException runtimeException) {
            throw runtimeException;
        } else if (transformed instanceof Error error) {
            throw error;
        } else {
            throw new SystemException(
                    "Got unexpected exception: %s: %s".formatted(
                            original.getClass().getName(), original.getMessage()),
                    original);
        }
    }

    private ConnIdOperation recordIcfOperationStart() {
        return connectorInstance.recordIcfOperationStart(
                operationContext.ucfExecutionContext(), ProvisioningOperation.ICF_SEARCH, resourceObjectDefinition);
    }

    private void recordIcfOperationEnd(ConnIdOperation operation, Throwable ex) {
        connectorInstance.recordIcfOperationEnd(operationContext.ucfExecutionContext(), operation, ex);
    }

    private void recordIcfOperationResume(@NotNull ConnIdOperation operation) {
        connectorInstance.recordIcfOperationResume(operationContext.ucfExecutionContext(), operation);
    }

    private void recordIcfOperationSuspend(@NotNull ConnIdOperation operation) {
        connectorInstance.recordIcfOperationSuspend(operationContext.ucfExecutionContext(), operation);
    }

    @Nullable
    private SearchResultMetadata createResultMetadata(SearchResult connIdSearchResult, OperationOptions connIdOptions) {
        if (connIdSearchResult == null) {
            return null;
        }

        SearchResultMetadata metadata = new SearchResultMetadata();
        metadata.setPagingCookie(connIdSearchResult.getPagedResultsCookie());
        int remainingPagedResults = connIdSearchResult.getRemainingPagedResults();
        if (remainingPagedResults >= 0) {
            int offset;
            Integer connIdOffset = connIdOptions.getPagedResultsOffset();
            if (connIdOffset != null && connIdOffset > 0) {
                offset = connIdOffset - 1;
            } else {
                offset = 0;
            }
            int allResults = remainingPagedResults + offset + objectsFetched.get();
            metadata.setApproxNumberOfAllResults(allResults);
        }
        if (!connIdSearchResult.isAllResultsReturned()) {
            metadata.setPartialResults(true);
        }
        return metadata;
    }

    private boolean isNoConnectorPaging() {
        return pagedSearchConfiguration == null;
    }

    private class SearchResultsHandler implements ResultsHandler {

        @NotNull private final ConnIdOperation operation;

        /** This is the operation result connected to the whole search operation. */
        private final OperationResult parentResult;

        SearchResultsHandler(@NotNull ConnIdOperation operation, OperationResult parentResult) {
            this.operation = operation;
            this.parentResult = parentResult;
        }

        @Override
        public boolean handle(ConnectorObject connectorObject) {
            Validate.notNull(connectorObject, "null connector object"); // todo apply error reporting method?

            int number = objectsFetched.getAndIncrement(); // The numbering starts at 0
            var result = parentResult.subresult(ConnectorInstanceConnIdImpl.OP_HANDLE_OBJECT_FOUND)
                    .addParam("objectNumber", number)
                    .addParam("uid", connectorObject.getUid().getUidValue())
                    .addParam("name", connectorObject.getName().getNameValue())
                    .setMinor()
                    .build();
            recordIcfOperationSuspend(operation);
            try {
                if (isNoConnectorPaging()) {
                    if (query != null && query.getPaging() != null) {
                        int offset = MoreObjects.firstNonNull(query.getPaging().getOffset(), 0);
                        Integer maxSize = query.getPaging().getMaxSize();
                        if (number < offset) {
                            return true;
                        }
                        if (maxSize != null && number >= offset + maxSize) {
                            return false;
                        }
                    }
                }

                var ucfObject = connectorInstance.connIdObjectConvertor.convertToUcfObject(
                        connectorObject, resourceObjectDefinition, errorReportingMethod, operationContext, result);

                return handler.handle(ucfObject, result);

            } catch (SchemaException e) {
                result.recordException(e);
                throw new IntermediateSchemaException(e);
            } finally {
                recordIcfOperationResume(operation);
                result.close();
                result.deleteSubresultsIfPossible();
                parentResult.summarize();
            }
        }

        @Override
        public String toString() {
            return "(midPoint searching result handler)";
        }
    }
}

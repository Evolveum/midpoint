/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdUtil.processConnIdException;
import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnectorInstanceConnIdImpl.toShadowDefinition;
import static com.evolveum.midpoint.schema.result.OperationResult.HANDLE_OBJECT_FOUND;

import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.reporting.ConnIdOperation;

import com.evolveum.midpoint.provisioning.ucf.api.UcfExecutionContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.Validate;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;

/**
 * Executes `search` operation. (Offloads {@link ConnectorInstanceConnIdImpl} from this task.)
 */
class SearchExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(SearchExecutor.class);

    private static final String OP_HANDLE_OBJECT_FOUND = SearchExecutor.class.getName() + "." + HANDLE_OBJECT_FOUND;

    @NotNull private final ResourceObjectDefinition resourceObjectDefinition;
    @NotNull private final PrismObjectDefinition<ShadowType> prismObjectDefinition;
    @NotNull private final ObjectClass icfObjectClass;
    private final ObjectQuery query;
    private final Filter connIdFilter;
    @NotNull private final UcfObjectHandler handler;
    private final AttributesToReturn attributesToReturn;
    private final PagedSearchCapabilityType pagedSearchConfiguration;
    private final SearchHierarchyConstraints searchHierarchyConstraints;
    private final UcfFetchErrorReportingMethod errorReportingMethod;
    private final UcfExecutionContext reporter;
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
            AttributesToReturn attributesToReturn,
            PagedSearchCapabilityType pagedSearchConfiguration,
            SearchHierarchyConstraints searchHierarchyConstraints,
            UcfFetchErrorReportingMethod errorReportingMethod,
            UcfExecutionContext reporter,
            @NotNull ConnectorInstanceConnIdImpl connectorInstance) throws SchemaException {

        this.resourceObjectDefinition = resourceObjectDefinition;
        this.prismObjectDefinition = toShadowDefinition(resourceObjectDefinition);
        this.icfObjectClass = connectorInstance.objectClassToConnId(resourceObjectDefinition);
        this.query = query;
        this.connIdFilter = connectorInstance.convertFilterToIcf(query, resourceObjectDefinition);
        this.handler = handler;
        this.attributesToReturn = attributesToReturn;
        this.pagedSearchConfiguration = pagedSearchConfiguration;
        this.searchHierarchyConstraints = searchHierarchyConstraints;
        this.errorReportingMethod = errorReportingMethod;
        this.reporter = reporter;
        this.connectorInstance = connectorInstance;
    }

    public SearchResultMetadata execute(OperationResult result) throws CommunicationException, ObjectNotFoundException,
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
        connectorInstance.convertToIcfAttrsToGet(resourceObjectDefinition, attributesToReturn, optionsBuilder);
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
            String orderByIcfName = connectorInstance.connIdNameMapper.convertAttributeNameToConnId(
                    orderByAttributeName, resourceObjectDefinition, desc);
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
            ResourceObjectIdentification baseContextIdentification = searchHierarchyConstraints.getBaseContext();
            if (baseContextIdentification != null) {
                // Only LDAP connector really supports base context. And this one will work better with
                // DN. And DN is secondary identifier (__NAME__). This is ugly, but practical. It works around ConnId problems.
                ResourceAttribute<?> secondaryIdentifier = baseContextIdentification.getSecondaryIdentifier();
                String identifierValue;
                if (secondaryIdentifier == null) {
                    if (resourceObjectDefinition.getSecondaryIdentifiers().isEmpty()) {
                        // This object class obviously has __NAME__ and __UID__ the same. Primary identifier will work here.
                        identifierValue = baseContextIdentification.getPrimaryIdentifier().getRealValue(String.class);
                    } else {
                        throw new SchemaException("No secondary identifier in base context identification " + baseContextIdentification);
                    }
                } else {
                    identifierValue = secondaryIdentifier.getRealValue(String.class);
                }
                ObjectClass baseContextIcfObjectClass = connectorInstance.objectClassToConnId(
                        baseContextIdentification.getResourceObjectDefinition());
                QualifiedUid containerQualifiedUid = new QualifiedUid(baseContextIcfObjectClass, new Uid(identifierValue));
                optionsBuilder.setContainer(containerQualifiedUid);
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
            connIdSearchResult = connectorInstance.getConnIdConnectorFacade()
                    .search(
                            icfObjectClass,
                            connIdFilter,
                            new SearchResultsHandler(operation, result),
                            connIdOptions);
            recordIcfOperationEnd(operation, null);

            result.recordSuccess();
        } catch (IntermediateException inEx) {
            Throwable ex = inEx.getCause();
            recordIcfOperationEnd(operation, ex);
            result.recordFatalError(ex);
            throwProperException(ex, ex);
            throw new AssertionError("should not get here");
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
    private void throwProperException(Throwable transformed, Throwable original) throws CommunicationException,
            ObjectNotFoundException, GenericFrameworkException, SchemaException, SecurityViolationException {
        if (transformed instanceof CommunicationException) {
            throw (CommunicationException) transformed;
        } else if (transformed instanceof ObjectNotFoundException) {
            throw (ObjectNotFoundException) transformed;
        } else if (transformed instanceof GenericFrameworkException) {
            throw (GenericFrameworkException) transformed;
        } else if (transformed instanceof SchemaException) {
            throw (SchemaException) transformed;
        } else if (transformed instanceof SecurityViolationException) {
            throw (SecurityViolationException) transformed;
        } else if (transformed instanceof RuntimeException) {
            throw (RuntimeException) transformed;
        } else if (transformed instanceof Error) {
            throw (Error) transformed;
        } else {
            throw new SystemException("Got unexpected exception: " + original.getClass().getName() + ": " + original.getMessage(),
                    original);
        }
    }

    private ConnIdOperation recordIcfOperationStart() {
        return connectorInstance.recordIcfOperationStart(reporter, ProvisioningOperation.ICF_SEARCH, resourceObjectDefinition);
    }

    private void recordIcfOperationEnd(ConnIdOperation operation, Throwable ex) {
        connectorInstance.recordIcfOperationEnd(reporter, operation, ex);
    }

    private void recordIcfOperationResume(@NotNull ConnIdOperation operation) {
        connectorInstance.recordIcfOperationResume(reporter, operation);
    }

    private void recordIcfOperationSuspend(@NotNull ConnIdOperation operation) {
        connectorInstance.recordIcfOperationSuspend(reporter, operation);
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
            var result = parentResult.subresult(OP_HANDLE_OBJECT_FOUND)
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

                UcfObjectFound ucfObject = connectorInstance.connIdConvertor.convertToUcfObject(
                        connectorObject, prismObjectDefinition, false, connectorInstance.isCaseIgnoreAttributeNames(),
                        connectorInstance.isLegacySchema(), errorReportingMethod, result);

                return handler.handle(ucfObject, result);

            } catch (SchemaException e) {
                result.recordException(e);
                throw new IntermediateException(e);
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

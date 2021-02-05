/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

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
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.ObjectUtils;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.util.concurrent.atomic.AtomicInteger;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdUtil.processConnIdException;
import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnectorInstanceConnIdImpl.toShadowDefinition;

/**
 * Executes `search` operation. (Offloads {@link ConnectorInstanceConnIdImpl} from this task.)
 */
class SearchExecutor {

    @NotNull private final ObjectClassComplexTypeDefinition objectClassDefinition;
    @NotNull private final PrismObjectDefinition<ShadowType> objectDefinition;
    @NotNull private final ObjectClass icfObjectClass;
    private final ObjectQuery query;
    private final Filter connIdFilter;
    @NotNull private final FetchedObjectHandler handler;
    private final AttributesToReturn attributesToReturn;
    private final PagedSearchCapabilityType pagedSearchConfiguration;
    private final SearchHierarchyConstraints searchHierarchyConstraints;
    private final UcfFetchErrorReportingMethod errorReportingMethod;
    private final StateReporter reporter;
    @NotNull private final OperationResult result;
    @NotNull private final ConnectorInstanceConnIdImpl connectorInstance;

    /**
     * Increases on each object fetched. Used for simulated paging and overall result construction.
     * We can assume we work in a single thread, but let's play it safe.
     */
    private final AtomicInteger objectsFetched = new AtomicInteger(0);

    SearchExecutor(@NotNull ObjectClassComplexTypeDefinition objectClassDefinition, ObjectQuery query,
            @NotNull FetchedObjectHandler handler, AttributesToReturn attributesToReturn,
            PagedSearchCapabilityType pagedSearchConfiguration, SearchHierarchyConstraints searchHierarchyConstraints,
            UcfFetchErrorReportingMethod errorReportingMethod, StateReporter reporter, @NotNull OperationResult result,
            @NotNull ConnectorInstanceConnIdImpl connectorInstance) throws SchemaException {

        this.objectClassDefinition = objectClassDefinition;
        this.objectDefinition = toShadowDefinition(objectClassDefinition);
        this.icfObjectClass = connectorInstance.objectClassToConnId(objectClassDefinition);
        this.query = query;
        this.connIdFilter = connectorInstance.convertFilterToIcf(query, objectClassDefinition);
        this.handler = handler;
        this.attributesToReturn = attributesToReturn;
        this.pagedSearchConfiguration = pagedSearchConfiguration;
        this.searchHierarchyConstraints = searchHierarchyConstraints;
        this.errorReportingMethod = errorReportingMethod;
        this.reporter = reporter;
        this.result = result;
        this.connectorInstance = connectorInstance;
    }

    public SearchResultMetadata execute() throws CommunicationException, ObjectNotFoundException, GenericFrameworkException,
            SchemaException, SecurityViolationException {

        if (isNoConnectorPaging() && query != null && query.getPaging() != null &&
                (query.getPaging().getOffset() != null || query.getPaging().getMaxSize() != null)) {
            InternalMonitor.recordCount(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);
        }

        OperationOptions connIdOptions = createOperationOptions();
        ResultsHandler connIdHandler = new SearchResultsHandler();

        SearchResult connIdSearchResult = executeConnIdSearch(connIdOptions, connIdHandler);

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
        connectorInstance.convertToIcfAttrsToGet(objectClassDefinition, attributesToReturn, optionsBuilder);
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
        ItemPath orderByPath = paging.getOrderBy();
        String desc;
        if (ItemPath.isNotEmpty(orderByPath)) {
            orderByAttributeName = ShadowUtil.getAttributeName(orderByPath, "OrderBy path");
            if (SchemaConstants.C_NAME.equals(orderByAttributeName)) {
                orderByAttributeName = SchemaConstants.ICFS_NAME; // What a hack...
            }
            isAscending = paging.getDirection() != OrderDirection.DESCENDING;
            desc = "(explicitly specified orderBy attribute)";
        } else {
            orderByAttributeName = pagedSearchConfiguration.getDefaultSortField();
            isAscending = pagedSearchConfiguration.getDefaultSortDirection() != OrderDirectionType.DESCENDING;
            desc = "(default orderBy attribute from capability definition)";
        }
        if (orderByAttributeName != null) {
            String orderByIcfName = connectorInstance.connIdNameMapper.convertAttributeNameToConnId(orderByAttributeName,
                    objectClassDefinition, desc);
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
                if (secondaryIdentifier == null) {
                    throw new SchemaException("No secondary identifier in base context identification " + baseContextIdentification);
                }
                String secondaryIdentifierValue = secondaryIdentifier.getRealValue(String.class);
                ObjectClass baseContextIcfObjectClass = connectorInstance.objectClassToConnId(baseContextIdentification.getObjectClassDefinition());
                QualifiedUid containerQualifiedUid = new QualifiedUid(baseContextIcfObjectClass, new Uid(secondaryIdentifierValue));
                optionsBuilder.setContainer(containerQualifiedUid);
            }
            SearchHierarchyScope scope = searchHierarchyConstraints.getScope();
            if (scope != null) {
                optionsBuilder.setScope(scope.getScopeString());
            }
        }
    }

    private SearchResult executeConnIdSearch(OperationOptions connIdOptions, ResultsHandler connIdHandler)
            throws CommunicationException, ObjectNotFoundException, GenericFrameworkException, SchemaException,
            SecurityViolationException {

        // Connector operation cannot create result for itself, so we need to create result for it
        OperationResult icfOpResult = result.createSubresult(ConnectorFacade.class.getName() + ".search");
        icfOpResult.addArbitraryObjectAsParam("objectClass", icfObjectClass);

        SearchResult connIdSearchResult;
        try {

            InternalMonitor.recordConnectorOperation("search");
            recordIcfOperationStart();
            connIdSearchResult = connectorInstance.getConnIdConnectorFacade()
                    .search(icfObjectClass, connIdFilter, connIdHandler, connIdOptions);
            recordIcfOperationEnd(null);

            icfOpResult.recordSuccess();
        } catch (IntermediateException inEx) {
            Throwable ex = inEx.getCause();
            recordIcfOperationEnd(ex);
            icfOpResult.recordFatalError(ex);
            throwProperException(ex, ex);
            throw new AssertionError("should not get here");
        } catch (Throwable ex) {
            recordIcfOperationEnd(ex);
            Throwable midpointEx = processConnIdException(ex, connectorInstance, icfOpResult);
            throwProperException(midpointEx, ex);
            throw new AssertionError("should not get here");
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

    private void recordIcfOperationStart() {
        connectorInstance.recordIcfOperationStart(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition);
    }

    private void recordIcfOperationEnd(Throwable ex) {
        connectorInstance.recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition, ex);
    }

    private void recordIcfOperationResume() {
        connectorInstance.recordIcfOperationResume(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition);
    }

    private void recordIcfOperationSuspend() {
        connectorInstance.recordIcfOperationSuspend(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition);
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

        @Override
        public boolean handle(ConnectorObject connectorObject) {
            Validate.notNull(connectorObject, "null connector object"); // todo apply error reporting method?

            recordIcfOperationSuspend();
            try {
                int number = objectsFetched.getAndIncrement(); // The numbering starts at 0
                if (isNoConnectorPaging()) {
                    if (query != null && query.getPaging() != null) {
                        int offset = ObjectUtils.defaultIfNull(query.getPaging().getOffset(), 0);
                        Integer maxSize = query.getPaging().getMaxSize();
                        if (number < offset) {
                            return true;
                        }
                        if (maxSize != null && number >= offset + maxSize) {
                            return false;
                        }
                    }
                }

                FetchedUcfObject ucfObject = connectorInstance.connIdConvertor.convertToUcfObject(
                        connectorObject, objectDefinition, false, connectorInstance.isCaseIgnoreAttributeNames(),
                        connectorInstance.isLegacySchema(), errorReportingMethod, result);

                return handler.handle(ucfObject);

            } catch (SchemaException e) {
                throw new IntermediateException(e);
            } finally {
                recordIcfOperationResume();
            }
        }

        @Override
        public String toString() {
            return "(midPoint searching result handler)";
        }
    }
}

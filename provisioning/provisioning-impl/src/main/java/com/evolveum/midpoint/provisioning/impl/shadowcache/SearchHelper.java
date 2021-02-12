package com.evolveum.midpoint.provisioning.impl.shadowcache;

import static com.evolveum.midpoint.schema.GetOperationOptions.getErrorReportingMethod;
import static com.evolveum.midpoint.schema.GetOperationOptions.isMaxStaleness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.FetchedResourceObject;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectHandler;
import com.evolveum.midpoint.provisioning.impl.shadowmanager.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsSimulateType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

/**
 * Helps with the `search` and `count` operations.
 */
@Experimental
@Component
class SearchHelper {

    private static final Trace LOGGER = TraceManager.getTrace(SearchHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired private SchemaHelper schemaHelper;
    @Autowired private ResourceObjectConverter resourceObjectConverter;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired protected ShadowManager shadowManager;
    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private DefinitionsHelper definitionsHelper;
    @Autowired private LocalBeans localBeans;

    public SearchResultMetadata searchObjectsIterative(ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<ShadowType> handler,
            boolean updateRepository, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ProvisioningContext ctx = createContextForSearch(query, options, task, parentResult);
        return searchObjectsIterative(ctx, query, options, handler, updateRepository, parentResult);
    }

    private ProvisioningContext createContextForSearch(ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ResourceShadowDiscriminator coordinates = ObjectQueryUtil.getCoordinates(query != null ? query.getFilter() : null,
                prismContext);
        final ProvisioningContext ctx = ctxFactory.create(coordinates, task, parentResult);
        ctx.setGetOperationOptions(options);
        ctx.assertDefinition();
        return ctx;
    }

    @NotNull
    public SearchResultList<PrismObject<ShadowType>> searchObjects(ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ProvisioningContext ctx = createContextForSearch(query, options, task, parentResult);
        return searchObjects(ctx, query, options, parentResult);
    }

    public SearchResultMetadata searchObjectsIterative(ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<ShadowType> handler,
            boolean updateRepository, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        definitionsHelper.applyDefinition(ctx, query);

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        if (ProvisioningUtil.shouldDoRepoSearch(rootOptions)) {
            return searchShadowsInRepositoryIteratively(ctx, query, options, handler, parentResult);
        } else {
            return searchObjectIterativeResource(ctx, query, options, handler, updateRepository, parentResult, rootOptions);
        }
    }

    private SearchResultMetadata searchObjectIterativeResource(ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<ShadowType> handler,
            boolean updateRepository, OperationResult parentResult, GetOperationOptions rootOptions)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException {

        FetchErrorReportingMethodType ucfErrorReportingMethod = getErrorReportingMethod(rootOptions);

        // We need to record the fetch down here. Now it is certain that we are
        // going to fetch from resource
        // (we do not have raw/noFetch option)
        InternalMonitor.recordCount(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        ResourceObjectHandler resultHandler = (FetchedResourceObject fetchedObject, OperationResult objResult) -> {

            AdoptedResourceObject adopted = new AdoptedResourceObject(fetchedObject, localBeans, ctx, updateRepository);
            adopted.initialize(ctx.getTask(), objResult);
            PrismObject<ShadowType> resultShadow = adopted.getResultingObject(ucfErrorReportingMethod);

            try {
                return handler.handle(resultShadow, objResult);
            } catch (Throwable t) {
                objResult.recordFatalError(t);
                throw t;
            } finally {
                objResult.computeStatusIfUnknown();
            }
        };

        ObjectQuery attributeQuery = createAttributeQuery(query);
        boolean fetchAssociations = SelectorOptions.hasToLoadPath(ShadowType.F_ASSOCIATION, options);
        try {
            return resourceObjectConverter.searchResourceObjects(ctx, resultHandler, attributeQuery,
                    fetchAssociations, ucfErrorReportingMethod, parentResult);
        } catch (TunnelException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException) cause;
            } else if (cause instanceof SchemaException) {
                throw (SchemaException) cause;
            } else if (cause instanceof CommunicationException) {
                throw (CommunicationException) cause;
            } else if (cause instanceof ConfigurationException) {
                throw (ConfigurationException) cause;
            } else if (cause instanceof SecurityViolationException) {
                throw (SecurityViolationException) cause;
            } else if (cause instanceof ExpressionEvaluationException) {
                throw (ExpressionEvaluationException) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new SystemException(cause.getMessage(), cause);
            }
        }
    }

    private ObjectQuery createAttributeQuery(ObjectQuery query) throws SchemaException {
        QueryFactory queryFactory = prismContext.queryFactory();

        ObjectFilter filter = null;
        if (query != null) {
            filter = query.getFilter();
        }

        ObjectQuery attributeQuery = null;

        if (filter instanceof AndFilter) {
            List<? extends ObjectFilter> conditions = ((AndFilter) filter).getConditions();
            List<ObjectFilter> attributeFilter = createAttributeQueryInternal(conditions);
            if (attributeFilter.size() > 1) {
                attributeQuery = queryFactory.createQuery(queryFactory.createAnd(attributeFilter));
            } else if (attributeFilter.size() < 1) {
                LOGGER.trace("No attribute filter defined in the query.");
            } else {
                attributeQuery = queryFactory.createQuery(attributeFilter.iterator().next());
            }
        }

        if (query != null && query.getPaging() != null) {
            if (attributeQuery == null) {
                attributeQuery = queryFactory.createQuery();
            }
            attributeQuery.setPaging(query.getPaging());
        }
        if (query != null && query.isAllowPartialResults()) {
            if (attributeQuery == null) {
                attributeQuery = queryFactory.createQuery();
            }
            attributeQuery.setAllowPartialResults(true);
        }

        if (InternalsConfig.consistencyChecks && attributeQuery != null
                && attributeQuery.getFilter() != null) {
            attributeQuery.getFilter().checkConsistence(true);
        }
        return attributeQuery;
    }

    private List<ObjectFilter> createAttributeQueryInternal(List<? extends ObjectFilter> conditions)
            throws SchemaException {
        List<ObjectFilter> attributeFilter = new ArrayList<>();
        for (ObjectFilter f : conditions) {
            if (f instanceof PropertyValueFilter) { // TODO
                ItemPath parentPath = ((PropertyValueFilter) f).getParentPath();
                if (parentPath.isEmpty()) {
                    QName elementName = ((PropertyValueFilter) f).getElementName();
                    if (QNameUtil.match(ShadowType.F_OBJECT_CLASS, elementName) ||
                            QNameUtil.match(ShadowType.F_AUXILIARY_OBJECT_CLASS, elementName) ||
                            QNameUtil.match(ShadowType.F_KIND, elementName) ||
                            QNameUtil.match(ShadowType.F_INTENT, elementName)) {
                        continue;
                    }
                    throw new SchemaException("Cannot combine on-resource and off-resource properties in a shadow search query. Encountered property " +
                            ((PropertyValueFilter) f).getFullPath());
                }
                attributeFilter.add(f);
            } else if (f instanceof NaryLogicalFilter) {
                List<ObjectFilter> subFilters = createAttributeQueryInternal(
                        ((NaryLogicalFilter) f).getConditions());
                if (subFilters.size() > 1) {
                    if (f instanceof OrFilter) {
                        attributeFilter.add(prismContext.queryFactory().createOr(subFilters));
                    } else if (f instanceof AndFilter) {
                        attributeFilter.add(prismContext.queryFactory().createAnd(subFilters));
                    } else {
                        throw new IllegalArgumentException(
                                "Could not translate query filter. Unknown type: " + f);
                    }
                } else if (subFilters.size() < 1) {
                    continue;
                } else {
                    attributeFilter.add(subFilters.iterator().next());
                }
            } else if (f instanceof UnaryLogicalFilter) {
                ObjectFilter subFilter = ((UnaryLogicalFilter) f).getFilter();
                attributeFilter.add(prismContext.queryFactory().createNot(subFilter));
            } else if (f instanceof SubstringFilter) { // TODO fix
                attributeFilter.add(f);
            } else if (f instanceof RefFilter) {
                ItemPath parentPath = ((RefFilter)f).getParentPath();
                if (parentPath.isEmpty()) {
                    QName elementName = ((RefFilter) f).getElementName();
                    if (QNameUtil.match(ShadowType.F_RESOURCE_REF, elementName)) {
                        continue;
                    }
                }
                throw new SchemaException("Cannot combine on-resource and off-resource properties in a shadow search query. Encountered filter " + f);
            } else {
                throw new SchemaException("Cannot combine on-resource and off-resource properties in a shadow search query. Encountered filter " + f);
            }

        }

        return attributeFilter;
    }

    @NotNull
    public SearchResultList<PrismObject<ShadowType>> searchObjects(final ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, final OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        definitionsHelper.applyDefinition(ctx, query);

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        if (ProvisioningUtil.shouldDoRepoSearch(rootOptions)) {
            return searchShadowsInRepository(ctx, query, options, parentResult);
        } else {
            SearchResultList<PrismObject<ShadowType>> rv = new SearchResultList<>();
            SearchResultMetadata metadata = searchObjectsIterative(ctx, query, options, (s, opResult) -> rv.add(s), true,
                    parentResult);
            rv.setMetadata(metadata);
            return rv;
        }
    }

    public Integer countObjects(ObjectQuery query, Task task, final OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        ObjectFilter filter = query != null ? query.getFilter() : null;
        ResourceShadowDiscriminator coordinates = ObjectQueryUtil.getCoordinates(filter, prismContext);
        assert query != null; // otherwise coordinates couldn't be found

        ProvisioningContext ctx = ctxFactory.create(coordinates, null, result);
        ctx.assertDefinition();
        definitionsHelper.applyDefinition(ctx, query);

        RefinedObjectClassDefinition objectClassDef = ctx.getObjectClassDefinition();
        ResourceType resourceType = ctx.getResource();
        CountObjectsCapabilityType countObjectsCapabilityType = objectClassDef
                .getEffectiveCapability(CountObjectsCapabilityType.class, resourceType);
        if (countObjectsCapabilityType == null) {
            // Unable to count. Return null which means "I do not know"
            LOGGER.trace("countObjects: cannot count (no counting capability)");
            result.recordNotApplicableIfUnknown();
            return null;
        } else {
            CountObjectsSimulateType simulate = countObjectsCapabilityType.getSimulate();
            if (simulate == null) {
                // We have native capability

                LOGGER.trace("countObjects: counting with native count capability");
                ConnectorInstance connector = ctx.getConnector(ReadCapabilityType.class, result);
                try {
                    ObjectQuery attributeQuery = createAttributeQuery(query);
                    int count;
                    try {
                        count = connector.count(objectClassDef.getObjectClassDefinition(), attributeQuery,
                                objectClassDef.getPagedSearches(resourceType), ctx, result);
                    } catch (CommunicationException | GenericFrameworkException | SchemaException
                            | UnsupportedOperationException e) {
                        result.recordFatalError(e);
                        throw e;
                    }
                    result.computeStatus();
                    result.cleanupResult();
                    return count;
                } catch (GenericFrameworkException | UnsupportedOperationException e) {
                    SystemException ex = new SystemException(
                            "Couldn't count objects on resource " + resourceType + ": " + e.getMessage(), e);
                    result.recordFatalError(ex);
                    throw ex;
                }

            } else if (simulate == CountObjectsSimulateType.PAGED_SEARCH_ESTIMATE) {

                LOGGER.trace("countObjects: simulating counting with paged search estimate");
                if (!objectClassDef.isPagedSearchEnabled(resourceType)) {
                    throw new ConfigurationException(
                            "Configured count object capability to be simulated using a paged search but paged search capability is not present");
                }

                final Holder<Integer> countHolder = new Holder<>(0);

                final ResultHandler<ShadowType> handler = new ResultHandler<>() {
                    @Override
                    public boolean handle(PrismObject<ShadowType> shadow, OperationResult objResult) {
                        countHolder.setValue(countHolder.getValue() + 1);
                        return true;
                    }

                    @Override
                    public String toString() {
                        return "(ShadowCache simulated counting handler)";
                    }
                };

                query = query.clone();
                ObjectPaging paging = prismContext.queryFactory().createPaging();
                // Explicitly set offset. This makes a difference for some resources.
                // E.g. LDAP connector will detect presence of an offset and it will initiate VLV search which
                // can estimate number of results. If no offset is specified then continuous/linear search is
                // assumed (e.g. Simple Paged Results search). Such search does not have ability to estimate
                // number of results.
                paging.setOffset(0);
                paging.setMaxSize(1);
                query.setPaging(paging);
                Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
                        .item(ShadowType.F_ASSOCIATION).dontRetrieve()
                        .build();
                SearchResultMetadata resultMetadata;
                try {
                    resultMetadata = searchObjectsIterative(query, options, handler, false, task, result);
                } catch (SchemaException | ObjectNotFoundException | ConfigurationException
                        | SecurityViolationException e) {
                    result.recordFatalError(e);
                    throw e;
                }
                result.computeStatus();
                result.cleanupResult();

                return resultMetadata.getApproxNumberOfAllResults();

            } else if (simulate == CountObjectsSimulateType.SEQUENTIAL_SEARCH) {
                //fix for MID-5204. as sequentialSearch option causes to fetch all resource objects,
                // query paging is senseless here
                query = query.clone();
                query.setPaging(null);
                LOGGER.trace("countObjects: simulating counting with sequential search (likely performance impact)");
                // traditional way of counting objects (i.e. counting them one by one)
                final Holder<Integer> countHolder = new Holder<>(0);

                final ResultHandler<ShadowType> handler = (shadow, objResult) -> {
                    countHolder.setValue(countHolder.getValue() + 1);
                    return true;
                };

                Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
                        .item(ShadowType.F_ASSOCIATION).dontRetrieve()
                        .build();

                searchObjectsIterative(query, options, handler, false, task, result);
                // TODO: better error handling
                result.computeStatus();
                result.cleanupResult();
                return countHolder.getValue();

            } else {
                throw new IllegalArgumentException("Unknown count capability simulate type " + simulate);
            }
        }
    }

    private SearchResultMetadata searchShadowsInRepositoryIteratively(final ProvisioningContext ctx,
            ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            final ResultHandler<ShadowType> shadowHandler, OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException {
        ResultHandler<ShadowType> repoHandler = createRepoShadowHandler(ctx, options, shadowHandler);
        return shadowManager.searchShadowsIterative(ctx, query, options, repoHandler, parentResult);
    }

    @NotNull
    private ResultHandler<ShadowType> createRepoShadowHandler(ProvisioningContext ctx,
            Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<ShadowType> shadowHandler) {
        return (PrismObject<ShadowType> shadow, OperationResult objResult) -> {
            try {
                processRepoShadow(ctx, shadow, options, objResult);

                boolean cont = shadowHandler == null || shadowHandler.handle(shadow, objResult);

                objResult.computeStatus();
                objResult.recordSuccessIfUnknown();
                if (!objResult.isSuccess()) {
                    OperationResultType resultType = objResult.createOperationResultType();
                    shadow.asObjectable().setFetchResult(resultType);
                }

                return cont;
            } catch (RuntimeException e) {
                objResult.recordFatalError(e);
                throw e;
            } catch (SchemaException | ConfigurationException | ObjectNotFoundException | CommunicationException |
                    ExpressionEvaluationException | SecurityViolationException e) {
                objResult.recordFatalError(e);
                shadow.asObjectable().setFetchResult(objResult.createOperationResultType());
                throw new SystemException(e);
            }
        };
    }

    @NotNull
    private SearchResultList<PrismObject<ShadowType>> searchShadowsInRepository(ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException {
        SearchResultList<PrismObject<ShadowType>> objects = shadowManager.searchShadows(ctx, query, options, parentResult);
        ResultHandler<ShadowType> repoHandler = createRepoShadowHandler(ctx, options, null);
        parentResult.setSummarizeSuccesses(true);
        for (PrismObject<ShadowType> object : objects) {
            repoHandler.handle(object, parentResult.createMinorSubresult(ShadowCache.class.getName() + ".handleObject"));
        }
        parentResult.summarize(); // todo is this ok?
        return objects;
    }

    private void processRepoShadow(ProvisioningContext ctx, PrismObject<ShadowType> shadow,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult objResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, SecurityViolationException {
        shadowCaretaker.applyAttributesDefinition(ctx, shadow);
        // fixing MID-1640; hoping that the protected object filter uses only identifiers
        // (that are stored in repo)
        ProvisioningUtil.setProtectedFlag(ctx, shadow, matchingRuleRegistry, relationRegistry, expressionFactory, objResult);

        ProvisioningUtil.validateShadow(shadow, true);

        if (isMaxStaleness(SelectorOptions.findRootOptions(options))) {
            CachingMetadataType cachingMetadata = shadow.asObjectable().getCachingMetadata();
            if (cachingMetadata == null) {
                objResult.recordFatalError("Requested cached data but no cached data are available in the shadow");
            }
        }
    }

}

/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.schema.GetOperationOptions.getErrorReportingMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectFound;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectHandler;
import com.evolveum.midpoint.provisioning.util.DefinitionsUtil;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Implements `search` and `count` operations.
 */
class ShadowSearchLikeOperation {

    private static final String OP_PROCESS_REPO_SHADOW = ShadowSearchLikeOperation.class.getName() + ".processRepoShadow";

    private static final Trace LOGGER = TraceManager.getTrace(ShadowSearchLikeOperation.class);

    @NotNull private final ProvisioningContext ctx;
    @Nullable private final ObjectQuery query;

    /** The "readOnly" is never set. This is because we need to modify the shadow during post-processing. */
    @Nullable private final Collection<SelectorOptions<GetOperationOptions>> options;

    @Nullable private final GetOperationOptions rootOptions;
    @NotNull private final ShadowsLocalBeans localBeans;

    private ShadowSearchLikeOperation(
            @NotNull ProvisioningContext ctx,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ShadowsLocalBeans localBeans) throws SchemaException {
        this.ctx = ctx;
        this.query = query;
        DefinitionsUtil.applyDefinition(ctx, query);
        this.options = GetOperationOptions.updateToReadWrite(options);
        this.rootOptions = SelectorOptions.findRootOptions(this.options);
        this.localBeans = localBeans;
    }

    static ShadowSearchLikeOperation create(
            ProvisioningContext ctx,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            ShadowsLocalBeans localBeans)
            throws SchemaException, ExpressionEvaluationException, ConfigurationException, ObjectNotFoundException {
        return new ShadowSearchLikeOperation(ctx, query, options, localBeans);
    }

    static ShadowSearchLikeOperation create(
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            ProvisioningOperationContext context,
            Task task,
            OperationResult result,
            ShadowsLocalBeans localBeans)
            throws SchemaException, ExpressionEvaluationException, ConfigurationException, ObjectNotFoundException {
        return new ShadowSearchLikeOperation(
                createContext(query, options, context, task, localBeans, result),
                query,
                options,
                localBeans);
    }

    private static ProvisioningContext createContext(
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            ProvisioningOperationContext context,
            Task task,
            ShadowsLocalBeans localBeans,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, ExpressionEvaluationException {
        ResourceOperationCoordinates operationCoordinates = ObjectQueryUtil.getOperationCoordinates(query);
        operationCoordinates.checkNotUnknown();
        operationCoordinates.checkNotResourceScoped();
        ProvisioningContext ctx = localBeans.ctxFactory.createForBulkOperation(operationCoordinates, context, task, result);
        ctx.setGetOperationOptions(options);
        ctx.assertDefinition();
        return ctx;
    }

    SearchResultMetadata executeIterativeSearch(ResultHandler<ShadowType> handler, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (shouldDoRepoSearch()) {
            return executeIterativeSearchInRepository(handler, result);
        } else {
            return executeIterativeSearchOnResource(handler, result);
        }
    }

    SearchResultList<PrismObject<ShadowType>> executeNonIterativeSearch(OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (shouldDoRepoSearch()) {
            return executeNonIterativeSearchInRepository(result);
        } else {
            // The only way of searching on repository is the iterative search
            SearchResultList<PrismObject<ShadowType>> objects = new SearchResultList<>();
            SearchResultMetadata metadata = executeIterativeSearchOnResource((s, lResult) -> objects.add(s), result);
            objects.setMetadata(metadata);
            return objects;
        }
    }

    Integer executeCount(OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        if (shouldDoRepoSearch()) {
            return localBeans.shadowFinder.countShadows(ctx, query, options, result);
        } else {
            // We record the fetch operation even if it's possible that it is not supported.
            InternalMonitor.recordCount(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
            return localBeans.resourceObjectConverter.countResourceObjects(ctx, createOnResourceQuery(), result);
        }
    }

    private SearchResultMetadata executeIterativeSearchOnResource(ResultHandler<ShadowType> handler, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException {

        FetchErrorReportingMethodType ucfErrorReportingMethod = getErrorReportingMethod(rootOptions);

        // We need to record the fetch down here. Now it is certain that we are going to fetch from resource.
        InternalMonitor.recordCount(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        ResourceObjectHandler shadowHandler = (ResourceObjectFound objectFound, OperationResult objParentResult) -> {

            // See ResultHandler#providingOwnOperationResult
            var objResult = objParentResult
                    .subresult(ShadowsFacade.OP_HANDLE_RESOURCE_OBJECT_FOUND)
                    .addArbitraryObjectAsParam(OperationResult.PARAM_OBJECT, objectFound)
                    .setMinor()
                    .build();
            try {
                ShadowedObjectFound shadowedObjectFound = new ShadowedObjectFound(objectFound, localBeans, ctx);
                shadowedObjectFound.initialize(ctx.getTask(), objResult);
                ShadowType shadowedObject = shadowedObjectFound.getResultingObject(ucfErrorReportingMethod);

                return handler.handle(shadowedObject.asPrismObject(), objResult);
            } catch (Throwable t) {
                objResult.recordException(t);
                throw t;
            } finally {
                objResult.close();
                objResult.deleteSubresultsIfPossible();
                objParentResult.summarize();
            }
        };

        boolean fetchAssociations = SelectorOptions.hasToIncludePath(ShadowType.F_ASSOCIATION, options, true);
        try {
            return localBeans.resourceObjectConverter.searchResourceObjects(
                    ctx, shadowHandler, createOnResourceQuery(), fetchAssociations, ucfErrorReportingMethod, parentResult);
        } catch (TunnelException e) {
            unwrapAndThrowSearchingTunnelException(e);
            throw new AssertionError();
        }
    }

    private ObjectQuery createOnResourceQuery() throws SchemaException {
        if (query == null) {
            return null;
        }

        ObjectFilter onResourceFilter = createOnResourceFilter(query.getFilter());
        ObjectPaging paging = query.getPaging();
        boolean allowPartialResults = query.isAllowPartialResults();

        if (onResourceFilter == null && paging == null && !allowPartialResults) {
            return null;
        } else {
            ObjectQuery onResourceQuery = PrismContext.get().queryFactory().createQuery();
            onResourceQuery.setFilter(onResourceFilter);
            onResourceQuery.setPaging(paging);
            onResourceQuery.setAllowPartialResults(allowPartialResults);
            return onResourceQuery;
        }
    }

    private ObjectFilter createOnResourceFilter(ObjectFilter filter) throws SchemaException {
        if (filter == null) {
            return null;
        }
        List<ObjectFilter> onResourceConjuncts = new ArrayList<>();
        for (ObjectFilter conjunct : getAllConjuncts(filter)) {
            if (isEvaluatedOnResource(conjunct)) {
                onResourceConjuncts.add(conjunct);
            } else if (wasAlreadyProcessed(conjunct)) {
                // OK
            } else {
                throw new SchemaException(
                        String.format("Cannot combine on-resource and off-resource properties in a shadow search query. "
                                + "Encountered filter '%s'", filter));
            }
        }
        ObjectFilter onResourceFilter;
        if (onResourceConjuncts.size() > 1) {
            onResourceFilter = PrismContext.get().queryFactory().createAnd(onResourceConjuncts);
        } else if (onResourceConjuncts.size() == 1) {
            onResourceFilter = onResourceConjuncts.get(0);
        } else {
            LOGGER.trace("No 'on-resource' filter defined in the query.");
            onResourceFilter = null;
        }
        if (InternalsConfig.consistencyChecks && onResourceFilter != null) {
            onResourceFilter.checkConsistence(true);
        }
        return onResourceFilter;
    }

    /** Flattens "AND" structure to primitive conjuncts. */
    private List<ObjectFilter> getAllConjuncts(@NotNull ObjectFilter filter) {
        if (filter instanceof AndFilter) {
            List<ObjectFilter> conjuncts = new ArrayList<>();
            for (ObjectFilter condition : ((AndFilter) filter).getConditions()) {
                conjuncts.addAll(getAllConjuncts(condition));
            }
            return conjuncts;
        } else {
            return List.of(filter);
        }
    }

    private boolean isEvaluatedOnResource(ObjectFilter filter) {
        if (filter instanceof PropertyValueFilter) {
            ItemPath path = ((PropertyValueFilter<?>) filter).getPath();
            return path.startsWith(ShadowType.F_ATTRIBUTES)
                    || path.startsWith(ShadowType.F_ACTIVATION); // TODO but not all of these! (this is approx how it was before 4.7)
        } else if (filter instanceof LogicalFilter) {
            return ((LogicalFilter) filter).getConditions().stream()
                    .allMatch(this::isEvaluatedOnResource);
        } else {
            return false;
        }
    }

    /** Returns true if this filter can be safely ignored, as it was already processed. */
    private boolean wasAlreadyProcessed(ObjectFilter filter) {
        if (filter instanceof PropertyValueFilter) {
            ItemPath path = ((PropertyValueFilter<?>) filter).getPath();
            return path.equivalent(ShadowType.F_OBJECT_CLASS)
                    || path.equivalent(ShadowType.F_AUXILIARY_OBJECT_CLASS) // TODO also this one?
                    || path.equivalent(ShadowType.F_KIND)
                    || path.equivalent(ShadowType.F_INTENT);
        } else if (filter instanceof RefFilter) {
            ItemPath path = ((RefFilter) filter).getPath();
            return path.equivalent(ShadowType.F_RESOURCE_REF);
        } else {
            return false;
        }
    }

    private SearchResultMetadata executeIterativeSearchInRepository(
            ResultHandler<ShadowType> upstreamHandler, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        try {
            return localBeans.shadowFinder.searchShadowsIterative(
                    ctx,
                    query,
                    options,
                    createRepoShadowHandler(upstreamHandler),
                    result);
        } catch (TunnelException e) {
            unwrapAndThrowSearchingTunnelException(e);
            throw new AssertionError();
        }
    }

    private @NotNull SearchResultList<PrismObject<ShadowType>> executeNonIterativeSearchInRepository(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        SearchResultList<PrismObject<ShadowType>> shadows = localBeans.shadowFinder.searchShadows(ctx, query, options, result);

        ResultHandler<ShadowType> repoShadowHandler = createRepoShadowHandler(null);
        for (PrismObject<ShadowType> shadow : shadows) {
            try {
                repoShadowHandler.handle(shadow, result);
            } catch (TunnelException e) {
                unwrapAndThrowSearchingTunnelException(e);
                throw new AssertionError();
            }
        }
        return shadows;
    }

    /**
     * Provides common processing for shadows found in repo during iterative and non-iterative searches.
     * Analogous to {@link ShadowGetOperation#returnCached(String)}.
     */
    private ResultHandler<ShadowType> createRepoShadowHandler(ResultHandler<ShadowType> upstreamHandler) {
        return (PrismObject<ShadowType> shadow, OperationResult result) -> {
            OperationResult lResult = result.createMinorSubresult(OP_PROCESS_REPO_SHADOW);
            boolean cont;
            try {
                processRepoShadow(shadow, lResult);
                cont = upstreamHandler == null || upstreamHandler.handle(shadow, lResult);
            } catch (CommonException e) {
                lResult.recordException(e);
                throw SystemException.unexpected(e); // TODO shouldn't we honor FetchErrorHandlingType here?
            } catch (Throwable t) {
                lResult.recordException(t);
                throw t; // TODO shouldn't we honor FetchErrorHandlingType here?
            } finally {
                lResult.close();
            }
            if (!lResult.isSuccess()) {
                shadow.asObjectable().setFetchResult(
                        lResult.createBeanReduced());
            }
            result.summarize();
            return cont;
        };
    }

    private void processRepoShadow(PrismObject<ShadowType> shadow, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, SecurityViolationException {

        ShadowType shadowBean = shadow.asObjectable();

        ctx.applyAttributesDefinition(shadowBean);
        if (isRaw()) {
            return;
        }

        ctx.updateShadowState(shadowBean);

        // Fixing MID-1640; hoping that the protected object filter uses only identifiers (that are stored in repo)
        // TODO we will eventually store the "protected" flag right in the repo shadow, so this code will be obsolete
        ProvisioningUtil.setEffectiveProvisioningPolicy(ctx, shadowBean, localBeans.expressionFactory, result);

        ProvisioningUtil.validateShadow(shadow, true);

        if (isMaxStaleness()) {
            if (shadowBean.getCachingMetadata() == null) {
                result.recordFatalError("Requested cached data but no cached data are available in the shadow");
            }
        }
    }

    private void unwrapAndThrowSearchingTunnelException(TunnelException e) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
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

    private boolean shouldDoRepoSearch() {
        return isRaw() || isNoFetch() || isMaxStaleness();
    }

    private boolean isRaw() {
        return GetOperationOptions.isRaw(rootOptions);
    }

    private boolean isNoFetch() {
        return GetOperationOptions.isNoFetch(rootOptions);
    }

    private boolean isMaxStaleness() {
        return GetOperationOptions.isMaxStaleness(rootOptions);
    }
}

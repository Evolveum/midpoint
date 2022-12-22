/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.cache;

import static com.evolveum.midpoint.repo.cache.other.MonitoringUtil.repoOpEnd;
import static com.evolveum.midpoint.repo.cache.other.MonitoringUtil.repoOpStart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.api.perf.PerformanceMonitor;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.repo.cache.global.GlobalObjectCache;
import com.evolveum.midpoint.repo.cache.global.GlobalQueryCache;
import com.evolveum.midpoint.repo.cache.global.GlobalVersionCache;
import com.evolveum.midpoint.repo.cache.handlers.GetObjectOpHandler;
import com.evolveum.midpoint.repo.cache.handlers.GetVersionOpHandler;
import com.evolveum.midpoint.repo.cache.handlers.ModificationOpHandler;
import com.evolveum.midpoint.repo.cache.handlers.SearchOpHandler;
import com.evolveum.midpoint.repo.cache.invalidation.Invalidator;
import com.evolveum.midpoint.repo.cache.local.LocalRepoCacheCollection;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Read-through write-through repository cache.
 * <p>
 * This is an umbrella class providing RepositoryService and {@link Cache} interfaces.
 * Majority of the work is delegated to operation handlers (and other classes).
 */
@Component(value = "cacheRepositoryService")
public class RepositoryCache implements RepositoryService, Cache {

    public static final String CLASS_NAME_WITH_DOT = RepositoryCache.class.getName() + ".";
    private static final String EXECUTE_QUERY_DIAGNOSTICS = CLASS_NAME_WITH_DOT + "executeQueryDiagnostics";

    @Autowired private RepositoryService repositoryService;
    @Autowired private CacheRegistry cacheRegistry;

    // individual caches
    @Autowired private GlobalQueryCache globalQueryCache;
    @Autowired private GlobalObjectCache globalObjectCache;
    @Autowired private GlobalVersionCache globalVersionCache;
    @Autowired private LocalRepoCacheCollection localRepoCacheCollection;

    // handlers
    @Autowired private GetObjectOpHandler getObjectOpHandler;
    @Autowired private ModificationOpHandler modificationOpHandler;
    @Autowired private SearchOpHandler searchOpHandler;
    @Autowired private GetVersionOpHandler getVersionOpHandler;

    // other
    @Autowired private Invalidator invalidator;

    public RepositoryCache() {
    }

    /**
     * Enters thread-local caches.
     */
    public static void enterLocalCaches(CacheConfigurationManager mgr) {
        LocalRepoCacheCollection.enter(mgr);
    }

    /**
     * Exits thread-local caches.
     */
    public static void exitLocalCaches() {
        LocalRepoCacheCollection.exit();
    }

    //region --- GET, SEARCH and COUNT operations ------------------------------------------------------------------

    @NotNull
    @Override
    public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        return getObjectOpHandler.getObject(type, oid, options, parentResult);
    }

    @Override
    public <T extends ObjectType> String getVersion(Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        return getVersionOpHandler.getVersion(type, oid, parentResult);
    }

    @NotNull
    @Override
    public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(@NotNull Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, @NotNull OperationResult parentResult) throws SchemaException {
        return searchOpHandler.searchObjects(type, query, options, parentResult);
    }

    @Override
    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query,
            ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options,
            boolean strictlySequential, OperationResult parentResult) throws SchemaException {
        return searchOpHandler.searchObjectsIterative(type, query, handler, options, strictlySequential, parentResult);
    }

    @Override
    public <T extends Containerable> SearchResultList<T> searchContainers(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
        return searchOpHandler.searchContainers(type, query, options, parentResult);
    }

    @Override
    public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        return searchOpHandler.countObjects(type, query, options, parentResult);
    }

    @Override
    public <T extends Containerable> int countContainers(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        return searchOpHandler.countContainers(type, query, options, parentResult);
    }
    //endregion

    //region --- ADD, MODIFY, DELETE and other modifications -------------------------------------------------------

    @NotNull
    @Override
    public <T extends ObjectType> String addObject(@NotNull PrismObject<T> object, RepoAddOptions options,
            @NotNull OperationResult parentResult)
            throws ObjectAlreadyExistsException, SchemaException {
        return modificationOpHandler.addObject(object, options, parentResult);
    }

    @NotNull
    public <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type, @NotNull String oid, @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        return modifyObject(type, oid, modifications, null, parentResult);
    }

    @NotNull
    @Override
    public <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type, @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            RepoModifyOptions options, @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        try {
            return modifyObject(type, oid, modifications, null, options, parentResult);
        } catch (PreconditionViolationException e) {
            throw new AssertionError(e);
        }
    }

    @NotNull
    @Override
    public <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type, @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            ModificationPrecondition<T> precondition,
            RepoModifyOptions options, @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException {
        return modificationOpHandler.modifyObject(type, oid, modifications, precondition, options, parentResult);
    }

    @Override
    public @NotNull <T extends ObjectType> ModifyObjectResult<T> modifyObjectDynamically(
            @NotNull Class<T> type,
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> getOptions,
            @NotNull ModificationsSupplier<T> modificationsSupplier,
            @Nullable RepoModifyOptions modifyOptions,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        // TODO implement properly, currently only to support tests, probably not used via cache in normal code
        return repositoryService.modifyObjectDynamically(
                type, oid, getOptions, modificationsSupplier, modifyOptions, parentResult);
    }

    @NotNull
    @Override
    public <T extends ObjectType> DeleteObjectResult deleteObject(Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException {
        return modificationOpHandler.deleteObject(type, oid, parentResult);
    }

    @Override
    public long advanceSequence(String oid, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException {
        return modificationOpHandler.advanceSequence(oid, parentResult);
    }

    @Override
    public void returnUnusedValuesToSequence(String oid, Collection<Long> unusedValues, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        modificationOpHandler.returnUnusedValuesToSequence(oid, unusedValues, parentResult);
    }

    @Override
    public <T extends ObjectType> void addDiagnosticInformation(Class<T> type, String oid, DiagnosticInformationType information,
            OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        modificationOpHandler.addDiagnosticInformation(type, oid, information, parentResult);
    }

    //endregion

    //region --- Other methods (delegated directly to repository service) ------------------------------------------

    @Override
    public boolean supports(@NotNull Class<? extends ObjectType> type) {
        return repositoryService.supports(type);
    }

    @Override
    public RepositoryDiag getRepositoryDiag() {
        Long startTime = repoOpStart();
        try {
            return repositoryService.getRepositoryDiag();
        } finally {
            repoOpEnd(startTime);
        }
    }

    @Override
    public @NotNull String getRepositoryType() {
        return repositoryService.getRepositoryType();
    }

    @Override
    public void repositorySelfTest(OperationResult parentResult) {
        Long startTime = repoOpStart();
        try {
            repositoryService.repositorySelfTest(parentResult);
        } finally {
            repoOpEnd(startTime);
        }
    }

    @Override
    public void testOrgClosureConsistency(boolean repairIfNecessary, OperationResult testResult) {
        Long startTime = repoOpStart();
        try {
            repositoryService.testOrgClosureConsistency(repairIfNecessary, testResult);
        } finally {
            repoOpEnd(startTime);
        }
    }

    @Override
    public <O extends ObjectType> boolean isDescendant(PrismObject<O> object, String ancestorOrgOid)
            throws SchemaException {
        Long startTime = repoOpStart();
        try {
            return repositoryService.isDescendant(object, ancestorOrgOid);
        } finally {
            repoOpEnd(startTime);
        }
    }

    @Override
    public <O extends ObjectType> boolean isAncestor(PrismObject<O> object, String descendantOrgOid)
            throws SchemaException {
        Long startTime = repoOpStart();
        try {
            return repositoryService.isAncestor(object, descendantOrgOid);
        } finally {
            repoOpEnd(startTime);
        }
    }

    @Override
    public <O extends ObjectType> boolean selectorMatches(ObjectSelectorType objectSelector,
            PrismObject<O> object, ObjectFilterExpressionEvaluator filterEvaluator, Trace logger, String logMessagePrefix)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        Long startTime = repoOpStart();
        try {
            return repositoryService.selectorMatches(objectSelector, object, filterEvaluator, logger, logMessagePrefix);
        } finally {
            repoOpEnd(startTime);
        }
    }

    @Override
    public RepositoryQueryDiagResponse executeQueryDiagnostics(RepositoryQueryDiagRequest request, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(EXECUTE_QUERY_DIAGNOSTICS)
                .build();
        try {
            Long startTime = repoOpStart();
            try {
                return repositoryService.executeQueryDiagnostics(request, result);
            } finally {
                repoOpEnd(startTime);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void applyFullTextSearchConfiguration(FullTextSearchConfigurationType fullTextSearch) {
        Long startTime = repoOpStart();
        try {
            repositoryService.applyFullTextSearchConfiguration(fullTextSearch);
        } finally {
            repoOpEnd(startTime);
        }
    }

    @Override
    public FullTextSearchConfigurationType getFullTextSearchConfiguration() {
        Long startTime = repoOpStart();
        try {
            return repositoryService.getFullTextSearchConfiguration();
        } finally {
            repoOpEnd(startTime);
        }
    }

    @Override
    public ConflictWatcher createAndRegisterConflictWatcher(@NotNull String oid) {
        return repositoryService.createAndRegisterConflictWatcher(oid);
    }

    @Override
    public void unregisterConflictWatcher(ConflictWatcher watcher) {
        repositoryService.unregisterConflictWatcher(watcher);
    }

    @Override
    public boolean hasConflict(ConflictWatcher watcher, OperationResult result) {
        return repositoryService.hasConflict(watcher, result);
    }

    @Override
    public PerformanceMonitor getPerformanceMonitor() {
        return repositoryService.getPerformanceMonitor();
    }
    //endregion

    @Override
    public void postInit(OperationResult result) throws SchemaException {
        repositoryService.postInit(result);     // TODO resolve somehow multiple calls to repositoryService postInit method
        globalObjectCache.initialize();
        globalVersionCache.initialize();
        globalQueryCache.initialize();
        cacheRegistry.registerCache(this);
    }

    @PreDestroy
    public void unregister() {
        cacheRegistry.unregisterCache(this);
    }

    //region Cacheable interface

    // This is what is called from cache dispatcher (on local node with the full context; on remote nodes with reduced context)
    @Override
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        invalidator.invalidate(type, oid, context);
    }

    @NotNull
    @Override
    public Collection<SingleCacheStateInformationType> getStateInformation() {
        List<SingleCacheStateInformationType> rv = new ArrayList<>();
        localRepoCacheCollection.getStateInformation(rv);
        rv.addAll(globalObjectCache.getStateInformation());
        rv.addAll(globalVersionCache.getStateInformation());
        rv.addAll(globalQueryCache.getStateInformation());
        return rv;
    }

    @Override
    public void dumpContent() {
        localRepoCacheCollection.dumpContent();
        globalObjectCache.dumpContent();
        globalVersionCache.dumpContent();
        globalQueryCache.dumpContent();
    }
    //endregion

    //region Instrumentation

    public void setModifyRandomDelayRange(Integer modifyRandomDelayRange) {
        modificationOpHandler.setModifyRandomDelayRange(modifyRandomDelayRange);
    }

    //endregion
}

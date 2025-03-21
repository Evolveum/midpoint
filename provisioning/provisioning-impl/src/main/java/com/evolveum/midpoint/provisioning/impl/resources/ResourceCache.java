/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.Cache;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.CacheRegistry;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.processor.ResourceSchemaRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import static com.evolveum.midpoint.util.MiscUtil.schemaCheck;
import static com.evolveum.midpoint.util.caching.CacheConfiguration.StatisticsLevel.PER_CACHE;

/**
 * Caches {@link ResourceType} instances with a parsed schemas.
 *
 * Resource cache is similar to repository cache. One of the differences is that it does not expire its entries.
 * It relies on versions and on invalidation events instead. So we have to use resource object versions when querying it.
 * (This could be perhaps changed in the future. But not now.)
 *
 * The cache deals with concrete resources, i.e. _not_ the abstract ones. So, when an abstract resource is invalidated,
 * all concrete ones that inherit from it should be invalidated as well.
 *
 * @author Radovan Semancik
 */
@Component
public class ResourceCache implements Cache {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceCache.class);
    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(ResourceCache.class.getName() + ".content");

    @Autowired private ResourceSchemaRegistry resourceSchemaRegistry;
    @Autowired private CacheRegistry cacheRegistry;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    @PostConstruct
    public void register() {
        cacheRegistry.registerCache(this);
    }

    @PreDestroy
    public void unregister() {
        cacheRegistry.unregisterCache(this);
    }

    /**
     * Note that prism objects in this map are always not null and immutable.
     * And they must remain immutable after getting them from the cache.
     *
     * As for ConcurrentHashMap: Although we use synchronization whenever possible, let's be extra cautious here.
     */
    private final Map<String, PrismObject<ResourceType>> cache = new ConcurrentHashMap<>();

    /**
     * `K -> V` means that (concrete) resource `V` depends on (concrete or abstract) resource `K`.
     */
    private final SetMultimap<String, String> dependencyMap = HashMultimap.create();

    /**
     * Puts a (complete) resource into the cache.
     *
     * @param resource The object to cache.
     * @param ancestorsOids OIDs of the resource ancestor(s), if any. Invalidation of any of these results in the invalidation
     * of the cached resource.
     */
    synchronized void put(
            @NotNull ResourceType resource,
            @NotNull Collection<String> ancestorsOids) throws SchemaException, ConfigurationException {
        String oid = resource.getOid();
        schemaCheck(oid != null, "Attempt to cache %s without an OID", resource);

        String version = resource.getVersion();
        schemaCheck(version != null, "Attempt to cache %s without version", resource);

        updateDependencies(oid, ancestorsOids);

        PrismObject<ResourceType> cachedResource = cache.get(oid);
        if (cachedResource == null) {
            LOGGER.debug("Caching(new): {}", resource);
            cache.put(oid, resource.asPrismObject().immutableCopy());
        } else if (compareVersion(resource.getVersion(), cachedResource.getVersion())) {
            LOGGER.debug("Caching fizzle, resource already cached: {}", resource);
            // We already have equivalent resource, nothing to do
            //  TODO is this correct? What if the resource being put here is newer than the existing one (although having the same version)?
        } else {
            LOGGER.debug("Caching(replace): {}", resource);
            cache.put(oid, resource.asPrismObject().immutableCopy());
            resourceSchemaRegistry.putSchema(oid, ResourceSchemaFactory.getCompleteSchema(resource));
        }
    }

    /**
     * Updates the {@link #dependencyMap} with the current information about ancestors of given (concrete) resource.
     *
     * Guarded by `this` (responsible of the caller).
     */
    private void updateDependencies(String concreteResourceOid, Collection<String> ancestorsOids) {
        // Removing no-longer-valid ancestor OIDs
        dependencyMap.entries().removeIf(
                entry ->
                        concreteResourceOid.equals(entry.getValue())
                                && !ancestorsOids.contains(entry.getKey()));

        // Re-adding (all) ancestor OIDs; some are already there, but ignoring it.
        ancestorsOids.forEach(
                ancestorOid -> dependencyMap.put(ancestorOid, concreteResourceOid));
    }

    private boolean compareVersion(String version1, String version2) {
        return version1 == null && version2 == null || version1 != null && version1.equals(version2);
    }

    /**
     * Gets a resource if it has specified version. If it has not, purges it from the cache (even if it exists there).
     */
    synchronized PrismObject<ResourceType> get(@NotNull String oid, String requestedVersion, boolean readOnly) {
        InternalMonitor.getResourceCacheStats().recordRequest();

        PrismObject<ResourceType> resourceToReturn;
        PrismObject<ResourceType> cachedResource = cache.get(oid);
        if (cachedResource == null) {
            LOGGER.debug("MISS(not cached) for {} (get)", oid);
            resourceToReturn = null;
        } else if (!compareVersion(requestedVersion, cachedResource.getVersion())) {
            LOGGER.debug("MISS(wrong version) for {} (req={}, actual={})", oid, requestedVersion, cachedResource.getVersion());
            LOGGER.trace("Cached resource version {} does not match requested resource version {}, purging from cache",
                    cachedResource.getVersion(), requestedVersion);
            invalidateSingle(oid);
            resourceToReturn = null;
        } else if (readOnly) {
            cachedResource.checkImmutable();
            LOGGER.trace("HIT(read only) for {} (v{})", cachedResource, cachedResource.getVersion());
            resourceToReturn = cachedResource;
        } else {
            LOGGER.debug("HIT(returning clone) for {} (v{})", cachedResource, cachedResource.getVersion());
            resourceToReturn = cachedResource.clone();
        }

        if (resourceToReturn != null) {
            CachePerformanceCollector.INSTANCE.registerHit(ResourceCache.class, ResourceType.class, PER_CACHE);
            InternalMonitor.getResourceCacheStats().recordHit();
        } else {
            CachePerformanceCollector.INSTANCE.registerMiss(ResourceCache.class, ResourceType.class, PER_CACHE);
            InternalMonitor.getResourceCacheStats().recordMiss();
        }
        return resourceToReturn;
    }

    /**
     * Gets a resource without specifying requested version: returns one only if it has the same version as in the repo.
     *
     * This requires a cooperation with the repository cache. Therefore this method is NOT synchronized
     * and has operation result as its parameter.
     */
    PrismObject<ResourceType> getIfLatest(@NotNull String oid, boolean readonly, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        // First let's check if the cache contains given resource. If not, we can avoid getting version from the repo.
        if (contains(oid)) {
            String version = repositoryService.getVersion(ResourceType.class, oid, parentResult);
            return get(oid, version, readonly);
        } else {
            LOGGER.debug("MISS(not cached) for {} (getIfLatest)", oid);
            CachePerformanceCollector.INSTANCE.registerMiss(ResourceCache.class, ResourceType.class, PER_CACHE);
            InternalMonitor.getResourceCacheStats().recordMiss();
            return null;
        }
    }

    private synchronized boolean contains(@NotNull String oid) {
        return cache.containsKey(oid);
    }

    /**
     * Returns currently cached version. FOR DIAGNOSTICS ONLY.
     */
    synchronized String getVersion(String oid) {
        if (oid == null) {
            return null;
        }
        PrismObject<ResourceType> cachedResource = cache.get(oid);
        if (cachedResource == null) {
            return null;
        }
        return cachedResource.getVersion();
    }

    @Override
    public synchronized void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        if (type == null || type.isAssignableFrom(ResourceType.class)) {
            if (oid != null) {
                invalidateSingle(oid);
            } else {
                invalidateAll();
            }
        }
    }

    /** Invalidates single (concrete) resource and all its descendants. */
    synchronized void invalidateSingle(@NotNull String oid) {
        Set<String> descendants = new HashSet<>(dependencyMap.get(oid));
        LOGGER.trace("Invalidating {} and all its descendants: {}", oid, descendants);

        invalidateSingleShallow(oid);
        descendants.forEach(this::invalidateSingle);
    }

    /**
     * Removes the specific resource from {@link #cache} and {@link #dependencyMap}). Not touching the descendants.
     * Must be guarded by `this` (caller's responsibility).
     */
    private void invalidateSingleShallow(@NotNull String oid) {
        cache.remove(oid);
        dependencyMap.removeAll(oid);
        dependencyMap.entries().removeIf(
                entry -> oid.equals(entry.getValue()));
    }

    /** Invalidates the whole cache. Must be guarded by `this` (caller's responsibility). */
    private void invalidateAll() {
        LOGGER.trace("Invalidating the whole cache");
        cache.clear();
        dependencyMap.clear();
    }

    @NotNull
    @Override
    public synchronized Collection<SingleCacheStateInformationType> getStateInformation() {
        return Collections.singleton(
                new SingleCacheStateInformationType()
                        .name(ResourceCache.class.getName())
                        .size(cache.size())
        );
    }

    @Override
    public void dumpContent() {
        if (LOGGER_CONTENT.isInfoEnabled()) {
            cache.forEach((oid, resource) -> LOGGER_CONTENT.info("Cached resource: {}: {} (version: {})",
                    oid, resource, resource.getVersion()));
        }
    }
}

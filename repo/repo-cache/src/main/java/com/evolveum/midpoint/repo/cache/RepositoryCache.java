/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.repo.cache;

import java.util.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.DiagnosticContextHolder;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Read-through write-through per-session repository cache.
 *
 * TODO doc
 * TODO logging perf measurements
 *
 * @author Radovan Semancik
 *
 */
@Component(value="cacheRepositoryService")
public class RepositoryCache implements RepositoryService {

	private static final Trace LOGGER = TraceManager.getTrace(RepositoryCache.class);
	private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

	private static final String PROPERTY_CACHE_MAX_TTL = "cacheMaxTTL";

	private static final Set<Class<? extends ObjectType>> GLOBAL_CACHE_SUPPORTED_TYPES;

	static {
		Set<Class<? extends ObjectType>> set = new HashSet<>();
		set.add(ConnectorType.class);
		set.add(ObjectTemplateType.class);
		set.add(SecurityPolicyType.class);
		set.add(SystemConfigurationType.class);
		set.add(ValuePolicyType.class);
		// enabled if needed
//		set.add(RoleType.class);
//		set.add(OrgType.class);
//		set.add(ServiceType.class);
//		set.add(ShadowType.class);

		GLOBAL_CACHE_SUPPORTED_TYPES = Collections.unmodifiableSet(set);
	}

	private static final ThreadLocal<Cache> cacheInstance = new ThreadLocal<>();

	@Autowired private RepositoryService repositoryService;
	@Autowired private PrismContext prismContext;
	@Autowired private MidpointConfiguration midpointConfiguration;
	@Autowired private CacheDispatcher cacheDispatcher;
	@Autowired private MatchingRuleRegistry matchingRuleRegistry;
	@Autowired private GlobalCache globalCache;

	private long cacheMaxTTL;

	private static final Random RND = new Random();

	private Integer modifyRandomDelayRange;

	public RepositoryCache() {
    }


	@PostConstruct
	public void initialize() {
		int cacheMaxTTL = midpointConfiguration.getConfiguration(MidpointConfiguration.REPOSITORY_CONFIGURATION)
				.getInt(PROPERTY_CACHE_MAX_TTL, 0);
		if (cacheMaxTTL < 0) {
			cacheMaxTTL = 0;
		}
		this.cacheMaxTTL = cacheMaxTTL * 1000;
	}

	private static Cache getCache() {
		return cacheInstance.get();
	}

	public static void init() {
	}

	public static void destroy() {
		Cache.destroy(cacheInstance, LOGGER);
	}

	public static void enter() {
		Cache.enter(cacheInstance, Cache.class, LOGGER);
	}

	public static void exit() {
		Cache.exit(cacheInstance, LOGGER);
	}

	public static boolean exists() {
		return Cache.exists(cacheInstance);
	}

	public Integer getModifyRandomDelayRange() {
		return modifyRandomDelayRange;
	}

	public void setModifyRandomDelayRange(Integer modifyRandomDelayRange) {
		this.modifyRandomDelayRange = modifyRandomDelayRange;
	}

	public static String debugDump() {
		return Cache.debugDump(cacheInstance);
	}

	@Override
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

		CachePerformanceCollector collector = CachePerformanceCollector.INSTANCE;

		/*
		 * Checks related to both caches
		 */

		Cache localCache = getCache();
		if (notCacheable(type) || harmfulOptions(options, type)) {
			// local nor global cache not interested in caching this object
			if (localCache != null) {
				localCache.recordPass();
			}
			collector.registerPass(GlobalCache.class);
			log("Cache (local/global): PASS getObject {} ({}, {})", oid, type.getSimpleName(), options);

			return getObjectInternal(type, oid, options, parentResult);
		}

		/*
		 * Let's try local cache
		 */
		boolean readOnly = GetOperationOptions.isReadOnly(SelectorOptions.findRootOptions(options));

		if (localCache == null) {
			log("Cache (local): NULL getObject {} ({})", oid, type.getSimpleName());
			registerNotAvailable();
		} else {
			//noinspection unchecked
			PrismObject<T> object = (PrismObject) localCache.getObject(oid);
			if (object != null) {
				localCache.recordHit();
				log("Cache (local): HIT {} getObject {} ({})", readOnly ? "" : "(clone)", oid, type.getSimpleName());
				return cloneIfNecessary(object, readOnly);
			}
			localCache.recordMiss();
			log("Cache (local): MISS {} getObject ({})", oid, type.getSimpleName());
			//LOGGER.info("Cache: MISS (getObject) {} ({}) #{}", oid, type.getSimpleName(), cache.getMisses());
		}

		/*
		 * Then try global cache
		 */

		if (!supportsGlobalCaching(type)) {
			// caller is not interested in cached value, or global cache doesn't want to cache value
			collector.registerPass(GlobalCache.class);
			PrismObject<T> object = getObjectInternal(type, oid, options, parentResult);
			locallyCacheObject(localCache, object, readOnly);
			return object;
		}

		GlobalCacheObjectKey key = new GlobalCacheObjectKey(type, oid);
		GlobalCacheObjectValue<T> cacheObject = globalCache.getObject(key);

		PrismObject<T> object;
		if (cacheObject == null) {
			collector.registerMiss(GlobalCache.class);
			log("Cache (global): MISS getObject {}", key);
			object = loadAndCacheObject(key, options, readOnly, localCache, parentResult);
		} else {
			if (!shouldCheckVersion(cacheObject)) {
				collector.registerHit(GlobalCache.class);
				log("Cache (global): HIT getObject {}", key);
				object = cacheObject.getObject();
				locallyCacheObjectWithoutCloning(localCache, object);
				object = cloneIfNecessary(object, readOnly);
			} else {
				if (hasVersionChanged(key, cacheObject, parentResult)) {
					collector.registerMiss(GlobalCache.class);
					log("Cache (global): MISS because of version changed - getObject {}", key);
					object = loadAndCacheObject(key, options, readOnly, localCache, parentResult);
				} else {
					cacheObject.setTimeToLive(System.currentTimeMillis() + cacheMaxTTL);    // version matches, renew ttl
					collector.registerWeakHit(GlobalCache.class);
					log("Cache (global): HIT with version check - getObject {}", key);
					object = cacheObject.getObject();
					locallyCacheObjectWithoutCloning(localCache, object);
					object = cloneIfNecessary(object, readOnly);
				}
			}
		}
        return object;
	}

	private void registerNotAvailable() {
		CachePerformanceCollector.INSTANCE.registerNotAvailable(Cache.class);
	}

	private <T extends ObjectType> PrismObject<T> cloneIfNecessary(PrismObject<T> object, boolean readOnly) {
		if (readOnly) {
			return object;
		} else {
			// if client requested writable object, we need to provide him with a copy
			return object.clone();
		}
	}

	private <T extends ObjectType> PrismObject<T> getObjectInternal(Class<T> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
		Long startTime = repoOpStart();
		try {
			return repositoryService.getObject(type, oid, options, parentResult);
		} finally {
			repoOpEnd(startTime);
		}
	}

	private Long repoOpStart() {
		RepositoryPerformanceMonitor monitor = DiagnosticContextHolder.get(RepositoryPerformanceMonitor.class);
		if (monitor == null) {
			return null;
		} else {
			return System.currentTimeMillis();
		}
	}
	
	private void repoOpEnd(Long startTime) {
		RepositoryPerformanceMonitor monitor = DiagnosticContextHolder.get(RepositoryPerformanceMonitor.class);
		if (monitor != null) {
			monitor.recordRepoOperation(System.currentTimeMillis() - startTime);
		}
	}
	
	private boolean notCacheable(Class<?> type) {
		return type.equals(TaskType.class);
	}

	@Override
	public <T extends ObjectType> String addObject(PrismObject<T> object, RepoAddOptions options, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException {
		String oid;
		Long startTime = repoOpStart();
		try {
			oid = repositoryService.addObject(object, options, parentResult);
		} finally {
			repoOpEnd(startTime);
		}
		// DON't cache the object here. The object may not have proper "JAXB" form, e.g. some pieces may be
		// DOM element instead of JAXB elements. Not to cache it is safer and the performance loss
		// is acceptable.
		if (options != null && options.isOverwrite()) {
			invalidateCacheEntries(object.getCompileTimeClass(), oid,
					new ModifyObjectResult<>(object.getUserData(RepositoryService.KEY_ORIGINAL_OBJECT), object));
		} else {
			// just for sure (the object should not be there but ...)
			invalidateCacheEntries(object.getCompileTimeClass(), oid, new AddObjectResult<>(object));
		}
		return oid;
	}

	@NotNull
	@Override
	public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {

		CachePerformanceCollector collector = CachePerformanceCollector.INSTANCE;

		/*
		 * Checks related to both caches
		 */

		Cache localCache = getCache();
		if (notCacheable(type) || harmfulOptions(options, type)) {
			if (localCache != null) {
				localCache.recordPass();
			}
			collector.registerPass(GlobalCache.class);
			log("Cache (local/global): PASS searchObjects ({}, {})", type.getSimpleName(), options);
			return searchObjectsInternal(type, query, options, parentResult);
		}

		/*
		 * Let's try local cache
		 */

		boolean readOnly = GetOperationOptions.isReadOnly(SelectorOptions.findRootOptions(options));
		if (localCache == null) {
			log("Cache (local): NULL ({})", type.getSimpleName());
			registerNotAvailable();
		} else {
			SearchResultList queryResult = localCache.getQueryResult(type, query);
			if (queryResult != null) {
				localCache.recordHit();
				if (readOnly) {
					log("Cache: HIT searchObjects {} ({})", query, type.getSimpleName());
					//noinspection unchecked
					return queryResult;
				} else {
					log("Cache: HIT(clone) searchObjects {} ({})", query, type.getSimpleName());
					//noinspection unchecked
					return queryResult.clone();
				}
			}
			localCache.recordMiss();
			log("Cache: MISS searchObjects {} ({})", query, type.getSimpleName());
			//LOGGER.info("Cache: MISS (searchObjects) {} ({}) #{}", query, type.getSimpleName(), cache.getMisses());
		}

		/*
		 * Then try global cache
		 */
		QueryKey key = new QueryKey(type, query);
		if (!supportsGlobalCaching(type)) {
			// caller is not interested in cached value, or global cache doesn't want to cache value
			collector.registerPass(GlobalCache.class);
			SearchResultList<PrismObject<T>> objects = searchObjectsInternal(type, query, options, parentResult);
			locallyCacheSearchResult(localCache, key, readOnly, objects);
			return objects;
		}

		SearchResultList<PrismObject<T>> searchResult = globalCache.getQuery(key);

		if (searchResult == null) {
			collector.registerMiss(GlobalCache.class);
			log("Cache (global): MISS searchObjects {}", key);
			searchResult = executeAndCacheSearch(key, options, readOnly, localCache, parentResult);
		} else {
			collector.registerHit(GlobalCache.class);
			log("Cache (global): HIT searchObjects {}", key);
			locallyCacheSearchResult(localCache, key, readOnly, searchResult);
		}
		return searchResult;
	}

	private <T extends ObjectType> void locallyCacheSearchResult(Cache cache, QueryKey key,
			boolean readOnly, SearchResultList<PrismObject<T>> objects) {
		if (cache != null) {
			for (PrismObject<T> object : objects) {
				locallyCacheObject(cache, object, readOnly);
			}
			// TODO cloning before storing into cache?
			cache.putQueryResult(key, objects);
		}
	}

	private <T extends ObjectType> void globallyCacheSearchResult(QueryKey key, boolean readOnly,
			SearchResultList<PrismObject<T>> objects) {
		for (PrismObject<T> object : objects) {
			globallyCacheObjectWithoutCloning(new GlobalCacheObjectKey(key.getType(), object.getOid()), object);
		}
		// TODO cloning before storing into cache?
		globalCache.putQuery(key, objects);
	}

	@NotNull
	private <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjectsInternal(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
			throws SchemaException {
		Long startTime = repoOpStart();
		try {
			return repositoryService.searchObjects(type, query, options, parentResult);
		} finally {
			repoOpEnd(startTime);
		}
	}

	@Override
	public <T extends Containerable> SearchResultList<T> searchContainers(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
		Long startTime = repoOpStart();
		try {
			return repositoryService.searchContainers(type, query, options, parentResult);
		} finally {
			repoOpEnd(startTime);
		}
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.repo.api.RepositoryService#searchObjectsIterative(java.lang.Class, com.evolveum.midpoint.prism.query.ObjectQuery, com.evolveum.midpoint.schema.ResultHandler, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query,
			final ResultHandler<T> handler, final Collection<SelectorOptions<GetOperationOptions>> options,
			boolean strictlySequential, OperationResult parentResult) throws SchemaException {
		// TODO use cached query result if applicable
		final Cache cache = getCache();
		if (cache != null) {
			cache.recordPass();
		}
		log("Cache: PASS searchObjectsIterative ({})", type.getSimpleName());
		ResultHandler<T> myHandler = (object, parentResult1) -> {
			locallyCacheObject(cache, object, GetOperationOptions.isReadOnly(SelectorOptions.findRootOptions(options)));
			return handler.handle(object, parentResult1);
		};
		Long startTime = repoOpStart();
		try {
			return repositoryService.searchObjectsIterative(type, query, myHandler, options, strictlySequential, parentResult);
		} finally {
			repoOpEnd(startTime);
		}
	}

	@Deprecated
	@Override
	public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, OperationResult parentResult)
			throws SchemaException {
		// TODO use cached query result if applicable
		log("Cache: PASS countObjects ({})", type.getSimpleName());
		Long startTime = repoOpStart();
		try {
			return repositoryService.countObjects(type, query, null, parentResult);
		} finally {
			repoOpEnd(startTime);
		}
	}

	@Override
	public <T extends Containerable> int countContainers(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
		log("Cache: PASS countContainers ({})", type.getSimpleName());
		Long startTime = repoOpStart();
		try {
			return repositoryService.countContainers(type, query, options, parentResult);
		} finally {
			repoOpEnd(startTime);
		}
	}

	@Override
	public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
			throws SchemaException {
		// TODO use cached query result if applicable
		log("Cache: PASS countObjects ({})", type.getSimpleName());
		Long startTime = repoOpStart();
		try {
			return repositoryService.countObjects(type, query, options, parentResult);
		} finally {
			repoOpEnd(startTime);
		}
	}

	@NotNull
	public <T extends ObjectType> ModifyObjectResult<T> modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
													OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		return modifyObject(type, oid, modifications, null, parentResult);
	}

	@NotNull
	@Override
	public <T extends ObjectType> ModifyObjectResult<T> modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
			RepoModifyOptions options, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		try {
			return modifyObject(type, oid, modifications, null, options, parentResult);
		} catch (PreconditionViolationException e) {
			throw new AssertionError(e);
		}
	}

	private void delay(Integer delayRange) {
		if (delayRange == null) {
			return;
		}
		int delay = RND.nextInt(delayRange);
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			// Nothing to do
		}
	}

	@NotNull
	@Override
	public <T extends ObjectType> ModifyObjectResult<T> modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
			ModificationPrecondition<T> precondition, RepoModifyOptions options, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException {
		delay(modifyRandomDelayRange);
		Long startTime = repoOpStart();
		ModifyObjectResult<T> modifyInfo = null;
		try {
			modifyInfo = repositoryService.modifyObject(type, oid, modifications, precondition, options, parentResult);
			return modifyInfo;
		} finally {
			repoOpEnd(startTime);
			// this changes the object. We are too lazy to apply changes ourselves, so just invalidate
			// the object in cache
			invalidateCacheEntries(type, oid, modifyInfo);
		}
	}

	private <T extends ObjectType> void invalidateCacheEntries(Class<T> type, String oid, Object additionalInfo) {
		Cache cache = getCache();
		if (cache != null) {
			cache.removeObject(oid);
			clearQueryResultsLocally(cache, type, additionalInfo, prismContext, matchingRuleRegistry);
		}

		globalCache.removeObject(new GlobalCacheObjectKey(type, oid));
		clearQueryResultsGlobally(type, additionalInfo, prismContext, matchingRuleRegistry);
		cacheDispatcher.dispatch(type, oid);
	}

	public <T extends ObjectType> void clearQueryResultsLocally(Cache cache, Class<T> type, Object additionalInfo, PrismContext prismContext,
			MatchingRuleRegistry matchingRuleRegistry) {
		// TODO implement more efficiently

		ChangeDescription change = getChangeDescription(type, additionalInfo, prismContext);

		long start = System.currentTimeMillis();
		int all = 0;
		int removed = 0;
		Iterator<Map.Entry<QueryKey, SearchResultList>> iterator = cache.getQueryIterator();
		while (iterator.hasNext()) {
			QueryKey queryKey = iterator.next().getKey();
			all++;
			if (queryKey.getType().isAssignableFrom(type) && (change == null || change.mayAffect(queryKey, matchingRuleRegistry))) {
				LOGGER.info("Removing (from local cache) query for type={}, change={}: {}", type, change, queryKey.getQuery());
				iterator.remove();
				removed++;
			}
		}
		LOGGER.info("Removed (from local cache) {} (of {}) query result entries of type {} in {} ms", removed, all, type, System.currentTimeMillis() - start);
	}

	public <T extends ObjectType> void clearQueryResultsGlobally(Class<T> type, Object additionalInfo, PrismContext prismContext,
			MatchingRuleRegistry matchingRuleRegistry) {
		// TODO implement more efficiently

		ChangeDescription change = getChangeDescription(type, additionalInfo, prismContext);

		long start = System.currentTimeMillis();
		AtomicInteger all = new AtomicInteger(0);
		AtomicInteger removed = new AtomicInteger(0);

		globalCache.invokeOnAllQueries(entry -> {
			QueryKey queryKey = entry.getKey();
			all.incrementAndGet();
			if (queryKey.getType().isAssignableFrom(type) && (change == null || change.mayAffect(queryKey, matchingRuleRegistry))) {
				LOGGER.info("Removing (from global cache) query for type={}, change={}: {}", type, change, queryKey.getQuery());
				entry.remove();
				removed.incrementAndGet();
			}
			return null;
		});
		LOGGER.info("Removed (from global cache) {} (of {}) query result entries of type {} in {} ms", removed, all, type, System.currentTimeMillis() - start);
	}

	@Nullable
	private <T extends ObjectType> ChangeDescription getChangeDescription(Class<T> type, Object additionalInfo,
			PrismContext prismContext) {
		ChangeDescription change;
		if (!LookupTableType.class.equals(type) && !AccessCertificationCampaignType.class.equals(type)) {
			change = ChangeDescription.getFrom(additionalInfo, prismContext);
		} else {
			change = null;      // these objects are tricky to query -- it's safer to evict their queries completely
		}
		return change;
	}

	@NotNull
	@Override
	public <T extends ObjectType> DeleteObjectResult deleteObject(Class<T> type, String oid, OperationResult parentResult)
			throws ObjectNotFoundException {
		Long startTime = repoOpStart();
		DeleteObjectResult deleteInfo = null;
		try {
			deleteInfo = repositoryService.deleteObject(type, oid, parentResult);
			return deleteInfo;
		} finally {
			repoOpEnd(startTime);
			invalidateCacheEntries(type, oid, deleteInfo);
		}
	}

	@Override
	public <F extends FocusType> PrismObject<F> searchShadowOwner(
			String shadowOid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
		// TODO cache the search operation?
		PrismObject<F> ownerObject;
		Long startTime = repoOpStart();
		try {
			ownerObject = repositoryService.searchShadowOwner(shadowOid, options, parentResult);
		} finally {
			repoOpEnd(startTime);
		}
		if (ownerObject != null && !harmfulOptions(options, FocusType.class)) {
			locallyCacheObject(getCache(), ownerObject, GetOperationOptions.isReadOnly(SelectorOptions.findRootOptions(options)));
		}
		return ownerObject;
	}

	private boolean harmfulOptions(Collection<SelectorOptions<GetOperationOptions>> options, Class<?> objectType) {
		if (options == null || options.isEmpty()) {
			return false;
		}
		if (options.size() > 1) {
			//LOGGER.info("Cache: PASS REASON: size>1: {}", options);
			return true;
		}
		SelectorOptions<GetOperationOptions> selectorOptions = options.iterator().next();
		if (!selectorOptions.isRoot()) {
			//LOGGER.info("Cache: PASS REASON: !root: {}", options);
			return true;
		}
		GetOperationOptions options1 = selectorOptions.getOptions();
		// TODO FIX THIS!!!
		if (options1 == null ||
				options1.equals(new GetOperationOptions()) ||
				options1.equals(GetOperationOptions.createAllowNotFound()) ||
				options1.equals(GetOperationOptions.createExecutionPhase()) ||
				options1.equals(GetOperationOptions.createReadOnly()) ||
				options1.equals(GetOperationOptions.createNoFetch())) {
			return false;
		}
		if (options1.equals(GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE))) {
			if (SelectorOptions.isRetrievedFullyByDefault(objectType)) {
				return false;
			} else {
				//LOGGER.info("Cache: PASS REASON: INCLUDE for {}: {}", objectType, options);
				return true;
			}
		}
		//LOGGER.info("Cache: PASS REASON: other: {}", options);
		return true;
	}

	@Override
	@Deprecated
	public PrismObject<UserType> listAccountShadowOwner(String accountOid, OperationResult parentResult)
			throws ObjectNotFoundException {
		Long startTime = repoOpStart();
		try {
			return repositoryService.listAccountShadowOwner(accountOid, parentResult);
		} finally {
			repoOpEnd(startTime);
		}
	}

	@Override
	public <T extends ShadowType> List<PrismObject<T>> listResourceObjectShadows(String resourceOid,
			Class<T> resourceObjectShadowType, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException {
		Long startTime = repoOpStart();
		try {
			return repositoryService.listResourceObjectShadows(resourceOid, resourceObjectShadowType, parentResult);
		} finally {
			repoOpEnd(startTime);
		}
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.repo.api.RepositoryService#getVersion(java.lang.Class, java.lang.String, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <T extends ObjectType> String getVersion(Class<T> type, String oid, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		Cache cache = getCache();
		if (notCacheable(type)) {
			if (cache != null) {
				cache.recordPass();
			}
			log("Cache: PASS {} ({})", oid, type.getSimpleName());
			Long startTime = repoOpStart();
			try {
				return repositoryService.getVersion(type, oid, parentResult);
			} finally {
				repoOpEnd(startTime);
			}
		}
		if (cache == null) {
			log("Cache: NULL {} ({})", oid, type.getSimpleName());
			registerNotAvailable();
		} else {
			String version = cache.getObjectVersion(oid);
			if (version != null) {
				cache.recordHit();
				log("Cache: HIT {} ({})", oid, type.getSimpleName());
				return version;
			}
			cache.recordMiss();
			log("Cache: MISS {} ({})", oid, type.getSimpleName());
			//LOGGER.info("Cache: MISS (getVersion) {} ({}) #{}", oid, type.getSimpleName(), cache.getMisses());
		}
		String version;
		Long startTime = repoOpStart();
		try {
			version = repositoryService.getVersion(type, oid, parentResult);
		} finally {
			repoOpEnd(startTime);
		}
		cacheObjectVersion(cache, oid, version);
		return version;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.repo.api.RepositoryService#getRepositoryDiag()
	 */
	@Override
	public RepositoryDiag getRepositoryDiag() {
		Long startTime = repoOpStart();
		try {
			return repositoryService.getRepositoryDiag();
		} finally {
			repoOpEnd(startTime);
		}
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.repo.api.RepositoryService#repositorySelfTest(com.evolveum.midpoint.schema.result.OperationResult)
	 */
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

    private <T extends ObjectType> void locallyCacheObject(Cache cache, PrismObject<T> object, boolean readOnly) {
		if (cache != null) {
			cache.putObject(object.getOid(), prepareObjectToCache(object, readOnly));
		}
	}

    private <T extends ObjectType> void locallyCacheObjectWithoutCloning(Cache cache, PrismObject<T> object) {
		if (cache != null) {
			cache.putObject(object.getOid(), object);
		}
	}

	private <T extends ObjectType> void cacheObjectVersion(Cache cache, String oid, String version) {
		if (cache != null) {
			cache.putObjectVersion(oid, version);
		}
	}

	@Override
	public boolean isAnySubordinate(String upperOrgOid, Collection<String> lowerObjectOids)
			throws SchemaException {
		Long startTime = repoOpStart();
		try {
			return repositoryService.isAnySubordinate(upperOrgOid, lowerObjectOids);
		} finally {
			repoOpEnd(startTime);
		}
	}

	@Override
	public <O extends ObjectType> boolean isDescendant(PrismObject<O> object, String orgOid)
			throws SchemaException {
		Long startTime = repoOpStart();
		try {
			return repositoryService.isDescendant(object, orgOid);
		} finally {
			repoOpEnd(startTime);
		}
	}

	@Override
	public <O extends ObjectType> boolean isAncestor(PrismObject<O> object, String oid)
			throws SchemaException {
		Long startTime = repoOpStart();
		try {
			return repositoryService.isAncestor(object, oid);
		} finally {
			repoOpEnd(startTime);
		}
	}

	@Override
	public <O extends ObjectType> boolean selectorMatches(ObjectSelectorType objectSelector,
			PrismObject<O> object, ObjectFilterExpressionEvaluator filterEvaluator, Trace logger, String logMessagePrefix) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		Long startTime = repoOpStart();
		try {
			return repositoryService.selectorMatches(objectSelector, object, filterEvaluator, logger, logMessagePrefix);
		} finally {
			repoOpEnd(startTime);
		}
	}

	private void log(String message, Object... params) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(message, params);
		}
		if (PERFORMANCE_ADVISOR.isTraceEnabled()) {
			PERFORMANCE_ADVISOR.trace(message, params);
		}
	}

	@Override
	public long advanceSequence(String oid, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException {
		Long startTime = repoOpStart();
		try {
			return repositoryService.advanceSequence(oid, parentResult);
		} finally {
			repoOpEnd(startTime);
			invalidateCacheEntries(SequenceType.class, oid, null);
		}
	}

	@Override
	public void returnUnusedValuesToSequence(String oid, Collection<Long> unusedValues, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		Long startTime = repoOpStart();
		try {
			repositoryService.returnUnusedValuesToSequence(oid, unusedValues, parentResult);
		} finally {
			repoOpEnd(startTime);
			invalidateCacheEntries(SequenceType.class, oid, null);
		}
	}

	@Override
	public RepositoryQueryDiagResponse executeQueryDiagnostics(RepositoryQueryDiagRequest request, OperationResult result) {
		Long startTime = repoOpStart();
		try {
			return repositoryService.executeQueryDiagnostics(request, result);
		} finally {
			repoOpEnd(startTime);
		}
	}

	@Override
	public QName getApproximateSupportedMatchingRule(Class<?> dataType, QName originalMatchingRule) {
		Long startTime = repoOpStart();
		try {
			return repositoryService.getApproximateSupportedMatchingRule(dataType, originalMatchingRule);
		} finally {
			repoOpEnd(startTime);
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
	public void postInit(OperationResult result) throws SchemaException {
		repositoryService.postInit(result);
	}

	@Override
	public ConflictWatcher createAndRegisterConflictWatcher(String oid) {
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

	private <T extends ObjectType> boolean supportsGlobalCaching(Class<T> type) {
		return cacheMaxTTL > 0 && GLOBAL_CACHE_SUPPORTED_TYPES.contains(type);
	}

	private <T extends ObjectType> void removeObject(Class<T> type, String oid) {
		Validate.notNull(type, "Type must not be null");
		Validate.notNull(oid, "Oid must not be null");

		globalCache.removeObject(new GlobalCacheObjectKey(type, oid));
	}

	private boolean hasVersionChanged(GlobalCacheObjectKey key, GlobalCacheObjectValue object, OperationResult result)
			throws ObjectNotFoundException, SchemaException {

		try {
			String version = repositoryService.getVersion(object.getObjectType(), object.getObjectOid(), result);

			return !Objects.equals(version, object.getObjectVersion());
		} catch (ObjectNotFoundException | SchemaException ex) {
			removeObject(key.getType(), key.getOid());

			throw ex;
		}
	}

	private boolean shouldCheckVersion(GlobalCacheObjectValue object) {
		return object.getTimeToLive() < System.currentTimeMillis();
	}

	private <T extends ObjectType> PrismObject<T> loadAndCacheObject(GlobalCacheObjectKey key,
			Collection<SelectorOptions<GetOperationOptions>> options, boolean readOnly, Cache localCache, OperationResult result)
			throws ObjectNotFoundException, SchemaException {
		try {
			//noinspection unchecked
			PrismObject<T> object = (PrismObject<T>) getObjectInternal(key.getType(), key.getOid(), options, result);
			PrismObject<T> objectToCache = prepareObjectToCache(object, readOnly);
			globallyCacheObjectWithoutCloning(key, objectToCache);
			locallyCacheObjectWithoutCloning(localCache, objectToCache);
			return object;
		} catch (ObjectNotFoundException | SchemaException ex) {
			globalCache.removeObject(key);
			throw ex;
		}
	}

	private <T extends ObjectType> void globallyCacheObjectWithoutCloning(GlobalCacheObjectKey key,
			PrismObject<T> objectToCache) {
		long ttl = System.currentTimeMillis() + cacheMaxTTL;
		globalCache.putObject(key, new GlobalCacheObjectValue<>(objectToCache, ttl));
	}

	private <T extends ObjectType> SearchResultList<PrismObject<T>> executeAndCacheSearch(QueryKey key,
			Collection<SelectorOptions<GetOperationOptions>> options, boolean readOnly, Cache localCache, OperationResult result)
			throws SchemaException {
		try {
			//noinspection unchecked
			SearchResultList<PrismObject<T>> searchResult = (SearchResultList) searchObjectsInternal(key.getType(), key.getQuery(), options, result);
			locallyCacheSearchResult(localCache, key, readOnly, searchResult);
			globallyCacheSearchResult(key, readOnly, searchResult);
			return searchResult;
		} catch (SchemaException ex) {
			globalCache.removeQuery(key);
			throw ex;
		}
	}

	@NotNull
	private <T extends ObjectType> PrismObject<T> prepareObjectToCache(PrismObject<T> object, boolean readOnly) {
		PrismObject<T> objectToCache;
		if (readOnly) {
			object.setImmutable(true);
			objectToCache = object;
		} else {
			// We are going to return the object (as mutable), so we must store a clone
			objectToCache = object.clone();
		}
		return objectToCache;
	}

	@Override
	public <T extends ObjectType> void addDiagnosticInformation(Class<T> type, String oid, DiagnosticInformationType information,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		delay(modifyRandomDelayRange);
		Long startTime = repoOpStart();
		try {
			repositoryService.addDiagnosticInformation(type, oid, information, parentResult);
		} finally {
			repoOpEnd(startTime);
			// this changes the object. We are too lazy to apply changes ourselves, so just invalidate
			// the object in cache
			// TODO specify additional info more precisely (but currently we use this method only in connection with TaskType
			//  and this kind of object is not cached anyway, so let's ignore this
			invalidateCacheEntries(type, oid, null);
		}
	}

	@Override
	public PerformanceMonitor getPerformanceMonitor() {
		return repositoryService.getPerformanceMonitor();
	}
}

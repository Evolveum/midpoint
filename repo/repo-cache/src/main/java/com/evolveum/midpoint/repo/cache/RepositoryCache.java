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

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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

	private static final String CONFIGURATION_COMPONENT = "midpoint.repository";
	private static final String PROPERTY_CACHE_MAX_TTL = "cacheMaxTTL";

	@Deprecated
	private static final Set<Class<? extends ObjectType>> GLOBAL_CACHE_SUPPORTED_TYPES;

	static {
		Set<Class<? extends ObjectType>> set = new HashSet<>();
		set.add(ConnectorType.class);
		set.add(ObjectTemplateType.class);
		set.add(SecurityPolicyType.class);
		set.add(SystemConfigurationType.class);
		set.add(ValuePolicyType.class);

		GLOBAL_CACHE_SUPPORTED_TYPES = Collections.unmodifiableSet(set);
	}

	private static final ThreadLocal<Cache> cacheInstance = new ThreadLocal<>();

	@Autowired private RepositoryService repositoryService;

	@Autowired private PrismContext prismContext;

	@Autowired private MidpointConfiguration midpointConfiguration;

	@Autowired private CacheDispatcher cacheDispatcher;

	private long cacheMaxTTL;

	private static final Random RND = new Random();

	private Integer modifyRandomDelayRange;

	private CacheManager cacheManager;

	public RepositoryCache() {
    }

	@PostConstruct
	public void initialize() {
		Integer cacheMaxTTL = midpointConfiguration.getConfiguration(CONFIGURATION_COMPONENT)
				.getInt(PROPERTY_CACHE_MAX_TTL,0);
		if (cacheMaxTTL == null || cacheMaxTTL < 0) {
			cacheMaxTTL = 0;
		}
		this.cacheMaxTTL = cacheMaxTTL * 1000;

		initializeGlobalCache();
	}

	private void initializeGlobalCache() {
		CacheConfigurationBuilder ccBuilder = CacheConfigurationBuilder.newCacheConfigurationBuilder(
				String.class, CacheObject.class, ResourcePoolsBuilder.heap(5000))
				.withExpiry(Expirations.timeToLiveExpiration(Duration.of(15, TimeUnit.MINUTES)));

		cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
				.build(true);

		for (ObjectTypes ot : ObjectTypes.values()) {
			Class type =  ot.getClassDefinition();
			if (Modifier.isAbstract(type.getModifiers())) {
				continue;
			}

			cacheManager.createCache(type.getSimpleName(), ccBuilder);
		}
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

		boolean readOnly = GetOperationOptions.isReadOnly(SelectorOptions.findRootOptions(options));

		if (!isCacheable(type) || !nullOrHarmlessOptions(options)) {
			// local cache not interested in caching this object
			log("Cache: PASS {} ({})", oid, type.getSimpleName());

			return getObjectTryGlobalCache(type, oid, options, parentResult, readOnly);
		}

		Cache cache = getCache();
		if (cache == null) {
			log("Cache: NULL {} ({})", oid, type.getSimpleName());
		} else {
			PrismObject<T> object = (PrismObject) cache.getObject(oid);
			if (object != null) {
				log("Cache: HIT{} {} ({})", readOnly ? "" : "(clone)", oid, type.getSimpleName());
				return cloneIfNecessary(object, readOnly);
			}
			log("Cache: MISS {} ({})", oid, type.getSimpleName());
		}

		PrismObject<T> object = getObjectTryGlobalCache(type, oid, options, parentResult, readOnly);
		cacheObject(cache, object, readOnly);
		return object;
	}

	private <T extends ObjectType> PrismObject<T> cloneIfNecessary(PrismObject<T> object, boolean readOnly) {
		if (readOnly) {
			return object;
		}

		return object.clone();
	}

	private <T extends ObjectType> PrismObject<T> getObjectTryGlobalCache(Class<T> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options,
																		  OperationResult parentResult, boolean readOnly) throws SchemaException, ObjectNotFoundException {
		if (!shouldUseGlobalCache(type, options)) {
			// caller is not interested in cached value, or global cache doesn't want to cache value
			return getObjectInternal(type, oid, options, parentResult);
		}

		org.ehcache.Cache<String, CacheObject<T>> gCache = getGlobalCache(type);
		CacheObject cacheObject = gCache != null ? gCache.get(oid) : null;

		PrismObject<T> object;
		if (cacheObject == null) {
			object = reloadObjectInGlobalCache(type, oid, options, parentResult, readOnly);
		} else {
			if (!shouldCheckVersion(cacheObject, options)) {
				log("Cache: Global HIT {} ({})", oid, type.getSimpleName());
				object = cacheObject.getObject();
			} else {
				if (hasVersionChanged(cacheObject, parentResult)) {
					object = reloadObjectInGlobalCache(type, oid, options, parentResult, readOnly);
				} else {
					// version matches, just update last version check
					cacheObject.setLastVersionCheck(System.currentTimeMillis());

					log("Cache: Global HIT, version check {} ({})", oid, type.getSimpleName());
					object = cacheObject.getObject();
				}
			}
		}

		return cloneIfNecessary(object, readOnly);
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

	private <T extends ObjectType> boolean shouldUseGlobalCache(Class<T> type, Collection<SelectorOptions<GetOperationOptions>> options) {
		// todo remove this if after model code starts to use PointInTimeType.CACHED and stallesness options correctly
        if (supportsGlobalCaching(type, options)) {
            return true;
        }

		if (!isCacheable(type)) {
			return false;
		}

		GetOperationOptions opts = SelectorOptions.findRootOptions(options);
        if (opts == null) {
            return false;
        }

		if (Objects.equals(PointInTimeType.CACHED, opts.getPointInTimeType())) {
			Long stalesness = opts.getStaleness();
			if (stalesness != null && stalesness > 0) {
				return true;
			}
		}

		return false;
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

	private boolean isCacheable(Class<?> type) {
		if (type.equals(TaskType.class)) {
			return false;
		}
//		if (ShadowType.class.isAssignableFrom(type)) {
//			return false;
//		}
		return true;
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
		Cache cache = getCache();
		// DON't cache the object here. The object may not have proper "JAXB" form, e.g. some pieces may be
		// DOM element instead of JAXB elements. Not to cache it is safer and the performance loss
		// is acceptable.
		if (cache != null) {
			// Invalidate the cache entry if it happens to be there
			cache.removeObject(oid);
			cache.clearQueryResults(object.getCompileTimeClass());
		}
		return oid;
	}

	@NotNull
	@Override
	public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
		if (!isCacheable(type) || !nullOrHarmlessOptions(options)) {
			log("Cache: PASS ({})", type.getSimpleName());
			Long startTime = repoOpStart();
			try {
				return repositoryService.searchObjects(type, query, options, parentResult);
			} finally {
				repoOpEnd(startTime);
			}
		}
		Cache cache = getCache();
		boolean readOnly = GetOperationOptions.isReadOnly(SelectorOptions.findRootOptions(options));
		if (cache == null) {
			log("Cache: NULL ({})", type.getSimpleName());
		} else {
			SearchResultList queryResult = cache.getQueryResult(type, query, prismContext);
			if (queryResult != null) {
				if (readOnly) {
					log("Cache: HIT {} ({})", query, type.getSimpleName());
					return queryResult;
				} else {
					log("Cache: HIT(clone) {} ({})", query, type.getSimpleName());
					return queryResult.clone();
				}
			}
			log("Cache: MISS {} ({})", query, type.getSimpleName());
		}

		// Cannot satisfy from cache, pass down to repository
		SearchResultList<PrismObject<T>> objects;
		Long startTime = repoOpStart();
		try {
			objects = repositoryService.searchObjects(type, query, options, parentResult);
		} finally {
			repoOpEnd(startTime);
		}
		if (cache != null && options == null) {
			for (PrismObject<T> object : objects) {
				cacheObject(cache, object, readOnly);
			}
			// TODO cloning before storing into cache?
			cache.putQueryResult(type, query, objects, prismContext);
		}
		return objects;
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
		log("Cache: PASS searchObjectsIterative ({})", type.getSimpleName());
		final Cache cache = getCache();
		ResultHandler<T> myHandler = new ResultHandler<T>() {
			@Override
			public boolean handle(PrismObject<T> object, OperationResult parentResult) {
				cacheObject(cache, object, GetOperationOptions.isReadOnly(SelectorOptions.findRootOptions(options)));
				return handler.handle(object, parentResult);
			}
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

	public <T extends ObjectType> void modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
													OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		modifyObject(type, oid, modifications, null, parentResult);
	}

	@Override
	public <T extends ObjectType> void modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
			RepoModifyOptions options, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		try {
			modifyObject(type, oid, modifications, null, options, parentResult);
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

	@Override
	public <T extends ObjectType> void modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
			ModificationPrecondition<T> precondition, RepoModifyOptions options, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException {
		delay(modifyRandomDelayRange);
		Long startTime = repoOpStart();
		try {
			repositoryService.modifyObject(type, oid, modifications, precondition, options, parentResult);
		} finally {
			repoOpEnd(startTime);
			// this changes the object. We are too lazy to apply changes ourselves, so just invalidate
			// the object in cache
			invalidateCacheEntry(type, oid);
		}
	}

	protected <T extends ObjectType> void invalidateCacheEntry(Class<T> type, String oid) {
		Cache cache = getCache();
		if (cache != null) {
			cache.removeObject(oid);
			cache.clearQueryResults(type);
		}

		org.ehcache.Cache gCache = getGlobalCache(type);
		if (gCache != null) {
			gCache.remove(oid);
		}

		cacheDispatcher.dispatch(type, oid);
	}

	@Override
	public <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult parentResult)
			throws ObjectNotFoundException {
		Long startTime = repoOpStart();
		try {
			repositoryService.deleteObject(type, oid, parentResult);
		} finally {
			repoOpEnd(startTime);
			invalidateCacheEntry(type, oid);
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
		if (ownerObject != null && nullOrHarmlessOptions(options)) {
			cacheObject(getCache(), ownerObject, GetOperationOptions.isReadOnly(SelectorOptions.findRootOptions(options)));
		}
		return ownerObject;
	}

	private boolean nullOrHarmlessOptions(Collection<SelectorOptions<GetOperationOptions>> options) {
		if (options == null || options.isEmpty()) {
			return true;
		}
		if (options.size() > 1) {
			return false;
		}
		SelectorOptions<GetOperationOptions> selectorOptions = options.iterator().next();
		if (!selectorOptions.isRoot()) {
			return false;
		}
		GetOperationOptions options1 = selectorOptions.getOptions();
		if (options1 == null || options1.equals(new GetOperationOptions()) || options1.equals(GetOperationOptions.createAllowNotFound())) {
			return true;
		}
		return false;
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
		if (!isCacheable(type)) {
			log("Cache: PASS {} ({})", oid, type.getSimpleName());
			Long startTime = repoOpStart();
			try {
				return repositoryService.getVersion(type, oid, parentResult);
			} finally {
				repoOpEnd(startTime);
			}
		}
		Cache cache = getCache();
		if (cache == null) {
			log("Cache: NULL {} ({})", oid, type.getSimpleName());
		} else {
			String version = cache.getObjectVersion(oid);
			if (version != null) {
				log("Cache: HIT {} ({})", oid, type.getSimpleName());
				return version;
			}
			log("Cache: MISS {} ({})", oid, type.getSimpleName());
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

    private <T extends ObjectType> void cacheObject(Cache cache, PrismObject<T> object, boolean readOnly) {
		if (cache != null) {
			PrismObject<ObjectType> objectToCache;
			if (readOnly) {
				object.setImmutable(true);
				objectToCache = (PrismObject<ObjectType>) object;
			} else {
				objectToCache = (PrismObject<ObjectType>) object.clone();
			}
			cache.putObject(object.getOid(), objectToCache);
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
			invalidateCacheEntry(SequenceType.class, oid);
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
			invalidateCacheEntry(SequenceType.class, oid);
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

    private <T extends ObjectType> org.ehcache.Cache<String, CacheObject<T>> getGlobalCache(Class<T> type) {
        return (org.ehcache.Cache) cacheManager.getCache(type.getSimpleName(), String.class, CacheObject.class);
    }

	private <T extends ObjectType> boolean supportsGlobalCaching(
			Class<T> type, Collection<SelectorOptions<GetOperationOptions>> options) {

		if (cacheMaxTTL <= 0) {
			return false;
		}

		if (!GLOBAL_CACHE_SUPPORTED_TYPES.contains(type)) {
			return false;
		}

		if (!nullOrHarmlessOptions(options)) {
			return false;
		}

		return true;
	}

	private boolean hasVersionChanged(CacheObject object, OperationResult result)
			throws ObjectNotFoundException, SchemaException {

		Class type = object.getObjectType();
		String oid = object.getObjectOid();

		try {
			String version = repositoryService.getVersion(type, oid, result);

			return !Objects.equals(version, object.getObjectVersion());
		} catch (ObjectNotFoundException | SchemaException ex) {
			invalidateCacheEntry(type, oid);

			throw ex;
		}
	}

	private boolean shouldCheckVersion(CacheObject object, Collection<SelectorOptions<GetOperationOptions>> options) {
		GetOperationOptions opts = SelectorOptions.findRootOptions(options);
		Long staleness = opts != null ? opts.getStaleness() : null;

		if (staleness == null) {
			return object.getLastVersionCheck() + cacheMaxTTL < System.currentTimeMillis();
		}

		if (staleness <= 0) {
			return true;
		}

		return object.getLastVersionCheck() + staleness < System.currentTimeMillis();
	}

	private <T extends ObjectType> PrismObject<T> reloadObjectInGlobalCache(Class<T> type, String oid,
																			Collection<SelectorOptions<GetOperationOptions>> options,
																			OperationResult result, boolean readOnly)
			throws ObjectNotFoundException, SchemaException {

		log("Cache: Global MISS {} ({})", oid, type.getSimpleName());

		try {
			PrismObject object = getObjectInternal(type, oid, options, result);
			object.setImmutable(true);

			PrismObject<T> objectToCache;
			if (readOnly) {
				object.setImmutable(true);
				objectToCache = (PrismObject<T>) object;
			} else {
				objectToCache = (PrismObject<T>) object.clone();
			}

			org.ehcache.Cache<String, CacheObject<T>> gCache = getGlobalCache(objectToCache.getCompileTimeClass());
			if (gCache != null) {
				CacheObject<T> cacheObject = new CacheObject<>(objectToCache, System.currentTimeMillis());
				gCache.put(oid, cacheObject);
			}

			return object;
		} catch (ObjectNotFoundException | SchemaException ex) {
			invalidateCacheEntry(type, oid);

			throw ex;
		}
	}
}

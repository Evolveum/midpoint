/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
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

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Read-through write-through per-session repository cache.
 *
 * TODO doc
 * TODO logging perf measurements
 *
 * @author Radovan Semancik
 *
 */
public class RepositoryCache implements RepositoryService {

	private static ThreadLocal<Cache> cacheInstance = new ThreadLocal<>();

	private RepositoryService repository;

	private static final Trace LOGGER = TraceManager.getTrace(RepositoryCache.class);
	private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();
	private static final Random RND = new Random();

	private Integer modifyRandomDelayRange;

	private PrismContext prismContext;

	public RepositoryCache() {
    }

    public void setRepository(RepositoryService service, PrismContext prismContext) {
        Validate.notNull(service, "Repository service must not be null.");
		Validate.notNull(prismContext, "Prism context service must not be null.");
        this.repository = service;
		this.prismContext = prismContext;
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
		if (!isCacheable(type) || !nullOrHarmlessOptions(options)) {
			log("Cache: PASS {} ({})", oid, type.getSimpleName());
			return repository.getObject(type, oid, options, parentResult);
		}
		Cache cache = getCache();
		boolean readOnly = GetOperationOptions.isReadOnly(SelectorOptions.findRootOptions(options));
		if (cache == null) {
			log("Cache: NULL {} ({})", oid, type.getSimpleName());
		} else {
			PrismObject<T> object = (PrismObject) cache.getObject(oid);
			if (object != null) {
				// TODO: result?
				if (readOnly) {
					log("Cache: HIT {} ({})", oid, type.getSimpleName());
					return object;
				} else {
					log("Cache: HIT(clone) {} ({})", oid, type.getSimpleName());
					return object.clone();
				}
			}
			log("Cache: MISS {} ({})", oid, type.getSimpleName());
		}
		PrismObject<T> object = repository.getObject(type, oid, null, parentResult);
		cacheObject(cache, object, readOnly);
		return object;
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
		String oid = repository.addObject(object, options, parentResult);
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
			return repository.searchObjects(type, query, options, parentResult);
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
		SearchResultList<PrismObject<T>> objects = repository.searchObjects(type, query, options, parentResult);
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
		return repository.searchContainers(type, query, options, parentResult);
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
		return repository.searchObjectsIterative(type, query, myHandler, options, strictlySequential, parentResult);
	}

	@Deprecated
	@Override
	public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, OperationResult parentResult)
			throws SchemaException {
		// TODO use cached query result if applicable
		log("Cache: PASS countObjects ({})", type.getSimpleName());
		return repository.countObjects(type, query, null, parentResult);
	}

	@Override
	public <T extends Containerable> int countContainers(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
		log("Cache: PASS countContainers ({})", type.getSimpleName());
		return repository.countContainers(type, query, options, parentResult);
	}

	@Override
	public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
			throws SchemaException {
		// TODO use cached query result if applicable
		log("Cache: PASS countObjects ({})", type.getSimpleName());
		return repository.countObjects(type, query, options, parentResult);
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
		try {
			repository.modifyObject(type, oid, modifications, precondition, options, parentResult);
		} finally {
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
	}

	@Override
	public <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult parentResult)
			throws ObjectNotFoundException {
		try {
			repository.deleteObject(type, oid, parentResult);
		} finally {
			invalidateCacheEntry(type, oid);
		}
	}

	@Override
	public <F extends FocusType> PrismObject<F> searchShadowOwner(
			String shadowOid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
		// TODO cache the search operation?
		PrismObject<F> ownerObject = repository.searchShadowOwner(shadowOid, options, parentResult);
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
		return repository.listAccountShadowOwner(accountOid, parentResult);
	}

	@Override
	public <T extends ShadowType> List<PrismObject<T>> listResourceObjectShadows(String resourceOid,
			Class<T> resourceObjectShadowType, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException {
		return repository.listResourceObjectShadows(resourceOid, resourceObjectShadowType, parentResult);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.repo.api.RepositoryService#getVersion(java.lang.Class, java.lang.String, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <T extends ObjectType> String getVersion(Class<T> type, String oid, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		if (!isCacheable(type)) {
			log("Cache: PASS {} ({})", oid, type.getSimpleName());
			return repository.getVersion(type, oid, parentResult);
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
		String version = repository.getVersion(type, oid, parentResult);
		cacheObjectVersion(cache, oid, version);
		return version;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.repo.api.RepositoryService#getRepositoryDiag()
	 */
	@Override
	public RepositoryDiag getRepositoryDiag() {
		return repository.getRepositoryDiag();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.repo.api.RepositoryService#repositorySelfTest(com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void repositorySelfTest(OperationResult parentResult) {
		repository.repositorySelfTest(parentResult);
	}

    @Override
    public void testOrgClosureConsistency(boolean repairIfNecessary, OperationResult testResult) {
        repository.testOrgClosureConsistency(repairIfNecessary, testResult);
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
		return repository.isAnySubordinate(upperOrgOid, lowerObjectOids);
	}

	@Override
	public <O extends ObjectType> boolean isDescendant(PrismObject<O> object, String orgOid)
			throws SchemaException {
		return repository.isDescendant(object, orgOid);
	}

	@Override
	public <O extends ObjectType> boolean isAncestor(PrismObject<O> object, String oid)
			throws SchemaException {
		return repository.isAncestor(object, oid);
	}

	@Override
	public <O extends ObjectType> boolean selectorMatches(ObjectSelectorType objectSelector,
			PrismObject<O> object, ObjectFilterExpressionEvaluator filterEvaluator, Trace logger, String logMessagePrefix) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		return repository.selectorMatches(objectSelector, object, filterEvaluator, logger, logMessagePrefix);
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
		try {
			return repository.advanceSequence(oid, parentResult);
		} finally {
			invalidateCacheEntry(SequenceType.class, oid);
		}
	}

	@Override
	public void returnUnusedValuesToSequence(String oid, Collection<Long> unusedValues, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		try {
			repository.returnUnusedValuesToSequence(oid, unusedValues, parentResult);
		} finally {
			invalidateCacheEntry(SequenceType.class, oid);
		}
	}

	@Override
	public RepositoryQueryDiagResponse executeQueryDiagnostics(RepositoryQueryDiagRequest request, OperationResult result) {
		return repository.executeQueryDiagnostics(request, result);
	}

	@Override
	public QName getApproximateSupportedMatchingRule(Class<?> dataType, QName originalMatchingRule) {
		return repository.getApproximateSupportedMatchingRule(dataType, originalMatchingRule);
	}

	@Override
	public void applyFullTextSearchConfiguration(FullTextSearchConfigurationType fullTextSearch) {
		repository.applyFullTextSearchConfiguration(fullTextSearch);
	}

	@Override
	public FullTextSearchConfigurationType getFullTextSearchConfiguration() {
		return repository.getFullTextSearchConfiguration();
	}

	@Override
	public void postInit(OperationResult result) throws SchemaException {
		repository.postInit(result);
	}

	@Override
	public ConflictWatcher createAndRegisterConflictWatcher(String oid) {
		return repository.createAndRegisterConflictWatcher(oid);
	}

	@Override
	public void unregisterConflictWatcher(ConflictWatcher watcher) {
		repository.unregisterConflictWatcher(watcher);
	}

	@Override
	public boolean hasConflict(ConflictWatcher watcher, OperationResult result) {
		return repository.hasConflict(watcher, result);
	}
}

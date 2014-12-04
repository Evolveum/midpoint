/*
 * Copyright (c) 2010-2013 Evolveum
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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.Duration;

import com.evolveum.midpoint.prism.PrismContext;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConcurrencyException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Read-through write-through per-session repository cache.
 * 
 * TODO
 * 
 * @author Radovan Semancik
 *
 */
public class RepositoryCache implements RepositoryService {

	private static ThreadLocal<Cache> cacheInstance = new ThreadLocal<>();
	private static ThreadLocal<Integer> cacheCount = new ThreadLocal<Integer>();
	
	private RepositoryService repository;
	
	private static final Trace LOGGER = TraceManager.getTrace(RepositoryCache.class);
	private PrismContext prismContext;

	public RepositoryCache(){
    }
	
//	public RepositoryCache(RepositoryService repository) {
//		setRepository(repository);
//	}
    
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
	
	public static boolean exists() {
		return cacheInstance.get()!=null;
	}
	
	public static void destroy() {
		Cache inst = cacheInstance.get();
		if (inst != null) {
			LOGGER.info("Cache: DESTROY for thread {}",Thread.currentThread().getName());
			cacheInstance.set(null);
		}
	}
	
	public static void enter() {
		Cache inst = cacheInstance.get();
		Integer count = cacheCount.get();
		LOGGER.trace("Cache: ENTER for thread {}, {}",Thread.currentThread().getName(), count);
		if (inst == null) {
			LOGGER.info("Cache: creating for thread {}",Thread.currentThread().getName());
			inst = new Cache();
			cacheInstance.set(inst);
		}
		if (count == null) {
			count = 0;
		}
		cacheCount.set(count + 1);
	}
	
	public static void exit() {
		Integer count = cacheCount.get();
		LOGGER.trace("Cache: EXIT for thread {}, {}",Thread.currentThread().getName(), count);
		if (count == null || count == 0) {
			LOGGER.error("Cache: Attempt to exit cache with count {}",count);
		} else {
			cacheCount.set(count - 1);
			if (count <= 1) {
				destroy();
			}
		}
	}
	
	public static String debugDump() {
		StringBuilder sb = new StringBuilder("Cache ");
		if (exists()) {
			sb.append("exists ");
		} else {
			sb.append("doesn't exist ");
		}
		sb.append(", count ");
		if (cacheCount.get() == null) {
			sb.append("null");
		} else {
			sb.append(cacheCount.get());
		}
		sb.append(", ");
		if (cacheInstance.get() == null) {
			sb.append("null content");
		} else {
			sb.append(cacheInstance.get().description());
		}
		return sb.toString();
	}

	@Override
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		if (!isCacheable(type) || options != null) {
			LOGGER.info("Cache: PASS {} ({})", oid, type.getSimpleName());
			return repository.getObject(type, oid, options, parentResult);
		}
		Cache cache = getCache();
		if (cache == null) {
			LOGGER.info("Cache: NULL {} ({})", oid, type.getSimpleName());
		} else {
			PrismObject<T> object = (PrismObject) cache.getObject(oid);
			if (object != null) {
				// TODO: result?
				LOGGER.info("Cache: HIT {} ({})", oid, type.getSimpleName());
				return object.clone();
			}
			LOGGER.info("Cache: MISS {} ({})", oid, type.getSimpleName());
		}
		PrismObject<T> object = repository.getObject(type, oid, null, parentResult);
		cacheObject(cache, object);
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
	
	@Override
	public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query, 
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
		if (!isCacheable(type) || options != null) {
			LOGGER.info("Cache: PASS ({})", type.getSimpleName());
			return repository.searchObjects(type, query, options, parentResult);
		}
		Cache cache = getCache();
		if (cache == null) {
			LOGGER.info("Cache: NULL ({})", type.getSimpleName());
		} else {
			SearchResultList queryResult = cache.getQueryResult(type, query, prismContext);
			if (queryResult != null) {
				LOGGER.info("Cache: HIT {} ({})", query, type.getSimpleName());
				return queryResult.clone();
			}
			LOGGER.info("Cache: MISS {} ({})", query, type.getSimpleName());
		}

		// Cannot satisfy from cache, pass down to repository
		SearchResultList<PrismObject<T>> objects = repository.searchObjects(type, query, options, parentResult);
		if (cache != null && options == null) {
			for (PrismObject<T> object : objects) {
				cacheObject(cache, object);
			}
			cache.putQueryResult(type, query, objects, prismContext);
		}
		return objects;
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.repo.api.RepositoryService#searchObjectsIterative(java.lang.Class, com.evolveum.midpoint.prism.query.ObjectQuery, com.evolveum.midpoint.schema.ResultHandler, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query,
			final ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
		// TODO use cached query result if applicable
		LOGGER.info("Cache: PASS searchObjectsIterative ({})", type.getSimpleName());
		final Cache cache = getCache();
		ResultHandler<T> myHandler = new ResultHandler<T>() {
			@Override
			public boolean handle(PrismObject<T> object, OperationResult parentResult) {
				cacheObject(cache, object);
				return handler.handle(object, parentResult);
			}
		};
		return repository.searchObjectsIterative(type, query, myHandler, options, parentResult);
	}
	
	@Override
	public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, OperationResult parentResult)
			throws SchemaException {
		// TODO use cached query result if applicable
		LOGGER.info("Cache: PASS countObjects ({})", type.getSimpleName());
		return repository.countObjects(type, query, parentResult);
	}

	@Override
	public <T extends ObjectType> void modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		repository.modifyObject(type, oid, modifications, parentResult);
		// this changes the object. We are too lazy to apply changes ourselves, so just invalidate
		// the object in cache
		Cache cache = getCache();
		if (cache != null) {
			cache.removeObject(oid);
			cache.clearQueryResults(type);
		}
	}

	@Override
	public <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult parentResult)
			throws ObjectNotFoundException {
		repository.deleteObject(type, oid, parentResult);
		Cache cache = getCache();
		if (cache != null) {
			cache.removeObject(oid);
			cache.clearQueryResults(type);
		}
	}
	
	@Override
	public <F extends FocusType> PrismObject<F> searchShadowOwner(
			String shadowOid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws ObjectNotFoundException {
		// TODO cache?
		return repository.searchShadowOwner(shadowOid, options, parentResult);
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
			LOGGER.info("Cache: PASS {} ({})", oid, type.getSimpleName());
			return repository.getVersion(type, oid, parentResult);
		}
		Cache cache = getCache();
		if (cache == null) {
			LOGGER.info("Cache: NULL {} ({})", oid, type.getSimpleName());
		} else {
			String version = cache.getObjectVersion(oid);
			if (version != null) {
				LOGGER.info("Cache: HIT {} ({})", oid, type.getSimpleName());
				return version;
			}
			LOGGER.info("Cache: MISS {} ({})", oid, type.getSimpleName());
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

    private <T extends ObjectType> void cacheObject(Cache cache, PrismObject<T> object) {
		if (cache != null) {
			cache.putObject(object.getOid(), (PrismObject<ObjectType>) object.clone());
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
}

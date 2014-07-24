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

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.ResultHandler;
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

	private static ThreadLocal<Map<String,PrismObject<ObjectType>>> cacheInstance =
		new ThreadLocal<Map<String,PrismObject<ObjectType>>>();
	private static ThreadLocal<Integer> cacheCount = new ThreadLocal<Integer>();
	
	private RepositoryService repository;
	
	private static final Trace LOGGER = TraceManager.getTrace(RepositoryCache.class);
    
    public RepositoryCache(){        
    }
	
	public RepositoryCache(RepositoryService repository) {
		setRepository(repository);
	}
    
    public void setRepository(RepositoryService service) {
        Validate.notNull(service, "Repository service must not be null.");
        this.repository = service;
    }
	
	private static Map<String,PrismObject<ObjectType>> getCache() {
		return cacheInstance.get();
	}
	
	public static void init() {
	}
	
	public static boolean exists() {
		return cacheInstance.get()!=null;
	}
	
	public static void destroy() {
		Map<String,PrismObject<ObjectType>> inst = cacheInstance.get();
		if (inst != null) {
			LOGGER.trace("Cache: DESTROY for thread {}",Thread.currentThread().getName());
			cacheInstance.set(null);
		}
	}
	
	public static void enter() {
		Map<String,PrismObject<ObjectType>> inst = cacheInstance.get();
		Integer count = cacheCount.get();
		LOGGER.trace("Cache: ENTER for thread {}, {}",Thread.currentThread().getName(), count);
		if (inst == null) {
			LOGGER.trace("Cache: creating for thread {}",Thread.currentThread().getName());
			inst = new HashMap<String, PrismObject<ObjectType>>();
			cacheInstance.set(inst);
		}
		if (count == null) {
			count = 0;
		}
		cacheCount.set(count+1);
	}
	
	public static void exit() {
		Integer count = cacheCount.get();
		LOGGER.trace("Cache: EXIT for thread {}, {}",Thread.currentThread().getName(), count);
		if (count == null || count == 0) {
			LOGGER.error("Cache: Attempt to exit cache with count {}",count);
		} else {
			cacheCount.set(count-1);
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
			sb.append(cacheInstance.get().entrySet().size());
			sb.append(" entries");
		}
		return sb.toString();
	}

	@Override
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		if (!isCacheable(type) || options != null) {
			LOGGER.trace("Cache: PASS {} ({})", oid, type.getSimpleName());
			return repository.getObject(type, oid, options, parentResult);
		}
		Map<String, PrismObject<ObjectType>> cache = getCache();
		if (cache == null) {
			LOGGER.trace("Cache: NULL {} ({})", oid, type.getSimpleName());
		} else {
			if (cache.containsKey(oid)) {
				// TODO: result?
				LOGGER.trace("Cache: HIT {} ({})", oid, type.getSimpleName());
				return ((PrismObject<T>) cache.get(oid)).clone();
			}
			LOGGER.trace("Cache: MISS {} ({})", oid, type.getSimpleName());
		}
		PrismObject<T> object = repository.getObject(type, oid, null, parentResult);
		cacheObject(cache, object);
		return object;
	}

	private boolean isCacheable(Class<?> type) {
		if (type.equals(TaskType.class)) {
			return false;
		}
		if (ShadowType.class.isAssignableFrom(type)) {
			return false;
		}
		return true;
	}

	@Override
	public <T extends ObjectType> String addObject(PrismObject<T> object, RepoAddOptions options, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException {
		String oid = repository.addObject(object, options, parentResult);
		Map<String, PrismObject<ObjectType>> cache = getCache();
		// DON't cache the object here. The object may not have proper "JAXB" form, e.g. some pieces may be
		// DOM element instead of JAXB elements. Not to cache it is safer and the performance loss
		// is acceptable.
		if (cache != null) {
			// Invalidate the cache entry if it happens to be there
			cache.remove(oid);
		}
		return oid;
	}
	
	@Override
	public <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query, 
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
		// Cannot satisfy from cache, pass down to repository
		List<PrismObject<T>> objects = repository.searchObjects(type, query, options, parentResult);
		Map<String, PrismObject<ObjectType>> cache = getCache();
		if (cache != null && options == null) {
			for (PrismObject<T> object : objects) {
				cacheObject(cache, object);
			}
		}
		return objects;
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.repo.api.RepositoryService#searchObjectsIterative(java.lang.Class, com.evolveum.midpoint.prism.query.ObjectQuery, com.evolveum.midpoint.schema.ResultHandler, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <T extends ObjectType> void searchObjectsIterative(Class<T> type, ObjectQuery query,
			final ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
		final Map<String, PrismObject<ObjectType>> cache = getCache();
		ResultHandler<T> myHandler = new ResultHandler<T>() {
			@Override
			public boolean handle(PrismObject<T> object, OperationResult parentResult) {
				cacheObject(cache, object);
				return handler.handle(object, parentResult);
			}
		};
		repository.searchObjectsIterative(type, query, myHandler, options, parentResult);
	}
	
	@Override
	public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, OperationResult parentResult)
			throws SchemaException {
		return repository.countObjects(type, query, parentResult);
	}

	@Override
	public <T extends ObjectType> void modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		repository.modifyObject(type, oid, modifications, parentResult);
		// this changes the object. We are too lazy to apply changes ourselves, so just invalidate
		// the object in cache
		Map<String, PrismObject<ObjectType>> cache = getCache();
		if (cache != null) {
			cache.remove(oid);
		}
	}

	@Override
	public <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult parentResult)
			throws ObjectNotFoundException {
		repository.deleteObject(type, oid, parentResult);
		Map<String, PrismObject<ObjectType>> cache = getCache();
		if (cache != null) {
			cache.remove(oid);
		}
	}
	
	@Override
	public <F extends FocusType> PrismObject<F> searchShadowOwner(
			String shadowOid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws ObjectNotFoundException {
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
		return repository.getVersion(type, oid, parentResult);
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
	
	private <T extends ObjectType> void cacheObject(Map<String, PrismObject<ObjectType>> cache, PrismObject<T> object) {
		if (cache != null) {
			cache.put(object.getOid(), (PrismObject<ObjectType>) object.clone());
		}
	}

	@Override
	public boolean isAnySubordinate(String upperOrgOid, Collection<String> lowerObjectOids)
			throws SchemaException {
		return repository.isAnySubordinate(upperOrgOid, lowerObjectOids);
	}
}

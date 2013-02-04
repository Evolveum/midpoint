/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.repo.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConcurrencyException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

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
		// Don't instantiate unless the count in non-zero.
		// Otherwise side-effects may happen (e.g. exit() never
		// called will mean that the cache is not destroyed)
		Integer count = cacheCount.get();
		if (count == null || count == 0) {
			return null;
		}
		Map<String,PrismObject<ObjectType>> inst = cacheInstance.get();
		if (inst == null) {
			LOGGER.trace("Cache: creating for thread {}",Thread.currentThread().getName());
			inst = new HashMap<String, PrismObject<ObjectType>>();
			cacheInstance.set(inst);
		}
		return inst;
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
	
	public static String dump() {
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
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		if (!isCacheable(type)) {
			LOGGER.trace("Cache: PASS {} ({})", oid, type.getSimpleName());
			return repository.getObject(type, oid, parentResult);
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
		PrismObject<T> object = repository.getObject(type, oid, parentResult);
		if (cache != null) {
			cache.put(oid, ((PrismObject<ObjectType>) object).clone());
		}
		return object;
	}

	private boolean isCacheable(Class<?> type) {
		if (type.equals(TaskType.class)) {
			return false;
		}
		if (ResourceObjectShadowType.class.isAssignableFrom(type)) {
			return false;
		}
		return true;
	}

	@Override
	public <T extends ObjectType> String addObject(PrismObject<T> object, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException {
		String oid = repository.addObject(object, parentResult);
		// DON't cache it here. The object may not have proper "JAXB" form, e.g. some pieces may be
		// DOM element instead of JAXB elements. Not to cache it is safer and the performance loss
		// is acceptable.
		//getCache().put(oid,object);
		return oid;
	}

//	@Override
//	public <T extends ObjectType> List<PrismObject<T>> listObjects(Class<T> type, PagingType paging,
//			OperationResult parentResult) {
//		// Cannot satisfy from cache, pass down to repository
//		List<PrismObject<T>> objects = repository.listObjects(type, paging, parentResult);
//		Map<String, PrismObject<ObjectType>> cache = getCache();
//		if (cache != null) {
//			for (PrismObject<T> object : objects) {
//				cache.put(object.getOid(), (PrismObject<ObjectType>) object);
//			}
//		}
//		return objects;
//	}

	
	@Override
	public <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query, OperationResult parentResult) throws SchemaException {
		// Cannot satisfy from cache, pass down to repository
		List<PrismObject<T>> objects = repository.searchObjects(type, query, parentResult);
		Map<String, PrismObject<ObjectType>> cache = getCache();
		if (cache != null) {
			for (PrismObject<T> object : objects) {
				cache.put(object.getOid(), (PrismObject<ObjectType>) object);
			}
		}
		return objects;
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
	public PrismObject<UserType> listAccountShadowOwner(String accountOid, OperationResult parentResult)
			throws ObjectNotFoundException {
		return repository.listAccountShadowOwner(accountOid, parentResult);
	}

	@Override
	public <T extends ResourceObjectShadowType> List<PrismObject<T>> listResourceObjectShadows(String resourceOid,
			Class<T> resourceObjectShadowType, OperationResult parentResult) throws ObjectNotFoundException {
		return repository.listResourceObjectShadows(resourceOid, resourceObjectShadowType, parentResult);
	}

	@Override
	public void claimTask(String oid, OperationResult parentResult) throws ObjectNotFoundException,
			ConcurrencyException, SchemaException {
		repository.claimTask(oid, parentResult);
		Map<String, PrismObject<ObjectType>> cache = getCache();
		if (cache != null) {
			cache.remove(oid);
		}
	}

	@Override
	public void releaseTask(String oid, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException {
		repository.releaseTask(oid, parentResult);
		Map<String, PrismObject<ObjectType>> cache = getCache();
		if (cache != null) {
			cache.remove(oid);
		}
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
}

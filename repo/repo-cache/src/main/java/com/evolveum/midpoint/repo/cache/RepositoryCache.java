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

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.exception.ConcurrencyException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * Read-through write-through per-session repository cache.
 * 
 * TODO
 * 
 * @author Radovan Semancik
 *
 */
public class RepositoryCache implements RepositoryService {

	private static ThreadLocal<Map<String,ObjectType>> cacheInstance;
	
	private RepositoryService repository;
	
	public RepositoryCache(RepositoryService repository) {
		super();
		this.repository = repository;
	}
	
	private static Map<String,ObjectType> getCache() {
		Map<String,ObjectType> inst = cacheInstance.get();
		if (inst == null) {
			inst = new HashMap<String, ObjectType>();
			cacheInstance.set(inst);
		}
		return inst;
	}
	
	public static void init() {
	}
	
	public static void destroy() {
		Map<String,ObjectType> inst = cacheInstance.get();
		if (inst != null) {
			cacheInstance.set(null);
		}
	}

	@Override
	public <T extends ObjectType> T getObject(Class<T> type, String oid, PropertyReferenceListType resolve,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		Map<String, ObjectType> cache = getCache();
		if (getCache().containsKey(oid)) {
			// TODO: result?
			return (T) cache.get(oid);
		}
		T object = repository.getObject(type, oid, resolve, parentResult);
		cache.put(oid, object);
		return object;
	}

	@Override
	public <T extends ObjectType> String addObject(T object, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException {
		String oid = repository.addObject(object, parentResult);
		getCache().put(oid,object);
		return oid;
	}

	@Override
	public <T extends ObjectType> ResultList<T> listObjects(Class<T> type, PagingType paging,
			OperationResult parentResult) {
		// Cannot satisfy from cache, pass down to repository
		ResultList<T> objects = repository.listObjects(type, paging, parentResult);
		for (ObjectType object : objects) {
			getCache().put(object.getOid(), object);
		}
		return objects;
	}

	@Override
	public <T extends ObjectType> ResultList<T> searchObjects(Class<T> type, QueryType query,
			PagingType paging, OperationResult parentResult) throws SchemaException {
		// Cannot satisfy from cache, pass down to repository
		ResultList<T> objects = repository.searchObjects(type, query, paging, parentResult);
		for (ObjectType object : objects) {
			getCache().put(object.getOid(), object);
		}
		return objects;
	}

	@Override
	public <T extends ObjectType> void modifyObject(Class<T> type, ObjectModificationType objectChange,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		repository.modifyObject(type, objectChange, parentResult);
		// this changes the object. We are too lazy to apply changes ourselves, so just invalidate
		// the object in cache
		getCache().remove(objectChange.getOid());
	}

	@Override
	public <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult parentResult)
			throws ObjectNotFoundException {
		repository.deleteObject(type, oid, parentResult);
		getCache().remove(oid);
	}

	@Override
	public <T extends ObjectType> PropertyAvailableValuesListType getPropertyAvailableValues(Class<T> type,
			String oid, PropertyReferenceListType properties, OperationResult parentResult)
			throws ObjectNotFoundException {
		return repository.getPropertyAvailableValues(type, oid, properties, parentResult);
	}

	@Override
	public UserType listAccountShadowOwner(String accountOid, OperationResult parentResult)
			throws ObjectNotFoundException {
		return repository.listAccountShadowOwner(accountOid, parentResult);
	}

	@Override
	public <T extends ResourceObjectShadowType> ResultList<T> listResourceObjectShadows(String resourceOid,
			Class<T> resourceObjectShadowType, OperationResult parentResult) throws ObjectNotFoundException {
		return repository.listResourceObjectShadows(resourceOid, resourceObjectShadowType, parentResult);
	}

	@Override
	public void claimTask(String oid, OperationResult parentResult) throws ObjectNotFoundException,
			ConcurrencyException, SchemaException {
		repository.claimTask(oid, parentResult);
		getCache().remove(oid);
	}

	@Override
	public void releaseTask(String oid, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException {
		repository.releaseTask(oid, parentResult);
		getCache().remove(oid);
	}
}

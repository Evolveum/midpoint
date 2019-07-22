/**
 * Copyright (c) 2017-2018 Evolveum
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
package com.evolveum.midpoint.repo.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collection;

/**
 * This is only used in tests. But due to complicated dependencies this is
 * part of main code. That does not hurt much.
 * 
 * @author Radovan Semancik
 *
 */
public class RepoObjectResolver implements ObjectResolver {

	@Autowired(required = true)
	private transient PrismContext prismContext;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;

		
	@Override
	public <O extends ObjectType> void searchIterative(Class<O> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<O> handler, Task task,
			OperationResult parentResult)
			throws SchemaException {
		cacheRepositoryService.searchObjectsIterative(type, query, handler, options, true, parentResult);
	}

	@Override
	public <O extends ObjectType> SearchResultList<PrismObject<O>> searchObjects(Class<O> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
			throws SchemaException {
		return cacheRepositoryService.searchObjects(type, query, options, parentResult);
	}

	@Override
	public <O extends ObjectType> O resolve(ObjectReferenceType ref, Class<O> expectedType,
			Collection<SelectorOptions<GetOperationOptions>> options, String contextDescription, Task task,
			OperationResult result) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <O extends ObjectType> O getObject(Class<O> expectedType, String oid,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		return cacheRepositoryService.getObject(expectedType, oid, options, parentResult).asObjectable();
	}

	

}

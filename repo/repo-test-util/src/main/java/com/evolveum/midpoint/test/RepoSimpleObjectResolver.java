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
package com.evolveum.midpoint.test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SimpleObjectResolver;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collection;

/**
 * @author semancik
 *
 */
public class RepoSimpleObjectResolver implements SimpleObjectResolver {

	@Autowired(required = true)
	private transient PrismContext prismContext;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;

	
	@Override
	public <O extends ObjectType> PrismObject<O> getObject(Class<O> expectedType, String oid,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		return cacheRepositoryService.getObject(expectedType, oid, options, parentResult);
	}
	
	public <O extends ObjectType> void searchIterative(Class<O> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<O> handler, Object task,
			OperationResult parentResult)
			throws SchemaException {
		cacheRepositoryService.searchObjectsIterative(type, query, handler, options, true, parentResult);
	}

	

}

/**
 * Copyright (c) 2017 Evolveum
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

import java.util.Collection;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class RepoObjectResolver implements ObjectResolver {

	@Autowired(required = true)
	private transient PrismContext prismContext;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;

	@Override
	public <O extends ObjectType> O resolve(ObjectReferenceType ref, Class<O> expectedType,
			Collection<SelectorOptions<GetOperationOptions>> options, String contextDescription, Object task,
			OperationResult result) throws ObjectNotFoundException, SchemaException {
		String oid = ref.getOid();
		Class<?> typeClass = null;
		QName typeQName = ref.getType();
		if (typeQName != null) {
			typeClass = prismContext.getSchemaRegistry().determineCompileTimeClass(typeQName);
		}
		if (typeClass != null && expectedType.isAssignableFrom(typeClass)) {
			expectedType = (Class<O>) typeClass;
		}
		try {
			return cacheRepositoryService.getObject(expectedType, oid, options, result).asObjectable();
		} catch (SystemException ex) {
			throw ex;
		} catch (ObjectNotFoundException ex) {
			throw ex;
		} catch (CommonException ex) {
			throw new SystemException("Error resolving object with oid '" + oid + "': "+ex.getMessage(), ex);
		}
	}

	@Override
	public <O extends ObjectType> void searchIterative(Class<O> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<O> handler, Object task,
			OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException, ExpressionEvaluationException {

		cacheRepositoryService.searchObjectsIterative(type, query, handler, options, false, parentResult);

	}

}

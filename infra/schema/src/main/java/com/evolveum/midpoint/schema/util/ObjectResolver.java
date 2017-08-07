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
package com.evolveum.midpoint.schema.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * The callback from some of the object utilities to resolve objects.
 * 
 * The classes implementing this will most likely fetch the objects from the
 * repository or from some kind of object cache.
 * 
 * @author Radovan Semancik
 */
public interface ObjectResolver {
	
	/**
	 * Resolve the provided reference to object (ObjectType).
	 * 
	 * Note: The reference is used instead of just OID because the reference
	 * also contains object type. This speeds up the repository operations.
	 * 
	 * @param ref object reference to resolve
	 * @param contextDescription short description of the context of resolution, e.g. "executing expression FOO". Used in error messages.
	 * @param task
	 * @return resolved object
	 * @throws ObjectNotFoundException
	 *             requested object does not exist
	 * @throws SchemaException
	 *             error dealing with storage schema
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 *
	 * TODO resolve module dependencies to allow task to be of type Task
	 */
	<T extends ObjectType> T resolve(ObjectReferenceType ref, Class<T> expectedType, Collection<SelectorOptions<GetOperationOptions>> options,
			String contextDescription, Object task, OperationResult result)
			throws ObjectNotFoundException, SchemaException;
	
	<O extends ObjectType> void searchIterative(Class<O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<O> handler, Object task, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

	// EXPERIMENTAL (implemented only for ModelObjectResolver)
	// TODO clean up this mess
	default void resolveAllReferences(Collection<PrismContainerValue> pcvs, Object taskObject, OperationResult result) {
		throw new UnsupportedOperationException();
	}

	interface Session {
		GetOperationOptions getOptions();
		boolean contains(String oid);
		void put(String oid, PrismObject<?> object);
		PrismObject<?> get(String oid);
	}

	default Session openResolutionSession(GetOperationOptions options) {
		return new Session() {
			private Map<String, PrismObject<?>> objects = new HashMap<>();

			@Override
			public GetOperationOptions getOptions() {
				return options;
			}

			@Override
			public boolean contains(String oid) {
				return objects.containsKey(oid);
			}

			@Override
			public void put(String oid, PrismObject<?> object) {
				objects.put(oid, object);
			}

			@Override
			public PrismObject<?> get(String oid) {
				return objects.get(oid);
			}
		};
	}

	default void resolveReference(PrismReferenceValue ref, String contextDescription,
			Session session, Object task, OperationResult result) {
		throw new UnsupportedOperationException();
	}

}

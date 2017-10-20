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
package com.evolveum.midpoint.model.common.stringpolicy;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyOriginType;

/**
 * @author semancik
 *
 */
public abstract class AbstractValuePolicyOriginResolver<O extends ObjectType> {
	
	private final PrismObject<O> object;
	private final ObjectResolver objectResolver;
	
	public AbstractValuePolicyOriginResolver(PrismObject<O> object, ObjectResolver objectResolver) {
		super();
		this.object = object;
		this.objectResolver = objectResolver;
	}

	public PrismObject<O> getObject() {
		return object;
	}
	
	public abstract ObjectQuery getOwnerQuery();
	
	public <R extends ObjectType> Class<R> getOwnerClass() {
		return (Class<R>) UserType.class;
	}

	// TODO: later maybe isolate this method to an interface (ValuePolicyTypeResolver)
	public <R extends ObjectType> void resolve(ResultHandler<R> handler, ValuePolicyOriginType originType, String contextDescription, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		if (originType == null) {
			handleObject(handler, result);
		}
		switch (originType) {
			case OBJECT:
				handleObject(handler, result);
				break;
			case OWNER:
				handleOwner(handler, contextDescription, result);
				break;
			case PERSONA:
				handlePersonas(handler, contextDescription, task, result);
				break;
			default:
				throw new IllegalArgumentException("Unexpected origin type "+originType);
		}
	}
	
	private <R extends ObjectType> void handleObject(ResultHandler<R> handler, OperationResult result) {
		handler.handle((PrismObject<R>) getObject(), result);
	}

	private <P extends ObjectType> void handlePersonas(ResultHandler<P> handler, String contextDescription, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<O> object = getObject();
		if (!object.canRepresent(UserType.class)) {
			return;
		}
		for (ObjectReferenceType personaRef: ((UserType)object.asObjectable()).getPersonaRef()) {
			UserType persona = objectResolver.resolve(personaRef, UserType.class, SelectorOptions.createCollection(GetOperationOptions.createReadOnly()), "resolving persona in " + contextDescription, task, result);
			handler.handle((PrismObject<P>) persona.asPrismObject(), result);
		}
	}

	private <P extends ObjectType> void handleOwner(ResultHandler<P> handler, String contextDescription, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		objectResolver.searchIterative(getOwnerClass(), getOwnerQuery(), SelectorOptions.createCollection(GetOperationOptions.createReadOnly()), 
				handler, "resolving owner in " + contextDescription, result);
	}
}

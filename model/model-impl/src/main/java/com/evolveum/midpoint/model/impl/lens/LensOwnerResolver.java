/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class LensOwnerResolver<F extends ObjectType> implements OwnerResolver {
	
	private static final Trace LOGGER = TraceManager.getTrace(LensOwnerResolver.class);
	
	private LensContext<F> context;
	private ObjectResolver objectResolver;
	private Task task;
	private OperationResult result;
	
	public LensOwnerResolver(LensContext<F> context, ObjectResolver objectResolver, Task task,
			OperationResult result) {
		super();
		this.context = context;
		this.objectResolver = objectResolver;
		this.task = task;
		this.result = result;
	}

	@Override
	public <FO extends FocusType, O extends ObjectType> PrismObject<FO> resolveOwner(PrismObject<O> object) {
		if (object == null) {
			return null;
		}
		if (object.canRepresent(ShadowType.class)) {
			LensFocusContext<F> focusContext = (LensFocusContext<F>) context.getFocusContext();
			if (focusContext == null) {
				return null;
			} else if (focusContext.getObjectNew() != null) {
				// If we create both owner and shadow in the same operation (see e.g. MID-2027), we have to provide object new
				// Moreover, if the authorization would be based on a property that is being changed along with the
				// the change being authorized, we would like to use changed version.
				return (PrismObject<FO>) focusContext.getObjectNew();
			} else if (focusContext.getObjectCurrent() != null) {
				// This could be useful if the owner is being deleted.
				return (PrismObject<FO>) focusContext.getObjectCurrent();
			} else {
				return null;
			}
		} else if (object.canRepresent(AbstractRoleType.class)) {
			ObjectReferenceType ownerRef = ((AbstractRoleType)(object.asObjectable())).getOwnerRef();
			if (ownerRef == null) {
				return null;
			}
			try {
				ObjectType ownerType = objectResolver.resolve(ownerRef, ObjectType.class, null, "resolving owner of "+object, task, result);
				if (ownerType == null) {
					return null;
				}
				return (PrismObject<FO>) ownerType.asPrismObject();
			} catch (ObjectNotFoundException | SchemaException e) {
				LOGGER.error("Error resolving owner of {}: {}", object, e.getMessage(), e);
				return null;
			}
		} else {
			LOGGER.warn("Cannot resolve owner of {}, owners can be resolved only for Shadows and AbstractRoles", object);
			return null;
		}
	}

	
	

}

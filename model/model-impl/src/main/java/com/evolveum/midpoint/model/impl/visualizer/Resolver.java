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

package com.evolveum.midpoint.model.impl.visualizer;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetch;
import static com.evolveum.midpoint.schema.SelectorOptions.createCollection;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.*;

/**
 * Resolves definitions and old values.
 * Currently NOT references.
 *
 * @author mederly
 */
@Component
public class Resolver {

	private static final Trace LOGGER = TraceManager.getTrace(Resolver.class);

	public static final String CLASS_DOT = Resolver.class.getName() + ".";
	private static final String OP_RESOLVE = CLASS_DOT + "resolve";

	@Autowired
	private PrismContext prismContext;

	@Autowired
	private ModelService modelService;

	@Autowired
	private ProvisioningService provisioningService;

	@Autowired
	private Visualizer visualizer;

	public <O extends ObjectType> void resolve(PrismObject<O> object, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
		/*if (object.getDefinition() == null) */{
			if (object == null) {
				return;
			}
			Class<O> clazz = object.getCompileTimeClass();
			if (clazz == null) {
				warn(result, "Compile time class for " + toShortString(object) + " is not known");
			} else {
				PrismObjectDefinition<O> def = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
				if (def != null) {
					if (ResourceType.class.isAssignableFrom(clazz) || ShadowType.class.isAssignableFrom(clazz)) {
						try {
							provisioningService.applyDefinition(object, task, result);
						} catch (ObjectNotFoundException|CommunicationException|ConfigurationException e) {
							LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply definition on {} -- continuing with no definition", e,
									ObjectTypeUtil.toShortString(object));
						}
					} else {
						object.applyDefinition(def);
					}
				} else {
					warn(result, "Definition for " + toShortString(object) + " couldn't be found");
				}
			}
		}
	}

	public <O extends ObjectType> void resolve(ObjectDelta<O> objectDelta, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
		if (objectDelta.isAdd()) {
			resolve(objectDelta.getObjectToAdd(), task, result);
		} else if (objectDelta.isDelete()) {
			// nothing to do
		} else {
			PrismObject<O> originalObject = null;
			boolean originalObjectFetched = false;
			final Class<O> clazz = objectDelta.getObjectTypeClass();
			boolean managedByProvisioning = ResourceType.class.isAssignableFrom(clazz) || ShadowType.class.isAssignableFrom(clazz);
			PrismObjectDefinition<O> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
			if (objectDefinition == null) {
				warn(result, "Definition for " + clazz + " couldn't be found");
			} else {
				if (managedByProvisioning) {
					try {
						provisioningService.applyDefinition(objectDelta, task, result);
					} catch (ObjectNotFoundException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
						LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply definition on {} -- continuing with no definition", e, objectDelta);
					}
				}
			}
			for (ItemDelta itemDelta : objectDelta.getModifications()) {
				if (objectDefinition != null && !managedByProvisioning) {
					ItemDefinition<?> def = objectDefinition.findItemDefinition(itemDelta.getPath());
					if (def != null) {
						itemDelta.applyDefinition(def);
					}
				}
				if (itemDelta.getEstimatedOldValues() == null) {
					final String oid = objectDelta.getOid();
					if (!originalObjectFetched && oid != null) {
						try {
							originalObject = modelService.getObject(clazz, oid, createCollection(createNoFetch()), task, result);
						} catch (ObjectNotFoundException e) {
							result.recordHandledError(e);
							LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Object {} does not exist", e, oid);
						} catch (RuntimeException | SchemaException | ConfigurationException | CommunicationException | SecurityViolationException e) {
							LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resolve object {}", e, oid);
							warn(result, "Couldn't resolve object " + oid + ": " + e.getMessage(), e);
						}
						originalObjectFetched = true;
					}
					if (originalObject != null) {
						Item<?,?> originalItem = originalObject.findItem(itemDelta.getPath());
						if (originalItem != null) {
							itemDelta.setEstimatedOldValues(new ArrayList(originalItem.getValues()));
						}
					}
				}
			}
		}
	}

	private void warn(OperationResult result, String text, Exception e) {
		result.createSubresult(OP_RESOLVE).recordWarning(text, e);
	}

	private void warn(OperationResult result, String text) {
		result.createSubresult(OP_RESOLVE).recordWarning(text);
	}

	// TODO caching retrieved objects
	public void resolve(List<ObjectDelta<? extends ObjectType>> deltas, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
		for (ObjectDelta<? extends ObjectType> delta : deltas) {
			resolve(delta, task, result);
		}
	}
}

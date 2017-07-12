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

package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import java.util.Collections;

/**
 * TODO find appropriate place for this class
 *
 * @author mederly
 */
public class ResourceUtils {

	public static void deleteSchema(PrismObject<ResourceType> resource, ModelService modelService, PrismContext prismContext, Task task, OperationResult parentResult)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
			ConfigurationException, PolicyViolationException, SecurityViolationException {

		PrismContainer<XmlSchemaType> schemaContainer = resource.findContainer(ResourceType.F_SCHEMA);
		if (schemaContainer != null && schemaContainer.getValue() != null) {
			PrismProperty<SchemaDefinitionType> definitionProperty = schemaContainer.findProperty(XmlSchemaType.F_DEFINITION);
			if (definitionProperty != null && !definitionProperty.isEmpty()) {
				PrismPropertyValue<SchemaDefinitionType> definitionValue = definitionProperty.getValue().clone();
				ObjectDelta<ResourceType> deleteSchemaDefinitionDelta = ObjectDelta
						.createModificationDeleteProperty(ResourceType.class,
								resource.getOid(),
								new ItemPath(ResourceType.F_SCHEMA, XmlSchemaType.F_DEFINITION),
								prismContext, definitionValue.getValue());		// TODO ...or replace with null?
				// delete schema
				modelService.executeChanges(
						Collections.<ObjectDelta<? extends ObjectType>>singleton(deleteSchemaDefinitionDelta), null, task,
						parentResult);

			}
		}
	}

}

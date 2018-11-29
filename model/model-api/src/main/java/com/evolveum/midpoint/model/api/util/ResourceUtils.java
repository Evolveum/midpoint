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
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import java.util.Collections;

import static java.util.Collections.singleton;

/**
 * TODO find appropriate place for this class
 *
 * @author mederly
 */
public class ResourceUtils {

	private static final ItemPath SCHEMA_PATH = ItemPath.create(ResourceType.F_SCHEMA, XmlSchemaType.F_DEFINITION);

	public static void deleteSchema(PrismObject<ResourceType> resource, ModelService modelService, PrismContext prismContext, Task task, OperationResult parentResult)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
			ConfigurationException, PolicyViolationException, SecurityViolationException {
		PrismProperty<SchemaDefinitionType> definition = resource.findProperty(SCHEMA_PATH);
		if (definition != null && !definition.isEmpty()) {
			ObjectDelta<ResourceType> delta = DeltaBuilder.deltaFor(ResourceType.class, prismContext)
					.item(SCHEMA_PATH).replace()
					.asObjectDelta(resource.getOid());
			modelService.executeChanges(singleton(delta), null, task, parentResult);
		}
	}
}

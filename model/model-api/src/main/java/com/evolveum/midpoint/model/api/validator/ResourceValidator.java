/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.model.api.validator;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.jetbrains.annotations.NotNull;

/**
 * EXPERIMENTAL
 *
 * TODO make interface generic and integrate it into model API
 *
 * @author mederly
 */
public interface ResourceValidator {

	String CAT_BASIC = "basic";
	String CAT_CONFIGURATION = "configuration";
	String CAT_SCHEMA_HANDLING = "schemaHandling";
	String CAT_SYNCHRONIZATION = "synchronization";
	String CAT_CAPABILITIES = "capabilities";

	String C_MISSING_OBJECT_CLASS = "missingObjectClass";
	String C_MULTIPLE_SCHEMA_HANDLING_DEFINITIONS = "multipleSchemaHandlingDefinitions";
	String C_MULTIPLE_SYNCHRONIZATION_DEFINITIONS = "multipleSynchronizationDefinitions";

	@NotNull
	ValidationResult validate(@NotNull PrismObject<ResourceType> resourceObject, @NotNull Scope scope, @NotNull Task task,
			@NotNull OperationResult result);
}

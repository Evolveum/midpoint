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

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import java.util.Collection;

/**
 * EXPERIMENTAL
 *
 * @author mederly
 */
public interface DataModelVisualizer {

	enum Target {
		DOT, CYTOSCAPE
	}

	String visualize(Collection<String> resourceOids, Target target, Task task, OperationResult result)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException;

	String visualize(ResourceType resource, Target target, Task task, OperationResult result)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException;
}

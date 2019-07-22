/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.CompiledTracingProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingProfileType;

/**
 *
 */
public interface Tracer {

	/**
	 * Stores trace to persistent storage (usually a file in "trace" directory).
	 *
	 * @param task Task containing the context information necessary e.g. to derive name of the trace file.
	 * @param result Result that is to be serialized and stored.
	 */
	void storeTrace(Task task, OperationResult result);

	/**
	 * Resolves a tracing profile - i.e. replaces references to other (named) profiles with their content.
	 *
	 * @throws SchemaException If the profile name cannot be resolved e.g. if the referenced profile does not exist
	 *                         or the name in ambiguous.
	 */
	TracingProfileType resolve(TracingProfileType tracingProfile, OperationResult result) throws SchemaException;

	TracingProfileType getDefaultProfile();

	CompiledTracingProfile compileProfile(TracingProfileType profile, OperationResult result) throws SchemaException;

	//TracingLevelType getLevel(@NotNull TracingProfileType resolvedProfile, @NotNull Class<TraceType> traceClass);
}

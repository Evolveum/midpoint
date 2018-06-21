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
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ProjectionMappingLoader<F extends ObjectType> implements MappingLoader<ShadowType> {
	
	private static final Trace LOGGER = TraceManager.getTrace(ProjectionMappingLoader.class);

	private LensProjectionContext projectionContext;
	private ContextLoader contextLoader;
	private LensContext<F> context;

	public ProjectionMappingLoader(LensContext<F> context, LensProjectionContext projectionContext, ContextLoader contextLoader) {
		super();
		this.context = context;
		this.projectionContext = projectionContext;
		this.contextLoader = contextLoader;
	}

	@Override
	public boolean isLoaded() {
		return projectionContext.hasFullShadow();
	}

	@Override
	public PrismObject load(String loadReason, Task task, OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		contextLoader.loadFullShadow(context, projectionContext, loadReason, task, result);
		if (SynchronizationPolicyDecision.BROKEN.equals(projectionContext.getSynchronizationPolicyDecision())) {
			LOGGER.debug("Attempt to load full object for {} failed, projection context is broken", projectionContext.getHumanReadableName());
			throw new ObjectNotFoundException("Projection loading failed, projection broken");
		}
		if (projectionContext.isThombstone()) {
			LOGGER.debug("Projection {} got thombstoned", projectionContext.getHumanReadableName());
			throw new ObjectNotFoundException("Projection loading failed, projection thombstoned");
		}
		return projectionContext.getObjectCurrent();
	}

}

/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.ContextLoader;
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
	public PrismObject<ShadowType> load(String loadReason, Task task, OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		contextLoader.loadFullShadow(context, projectionContext, loadReason, task, result);
		if (SynchronizationPolicyDecision.BROKEN.equals(projectionContext.getSynchronizationPolicyDecision())) {
			LOGGER.debug("Attempt to load full object for {} failed, projection context is broken", projectionContext.getHumanReadableName());
			throw new ObjectNotFoundException("Projection loading failed, projection broken");
		}
		if (projectionContext.isTombstone()) {
			LOGGER.debug("Projection {} got tombstoned", projectionContext.getHumanReadableName());
			throw new ObjectNotFoundException("Projection loading failed, projection tombstoned");
		}
		return projectionContext.getObjectCurrent();
	}

}

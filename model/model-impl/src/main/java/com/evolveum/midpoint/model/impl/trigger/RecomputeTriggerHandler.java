/*
 * Copyright (c) 2013-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.trigger;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class RecomputeTriggerHandler implements TriggerHandler {
	
	public static final String HANDLER_URI = ModelConstants.NS_MODEL_TRIGGER_PREFIX + "/recompute/handler-3";
	
	private static final transient Trace LOGGER = TraceManager.getTrace(RecomputeTriggerHandler.class);

	@Autowired(required = true)
	private TriggerHandlerRegistry triggerHandlerRegistry;
	
	@Autowired(required = true)
    private Clockwork clockwork;
	
	@Autowired(required=true)
	private PrismContext prismContext;
	
	@Autowired(required = true)
    private ProvisioningService provisioningService;
	
	@Autowired(required = true)
	private ContextFactory contextFactory;
	
	@PostConstruct
	private void initialize() {
		triggerHandlerRegistry.register(HANDLER_URI, this);
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.trigger.TriggerHandler#handle(com.evolveum.midpoint.prism.PrismObject)
	 */
	@Override
	public <O extends ObjectType> void handle(PrismObject<O> object, TriggerType trigger, Task task, OperationResult result) {
		try {
			
			LOGGER.trace("Recomputing {}", object);
			// Reconcile option used for compatibility. TODO: do we need it?
			LensContext<UserType> lensContext = contextFactory.createRecomputeContext(object, ModelExecuteOptions.createReconcile(), task, result);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Recomputing of {}: context:\n{}", object, lensContext.debugDump());
			}
			clockwork.run(lensContext, task, result);
			LOGGER.trace("Recomputing of {}: {}", object, result.getStatus());
			
		} catch (SchemaException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (ObjectNotFoundException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (ExpressionEvaluationException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (CommunicationException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (ObjectAlreadyExistsException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (ConfigurationException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (PolicyViolationException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (SecurityViolationException e) {
			LOGGER.error(e.getMessage(), e);
		}

	}

}

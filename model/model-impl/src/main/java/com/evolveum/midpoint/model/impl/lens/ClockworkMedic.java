/**
 * Copyright (c) 2018 Evolveum
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

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.util.ClockworkInspector;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType;

/**
 * @author semancik
 *
 */
@Component
public class ClockworkMedic {
	
	private static final Trace LOGGER = TraceManager.getTrace(ClockworkMedic.class);

	private ClockworkInspector clockworkInspector;

	public ClockworkInspector getClockworkInspector() {
		return clockworkInspector;
	}

	public void setClockworkInspector(ClockworkInspector clockworkInspector) {
		this.clockworkInspector = clockworkInspector;
	}

	public <F extends ObjectType> void clockworkStart(LensContext<F> context) {
		if (clockworkInspector != null) {
			clockworkInspector.clockworkStart(context);
		}
	}
	
	public <F extends ObjectType> void clockworkStateSwitch(LensContext<F> contextBefore, ModelState newState) {
		if (clockworkInspector != null) {
			clockworkInspector.clockworkStateSwitch(contextBefore, newState);
		}
	}

	public <F extends ObjectType> void clockworkFinish(LensContext<F> context) {
		if (clockworkInspector != null) {
			clockworkInspector.clockworkFinish(context);
		}
	}

	public <F extends ObjectType> void projectorStart(LensContext<F> context) {
		if (clockworkInspector != null) {
			clockworkInspector.projectorStart(context);
		}
	}

	public <F extends ObjectType> void projectorFinish(LensContext<F> context) {
		if (clockworkInspector != null) {
			clockworkInspector.projectorFinish(context);
		}
	}

	public <F extends ObjectType> void afterMappingEvaluation(LensContext<F> context,
			MappingImpl<?, ?> evaluatedMapping) {
		if (clockworkInspector != null) {
			clockworkInspector.afterMappingEvaluation(context, evaluatedMapping);
		}
	}
	
	public void partialExecute(String componentName, ProjectorComponentRunnable runnable, Supplier<PartialProcessingTypeType> optionSupplier)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException,
			PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PreconditionViolationException {
		partialExecute(componentName, runnable, optionSupplier, null);
	}

	public void partialExecute(String componentName, ProjectorComponentRunnable runnable,
			Supplier<PartialProcessingTypeType> optionSupplier, OperationResult result)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException,
			PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PreconditionViolationException {
		PartialProcessingTypeType option = optionSupplier.get();
		if (option == PartialProcessingTypeType.SKIP) {
			LOGGER.debug("Skipping projector component {} because partial execution option is set to {}", componentName, option);
			if (clockworkInspector != null) {
				clockworkInspector.projectorComponentSkip(componentName);
			}
		} else {
			LOGGER.trace("Projector component started: {}", componentName);
			if (clockworkInspector != null) {
				clockworkInspector.projectorComponentStart(componentName);
			}
			try {
				runnable.run();
				LOGGER.trace("Projector component finished: {}", componentName);
			} catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException
					| PolicyViolationException | ExpressionEvaluationException | ObjectAlreadyExistsException | PreconditionViolationException | RuntimeException | Error e) {
				LOGGER.trace("Projector component error: {}: {}: {}", componentName, e.getClass().getSimpleName(), e.getMessage());
				if (result != null) {
					result.recordFatalError(e);
				}
				throw e;
			} finally {
				if (clockworkInspector != null) {
					clockworkInspector.projectorComponentFinish(componentName);
				}
			}

		}
	}
	
	public static <F extends ObjectType> void traceContext(Trace logger, String activity, String phase,
			boolean important,  LensContext<F> context, boolean showTriples) throws SchemaException {
        if (logger.isTraceEnabled()) {
        	logger.trace("Lens context:\n"+
            		"---[ {} context {} ]--------------------------------\n"+
            		"{}\n",
					activity, phase, context.dump(showTriples));
        }
    }
	
}

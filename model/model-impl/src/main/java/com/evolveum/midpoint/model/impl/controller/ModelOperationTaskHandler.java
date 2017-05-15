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

package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LensContextType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Iterator;
import java.util.List;

/**
 * Handles a "ModelOperation task" - executes a given model operation in a context
 * of the task (i.e., in most cases, asynchronously).
 *
 * The context of the model operation (i.e., model context) is stored in task extension property
 * called "modelContext". When this handler is executed, the context is retrieved, unwrapped from
 * its XML representation, and the model operation is (re)started.
 *
 * @author mederly
 */

@Component
public class ModelOperationTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ModelOperationTaskHandler.class);

    private static final String DOT_CLASS = ModelOperationTaskHandler.class.getName() + ".";

    public static final String MODEL_OPERATION_TASK_URI = "http://midpoint.evolveum.com/xml/ns/public/model/operation/handler-3";

    @Autowired(required = true)
	private TaskManager taskManager;

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private ProvisioningService provisioningService;

    @Autowired(required = true)
    private Clockwork clockwork;

	@Override
	public TaskRunResult run(Task task) {

		OperationResult result = task.getResult().createSubresult(DOT_CLASS + "run");
		TaskRunResult runResult = new TaskRunResult();

		LensContextType contextType = task.getModelOperationContext();
		if (contextType == null) {
			LOGGER.trace("No model context found, skipping the model operation execution.");
			if (result.isUnknown()) {
				result.computeStatus();
			}
			runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
		} else {
            LensContext context = null;
            try {
                context = LensContext.fromLensContextType(contextType, prismContext, provisioningService, task, result);
            } catch (SchemaException e) {
                throw new SystemException("Cannot recover model context from task " + task + " due to schema exception", e);
            } catch (ObjectNotFoundException | ConfigurationException | ExpressionEvaluationException e) {
                throw new SystemException("Cannot recover model context from task " + task, e);
            } catch (CommunicationException e) {
                throw new SystemException("Cannot recover model context from task " + task, e);     // todo wait and retry
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Context to be executed = {}", context.debugDump());
            }

            try {
                // here we brutally remove all the projection contexts -- because if we are continuing after rejection of a role/resource assignment
                // that resulted in such projection contexts, we DO NOT want them to appear in the context any more
                context.rot();
                Iterator<LensProjectionContext> projectionIterator = context.getProjectionContextsIterator();
                while (projectionIterator.hasNext()) {
                    LensProjectionContext projectionContext = projectionIterator.next();
                    if (projectionContext.getPrimaryDelta() != null && !projectionContext.getPrimaryDelta().isEmpty()) {
                        continue;       // don't remove client requested actions!
                    }
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Removing projection context {}", projectionContext.getHumanReadableName());
                    }
                    projectionIterator.remove();
                }
				if (task.getChannel() == null) {
					task.setChannel(context.getChannel());
				}
                clockwork.run(context, task, result);

				task.setModelOperationContext(context.toLensContextType());
                task.savePendingModifications(result);

                if (result.isUnknown()) {
                    result.computeStatus();
                }
                runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
            } catch (Exception e) { // too many various exceptions; will be fixed with java7 :)
                String message = "An exception occurred within model operation, in task " + task;
                LoggingUtils.logUnexpectedException(LOGGER, message, e);
                result.recordPartialError(message, e);
                // TODO: here we do not know whether the error is temporary or permanent (in the future we could discriminate on the basis of particular exception caught)
                runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR);
            }
        }

        task.getResult().recomputeStatus();
		runResult.setOperationResult(task.getResult());
		return runResult;
	}

	@Override
	public Long heartbeat(Task task) {
		return null; // null - as *not* to record progress
	}

	@Override
	public void refreshStatus(Task task) {
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.WORKFLOW;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }

	@PostConstruct
	private void initialize() {
        if (LOGGER.isTraceEnabled()) {
		    LOGGER.trace("Registering with taskManager as a handler for " + MODEL_OPERATION_TASK_URI);
        }
		taskManager.registerHandler(MODEL_OPERATION_TASK_URI, this);
	}
}

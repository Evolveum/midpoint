/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.model.impl.sync;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeModelTaskHandler;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.CommonException;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * The task hander for user recompute.
 *
 *  This handler takes care of executing recompute "runs". The task will iterate over all objects of a given type
 *  and recompute their assignments and expressions. This is needed after the expressions are changed,
 *  e.g in resource outbound expressions or in a role definition.
 *
 * @author Radovan Semancik
 *
 */
@Component
public class RecomputeTaskHandler extends AbstractSearchIterativeModelTaskHandler<FocusType, AbstractSearchIterativeResultHandler<FocusType>> {

	public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/recompute/handler-3";

    @Autowired
	private TaskManager taskManager;

	@Autowired
	private PrismContext prismContext;

    @Autowired
    private ContextFactory contextFactory;

    @Autowired
    private Clockwork clockwork;

	private static final transient Trace LOGGER = TraceManager.getTrace(RecomputeTaskHandler.class);

	public RecomputeTaskHandler() {
        super("Recompute", OperationConstants.RECOMPUTE);
		setLogFinishInfo(true);
		setPreserveStatistics(false);
    }

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}

	@Override
	protected ObjectQuery createQuery(AbstractSearchIterativeResultHandler<FocusType> handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {
		return createQueryFromTask(handler, runResult, task, opResult);
	}

	@Override
    protected Class<? extends ObjectType> getType(Task task) {
		return getTypeFromTask(task, UserType.class);
	}

	@Override
	protected AbstractSearchIterativeResultHandler<FocusType> createHandler(TaskRunResult runResult, final Task coordinatorTask,
			OperationResult opResult) {

		AbstractSearchIterativeResultHandler<FocusType> handler = new AbstractSearchIterativeResultHandler<FocusType>(
				coordinatorTask, RecomputeTaskHandler.class.getName(), "recompute", "recompute task", taskManager) {
			@Override
			protected boolean handleObject(PrismObject<FocusType> object, Task workerTask, OperationResult result) throws CommonException, PreconditionViolationException {
				recompute(object, getOptions(coordinatorTask), workerTask, result);
				return true;
			}

		};
        handler.setStopOnError(false);
        return handler;
	}

	private ModelExecuteOptions getOptions(Task coordinatorTask) throws SchemaException {
		ModelExecuteOptions modelExecuteOptions = Utils.getModelExecuteOptions(coordinatorTask);
		if (modelExecuteOptions == null) {
			// Make reconcile the default (for compatibility). If there are no options
			// then assume reconcile.
			modelExecuteOptions =  ModelExecuteOptions.createReconcile();
		}
		LOGGER.trace("ModelExecuteOptions: {}", modelExecuteOptions);
		return modelExecuteOptions;
	}

	private void recompute(PrismObject<FocusType> focalObject, ModelExecuteOptions options, Task task, OperationResult result) throws SchemaException,
			ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException,
			ConfigurationException, PolicyViolationException, SecurityViolationException, PreconditionViolationException {
		LOGGER.trace("Recomputing object {}", focalObject);

		LensContext<FocusType> syncContext = contextFactory.createRecomputeContext(focalObject, options, task, result);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recomputing object {}: context:\n{}", focalObject, syncContext.debugDump());
		}
		clockwork.run(syncContext, task, result);
		LOGGER.trace("Recomputation of object {}: {}", focalObject, result.getStatus());
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.RECOMPUTATION;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }
}

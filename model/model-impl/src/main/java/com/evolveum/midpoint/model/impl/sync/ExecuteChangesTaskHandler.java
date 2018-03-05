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

import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeModelTaskHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

@Component
public class ExecuteChangesTaskHandler extends AbstractSearchIterativeModelTaskHandler<FocusType, AbstractSearchIterativeResultHandler<FocusType>> {

	public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/execute/handler-3";

    @Autowired
	private TaskManager taskManager;

	@Autowired
	private PrismContext prismContext;

    @Autowired
    private ModelService model;

	private static final transient Trace LOGGER = TraceManager.getTrace(ExecuteChangesTaskHandler.class);

	public ExecuteChangesTaskHandler() {
        super("Execute", OperationConstants.EXECUTE);
		setLogFinishInfo(true);
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
				coordinatorTask, ExecuteChangesTaskHandler.class.getName(), "execute", "execute task", taskManager) {
			@Override
			protected boolean handleObject(PrismObject<FocusType> object, Task workerTask, OperationResult result) throws CommonException {
				executeChange(object, coordinatorTask, workerTask, result);
				return true;
			}
		};
        handler.setStopOnError(false);
        return handler;
	}

	private <T extends ObjectType> void executeChange(PrismObject<T> focalObject, Task coordinatorTask, Task task, OperationResult result) throws SchemaException,
			ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException,
			ConfigurationException, PolicyViolationException, SecurityViolationException {
		LOGGER.trace("Recomputing object {}", focalObject);

		ObjectDelta<T> delta = createDeltaFromTask(coordinatorTask);
		if (delta == null) {
			throw new IllegalStateException("No delta in the task");
		}

		delta.setOid(focalObject.getOid());
		if (focalObject.getCompileTimeClass() != null) {
			delta.setObjectTypeClass(focalObject.getCompileTimeClass());
		}
		prismContext.adopt(delta);

		model.executeChanges(Collections.singletonList(delta), getExecuteOptionsFromTask(task), task, result);
		LOGGER.trace("Execute changes {} for object {}: {}", delta, focalObject, result.getStatus());
	}

	private <T extends ObjectType> ObjectDelta<T> createDeltaFromTask(Task task) throws SchemaException {
		PrismProperty<ObjectDeltaType> objectDeltaType = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA);
        if (objectDeltaType != null && objectDeltaType.getRealValue() != null) {
        	return DeltaConvertor.createObjectDelta(objectDeltaType.getRealValue(), prismContext);
        } else {
            return null;
        }
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.EXECUTE_CHANGES;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }

}

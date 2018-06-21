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

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Temporary/experimental implementation.
 *
 * @author mederly
 */

@Component
public class ExecuteDeltasTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ExecuteDeltasTaskHandler.class);

    private static final String DOT_CLASS = ExecuteDeltasTaskHandler.class.getName() + ".";

    @Autowired private TaskManager taskManager;
    @Autowired private PrismContext prismContext;
	@Autowired private ModelService modelService;

	@Override
	public TaskRunResult run(Task task) {

		OperationResult result = task.getResult().createSubresult(DOT_CLASS + "run");
		TaskRunResult runResult = new TaskRunResult();

		Collection<ObjectDeltaType> deltas;
		PrismProperty<ObjectDeltaType> deltasProperty = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTAS);
		if (deltasProperty == null || deltasProperty.isEmpty()) {
			PrismProperty<ObjectDeltaType> deltaProperty = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA);
			if (deltaProperty == null || deltaProperty.isEmpty()) {
				throw new IllegalArgumentException("No deltas to execute");
			} else {
				deltas = deltaProperty.getRealValues();
			}
		} else {
			deltas = deltasProperty.getRealValues();
		}
		PrismProperty<ModelExecuteOptionsType> optionsProperty = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_EXECUTE_OPTIONS);
		ModelExecuteOptions options = optionsProperty != null ?
				ModelExecuteOptions.fromModelExecutionOptionsType(optionsProperty.getRealValue()) : null;

		try {
			Collection<ObjectDelta<?>> objectDeltas = new ArrayList<>();
			for (ObjectDeltaType deltaBean : deltas) {
				objectDeltas.add(DeltaConvertor.createObjectDelta(deltaBean, prismContext));
			}
			//noinspection unchecked
			modelService.executeChanges((Collection) objectDeltas, options, task, result);
			result.computeStatusIfUnknown();
			runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
		} catch (CommonException | RuntimeException e) {
			String message = "An exception occurred when executing changes, in task " + task;
			LoggingUtils.logUnexpectedException(LOGGER, message, e);
			result.recordFatalError(message, e);
			runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR);
		}
        task.getResult().recomputeStatus();
		runResult.setOperationResult(task.getResult());
		return runResult;
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(ModelPublicConstants.EXECUTE_DELTAS_TASK_HANDLER_URI, this);
	}
}

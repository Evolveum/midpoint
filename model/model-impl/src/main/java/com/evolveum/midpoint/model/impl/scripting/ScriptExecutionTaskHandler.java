/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.api.ScriptExecutionResult;
import com.evolveum.midpoint.model.api.ScriptingService;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author mederly
 */

@Component
public class ScriptExecutionTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ScriptExecutionTaskHandler.class);

    private static final String DOT_CLASS = ScriptExecutionTaskHandler.class.getName() + ".";

    public static final String HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/model/scripting/handler-3";

    @Autowired
	private TaskManager taskManager;

    @Autowired
    private ScriptingService scriptingService;

	@Override
	public TaskRunResult run(Task task) {

		OperationResult result = task.getResult().createSubresult(DOT_CLASS + "run");
		TaskRunResult runResult = new TaskRunResult();

        PrismProperty<ExecuteScriptType> executeScriptProperty = task.getExtensionProperty(SchemaConstants.SE_EXECUTE_SCRIPT);
        if (executeScriptProperty == null || executeScriptProperty.getValue().getValue() == null ||
                executeScriptProperty.getValue().getValue().getScriptingExpression() == null) {
            throw new IllegalStateException("There's no script to be run in task " + task + " (property " + SchemaConstants.SE_EXECUTE_SCRIPT + ")");
        }

        try {
            task.startCollectingOperationStatsFromZero(true, false, true);
            task.setProgress(0);
            ScriptExecutionResult executionResult = scriptingService.evaluateExpression(executeScriptProperty.getRealValue(), task, result);
            LOGGER.debug("Execution output: {} item(s)", executionResult.getDataOutput().size());
            LOGGER.debug("Execution result:\n", executionResult.getConsoleOutput());
            result.computeStatus();
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
        } catch (ScriptExecutionException|SecurityViolationException|SchemaException e) {
            result.recordFatalError("Couldn't execute script: " + e.getMessage(), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute script", e);
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR);
        } finally {
            task.storeOperationStats();
        }

        task.getResult().computeStatus();
		runResult.setOperationResult(task.getResult());
        runResult.setProgress(task.getProgress());          // incremented directly in actions implementations
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
        return TaskCategory.BULK_ACTIONS;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }

	@PostConstruct
	private void initialize() {
        if (LOGGER.isTraceEnabled()) {
		    LOGGER.trace("Registering with taskManager as a handler for " + HANDLER_URI);
        }
		taskManager.registerHandler(HANDLER_URI, this);
	}
}

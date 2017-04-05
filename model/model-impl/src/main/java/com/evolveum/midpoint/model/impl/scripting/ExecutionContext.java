/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.model.api.ScriptExecutionResult;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionEvaluationOptionsType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context of a command execution.
 *
 * @author mederly
 */
public class ExecutionContext {
    private static final Trace LOGGER = TraceManager.getTrace(ExecutionContext.class);

    private final Task task;
    private final ScriptingExpressionEvaluationOptionsType options;
    private final StringBuilder consoleOutput = new StringBuilder();
    private final Map<String, Data> variables = new HashMap<>();
    private Data finalOutput;                                        // used only when passing result to external clients (TODO do this more cleanly)

    public ExecutionContext(ScriptingExpressionEvaluationOptionsType options, Task task) {
        this.task = task;
        this.options = options;
    }

    public Task getTask() {
        return task;
    }

	public ScriptingExpressionEvaluationOptionsType getOptions() {
		return options;
	}

	public boolean isContinueOnAnyError() {
    	return options != null && Boolean.TRUE.equals(options.isContinueOnAnyError());
	}

	public Data getVariable(String variableName) {
        return variables.get(variableName);
    }

    public void setVariable(String variableName, Item item) {
        variables.put(variableName, Data.create(item));
    }

    public void setVariable(String variableName, Data value) {
        variables.put(variableName, value);
    }

    public String getConsoleOutput() {
        return consoleOutput.toString();
    }

    public void println(Object o) {
        consoleOutput.append(o).append("\n");
        if (o != null) {
            LOGGER.info("Script console message: {}", o);          // temporary, until some better way of logging bulk action executions is found
        }
    }

    public Data getFinalOutput() {
        return finalOutput;
    }

    public void setFinalOutput(Data finalOutput) {
        this.finalOutput = finalOutput;
    }

    public ScriptExecutionResult toExecutionResult() {
        List<PrismValue> items = null;
        if (getFinalOutput() != null) {
            items = getFinalOutput().getData();
        }
        ScriptExecutionResult result = new ScriptExecutionResult(getConsoleOutput(), items);
        return result;
    }

    public String getChannel() {
        return task != null ? task.getChannel() : null;
    }

    public boolean canRun() {
        return task == null || task.canRun();
    }

    public void checkTaskStop() {
        if (!canRun()) {
            // TODO do this is a nicer way
            throw new SystemException("Stopping execution of a script because the task is stopping: " + task);
        }
    }
}

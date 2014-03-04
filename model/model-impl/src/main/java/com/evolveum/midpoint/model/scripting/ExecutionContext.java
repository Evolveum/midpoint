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

package com.evolveum.midpoint.model.scripting;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.task.api.Task;

import java.util.HashMap;
import java.util.Map;

/**
 * Context of a command execution.
 *
 * @author mederly
 */
public class ExecutionContext {
    private Task task;
    private StringBuilder consoleOutput = new StringBuilder();
    private Map<String, Data> variables = new HashMap<>();
    private Data finalOutput;                                        // used only when passing result to external clients (TODO do this more cleanly)

    public ExecutionContext(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
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
    }

    public Data getFinalOutput() {
        return finalOutput;
    }

    public void setFinalOutput(Data finalOutput) {
        this.finalOutput = finalOutput;
    }
}

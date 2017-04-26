/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.impl.messages;

import com.evolveum.midpoint.wf.impl.processes.ProcessInterfaceFinder;
import com.evolveum.midpoint.wf.impl.processes.ProcessMidPointInterface;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import org.activiti.engine.delegate.DelegateExecution;

import java.util.HashMap;
import java.util.Map;

/**
 * Process instance event - signals that something has happened with process instance.
 */
public class ProcessEvent {

    /**
     * Workflow process instance variables.
     */
    private final Map<String,Object> variables = new HashMap<>();

    /**
     * Workflow process instance ID.
     */
    private final String pid;

    /**
     * Is the process still running?
     */
    private boolean running;

    private String outcome;

	public ProcessEvent(DelegateExecution execution, ProcessInterfaceFinder processInterfaceFinder) {
		pid = execution.getProcessInstanceId();
		running = true;
		addVariablesFrom(execution.getVariables());
		computeOutcome(processInterfaceFinder);
	}

	private void computeOutcome(ProcessInterfaceFinder processInterfaceFinder) {
		ProcessMidPointInterface pmi = processInterfaceFinder.getProcessInterface(variables);
		outcome = pmi.getOutcome(variables);
	}

	public ProcessEvent(String pid, Map<String, Object> variables, ProcessInterfaceFinder processInterfaceFinder) {
		this.pid = pid;
		addVariablesFrom(variables);
		computeOutcome(processInterfaceFinder);
	}

	public String getPid() {
        return pid;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public <T> T getVariable(String name, Class<T> clazz) {
        return (T) variables.get(name);
    }

    public void putVariable(String name, Object value) {
        variables.put(name, value);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void addVariablesFrom(Map<String, Object> map) {
        variables.putAll(map);
    }

	public String getOutcome() {
		return outcome;
	}

	@Override
    public String toString() {
        return this.getClass().getSimpleName() + "[pid=" + pid + ", running=" + running + ", variables=" + variables + "]";
    }

    public boolean containsVariable(String varname) {
        if (variables == null) {
            return false;
        } else {
            return variables.containsKey(varname);
        }
    }

	public String getProcessDebugInfo() {
		return "pid=" + pid + ", name=" + getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME, String.class);
	}
}

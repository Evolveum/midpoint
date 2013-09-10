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

package com.evolveum.midpoint.wf.messages;

import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;

import java.util.HashMap;
import java.util.Map;

/**
 * Process instance event - signals that something has happened with process instance.
 */
public class ProcessEvent extends ActivitiToMidPointMessage {

    /**
     * Workflow process instance variables.
     */
    private Map<String,Object> variables = new HashMap<String,Object>();

    /**
     * Workflow process instance ID.
     */
    private String pid;

    /**
     * MidPoint monitoring task OID.
     */
    private String taskOid;

    /**
     * Is the process still running?
     */
    private boolean running;

    public String getAnswer() {
        return (String) variables.get(CommonProcessVariableNames.VARIABLE_WF_ANSWER);
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getTaskOid() {
        return taskOid;
    }

    public void setTaskOid(String taskOid) {
        this.taskOid = taskOid;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public Object getVariable(String name) {
        return variables.get(name);
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public void putVariable(String name, Object value) {
        if (variables == null) {
            variables = new HashMap<String,Object>();
        }
        variables.put(name, value);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setVariablesFrom(Map<String, Object> map) {
        variables = new HashMap<String,Object>(map);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[pid=" + pid + ", running=" + running + ", task=" + taskOid +  ", variables=" + variables + "]";
    }

    public boolean containsVariable(String varname) {
        if (variables == null) {
            return false;
        } else {
            return variables.containsKey(varname);
        }
    }
}

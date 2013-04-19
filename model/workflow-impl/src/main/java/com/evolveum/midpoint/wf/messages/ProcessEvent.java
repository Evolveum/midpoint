/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf.messages;

import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.processes.general.ProcessVariableNames;

import java.util.HashMap;
import java.util.Map;

/**
 * Process instance event - signals that something has happened with process instance.
 */
public class ProcessEvent extends ActivitiToMidPointMessage {

    /**
     * Workflow process instance variables.
     */
    private Map<String,Object> variables;

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

    public Boolean getAnswer() {
        Object a = variables.get(ProcessVariableNames.WFANSWER);
        if (a == null) {
            return null;
        } if (a instanceof Boolean) {
            return (Boolean) a;
        } else {
            return "true".equals(a);
        }
    }

    public String getState() {
        return (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_STATE);
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

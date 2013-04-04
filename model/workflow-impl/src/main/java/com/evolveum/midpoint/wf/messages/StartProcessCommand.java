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

import java.util.HashMap;
import java.util.Map;

/**
 * Command to start process instance.
 */
public class StartProcessCommand extends MidPointToActivitiMessage {

    private Map<String,Object> variables;
    private String processName;
    private String processOwner;
    private String taskOid;
    private boolean sendStartConfirmation;

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public boolean isSendStartConfirmation() {
        return sendStartConfirmation;
    }

    public void setSendStartConfirmation(boolean sendStartConfirmation) {
        this.sendStartConfirmation = sendStartConfirmation;
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

    public void setVariablesFrom(Map<String, Object> variables) {
        this.variables = new HashMap<String,Object>(variables);
    }

    public void addVariable(String name, Object value) {
        if (variables == null) {
            variables = new HashMap<String,Object>();
        }
        variables.put(name, value);
    }

    public String getProcessOwner() {
        return processOwner;
    }

    public void setProcessOwner(String processOwner) {
        this.processOwner = processOwner;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[process=" + processName + ", task=" + taskOid + ", variables=" + variables + ", sendStartConfirmation=" + sendStartConfirmation + "]";
    }
}

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

package com.evolveum.midpoint.wf.impl.messages;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an event related to activiti task (midPoint work item).
 *
 * @author mederly
 */
public class TaskEvent {

    /**
     * Workflow process instance variables, merged with form properties (TODO: verify this).
     */
    private Map<String,Object> variables = new HashMap<String,Object>();

    /**
     * Workflow task ID.
     */
    private String taskId;

    /**
     * Workflow task name.
     */
    private String taskName;

    /**
     * Task assignee (OID).
     */
    private String assigneeOid;

    /**
     * Name of related process instance.
     */
    private String processInstanceName;

    /**
     * ID of related process instance.
     */
    private String processInstanceId;

    /**
     * Create time.
     */
    private Date createTime;

    private Date dueDate;

    private List<String> candidateUsers = new ArrayList<>();
    private List<String> candidateGroups = new ArrayList<>();

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	private String owner;
    private String executionId;


    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getAssigneeOid() {
        return assigneeOid;
    }

    public void setAssigneeOid(String assigneeOid) {
        this.assigneeOid = assigneeOid;
    }

    public String getProcessInstanceName() {
        return processInstanceName;
    }

    public void setProcessInstanceName(String processInstanceName) {
        this.processInstanceName = processInstanceName;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public Date getCreateTime() {
        return createTime;
    }

	public Date getDueDate() {
		return dueDate;
	}

	public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getDebugName() {
        return getTaskName() + " (id " + getTaskId() + ")";
    }

    public List<String> getCandidateUsers() {
        return candidateUsers;
    }

    public void setCandidateUsers(List<String> candidateUsers) {
        this.candidateUsers = candidateUsers;
    }

    public List<String> getCandidateGroups() {
        return candidateGroups;
    }

    public void setCandidateGroups(List<String> candidateGroups) {
        this.candidateGroups = candidateGroups;
    }

    //    private String getWorkItemName(DelegateTask delegateTask) {
//        return (String) delegateTask.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME);
//    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "taskId='" + taskId + '\'' +
                ", taskName='" + taskName + '\'' +
                ", assigneeOid='" + assigneeOid + '\'' +
                ", processInstanceName='" + processInstanceName + '\'' +
                ", processInstanceId='" + processInstanceId + '\'' +
                ", createTime=" + createTime +
                ", dueDate=" + dueDate +
                ", candidateUsers=" + candidateUsers +
                ", candidateGroups=" + candidateGroups +
                ", owner='" + owner + '\'' +
                ", executionId='" + executionId + '\'' +
                ", variables=" + variables +
                '}';
    }
}

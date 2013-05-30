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

package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.api.*;
import com.evolveum.midpoint.wf.dao.ProcessInstanceManager;
import com.evolveum.midpoint.wf.dao.ProcessInstanceProvider;
import com.evolveum.midpoint.wf.dao.WorkItemManager;
import com.evolveum.midpoint.wf.dao.WorkItemProvider;

import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author mederly
 */
@Component("workflowService")
public class WorkflowServiceImpl implements WorkflowService {

    private static final transient Trace LOGGER = TraceManager.getTrace(WorkflowServiceImpl.class);

    @Autowired
    private PrismContext prismContext;
    
    @Autowired
    private com.evolveum.midpoint.repo.api.RepositoryService repositoryService;

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired
    private ProcessInstanceProvider processInstanceProvider;

    @Autowired
    private ProcessInstanceManager processInstanceManager;

    @Autowired
    private ProcessInstanceController processInstanceController;

    @Autowired
    private WorkItemProvider workItemProvider;

    @Autowired
    private WorkItemManager workItemManager;

    private static final String DOT_CLASS = WorkflowServiceImpl.class.getName() + ".";


    /*
     * Work items
     * ==========
     */

    @Override
    public int countWorkItemsRelatedToUser(String userOid, boolean assigned, OperationResult parentResult) throws WorkflowException {
        return workItemProvider.countWorkItemsRelatedToUser(userOid, assigned, parentResult);
    }

    @Override
    public List<WorkItem> listWorkItemsRelatedToUser(String userOid, boolean assigned, int first, int count, OperationResult parentResult) throws WorkflowException {
        return workItemProvider.listWorkItemsRelatedToUser(userOid, assigned, first, count, parentResult);
    }

    @Override
    public WorkItemDetailed getWorkItemDetailsByTaskId(String taskId, OperationResult parentResult) throws ObjectNotFoundException, WorkflowException {
        return workItemProvider.getWorkItemDetailsByTaskId(taskId, parentResult);
    }

    @Override
    public void approveOrRejectWorkItem(String taskId, boolean decision, OperationResult parentResult) {
        workItemManager.approveOrRejectWorkItemWithDetails(taskId, null, decision, parentResult);
    }

    @Override
    public void approveOrRejectWorkItemWithDetails(String taskId, PrismObject specific, boolean decision, OperationResult parentResult) {
        workItemManager.approveOrRejectWorkItemWithDetails(taskId, specific, decision, parentResult);
    }

    /*
     * Process instances
     * =================
     */

    @Override
    public int countProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, OperationResult parentResult) throws WorkflowException {
        return processInstanceProvider.countProcessInstancesRelatedToUser(userOid, requestedBy, requestedFor, finished, parentResult);
    }

    @Override
    public List<ProcessInstance> listProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, int first, int count, OperationResult parentResult) throws WorkflowException {
        return processInstanceProvider.listProcessInstancesRelatedToUser(userOid, requestedBy, requestedFor, finished, first, count, parentResult);
    }

    @Override
    public ProcessInstance getProcessInstanceByTaskId(String taskId, OperationResult parentResult) throws ObjectNotFoundException, WorkflowException {
        return processInstanceProvider.getProcessInstanceByTaskId(taskId, parentResult);
    }

    @Override
    public ProcessInstance getProcessInstanceByInstanceId(String instanceId, boolean historic, boolean getWorkItems, OperationResult parentResult) throws ObjectNotFoundException, WorkflowException {
        return processInstanceProvider.getProcessInstanceByInstanceId(instanceId, historic, getWorkItems, parentResult);
    }

    @Override
    public void stopProcessInstance(String instanceId, String username, OperationResult parentResult) {
        processInstanceManager.stopProcessInstance(instanceId, username, parentResult);
    }

    @Override
    public void deleteProcessInstance(String instanceId, OperationResult parentResult) {
        processInstanceManager.deleteProcessInstance(instanceId, parentResult);
    }

    /*
     * Other
     * =====
     */

    @Override
    public boolean isEnabled() {
        return wfConfiguration.isEnabled();
    }

    @Override
    public PrismContext getPrismContext() {
        return prismContext;
    }

    @Override
    public String getProcessInstanceDetailsPanelName(ProcessInstance processInstance) {
        String processor = (String) processInstance.getVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_PROCESSOR);
        Validate.notNull(processor, "There's no change processor name among the process instance variables");
        return wfConfiguration.findChangeProcessor(processor).getProcessInstanceDetailsPanelName(processInstance);
    }

    @Override
    public void registerProcessListener(ProcessListener processListener) {
        processInstanceController.registerProcessListener(processListener);
    }

    @Override
    public void registerWorkItemListener(WorkItemListener workItemListener) {
        processInstanceController.registerWorkItemListener(workItemListener);
    }
}

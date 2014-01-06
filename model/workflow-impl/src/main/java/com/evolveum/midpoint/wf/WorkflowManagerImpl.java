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
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.*;
import com.evolveum.midpoint.wf.dao.ProcessInstanceManager;
import com.evolveum.midpoint.wf.dao.ProcessInstanceProvider;
import com.evolveum.midpoint.wf.dao.WorkItemManager;
import com.evolveum.midpoint.wf.dao.WorkItemProvider;

import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.jobs.JobController;
import com.evolveum.midpoint.wf.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WfProcessInstanceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WfProcessInstanceVariableType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WorkItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author mederly
 */
@Component("workflowManager")
public class WorkflowManagerImpl implements WorkflowManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(WorkflowManagerImpl.class);

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
    private JobController jobController;

    @Autowired
    private WorkItemProvider workItemProvider;

    @Autowired
    private WorkItemManager workItemManager;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    @Autowired
    private MiscDataUtil miscDataUtil;

    private static final String DOT_CLASS = WorkflowManagerImpl.class.getName() + ".";


    /*
     * Work items
     * ==========
     */

    @Override
    public int countWorkItemsRelatedToUser(String userOid, boolean assigned, OperationResult parentResult) {
        return workItemProvider.countWorkItemsRelatedToUser(userOid, assigned, parentResult);
    }

    @Override
    public List<WorkItemType> listWorkItemsRelatedToUser(String userOid, boolean assigned, int first, int count, OperationResult parentResult) {
        return workItemProvider.listWorkItemsRelatedToUser(userOid, assigned, first, count, parentResult);
    }

    @Override
    public WorkItemType getWorkItemDetailsById(String taskId, OperationResult parentResult) throws ObjectNotFoundException {
        return workItemProvider.getWorkItemDetailsById(taskId, parentResult);
    }

    @Override
    public void approveOrRejectWorkItem(String taskId, boolean decision, OperationResult parentResult) {
        workItemManager.completeWorkItemWithDetails(taskId, null, CommonProcessVariableNames.approvalStringValue(decision), parentResult);
    }

    @Override
    public void approveOrRejectWorkItemWithDetails(String taskId, PrismObject specific, boolean decision, OperationResult parentResult) {
        workItemManager.completeWorkItemWithDetails(taskId, specific, CommonProcessVariableNames.approvalStringValue(decision), parentResult);
    }

    @Override
    public void completeWorkItemWithDetails(String taskId, PrismObject specific, String decision, OperationResult parentResult) {
        workItemManager.completeWorkItemWithDetails(taskId, specific, decision, parentResult);
    }

    /*
     * Process instances
     * =================
     */

    @Override
    public int countProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, OperationResult parentResult) {
        return processInstanceProvider.countProcessInstancesRelatedToUser(userOid, requestedBy, requestedFor, finished, parentResult);
    }

    @Override
    public List<WfProcessInstanceType> listProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, int first, int count, OperationResult parentResult) {
        return processInstanceProvider.listProcessInstancesRelatedToUser(userOid, requestedBy, requestedFor, finished, first, count, parentResult);
    }

    @Override
    public WfProcessInstanceType getProcessInstanceByWorkItemId(String taskId, OperationResult parentResult) throws ObjectNotFoundException {
        return processInstanceProvider.getProcessInstanceByTaskId(taskId, parentResult);
    }

    @Override
    public WfProcessInstanceType getProcessInstanceById(String instanceId, boolean historic, boolean getWorkItems, OperationResult parentResult) throws ObjectNotFoundException {
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
    public String getProcessInstanceDetailsPanelName(WfProcessInstanceType processInstance) {

        String processor = null;
        for (WfProcessInstanceVariableType var : processInstance.getVariables()) {
            if (CommonProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_PROCESSOR.equals(var.getName())) {
                processor = var.getValue();         // we assume it's not encoded
                break;
            }
        }
        if (processor == null) {
            LOGGER.error("There's no change processor name among the process instance variables; variables = " + processInstance.getVariables());
            throw new IllegalStateException("There's no change processor name among the process instance variables");
        }
        return wfConfiguration.findChangeProcessor(processor).getProcessInstanceDetailsPanelName(processInstance);
    }

    @Override
    public void registerProcessListener(ProcessListener processListener) {
        jobController.registerProcessListener(processListener);
    }

    @Override
    public void registerWorkItemListener(WorkItemListener workItemListener) {
        jobController.registerWorkItemListener(workItemListener);
    }

    @Override
    public List<? extends ObjectReferenceType> getApprovedBy(Task task, OperationResult result) throws SchemaException {
        return wfTaskUtil.getApprovedByFromTaskTree(task, result);
    }

    @Override
    public boolean isCurrentUserAuthorizedToSubmit(WorkItemType workItem) {
        return miscDataUtil.isCurrentUserAuthorizedToSubmit(workItem);
    }
}

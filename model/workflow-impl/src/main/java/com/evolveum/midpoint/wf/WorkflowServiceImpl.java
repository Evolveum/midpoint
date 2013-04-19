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

}

/*
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.wf.api;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;

import java.util.List;

/**
 * @author mederly
 */

public interface WorkflowService {

    /*
     * Work items for user
     * ===================
     */

    /**
     * Counts Work Items related to a user.
     *
     * @param userOid OID of the user
     * @param assigned whether to count assigned (true) or assignable (false) work items
     * @param parentResult
     * @return number of relevant work items
     * @throws WorkflowException
     */
    public int countWorkItemsRelatedToUser(String userOid, boolean assigned, OperationResult parentResult) throws WorkflowException;

    /**
     * Lists work items related to a user.
     *
     * @param userOid OID of the user
     * @param assigned whether to count assigned (true) or assignable (false) work items
     * @param first
     * @param count
     * @param parentResult
     * @return list of work items
     * @throws WorkflowException
     */
    public List<WorkItem> listWorkItemsRelatedToUser(String userOid, boolean assigned, int first, int count, OperationResult parentResult) throws WorkflowException;

    /*
    * Process instances for user
    * ==========================
    * (1) current
    * (2) historic (finished)
    */

    public int countProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, OperationResult parentResult) throws WorkflowException;

    public List<ProcessInstance> listProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, int first, int count, OperationResult parentResult) throws WorkflowException;

    /*
     * Work Item and Process Instance by Activiti Task ID or Process Instance ID
     * =========================================================================
     */

    public WorkItemDetailed getWorkItemDetailsByTaskId(String taskId, OperationResult parentResult) throws ObjectNotFoundException, WorkflowException;

    public ProcessInstance getProcessInstanceByTaskId(String taskId, OperationResult parentResult) throws ObjectNotFoundException, WorkflowException;

    public ProcessInstance getProcessInstanceByInstanceId(String instanceId, boolean historic, boolean getProcessDetails, OperationResult parentResult) throws ObjectNotFoundException, WorkflowException;

    /**
     * Approves or rejects a work item (without supplying any further information).
     *
     * @param taskId identifier of activiti task backing the work item
     * @param decision true = approve, false = reject
     * @param parentResult
     */
    public void approveOrRejectWorkItem(String taskId, boolean decision, OperationResult parentResult);

    public void approveOrRejectWorkItemWithDetails(String taskId, PrismObject specific, boolean decision, OperationResult result);

    public void stopProcessInstance(String instanceId, String username, OperationResult parentResult);

    public void deleteProcessInstance(String instanceId, OperationResult parentResult);

    public boolean isEnabled();

    public PrismContext getPrismContext();
}

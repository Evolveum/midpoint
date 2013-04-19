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
 * TODO specify and clean-up error handling
 *
 * @author mederly
 */

public interface WorkflowService {

    /*
     * Work items
     * ==========
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

    /**
     * Provides detailed information about a given work item (may be inefficient, so use with care).
     *
     * @param taskId
     * @param parentResult
     * @return
     * @throws ObjectNotFoundException
     * @throws WorkflowException
     */
    public WorkItemDetailed getWorkItemDetailsByTaskId(String taskId, OperationResult parentResult) throws ObjectNotFoundException, WorkflowException;

    /*
     * Process instances
     * =================
     */

    public int countProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, OperationResult parentResult) throws WorkflowException;

    public List<ProcessInstance> listProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, int first, int count, OperationResult parentResult) throws WorkflowException;

    public ProcessInstance getProcessInstanceByTaskId(String taskId, OperationResult parentResult) throws ObjectNotFoundException, WorkflowException;

    /**
     * Returns information about a process instance. WorkItems attribute is filled-in only upon request! (see getWorkItems parameter)
     *
     * @param instanceId
     * @param historic
     * @param getWorkItems
     * @param parentResult
     * @return
     * @throws ObjectNotFoundException
     * @throws WorkflowException
     */
    public ProcessInstance getProcessInstanceByInstanceId(String instanceId, boolean historic, boolean getWorkItems, OperationResult parentResult) throws ObjectNotFoundException, WorkflowException;

    /*
     * CHANGING THINGS
     * ===============
     */

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

    /*
     * MISC
     * ====
     */

    public boolean isEnabled();

    // TODO remove this
    public PrismContext getPrismContext();
}

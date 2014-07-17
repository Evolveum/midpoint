package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfProcessInstanceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;

import java.util.List;

/**
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
     */
    int countWorkItemsRelatedToUser(String userOid, boolean assigned, OperationResult parentResult) throws SchemaException, ObjectNotFoundException;

    /**
     * Lists work items related to a user.
     *
     * @param userOid OID of the user
     * @param assigned whether to count assigned (true) or assignable (false) work items
     * @param first
     * @param count
     * @param parentResult
     * @return list of work items
     */
    List<WorkItemType> listWorkItemsRelatedToUser(String userOid, boolean assigned, int first, int count, OperationResult parentResult) throws SchemaException, ObjectNotFoundException;

    /**
     * Provides detailed information about a given work item (may be inefficient, so use with care).
     *
     * @param taskId
     * @param parentResult
     * @return
     * @throws com.evolveum.midpoint.util.exception.ObjectNotFoundException
     * @throws WorkflowException
     */
    WorkItemType getWorkItemDetailsById(String workItemId, OperationResult parentResult) throws ObjectNotFoundException;

    /*
     * Process instances
     * =================
     */

    int countProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, OperationResult parentResult);

    List<WfProcessInstanceType> listProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, int first, int count, OperationResult parentResult);

    WfProcessInstanceType getProcessInstanceByWorkItemId(String workItemId, OperationResult parentResult) throws ObjectNotFoundException;

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
    public WfProcessInstanceType getProcessInstanceById(String instanceId, boolean historic, boolean getWorkItems, OperationResult parentResult) throws ObjectNotFoundException;

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
    void approveOrRejectWorkItem(String workItemId, boolean decision, OperationResult parentResult);

    void approveOrRejectWorkItemWithDetails(String workItemId, PrismObject specific, boolean decision, OperationResult result);

    void completeWorkItemWithDetails(String workItemId, PrismObject specific, String decision, OperationResult parentResult);

    void stopProcessInstance(String instanceId, String username, OperationResult parentResult);

    void deleteProcessInstance(String instanceId, OperationResult parentResult);

    void claimWorkItem(String workItemId, OperationResult parentResult);

    void releaseWorkItem(String workItemId, OperationResult parentResult);
}

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfProcessInstanceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemNewType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;

import java.util.List;

/**
 * @author mederly
 */
public interface WorkflowService {

    /*
     * Process instances
     * =================
     */

    int countProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, OperationResult parentResult);

    List<WfProcessInstanceType> listProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, int first, int count, OperationResult parentResult);

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
     *  @param taskId identifier of activiti task backing the work item
     * @param decision true = approve, false = reject
     * @param parentResult
     * @param comment
     */
    void approveOrRejectWorkItem(String workItemId, boolean decision, String comment, OperationResult parentResult);

    void stopProcessInstance(String instanceId, String username, OperationResult parentResult);

    void deleteProcessInstance(String instanceId, OperationResult parentResult);

    void claimWorkItem(String workItemId, OperationResult parentResult);

    void releaseWorkItem(String workItemId, OperationResult parentResult);
}

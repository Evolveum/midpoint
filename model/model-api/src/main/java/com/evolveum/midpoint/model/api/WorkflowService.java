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

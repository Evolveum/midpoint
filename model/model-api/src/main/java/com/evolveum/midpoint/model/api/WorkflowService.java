package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

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
    void approveOrRejectWorkItem(String workItemId, boolean decision, String comment, OperationResult parentResult) throws SecurityViolationException;

    void stopProcessInstance(String instanceId, String username, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, SecurityViolationException;

    void claimWorkItem(String workItemId, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException;

    void releaseWorkItem(String workItemId, OperationResult parentResult) throws ObjectNotFoundException, SecurityViolationException;
}

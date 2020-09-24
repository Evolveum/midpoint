/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.api;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.wf.util.PerformerCommentsFormatter;
import com.evolveum.midpoint.wf.util.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;

/**
 * TODO specify and clean-up error handling
 *
 * @author mederly
 */

public interface WorkflowManager {

    //region Work items
    /**
     * Approves or rejects a work item
     * @param decision     true = approve, false = reject
     */
    void completeWorkItem(WorkItemId workItemId, AbstractWorkItemOutputType output,
            WorkItemEventCauseInformationType causeInformation, Task task,
            OperationResult parentResult) throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException;

    void claimWorkItem(WorkItemId workItemId, Task task, OperationResult result)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException;

    void releaseWorkItem(WorkItemId workItemId, Task task, OperationResult result)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException;

    void delegateWorkItem(WorkItemId workItemId, WorkItemDelegationRequestType delegationRequest,
            Task task, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException;
    //endregion

    //region Process instances (cases)

    void cancelCase(String caseOid, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException;

    /**
     * Deletes obsolete cases, as specified in the policy.
     *
     * This method removes parent case object with all its children cases.
     *
     *
     * @param closedCasesPolicy specifies which tasks are to be deleted, e.g. how old they have to be
     * @param task task, within which context the cleanup executes (used to test for interruptions)
     */
    void cleanupCases(CleanupPolicyType closedCasesPolicy, RunningTask task, OperationResult opResult) throws SchemaException;

    //endregion

    /*
     * MISC
     * ====
     */

    public boolean isEnabled();

    // TODO remove this
    PrismContext getPrismContext();

    void registerWorkflowListener(WorkflowListener workflowListener);

    boolean isCurrentUserAuthorizedToSubmit(CaseWorkItemType workItem, Task task, OperationResult result) throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

    boolean isCurrentUserAuthorizedToClaim(CaseWorkItemType workItem);

    boolean isCurrentUserAuthorizedToDelegate(CaseWorkItemType workItem, Task task, OperationResult result) throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

    ChangesByState getChangesByState(CaseType rootCase, ModelInteractionService modelInteractionService, PrismContext prismContext, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException;

    ChangesByState getChangesByState(CaseType approvalCase, CaseType rootCase, ModelInteractionService modelInteractionService, PrismContext prismContext, OperationResult result)
            throws SchemaException, ObjectNotFoundException;

    /**
     * Retrieves information about actual or expected execution of an approval schema.
     * (So, this is restricted to approvals using this mechanism.)
     *
     * Does not need authorization checks before execution; it uses model calls in order to gather any information needed.
     *
     * @param taskOid OID of an approval task that should be analyzed
     * @param opTask task under which this operation is carried out
     * @param parentResult operation result
     */
    ApprovalSchemaExecutionInformationType getApprovalSchemaExecutionInformation(String taskOid, Task opTask, OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException;

    /**
     * Retrieves information about expected approval schema and its execution.
     * (So, this is restricted to approvals using this mechanism.)
     *
     * Does not need authorization checks before execution; it uses model calls in order to gather any information needed.
     *
     * @param modelContext model context with the projector run already carried out (so the policy rules are evaluated)
     * @param opTask task under which this operation is carried out
     * @param parentResult operation result
     */
    List<ApprovalSchemaExecutionInformationType> getApprovalSchemaPreview(ModelContext<?> modelContext, Task opTask, OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException;

    PerformerCommentsFormatter createPerformerCommentsFormatter(PerformerCommentsFormattingType formatting);
}

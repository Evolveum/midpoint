/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.wf.api.WorkflowListener;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.access.CaseManager;
import com.evolveum.midpoint.wf.impl.access.WorkItemManager;
import com.evolveum.midpoint.wf.impl.processes.common.ExpressionEvaluationHelper;
import com.evolveum.midpoint.wf.impl.access.AuthorizationHelper;
import com.evolveum.midpoint.wf.impl.engine.helpers.NotificationHelper;
import com.evolveum.midpoint.wf.impl.util.PerformerCommentsFormatterImpl;
import com.evolveum.midpoint.wf.impl.util.ChangesSorter;
import com.evolveum.midpoint.wf.util.PerformerCommentsFormatter;
import com.evolveum.midpoint.wf.util.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author mederly
 *
 * Note: don't autowire this class - because of Spring AOP use it couldn't be found by implementation class; only by its interface.
 */
@Component("workflowManager")
public class WorkflowManagerImpl implements WorkflowManager {

    private static final Trace LOGGER = TraceManager.getTrace(WorkflowManagerImpl.class);

    @Autowired private PrismContext prismContext;
    @Autowired private WfConfiguration wfConfiguration;
    @Autowired private CaseManager caseManager;
    @Autowired private NotificationHelper notificationHelper;
    @Autowired private WorkItemManager workItemManager;
    @Autowired private ChangesSorter changesSorter;
    @Autowired private AuthorizationHelper authorizationHelper;
    @Autowired private ApprovalSchemaExecutionInformationHelper approvalSchemaExecutionInformationHelper;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;
    @Autowired private ExpressionEvaluationHelper expressionEvaluationHelper;

    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

    @PostConstruct
    public void initialize() {
        LOGGER.debug("Workflow manager starting.");
    }

    //region Work items
    @Override
    public void completeWorkItem(WorkItemId workItemId, @NotNull AbstractWorkItemOutputType output,
            WorkItemEventCauseInformationType causeInformation, Task task, OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        try {
            workItemManager.completeWorkItem(workItemId, output, causeInformation, task, parentResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void claimWorkItem(WorkItemId workItemId, Task task, OperationResult result)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        workItemManager.claimWorkItem(workItemId, task, result);
    }

    @Override
    public void releaseWorkItem(WorkItemId workItemId, Task task, OperationResult result)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        workItemManager.releaseWorkItem(workItemId, task, result);
    }

    @Override
    public void delegateWorkItem(WorkItemId workItemId, WorkItemDelegationRequestType delegationRequest,
            Task task, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        workItemManager.delegateWorkItem(workItemId, delegationRequest, null, null, null, null, task, parentResult);
    }
    //endregion

    //region Process instances (cases)
    @Override
    public void cancelCase(String caseOid, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        caseManager.cancelCase(caseOid, task, parentResult);
    }

    @Override
    public void deleteCase(String caseOid, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        caseManager.deleteCase(caseOid, task, parentResult);
    }

    //endregion

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
    public void registerWorkflowListener(WorkflowListener workflowListener) {
        notificationHelper.registerWorkItemListener(workflowListener);
    }

    @Override
    public boolean isCurrentUserAuthorizedToSubmit(CaseWorkItemType workItem, Task task, OperationResult result) throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return authorizationHelper.isAuthorized(workItem, AuthorizationHelper.RequestedOperation.COMPLETE, task, result);
    }

    @Override
    public boolean isCurrentUserAuthorizedToClaim(CaseWorkItemType workItem) {
        return authorizationHelper.isAuthorizedToClaim(workItem);
    }

    @Override
    public boolean isCurrentUserAuthorizedToDelegate(CaseWorkItemType workItem, Task task, OperationResult result) throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return authorizationHelper.isAuthorized(workItem, AuthorizationHelper.RequestedOperation.DELEGATE, task, result);
    }

    @Override
    public ChangesByState getChangesByState(CaseType rootCase, ModelInteractionService modelInteractionService, PrismContext prismContext,
            Task task, OperationResult result) throws SchemaException {

        // TODO op subresult
        return changesSorter.getChangesByStateForRoot(rootCase, prismContext, result);
    }

    @Override
    public ChangesByState getChangesByState(CaseType approvalCase, CaseType rootCase, ModelInteractionService modelInteractionService, PrismContext prismContext,
            OperationResult result) throws SchemaException {

        // TODO op subresult
        return changesSorter.getChangesByStateForChild(approvalCase, rootCase, prismContext, result);
    }

    @Override
    public ApprovalSchemaExecutionInformationType getApprovalSchemaExecutionInformation(String taskOid, Task opTask,
            OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "getApprovalSchemaExecutionInformation");
        try {
            return approvalSchemaExecutionInformationHelper.getApprovalSchemaExecutionInformation(taskOid, opTask, result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't determine schema execution information: " + t.getMessage(), t);
            throw t;
        } finally {
            result.recordSuccessIfUnknown();
        }
    }

    @Override
    public List<ApprovalSchemaExecutionInformationType> getApprovalSchemaPreview(ModelContext<?> modelContext, Task opTask,
            OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "getApprovalSchemaPreview");
        try {
            return approvalSchemaExecutionInformationHelper.getApprovalSchemaPreview(modelContext, opTask, result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't compute approval schema preview: " + t.getMessage(), t);
            throw t;
        } finally {
            result.recordSuccessIfUnknown();
        }
    }

    @Override
    public PerformerCommentsFormatter createPerformerCommentsFormatter(PerformerCommentsFormattingType formatting) {
        return new PerformerCommentsFormatterImpl(formatting, repositoryService, expressionEvaluationHelper);
    }
}

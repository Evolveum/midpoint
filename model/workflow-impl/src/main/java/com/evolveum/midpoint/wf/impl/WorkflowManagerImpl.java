/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.ProcessListener;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.wf.api.WorkItemListener;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.access.ProcessInstanceManager;
import com.evolveum.midpoint.wf.impl.access.WorkItemManager;
import com.evolveum.midpoint.wf.impl.processes.common.WfExpressionEvaluationHelper;
import com.evolveum.midpoint.wf.impl.access.AuthorizationHelper;
import com.evolveum.midpoint.wf.impl.tasks.WfNotificationHelper;
import com.evolveum.midpoint.wf.impl._temp.TemporaryHelper;
import com.evolveum.midpoint.wf.impl.util.PerformerCommentsFormatterImpl;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.wf.util.PerformerCommentsFormatter;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.wf.util.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final transient Trace LOGGER = TraceManager.getTrace(WorkflowManagerImpl.class);

    @Autowired private PrismContext prismContext;
	@Autowired private WfConfiguration wfConfiguration;
	@Autowired private ProcessInstanceManager processInstanceManager;
	@Autowired private WfNotificationHelper notificationHelper;
	@Autowired private WorkItemManager workItemManager;
	@Autowired private TemporaryHelper temporaryHelper;
	@Autowired private MiscDataUtil miscDataUtil;
	@Autowired private AuthorizationHelper authorizationHelper;
	@Autowired private ApprovalSchemaExecutionInformationHelper approvalSchemaExecutionInformationHelper;
	@Autowired private TaskManager taskManager;
	@Autowired private RepositoryService repositoryService;
	@Autowired private WfExpressionEvaluationHelper expressionEvaluationHelper;

    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

	@PostConstruct
	public void initialize() {
		LOGGER.debug("Workflow manager starting.");
	}

	//region Work items
	@Override
    public void completeWorkItem(WorkItemId workItemId, boolean decision, String comment, ObjectDelta additionalDelta,
		    WorkItemEventCauseInformationType causeInformation, Task task, OperationResult parentResult)
			throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
	    try {
		    workItemManager.completeWorkItem(workItemId, ApprovalUtils.toUri(decision), comment, additionalDelta,
				    causeInformation, task, parentResult);
	    } catch (ObjectAlreadyExistsException e) {
		    throw new IllegalStateException(e);
	    }
    }

    @Override
    public void claimWorkItem(WorkItemId workItemId, Task task, OperationResult result)
		    throws ObjectNotFoundException, SecurityViolationException, SchemaException {
        workItemManager.claimWorkItem(workItemId, task, result);
    }

    @Override
    public void releaseWorkItem(WorkItemId workItemId, Task task, OperationResult result)
		    throws SecurityViolationException, ObjectNotFoundException, SchemaException {
        workItemManager.releaseWorkItem(workItemId, task, result);
    }

    @Override
    public void delegateWorkItem(WorkItemId workItemId, List<ObjectReferenceType> delegates, WorkItemDelegationMethodType method,
		    Task task, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        workItemManager.delegateWorkItem(workItemId, delegates, method, null, null, null, task, parentResult);
    }
    //endregion

	//region Process instances (cases)
    @Override
    public void stopProcessInstance(String caseOid, Task task, OperationResult parentResult)
		    throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        processInstanceManager.closeCase(caseOid, task, parentResult);
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

    public TemporaryHelper getTemporaryHelper() {
        return temporaryHelper;
    }

    public MiscDataUtil getMiscDataUtil() {
        return miscDataUtil;
    }

    @Override
    public void registerProcessListener(ProcessListener processListener) {
        notificationHelper.registerProcessListener(processListener);
    }

    @Override
    public void registerWorkItemListener(WorkItemListener workItemListener) {
	    notificationHelper.registerWorkItemListener(workItemListener);
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
	public ChangesByState getChangesByState(TaskType rootTask, ModelInteractionService modelInteractionService, PrismContext prismContext,
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {

		// TODO op subresult
		return miscDataUtil.getChangesByStateForRoot(rootTask, modelInteractionService, prismContext, task, result);
	}

	@Override
	public ChangesByState getChangesByState(TaskType childTask, TaskType rootTask, ModelInteractionService modelInteractionService, PrismContext prismContext,
			OperationResult result) throws SchemaException, ObjectNotFoundException {

		// TODO op subresult
		return miscDataUtil.getChangesByStateForChild(childTask, rootTask, modelInteractionService, prismContext, result);
	}

	@Override
	public ApprovalSchemaExecutionInformationType getApprovalSchemaExecutionInformation(String taskOid, Task opTask,
			OperationResult parentResult)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
			SecurityViolationException, ExpressionEvaluationException {
		OperationResult result = parentResult.createSubresult(DOT_INTERFACE + ".getApprovalSchemaExecutionInformation");
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
		OperationResult result = parentResult.createSubresult(DOT_INTERFACE + ".getApprovalSchemaPreview");
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

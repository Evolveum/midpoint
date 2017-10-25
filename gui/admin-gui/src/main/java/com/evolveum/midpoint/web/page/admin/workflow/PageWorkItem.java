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
package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.DefaultAjaxButton;
import com.evolveum.midpoint.web.component.DefaultAjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.schema.GetOperationOptions.resolveItemsNamed;
import static com.evolveum.midpoint.schema.GetOperationOptions.retrieveItemsNamed;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_PARENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_REQUESTER_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_WORK_ITEM;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType.F_ASSIGNEE_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType.F_ORIGINAL_ASSIGNEE_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType.F_EXTERNAL_ID;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/workItem", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminWorkItems.AUTH_APPROVALS_ALL,
                label = PageAdminWorkItems.AUTH_APPROVALS_ALL_LABEL,
                description = PageAdminWorkItems.AUTH_APPROVALS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_WORK_ITEM_URL,
                label = "PageWorkItem.auth.workItem.label",
                description = "PageWorkItem.auth.workItem.description")})
public class PageWorkItem extends PageAdminWorkItems {

	private static final String DOT_CLASS = PageWorkItem.class.getName() + ".";
    private static final String OPERATION_LOAD_WORK_ITEM = DOT_CLASS + "loadWorkItem";
    private static final String OPERATION_SAVE_WORK_ITEM = DOT_CLASS + "saveWorkItem";
    private static final String OPERATION_DELEGATE_WORK_ITEM = DOT_CLASS + "delegateWorkItem";
    private static final String OPERATION_CLAIM_WORK_ITEM = DOT_CLASS + "claimWorkItem";
    private static final String OPERATION_RELEASE_WORK_ITEM = DOT_CLASS + "releaseWorkItem";

    private static final String ID_SUMMARY_PANEL = "summaryPanel";
    private static final String ID_WORK_ITEM_PANEL = "workItemPanel";

    private static final Trace LOGGER = TraceManager.getTrace(PageWorkItem.class);
	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_CLAIM = "claim";
	private static final String ID_RELEASE = "release";
	private static final String ID_APPROVE = "approve";
	private static final String ID_REJECT = "reject";
	private static final String ID_DELEGATE = "delegate";
	private static final String ID_CANCEL = "cancel";

	private LoadableModel<WorkItemDto> workItemDtoModel;
	private String taskId;

    public PageWorkItem(PageParameters parameters) {

		taskId = parameters.get(OnePageParameterEncoder.PARAMETER).toString();
		if (taskId == null) {
			throw new IllegalStateException("Work item ID not specified.");
		}

        workItemDtoModel = new LoadableModel<WorkItemDto>(false) {
            @Override
            protected WorkItemDto load() {
                return loadWorkItemDtoIfNecessary();
            }
        };

        initLayout();
    }

	@Override
	protected void createBreadcrumb() {
		createInstanceBreadcrumb();			// to preserve page state (e.g. approver's comment)
	}

	private WorkItemDto loadWorkItemDtoIfNecessary() {
        if (workItemDtoModel.isLoaded()) {
            return workItemDtoModel.getObject();
        }
        Task task = createSimpleTask(OPERATION_LOAD_WORK_ITEM);
        OperationResult result = task.getResult();
        WorkItemDto workItemDto = null;
        try {
			final ObjectQuery query = QueryBuilder.queryFor(WorkItemType.class, getPrismContext())
					.item(F_EXTERNAL_ID).eq(taskId)
					.build();
			final Collection<SelectorOptions<GetOperationOptions>> options =
					resolveItemsNamed(F_ASSIGNEE_REF, F_ORIGINAL_ASSIGNEE_REF);
			List<WorkItemType> workItems = getModelService().searchContainers(WorkItemType.class, query, options, task, result);
			if (workItems.size() > 1) {
				throw new SystemException("More than one work item with ID of " + taskId);
			} else if (workItems.size() == 0) {
				throw new ObjectNotFoundException("No work item with ID of " + taskId);
			}
			final WorkItemType workItem = workItems.get(0);

			final String taskOid = WfContextUtil.getTaskOid(workItem);
			if (taskOid == null) {
				// this is a problem ... most probably we will not be able to do anything reasonable - let's give it up
				result.recordFatalError(getString("PageWorkItem.noRequest"));
				showResult(result, false);
				throw redirectBackViaRestartResponseException();
			}
			TaskType taskType = null;
			List<TaskType> relatedTasks = new ArrayList<>();
			final Collection<SelectorOptions<GetOperationOptions>> getTaskOptions = resolveItemsNamed(
					new ItemPath(F_WORKFLOW_CONTEXT, F_REQUESTER_REF));
			getTaskOptions.addAll(retrieveItemsNamed(new ItemPath(F_WORKFLOW_CONTEXT, F_WORK_ITEM)));
			try {
				taskType = getModelService().getObject(TaskType.class, taskOid, getTaskOptions, task, result).asObjectable();
			} catch (AuthorizationException e) {
				LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Access to the task {} was denied", e, taskOid);
			}
			if (taskType != null && taskType.getParent() != null) {
				final ObjectQuery relatedTasksQuery = QueryBuilder.queryFor(TaskType.class, getPrismContext())
						.item(F_PARENT).eq(taskType.getParent())
						.build();
				List<PrismObject<TaskType>> relatedTaskObjects = getModelService()
						.searchObjects(TaskType.class, relatedTasksQuery, null, task, result);
				for (PrismObject<TaskType> relatedObject : relatedTaskObjects) {
					relatedTasks.add(relatedObject.asObjectable());
				}
			}
			workItemDto = new WorkItemDto(workItem, taskType, relatedTasks);
			workItemDto.prepareDeltaVisualization("pageWorkItem.delta", getPrismContext(), getModelInteractionService(), task,
					result);
			result.recordSuccessIfUnknown();
		} catch (RestartResponseException e) {
        	throw e;	// already processed
        } catch (ObjectNotFoundException ex) {
			result.recordFatalError(getString("PageWorkItem.couldNotGetWorkItem"), ex);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get work item because it does not exist. (It might have been already completed or deleted.)", ex);
        } catch (CommonException|RuntimeException ex) {
            result.recordFatalError("Couldn't get work item.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get work item.", ex);
        }
        showResult(result, false);
        if (!result.isSuccess()) {
        	throw redirectBackViaRestartResponseException();
        }
        return workItemDto;
    }

    private void initLayout() {
        WorkItemSummaryPanel summaryPanel = new WorkItemSummaryPanel(ID_SUMMARY_PANEL,
                new PropertyModel<>(workItemDtoModel, WorkItemDto.F_WORK_ITEM), workItemDtoModel, this);
        add(summaryPanel);

        Form mainForm = new Form(ID_MAIN_FORM);
        mainForm.setMultiPart(true);
        add(mainForm);

        mainForm.add(new WorkItemPanel(ID_WORK_ITEM_PANEL, workItemDtoModel, mainForm, this));

        initButtons(mainForm);
    }

    public WorkItemPanel getWorkItemPanel() {
    	return (WorkItemPanel) get(ID_MAIN_FORM).get(ID_WORK_ITEM_PANEL);
	}

    private void initButtons(Form mainForm) {

        VisibleBehaviour isAllowedToSubmit = new VisibleBehaviour(() ->
				{
					try {
						return getWorkflowManager().isCurrentUserAuthorizedToSubmit(workItemDtoModel.getObject().getWorkItem(), getPageTask(), getPageTask().getResult());
					} catch (ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
						LoggingUtils.logUnexpectedException(LOGGER, "Authorization error: " + e.getMessage(), e);
						return false;
					}
				});

		VisibleBehaviour isAllowedToDelegate = new VisibleBehaviour(() ->
				{
					try {
						return getWorkflowManager().isCurrentUserAuthorizedToDelegate(workItemDtoModel.getObject().getWorkItem(), getPageTask(), getPageTask().getResult());
					} catch (ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
						LoggingUtils.logUnexpectedException(LOGGER, "Authorization error: " + e.getMessage(), e);
						return false;
					}
				});

		VisibleBehaviour isAllowedToClaim = new VisibleBehaviour(() ->
				workItemDtoModel.getObject().getWorkItem().getAssigneeRef() == null &&
                        getWorkflowManager().isCurrentUserAuthorizedToClaim(workItemDtoModel.getObject().getWorkItem()));

        VisibleBehaviour isAllowedToRelease = new VisibleBehaviour(() -> {
			WorkItemType workItem = workItemDtoModel.getObject().getWorkItem();
			MidPointPrincipal principal;
			try {
				principal = (MidPointPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			} catch (ClassCastException e) {
				return false;
			}
			String principalOid = principal.getOid();
			return workItem.getAssigneeRef() != null
					&& workItem.getAssigneeRef().stream().anyMatch(ref -> ref.getOid().equals(principalOid))
					&& (!workItem.getCandidateRef().isEmpty());
		});

        AjaxSubmitButton claim = new DefaultAjaxSubmitButton(ID_CLAIM, createStringResource("pageWorkItem.button.claim"),
				this, (target, form) -> claimPerformed(target));
        claim.add(isAllowedToClaim);
        mainForm.add(claim);

        AjaxSubmitButton release = new DefaultAjaxSubmitButton(ID_RELEASE, createStringResource("pageWorkItem.button.release"),
				this, (target, form) -> releasePerformed(target));
        release.add(isAllowedToRelease);
        mainForm.add(release);

        AjaxSubmitButton approve = new DefaultAjaxSubmitButton(ID_APPROVE, createStringResource("pageWorkItem.button.approve"),
				this, (target, form) -> savePerformed(target, true));
        approve.add(isAllowedToSubmit);
        mainForm.add(approve);

		AjaxSubmitButton reject = new DefaultAjaxSubmitButton(ID_REJECT, createStringResource("pageWorkItem.button.reject"),
				this, (target, form) -> savePerformed(target, false));
        reject.add(isAllowedToSubmit);
        mainForm.add(reject);

		AjaxSubmitButton delegate = new DefaultAjaxSubmitButton(ID_DELEGATE, createStringResource("pageWorkItem.button.delegate"),
				this, (target, form) -> delegatePerformed(target));
        delegate.add(isAllowedToDelegate);
        mainForm.add(delegate);

        AjaxButton cancel = new DefaultAjaxButton(ID_CANCEL, createStringResource("pageWorkItem.button.cancel"), this::cancelPerformed);
        mainForm.add(cancel);
    }

	@SuppressWarnings("unused")
    private void cancelPerformed(AjaxRequestTarget target) {
        redirectBack();
    }

    private void savePerformed(AjaxRequestTarget target, boolean approved) {
        OperationResult result = new OperationResult(OPERATION_SAVE_WORK_ITEM);
        try {
			WorkItemDto dto = workItemDtoModel.getObject();
			if (approved) {
				boolean requiredFieldsPresent = getWorkItemPanel().checkRequiredFields();
				if (!requiredFieldsPresent) {
					target.add(getFeedbackPanel());
					return;
				}
			}
			ObjectDelta delta = getWorkItemPanel().getDeltaFromForm();
            getWorkflowService().completeWorkItem(dto.getWorkItemId(), approved, dto.getApproverComment(), delta, result);
        } catch (Exception ex) {
            result.recordFatalError("Couldn't save work item.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save work item", ex);
        }
		processResult(target, result, false);
	}

    private void delegatePerformed(AjaxRequestTarget target) {
		ObjectBrowserPanel<UserType> panel = new ObjectBrowserPanel<UserType>(
				getMainPopupBodyId(), UserType.class,
				Collections.singletonList(UserType.COMPLEX_TYPE), false, this, null) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSelectPerformed(AjaxRequestTarget target, UserType user) {
				hideMainPopup(target);
				delegateConfirmedPerformed(target, user);
			}

		};
		panel.setOutputMarkupId(true);
		showMainPopup(panel, target);
    }

	private void delegateConfirmedPerformed(AjaxRequestTarget target, UserType delegate) {
		OperationResult result = new OperationResult(OPERATION_DELEGATE_WORK_ITEM);
		try {
			WorkItemDto dto = workItemDtoModel.getObject();
			List<ObjectReferenceType> delegates = Collections.singletonList(ObjectTypeUtil.createObjectRef(delegate));
			getWorkflowService().delegateWorkItem(dto.getWorkItemId(), delegates, WorkItemDelegationMethodType.ADD_ASSIGNEES, result);
		} catch (Exception ex) {
			result.recordFatalError("Couldn't delegate work item.", ex);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delegate work item", ex);
		}
		processResult(target, result, false);
	}

	private void claimPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_CLAIM_WORK_ITEM);
        WorkflowService workflowService = getWorkflowService();
        try {
            workflowService.claimWorkItem(workItemDtoModel.getObject().getWorkItemId(), result);
        } catch (SecurityViolationException | ObjectNotFoundException | RuntimeException e) {
            result.recordFatalError("Couldn't claim work item due to an unexpected exception.", e);
        }
		processResult(target, result, true);
	}

    private void releasePerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_RELEASE_WORK_ITEM);
        WorkflowService workflowService = getWorkflowService();
        try {
            workflowService.releaseWorkItem(workItemDtoModel.getObject().getWorkItem().getExternalId(), result);
        } catch (SecurityViolationException | ObjectNotFoundException | RuntimeException e) {
            result.recordFatalError("Couldn't release work item due to an unexpected exception.", e);
        }
        processResult(target, result, true);
    }
}

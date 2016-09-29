/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
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
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.schema.GetOperationOptions.resolveItemsNamed;
import static com.evolveum.midpoint.schema.GetOperationOptions.retrieveItemsNamed;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_PARENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_REQUESTER_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_WORK_ITEM;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType.F_ASSIGNEE_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType.F_WORK_ITEM_ID;

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
    private static final String OPERATION_CLAIM_WORK_ITEM = DOT_CLASS + "claimWorkItem";
    private static final String OPERATION_RELEASE_WORK_ITEM = DOT_CLASS + "releaseWorkItem";

    private static final String ID_SUMMARY_PANEL = "summaryPanel";
    private static final String ID_WORK_ITEM_PANEL = "workItemPanel";

    private static final Trace LOGGER = TraceManager.getTrace(PageWorkItem.class);

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
                    .item(F_WORK_ITEM_ID).eq(taskId)
                    .build();
			final Collection<SelectorOptions<GetOperationOptions>> options = resolveItemsNamed(F_ASSIGNEE_REF);
			List<WorkItemType> workItems = getModelService().searchContainers(WorkItemType.class, query, options, task, result);
            if (workItems.size() > 1) {
                throw new SystemException("More than one work item with ID of " + taskId);
            } else if (workItems.size() == 0) {
                throw new SystemException("No work item with ID of " + taskId);
            }
			final WorkItemType workItem = workItems.get(0);

			final String taskOid = workItem.getTaskRef() != null ? workItem.getTaskRef().getOid() : null;
			TaskType taskType = null;
			List<TaskType> relatedTasks = new ArrayList<>();
			if (taskOid != null) {
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
			}
			workItemDto = new WorkItemDto(workItem, taskType, relatedTasks);
			workItemDto.prepareDeltaVisualization("pageWorkItem.delta", getPrismContext(), getModelInteractionService(), task, result);
            result.recordSuccessIfUnknown();
        } catch (CommonException|RuntimeException ex) {
            result.recordFatalError("Couldn't get work item.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get work item.", ex);
        }
        showResult(result, false);
        if (!result.isSuccess()) {
            throw getRestartResponseException(PageDashboard.class);
        }
        return workItemDto;
    }

    private void initLayout() {
        WorkItemSummaryPanel summaryPanel = new WorkItemSummaryPanel(ID_SUMMARY_PANEL,
                new PropertyModel<WorkItemType>(workItemDtoModel, WorkItemDto.F_WORK_ITEM), workItemDtoModel);
        add(summaryPanel);

        Form mainForm = new Form("mainForm");
        mainForm.setMultiPart(true);
        add(mainForm);

        mainForm.add(new WorkItemPanel(ID_WORK_ITEM_PANEL, workItemDtoModel, this));

        initButtons(mainForm);
    }

    private void initButtons(Form mainForm) {

        VisibleEnableBehaviour isAllowedToSubmit = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
            	OperationResult result = new OperationResult("initButtons");
                return getWorkflowManager().isCurrentUserAuthorizedToSubmit(workItemDtoModel.getObject().getWorkItem(), result);
            }
        };

        VisibleEnableBehaviour isAllowedToClaim = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return workItemDtoModel.getObject().getWorkItem().getAssigneeRef() == null &&
                        getWorkflowManager().isCurrentUserAuthorizedToClaim(workItemDtoModel.getObject().getWorkItem());
            }
        };

        VisibleEnableBehaviour isAllowedToRelease = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                WorkItemType workItem = workItemDtoModel.getObject().getWorkItem();
                MidPointPrincipal principal;
                try {
                    principal = (MidPointPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                } catch (ClassCastException e) {
                    return false;
                }
                String principalOid = principal.getOid();
                if (workItem.getAssigneeRef() == null || !workItem.getAssigneeRef().getOid().equals(principalOid)) {
                    return false;
                }
                return !workItem.getCandidateUsersRef().isEmpty() || !workItem.getCandidateRolesRef().isEmpty();
            }
        };

        AjaxSubmitButton claim = new AjaxSubmitButton("claim", createStringResource("pageWorkItem.button.claim")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                claimPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        claim.add(isAllowedToClaim);
        mainForm.add(claim);

        AjaxSubmitButton release = new AjaxSubmitButton("release", createStringResource("pageWorkItem.button.release")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                releasePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        release.add(isAllowedToRelease);
        mainForm.add(release);

        AjaxSubmitButton approve = new AjaxSubmitButton("approve", createStringResource("pageWorkItem.button.approve")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target, true);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        approve.add(isAllowedToSubmit);
        mainForm.add(approve);

        AjaxSubmitButton reject = new AjaxSubmitButton("reject", createStringResource("pageWorkItem.button.reject")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target, false);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        reject.add(isAllowedToSubmit);
        mainForm.add(reject);

        AjaxButton cancel = new AjaxButton("cancel", createStringResource("pageWorkItem.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(cancel);
    }

	@SuppressWarnings("unused")
    private void cancelPerformed(AjaxRequestTarget target) {
        redirectBack();
    }

    private void savePerformed(AjaxRequestTarget target, boolean decision) {
        LOGGER.debug("Saving work item changes.");

        OperationResult result = new OperationResult(OPERATION_SAVE_WORK_ITEM);

        try {
			WorkItemDto dto = workItemDtoModel.getObject();
            getWorkflowService().approveOrRejectWorkItem(dto.getWorkItemId(), decision, dto.getApproverComment(), result);
        } catch (Exception ex) {
            result.recordFatalError("Couldn't save work item.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save work item", ex);
        }

        result.computeStatusIfUnknown();

        if (!result.isSuccess()) {
            showResult(result, false);
            target.add(getFeedbackPanel());
        } else {
        	showResult(result);
            redirectBack();
        }
    }

    private void claimPerformed(AjaxRequestTarget target) {

        OperationResult result = new OperationResult(OPERATION_CLAIM_WORK_ITEM);
        WorkflowService workflowService = getWorkflowService();
        try {
            workflowService.claimWorkItem(workItemDtoModel.getObject().getWorkItemId(), result);
        } catch (SecurityViolationException | ObjectNotFoundException | RuntimeException e) {
            result.recordFatalError("Couldn't claim work item due to an unexpected exception.", e);
        }
        result.computeStatusIfUnknown();

        if (!result.isSuccess()) {
            showResult(result);
            target.add(getFeedbackPanel());
        } else {
        	showResult(result);
            redirectBack();
        }
    }

    private void releasePerformed(AjaxRequestTarget target) {

        OperationResult result = new OperationResult(OPERATION_RELEASE_WORK_ITEM);
        WorkflowService workflowService = getWorkflowService();
        try {
            workflowService.releaseWorkItem(workItemDtoModel.getObject().getWorkItem().getWorkItemId(), result);
        } catch (SecurityViolationException | ObjectNotFoundException | RuntimeException e) {
            result.recordFatalError("Couldn't release work item due to an unexpected exception.", e);
        }
        result.computeStatusIfUnknown();

        if (!result.isSuccess()) {
            showResult(result);
            target.add(getFeedbackPanel());
        } else {
        	showResult(result);
            redirectBack();
        }
    }

}

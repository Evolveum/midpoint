/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.wf.WorkItemsPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDtoProvider;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;

import java.util.List;

/**
 * @author lazyman
 */
public abstract class PageWorkItems extends PageAdminWorkItems {

    //private static final Trace LOGGER = TraceManager.getTrace(PageWorkItems.class);
    private static final String DOT_CLASS = PageWorkItems.class.getName() + ".";
    private static final String OPERATION_APPROVE_OR_REJECT_ITEMS = DOT_CLASS + "approveOrRejectItems";
    private static final String OPERATION_APPROVE_OR_REJECT_ITEM = DOT_CLASS + "approveOrRejectItem";
    private static final String OPERATION_CLAIM_ITEMS = DOT_CLASS + "claimItems";
    private static final String OPERATION_CLAIM_ITEM = DOT_CLASS + "claimItem";
    private static final String OPERATION_RELEASE_ITEMS = DOT_CLASS + "releaseItems";
    private static final String OPERATION_RELEASE_ITEM = DOT_CLASS + "releaseItem";
    private static final String ID_WORK_ITEMS_PANEL = "workItemsPanel";

    private static final String ID_MAIN_FORM = "mainForm";

    private boolean claimable;
	private boolean all;

    public PageWorkItems(boolean claimable, boolean all) {
        this.claimable = claimable;
		this.all = all;
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        WorkItemsPanel panel = new WorkItemsPanel(ID_WORK_ITEMS_PANEL, new WorkItemDtoProvider(PageWorkItems.this, claimable, all),
                UserProfileStorage.TableId.PAGE_WORK_ITEMS, (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_WORK_ITEMS),
				WorkItemsPanel.View.FULL_LIST);

        panel.setOutputMarkupId(true);
        mainForm.add(panel);

        initItemButtons(mainForm);
    }

    private void initItemButtons(Form mainForm) {
        AjaxButton claim = new AjaxButton("claim", createStringResource("pageWorkItems.button.claim")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                claimWorkItemsPerformed(target);
            }
        };
        claim.setVisible(!all && claimable);
        mainForm.add(claim);

        AjaxButton release = new AjaxButton("release", createStringResource("pageWorkItems.button.release")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                releaseWorkItemsPerformed(target);
            }
        };
        release.setVisible(!all && !claimable);
        mainForm.add(release);

        // the following are shown irrespectively of whether the work item is assigned or not
        AjaxButton approve = new AjaxButton("approve",
                createStringResource("pageWorkItems.button.approve")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                approveOrRejectWorkItemsPerformed(target, true);
            }
        };
        mainForm.add(approve);

        AjaxButton reject = new AjaxButton("reject",
                createStringResource("pageWorkItems.button.reject")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                approveOrRejectWorkItemsPerformed(target, false);
            }
        };
        mainForm.add(reject);
    }

    private boolean isSomeItemSelected(List<WorkItemDto> items, AjaxRequestTarget target) {
        if (!items.isEmpty()) {
            return true;
        }

        warn(getString("pageWorkItems.message.noItemSelected"));
        target.add(getFeedbackPanel());
        return false;
    }

    private WorkItemsPanel getWorkItemsPanel() {
        return (WorkItemsPanel) get(ID_MAIN_FORM).get(ID_WORK_ITEMS_PANEL);
    }

    private void approveOrRejectWorkItemsPerformed(AjaxRequestTarget target, boolean approve) {
        List<WorkItemDto> workItemDtoList = getWorkItemsPanel().getSelectedWorkItems();
        if (!isSomeItemSelected(workItemDtoList, target)) {
            return;
        }

        OperationResult mainResult = new OperationResult(OPERATION_APPROVE_OR_REJECT_ITEMS);
        WorkflowService workflowService = getWorkflowService();
        for (WorkItemDto workItemDto : workItemDtoList) {
            OperationResult result = mainResult.createSubresult(OPERATION_APPROVE_OR_REJECT_ITEM);
            try {
                workflowService.completeWorkItem(workItemDto.getWorkItemId(), approve, null, null, result);
                result.computeStatus();
            } catch (Exception e) {
                result.recordPartialError("Couldn't approve/reject work item due to an unexpected exception.", e);
            }
        }
        if (mainResult.isUnknown()) {
            mainResult.recomputeStatus();
        }

        if (mainResult.isSuccess()) {
            mainResult.recordStatus(OperationResultStatus.SUCCESS, "The work item(s) have been successfully " + (approve ? "approved." : "rejected."));
        }

        showResult(mainResult);

        resetWorkItemCountModel();
		target.add(this);
    }

    private void claimWorkItemsPerformed(AjaxRequestTarget target) {
        List<WorkItemDto> workItemDtoList = getWorkItemsPanel().getSelectedWorkItems();
        if (!isSomeItemSelected(workItemDtoList, target)) {
            return;
        }

        OperationResult mainResult = new OperationResult(OPERATION_CLAIM_ITEMS);
        WorkflowService workflowService = getWorkflowService();
        for (WorkItemDto workItemDto : workItemDtoList) {
            OperationResult result = mainResult.createSubresult(OPERATION_CLAIM_ITEM);
            try {
                workflowService.claimWorkItem(workItemDto.getWorkItemId(), result);
                result.computeStatusIfUnknown();
            } catch (ObjectNotFoundException | SecurityViolationException | RuntimeException e) {
                result.recordPartialError("Couldn't claim work item due to an unexpected exception.", e);
            }
        }
        if (mainResult.isUnknown()) {
            mainResult.recomputeStatus();
        }

        if (mainResult.isSuccess()) {
            mainResult.recordStatus(OperationResultStatus.SUCCESS, "The work item(s) have been successfully claimed.");
        }

        showResult(mainResult);

		resetWorkItemCountModel();
		target.add(this);
    }

    private void releaseWorkItemsPerformed(AjaxRequestTarget target) {
        List<WorkItemDto> workItemDtoList = getWorkItemsPanel().getSelectedWorkItems();
        if (!isSomeItemSelected(workItemDtoList, target)) {
            return;
        }

        OperationResult mainResult = new OperationResult(OPERATION_RELEASE_ITEMS);
        WorkflowService workflowService = getWorkflowService();
        for (WorkItemDto workItemDto : workItemDtoList) {
            OperationResult result = mainResult.createSubresult(OPERATION_RELEASE_ITEM);
            try {
                workflowService.releaseWorkItem(workItemDto.getWorkItemId(), result);
                result.computeStatusIfUnknown();
            } catch (ObjectNotFoundException | SecurityViolationException | RuntimeException e) {
                result.recordPartialError("Couldn't release work item due to an unexpected exception.", e);
            }
        }
        if (mainResult.isUnknown()) {
            mainResult.recomputeStatus();
        }

        if (mainResult.isSuccess()) {
            mainResult.recordStatus(OperationResultStatus.SUCCESS, "The work item(s) have been successfully released.");
        }

        showResult(mainResult);

		resetWorkItemCountModel();
		target.add(this);
    }
}

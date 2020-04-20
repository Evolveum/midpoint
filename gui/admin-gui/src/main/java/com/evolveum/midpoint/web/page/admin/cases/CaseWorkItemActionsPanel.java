/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.CaseWorkItemUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.Collections;

/**
 * Created by honchar
 */
public class CaseWorkItemActionsPanel extends BasePanel<CaseWorkItemType> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(CaseWorkItemListWithDetailsPanel.class);

    private static final String DOT_CLASS = CaseWorkItemActionsPanel.class.getName() + ".";
    private static final String OPERATION_SAVE_WORK_ITEM = DOT_CLASS + "saveWorkItem";
    private static final String OPERATION_CLAIM_ITEMS = DOT_CLASS + "claimItem";
    private static final String OPERATION_FORWARD_WORK_ITEM = DOT_CLASS + "forwardWorkItem";
    private static final String OPERATION_COMPLETE_WORK_ITEM = DOT_CLASS + "completeWorkItem";
    private static final String OPERATION_CHECK_SUBMIT_ACTION_AUTHORIZATION = DOT_CLASS + "isAuthorizedToApproveAndReject";
    private static final String OPERATION_CHECK_DELEGATE_AUTHORIZATION = DOT_CLASS + "isAuthorizedToDelegate";

    private static final String ID_WORK_ITEM_APPROVE_BUTTON = "workItemApproveButton";
    private static final String ID_WORK_ITEM_REJECT_BUTTON = "workItemRejectButton";
    private static final String ID_WORK_ITEM_FORWARD_BUTTON = "workItemForwardButton";
    private static final String ID_WORK_ITEM_CLAIM_BUTTON = "workItemClaimButton";
    private static final String ID_ACTION_BUTTONS = "actionButtons";

    public CaseWorkItemActionsPanel(String id, IModel<CaseWorkItemType> caseWorkItemModel) {
        super(id, caseWorkItemModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer actionButtonsContainer = new WebMarkupContainer(ID_ACTION_BUTTONS);
        actionButtonsContainer.setOutputMarkupId(true);
        actionButtonsContainer.add(new VisibleBehaviour(() -> CaseWorkItemUtil.isCaseWorkItemNotClosed(CaseWorkItemActionsPanel.this.getModelObject())));
        add(actionButtonsContainer);

        AjaxButton workItemApproveButton = new AjaxButton(ID_WORK_ITEM_APPROVE_BUTTON, getApproveButtonTitleModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                OperationResult completionResult = new OperationResult(OPERATION_COMPLETE_WORK_ITEM);
                WebComponentUtil.workItemApproveActionPerformed(ajaxRequestTarget, getCaseWorkItemModelObject(), getWorkItemOutput(true),
                        getCustomForm(), getPowerDonor(), true, completionResult, getPageBase());
                afterActionFinished(ajaxRequestTarget);

            }
        };
        workItemApproveButton.add(new VisibleBehaviour(this::isApproveRejectButtonVisible));
        workItemApproveButton.setOutputMarkupId(true);
        actionButtonsContainer.add(workItemApproveButton);

        AjaxButton workItemRejectButton = new AjaxButton(ID_WORK_ITEM_REJECT_BUTTON, getRejectButtonTitleModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                OperationResult completionResult = new OperationResult(OPERATION_COMPLETE_WORK_ITEM);
                WebComponentUtil.workItemApproveActionPerformed(ajaxRequestTarget, getCaseWorkItemModelObject(), getWorkItemOutput(false),
                        getCustomForm(), getPowerDonor(), false, completionResult, getPageBase());
                afterActionFinished(ajaxRequestTarget);
            }
        };
        workItemRejectButton.setOutputMarkupId(true);
        workItemRejectButton.add(new VisibleBehaviour(this::isApproveRejectButtonVisible));
        actionButtonsContainer.add(workItemRejectButton);

        AjaxButton workItemForwardButton = new AjaxButton(ID_WORK_ITEM_FORWARD_BUTTON,
                createStringResource("pageWorkItem.button.forward")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                forwardPerformed(ajaxRequestTarget);
            }
        };
        workItemForwardButton.setOutputMarkupId(true);
        workItemForwardButton.add(new VisibleBehaviour(this::isForwardButtonVisible));
        actionButtonsContainer.add(workItemForwardButton);

        AjaxButton workItemClaimButton = new AjaxButton(ID_WORK_ITEM_CLAIM_BUTTON,
                createStringResource("pageWorkItem.button.claim")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                claimWorkItemPerformed(ajaxRequestTarget);
            }
        };
        workItemClaimButton.add(new VisibleBehaviour(() -> isClaimButtonVisible()));
        workItemClaimButton.setOutputMarkupId(true);

        actionButtonsContainer.add(workItemClaimButton);
    }

    private CaseWorkItemType getCaseWorkItemModelObject(){
        return getModelObject();
    }

    protected AbstractWorkItemOutputType getWorkItemOutput(boolean approved) {
        return new AbstractWorkItemOutputType(getPrismContext())
                .outcome(ApprovalUtils.toUri(approved));
    }

    protected WorkItemDelegationRequestType getDelegationRequest(UserType delegate) {
        PrismContext prismContext = getPrismContext();
        return new WorkItemDelegationRequestType(prismContext)
                .delegate(ObjectTypeUtil.createObjectRef(delegate, prismContext))
                .method(WorkItemDelegationMethodType.REPLACE_ASSIGNEES);
    }

    private void forwardPerformed(AjaxRequestTarget target) {
        ObjectBrowserPanel<UserType> panel = new ObjectBrowserPanel<UserType>(
                getPageBase().getMainPopupBodyId(), UserType.class,
                Collections.singletonList(UserType.COMPLEX_TYPE), false, getPageBase(), null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectPerformed(AjaxRequestTarget target, UserType user) {
                PageBase pageBase = CaseWorkItemActionsPanel.this.getPageBase();
                pageBase.hideMainPopup(target);
                ConfirmationPanel confirmationPanel = new ConfirmationPanel(pageBase.getMainPopupBodyId(),
                        createStringResource("CaseWorkItemActionsPanel.forwardConfirmationMessage", user.getName().getOrig())){
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        forwardConfirmedPerformed(target, user);
                    }

                    @Override
                    public StringResourceModel getTitle() {
                        return new StringResourceModel("CaseWorkItemActionsPanel.forwardConfirmationTitle");
                    }
                };
                pageBase.showMainPopup(confirmationPanel, target);
            }

        };
        panel.setOutputMarkupId(true);
        getPageBase().showMainPopup(panel, target);
    }

    private void forwardConfirmedPerformed(AjaxRequestTarget target, UserType delegate) {
        Task task = getPageBase().createSimpleTask(OPERATION_FORWARD_WORK_ITEM);
        OperationResult result = task.getResult();
        try {
            try {
                WebComponentUtil.assumePowerOfAttorneyIfRequested(result, getPowerDonor(), getPageBase());
                WorkItemDelegationRequestType delegationRequest = getDelegationRequest(delegate);
                getPageBase().getWorkflowService().delegateWorkItem(WorkItemId.of(getModelObject()), delegationRequest, task, result);
            } finally {
                WebComponentUtil.dropPowerOfAttorneyIfRequested(result, getPowerDonor(), getPageBase());
            }
        } catch (Exception ex) {
            result.recordFatalError(getString("CaseWorkItemActionsPanel.message.forwardConfirmedPerformed.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't forward work item", ex);
        }
        getPageBase().processResult(target, result, false);
        afterActionFinished(target);
    }

    private void claimWorkItemPerformed(AjaxRequestTarget target){
        WebComponentUtil.claimWorkItemActionPerformed(getModelObject(), OPERATION_CLAIM_ITEMS, target, getPageBase());

    }

    protected void afterActionFinished(AjaxRequestTarget target){
        getPageBase().redirectBack();
    }

    protected Component getCustomForm() {
        return null;
    }

    public PrismObject<UserType> getPowerDonor() {
        return null;
    }

    private IModel<String> getApproveButtonTitleModel() {
        return new IModel<String>() {
            @Override
            public String getObject() {
                CaseType parentCase = CaseWorkItemUtil.getCase(getCaseWorkItemModelObject());
                return CaseTypeUtil.isManualProvisioningCase(parentCase) ?
                        createStringResource("pageWorkItem.button.manual.doneSuccessfully").getString() :
                        createStringResource("pageWorkItem.button.approve").getString();
            }
        };
    }

    private IModel<String> getRejectButtonTitleModel() {
        return new IModel<String>() {
            @Override
            public String getObject() {
                CaseType parentCase = CaseWorkItemUtil.getCase(getCaseWorkItemModelObject());
                return CaseTypeUtil.isManualProvisioningCase(parentCase) ?
                        createStringResource("pageWorkItem.button.manual.operationFailed").getString()
                        : createStringResource("pageWorkItem.button.reject").getString();
            }
        };
    }

    private boolean isApproveRejectButtonVisible() {
        if (CaseWorkItemUtil.isCaseWorkItemClosed(getModelObject()) ||
                CaseWorkItemUtil.isWorkItemClaimable(getModelObject())) {
            return false; // checking separately to avoid needless authorization checking
        }
        try {
            OperationResult result = new OperationResult(OPERATION_CHECK_SUBMIT_ACTION_AUTHORIZATION);
            Task task = getPageBase().createSimpleTask(OPERATION_CHECK_SUBMIT_ACTION_AUTHORIZATION);
            return WebComponentUtil.runUnderPowerOfAttorneyIfNeeded(() ->
                            getPageBase().getWorkflowManager().isCurrentUserAuthorizedToSubmit(getModelObject(), task, result),
                    getPowerDonor(), getPageBase(), task, result);
        } catch (Exception ex) {
            LOGGER.error("Cannot check current user authorization to submit work item: {}", ex.getLocalizedMessage(), ex);
            return false;
        }
    }

    private boolean isForwardButtonVisible() {
        if (CaseWorkItemUtil.isCaseWorkItemClosed(getModelObject()) ||
                CaseWorkItemUtil.isWorkItemClaimable(getModelObject())) {
            return false; // checking separately to avoid needless authorization checking
        }
        try {
            OperationResult result = new OperationResult(OPERATION_CHECK_DELEGATE_AUTHORIZATION);
            Task task = getPageBase().createSimpleTask(OPERATION_CHECK_DELEGATE_AUTHORIZATION);
            return WebComponentUtil.runUnderPowerOfAttorneyIfNeeded(() ->
                            getPageBase().getWorkflowManager().isCurrentUserAuthorizedToDelegate(getModelObject(), task, result),
                    getPowerDonor(), getPageBase(), task, result);
        } catch (Exception ex) {
            LOGGER.error("Cannot check current user authorization to submit work item: {}", ex.getLocalizedMessage(), ex);
            return false;
        }
    }

    private boolean isClaimButtonVisible() {
        return CaseWorkItemUtil.isCaseWorkItemNotClosed(getModelObject()) &&
                CaseWorkItemUtil.isWorkItemClaimable(getModelObject()) &&
                getPageBase().getWorkflowManager().isCurrentUserAuthorizedToClaim(getModelObject());
    }

}

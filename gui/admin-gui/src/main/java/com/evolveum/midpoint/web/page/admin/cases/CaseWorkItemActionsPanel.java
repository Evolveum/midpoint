/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import java.util.Collections;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;

import com.evolveum.midpoint.prism.PrismContainerValue;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by honchar
 */
public class CaseWorkItemActionsPanel extends BasePanel<CaseWorkItemType> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(CaseWorkItemListWithDetailsPanel.class);

    private static final String DOT_CLASS = CaseWorkItemActionsPanel.class.getName() + ".";
    private static final String OPERATION_CLAIM_ITEMS = DOT_CLASS + "claimItem";
    private static final String OPERATION_RELEASE_ITEMS = DOT_CLASS + "releaseWorkItem";
    private static final String OPERATION_FORWARD_WORK_ITEM = DOT_CLASS + "forwardWorkItem";
    private static final String OPERATION_COMPLETE_WORK_ITEM = DOT_CLASS + "completeWorkItem";
    private static final String OPERATION_CHECK_SUBMIT_ACTION_AUTHORIZATION = DOT_CLASS + "isAuthorizedToApproveAndReject";
    private static final String OPERATION_CHECK_DELEGATE_AUTHORIZATION = DOT_CLASS + "isAuthorizedToDelegate";

    private static final String ID_WORK_ITEM_APPROVE_BUTTON = "workItemApproveButton";
    private static final String ID_WORK_ITEM_REJECT_BUTTON = "workItemRejectButton";
    private static final String ID_WORK_ITEM_FORWARD_BUTTON = "workItemForwardButton";
    private static final String ID_WORK_ITEM_CLAIM_BUTTON = "workItemClaimButton";
    private static final String ID_WORK_ITEM_RELEASE_BUTTON = "workItemReleaseButton";

    public CaseWorkItemActionsPanel(String id, IModel<CaseWorkItemType> caseWorkItemModel) {
        super(id, caseWorkItemModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);
        add(AttributeAppender.append("class", "d-flex gap-2"));
        add(new VisibleBehaviour(() -> CaseTypeUtil.isCaseWorkItemNotClosed(CaseWorkItemActionsPanel.this.getModelObject())));

        AjaxButton workItemApproveButton = new AjaxButton(ID_WORK_ITEM_APPROVE_BUTTON, getApproveButtonTitleModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                OperationResult completionResult = new OperationResult(OPERATION_COMPLETE_WORK_ITEM);
                PrismContainerValue<CaseWorkItemType> caseObj =
                        WebPrismUtil.cleanupEmptyContainerValue(getCaseWorkItemModelObject().asPrismContainerValue());
                WebComponentUtil.workItemApproveActionPerformed(ajaxRequestTarget, caseObj.asContainerable(),
                        getCustomForm(), getPowerDonor(), true, completionResult, getPageBase());
                afterActionFinished(ajaxRequestTarget);

            }
        };
        addAriaDescribedByForButton(workItemApproveButton);
        workItemApproveButton.add(new VisibleBehaviour(this::isApproveRejectButtonVisible));
        workItemApproveButton.setOutputMarkupId(true);
        add(workItemApproveButton);

        AjaxButton workItemUnclaimedButton = new AjaxButton(ID_WORK_ITEM_RELEASE_BUTTON,
                    createStringResource("pageWorkItems.button.reconsider")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                releaseWorkItemPerformed(ajaxRequestTarget);
                afterActionFinished(ajaxRequestTarget);

            }
        };
        addAriaDescribedByForButton(workItemUnclaimedButton);
        workItemUnclaimedButton.add(new VisibleBehaviour(this::isReleaseButtonVisible));
        workItemUnclaimedButton.setOutputMarkupId(true);
        add(workItemUnclaimedButton);

        AjaxButton workItemRejectButton = new AjaxButton(ID_WORK_ITEM_REJECT_BUTTON, getRejectButtonTitleModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                OperationResult completionResult = new OperationResult(OPERATION_COMPLETE_WORK_ITEM);
                PrismContainerValue<CaseWorkItemType> caseObj =
                        WebPrismUtil.cleanupEmptyContainerValue(getCaseWorkItemModelObject().asPrismContainerValue());
                WebComponentUtil.workItemApproveActionPerformed(ajaxRequestTarget, caseObj.asContainerable(),
                        getCustomForm(), getPowerDonor(), false, completionResult, getPageBase());
                afterActionFinished(ajaxRequestTarget);
            }
        };
        addAriaDescribedByForButton(workItemRejectButton);
        workItemRejectButton.setOutputMarkupId(true);
        workItemRejectButton.add(new VisibleBehaviour(this::isApproveRejectButtonVisible));
        add(workItemRejectButton);

        AjaxButton workItemForwardButton = new AjaxButton(ID_WORK_ITEM_FORWARD_BUTTON,
                createStringResource("pageWorkItem.button.forward")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                forwardPerformed(ajaxRequestTarget);
            }
        };
        addAriaDescribedByForButton(workItemForwardButton);
        workItemForwardButton.setOutputMarkupId(true);
        workItemForwardButton.add(new VisibleBehaviour(this::isForwardButtonVisible));
        add(workItemForwardButton);

        AjaxButton workItemClaimButton = new AjaxButton(ID_WORK_ITEM_CLAIM_BUTTON,
                createStringResource("pageWorkItem.button.claim")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                claimWorkItemPerformed(ajaxRequestTarget);
                afterActionFinished(ajaxRequestTarget);
            }
        };
        addAriaDescribedByForButton(workItemClaimButton);
        workItemClaimButton.add(new VisibleBehaviour(this::isClaimButtonVisible));
        workItemClaimButton.setOutputMarkupId(true);

        add(workItemClaimButton);
    }

    protected void addAriaDescribedByForButton(AjaxButton workItemApproveButton) {
    }

    private CaseWorkItemType getCaseWorkItemModelObject() {
        return getModelObject();
    }

    protected WorkItemDelegationRequestType getDelegationRequest(UserType delegate) {
        PrismContext prismContext = getPrismContext();
        return new WorkItemDelegationRequestType()
                .delegate(ObjectTypeUtil.createObjectRef(delegate))
                .method(WorkItemDelegationMethodType.REPLACE_ASSIGNEES);
    }

    private void forwardPerformed(AjaxRequestTarget target) {
        ObjectBrowserPanel<UserType> panel = new ObjectBrowserPanel<>(
                getPageBase().getMainPopupBodyId(), UserType.class,
                Collections.singletonList(UserType.COMPLEX_TYPE), false, getPageBase(), null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectPerformed(AjaxRequestTarget target, UserType user) {
                PageBase pageBase = CaseWorkItemActionsPanel.this.getPageBase();
                pageBase.hideMainPopup(target);
                forwardConfirmedPerformed(target, user);
            }

            @Override
            protected IModel<String> getWarningMessageModel() {
                return getPageBase().createStringResource("CaseWorkItemActionsPanel.forwardWarningMessage");
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
                getPageBase().getCaseService().delegateWorkItem(WorkItemId.of(getModelObject()), delegationRequest, task, result);
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

    private void claimWorkItemPerformed(AjaxRequestTarget target) {
        WebComponentUtil.claimWorkItemActionPerformed(getModelObject(), OPERATION_CLAIM_ITEMS, target, getPageBase());

    }

    private void releaseWorkItemPerformed(AjaxRequestTarget target) {
        WebComponentUtil.releaseWorkItemActionPerformed(getModelObject(), OPERATION_RELEASE_ITEMS, target, getPageBase());
    }

    protected void afterActionFinished(AjaxRequestTarget target) {
        getPageBase().redirectBack();
    }

    protected Component getCustomForm() {
        return null;
    }

    public PrismObject<UserType> getPowerDonor() {
        return null;
    }

    private IModel<String> getApproveButtonTitleModel() {
        return () -> {
            CaseType parentCase = CaseTypeUtil.getCase(getCaseWorkItemModelObject());
            return CaseTypeUtil.isManualProvisioningCase(parentCase)
                    ? createStringResource("pageWorkItem.button.manual.doneSuccessfully").getString()
                    : createStringResource("pageWorkItem.button.approve").getString();
        };
    }

    private IModel<String> getRejectButtonTitleModel() {
        return () -> {
            CaseType parentCase = CaseTypeUtil.getCase(getCaseWorkItemModelObject());
            return CaseTypeUtil.isManualProvisioningCase(parentCase)
                    ? createStringResource("pageWorkItem.button.manual.operationFailed").getString()
                    : createStringResource("pageWorkItem.button.reject").getString();
        };
    }

    private boolean isApproveRejectButtonVisible() {
        if (CaseTypeUtil.isCorrelationCase(CaseTypeUtil.getCase(getCaseWorkItemModelObject()))) {
            return false;
        }
        if (CaseTypeUtil.isCaseWorkItemClosed(getModelObject()) ||
                CaseTypeUtil.isWorkItemClaimable(getModelObject())) {
            return false; // checking separately to avoid needless authorization checking
        }
        try {
            OperationResult result = new OperationResult(OPERATION_CHECK_SUBMIT_ACTION_AUTHORIZATION);
            Task task = getPageBase().createSimpleTask(OPERATION_CHECK_SUBMIT_ACTION_AUTHORIZATION);
            return WebComponentUtil.runUnderPowerOfAttorneyIfNeeded(() ->
                            getPageBase().getCaseManager().isCurrentUserAuthorizedToComplete(getModelObject(), task, result),
                    getPowerDonor(), getPageBase(), task, result);
        } catch (Exception ex) {
            LOGGER.error("Cannot check current user authorization to submit work item: {}", ex.getLocalizedMessage(), ex);
            return false;
        }
    }

    private boolean isForwardButtonVisible() {
        if (CaseTypeUtil.isCaseWorkItemClosed(getModelObject()) ||
                CaseTypeUtil.isWorkItemClaimable(getModelObject())) {
            return false; // checking separately to avoid needless authorization checking
        }
        try {
            OperationResult result = new OperationResult(OPERATION_CHECK_DELEGATE_AUTHORIZATION);
            Task task = getPageBase().createSimpleTask(OPERATION_CHECK_DELEGATE_AUTHORIZATION);
            return WebComponentUtil.runUnderPowerOfAttorneyIfNeeded(() ->
                            getPageBase().getCaseManager().isCurrentUserAuthorizedToDelegate(getModelObject(), task, result),
                    getPowerDonor(), getPageBase(), task, result);
        } catch (Exception ex) {
            LOGGER.error("Cannot check current user authorization to submit work item: {}", ex.getLocalizedMessage(), ex);
            return false;
        }
    }

    private boolean isReleaseButtonVisible() {
        return CaseTypeUtil.isCaseWorkItemNotClosed(getModelObject()) &&
                CaseTypeUtil.isWorkItemReleasable(getModelObject()) &&
                getPageBase().getCaseManager().isCurrentUserAuthorizedToClaim(getModelObject());
    }

    private boolean isClaimButtonVisible() {
        return CaseTypeUtil.isCaseWorkItemNotClosed(getModelObject()) &&
                CaseTypeUtil.isWorkItemClaimable(getModelObject()) &&
                getPageBase().getCaseManager().isCurrentUserAuthorizedToClaim(getModelObject());
    }
}

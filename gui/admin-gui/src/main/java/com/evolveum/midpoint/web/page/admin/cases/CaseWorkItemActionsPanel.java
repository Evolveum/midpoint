/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
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
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import java.util.Collections;
import java.util.List;

/**
 * Created by honchar
 */
public class CaseWorkItemActionsPanel extends BasePanel<CaseWorkItemType> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(CaseWorkItemListWithDetailsPanel.class);

    private static final String DOT_CLASS = CaseWorkItemActionsPanel.class.getName() + ".";
    private static final String OPERATION_SAVE_WORK_ITEM = DOT_CLASS + "saveWorkItem";
    private static final String OPERATION_DELEGATE_WORK_ITEM = DOT_CLASS + "delegateWorkItem";
    private static final String OPERATION_COMPLETE_WORK_ITEM = DOT_CLASS + "completeWorkItem";


    private static final String ID_WORK_ITEM_APPROVE_BUTTON = "workItemApproveButton";
    private static final String ID_WORK_ITEM_REJECT_BUTTON = "workItemRejectButton";
    private static final String ID_WORK_ITEM_DELEGATE_BUTTON = "workItemDelegateButton";
    private static final String ID_ACTION_BUTTONS = "actionButtons";

    public CaseWorkItemActionsPanel(String id, IModel<CaseWorkItemType> caseWorkItemModel){
        super(id, caseWorkItemModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        WebMarkupContainer actionButtonsContainer = new WebMarkupContainer(ID_ACTION_BUTTONS);
        actionButtonsContainer.setOutputMarkupId(true);
        actionButtonsContainer.add(new VisibleBehaviour(() -> !isParentCaseClosed()));
        add(actionButtonsContainer);

        AjaxButton workItemApproveButton = new AjaxButton(ID_WORK_ITEM_APPROVE_BUTTON, getApproveButtonTitleModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                savePerformed(ajaxRequestTarget, getCaseWorkItemModelObject(), true);
            }
        };
        workItemApproveButton.setOutputMarkupId(true);
        actionButtonsContainer.add(workItemApproveButton);

        AjaxButton workItemRejectButton = new AjaxButton(ID_WORK_ITEM_REJECT_BUTTON, getRejectButtonTitleModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                savePerformed(ajaxRequestTarget, getCaseWorkItemModelObject(), false);
            }
        };
        workItemRejectButton.setOutputMarkupId(true);
        actionButtonsContainer.add(workItemRejectButton);

        AjaxButton workItemDelegateButton = new AjaxButton(ID_WORK_ITEM_DELEGATE_BUTTON,
                createStringResource("pageWorkItem.button.delegate")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                delegatePerformed(ajaxRequestTarget);
            }
        };
        workItemDelegateButton.setOutputMarkupId(true);
        actionButtonsContainer.add(workItemDelegateButton);
    }

    private CaseWorkItemType getCaseWorkItemModelObject(){
        return getModelObject();
    }

    private void savePerformed(AjaxRequestTarget target, CaseWorkItemType workItem, boolean approved) {
        CaseType parentCase = CaseWorkItemUtil.getCase(getCaseWorkItemModelObject());
        OperationResult result;
        if (CaseTypeUtil.isManualProvisioningCase(parentCase)){
            Task task = getPageBase().createSimpleTask(OPERATION_COMPLETE_WORK_ITEM);
            result = new OperationResult(OPERATION_COMPLETE_WORK_ITEM);
            try {
                AbstractWorkItemOutputType output = getCaseWorkItemModelObject().getOutput();
                if (output == null){
                    output = new AbstractWorkItemOutputType();
                }
                output.setOutcome(ApprovalUtils.toUri(approved));
                getPageBase().getCaseManagementService().completeWorkItem(parentCase.getOid(),
                        getCaseWorkItemModelObject().getId(), getCaseWorkItemModelObject().getOutput(), task, result);
            } catch (Exception ex){
                LoggingUtils.logUnexpectedException(LOGGER, "Unable to complete work item, ", ex);
                result.recordFatalError(ex);
            }
        } else {

            Task task = getPageBase().createSimpleTask(OPERATION_SAVE_WORK_ITEM);
            result = task.getResult();
            try {
                try {
                    Component formPanel = getCustomForm();
                    ObjectDelta additionalDelta = null;
                    if (formPanel != null && formPanel instanceof DynamicFormPanel) {
                        if (approved) {
                            boolean requiredFieldsPresent = ((DynamicFormPanel<?>) formPanel).checkRequiredFields(getPageBase());
                            if (!requiredFieldsPresent) {
                                target.add(getPageBase().getFeedbackPanel());
                                return;
                            }
                        }
                        additionalDelta = ((DynamicFormPanel<?>) formPanel).getObjectDelta();
                        if (additionalDelta != null) {
                            getPrismContext().adopt(additionalDelta);
                        }
                    }
                    assumePowerOfAttorneyIfRequested(result);
                    getPageBase().getWorkflowService().completeWorkItem(WorkItemId.of(workItem), approved, getApproverComment(),
                            additionalDelta, task, result);
                } finally {
                    dropPowerOfAttorneyIfRequested(result);
                }
            } catch (Exception ex) {
                result.recordFatalError("Couldn't save work item.", ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save work item", ex);
            }
        }
        getPageBase().processResult(target, result, false);
        getPageBase().redirectBack();
    }

    protected String getApproverComment(){
        return "";
    }

    private boolean isParentCaseClosed(){
        return CaseTypeUtil.isClosed(CaseWorkItemUtil.getCase(getCaseWorkItemModelObject()));
    }

    private void delegatePerformed(AjaxRequestTarget target) {
        ObjectBrowserPanel<UserType> panel = new ObjectBrowserPanel<UserType>(
                getPageBase().getMainPopupBodyId(), UserType.class,
                Collections.singletonList(UserType.COMPLEX_TYPE), false, getPageBase(), null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectPerformed(AjaxRequestTarget target, UserType user) {
                CaseWorkItemActionsPanel.this.getPageBase().hideMainPopup(target);
                delegateConfirmedPerformed(target, user);
            }

        };
        panel.setOutputMarkupId(true);
        getPageBase().showMainPopup(panel, target);
    }

    private void delegateConfirmedPerformed(AjaxRequestTarget target, UserType delegate) {
        Task task = getPageBase().createSimpleTask(OPERATION_DELEGATE_WORK_ITEM);
        OperationResult result = task.getResult();
        try {
            List<ObjectReferenceType> delegates = Collections.singletonList(ObjectTypeUtil.createObjectRef(delegate, getPrismContext()));
            try {
                assumePowerOfAttorneyIfRequested(result);
                getPageBase().getWorkflowService().delegateWorkItem(WorkItemId.of(getModelObject()), delegates, WorkItemDelegationMethodType.ADD_ASSIGNEES,
                        task, result);
            } finally {
                dropPowerOfAttorneyIfRequested(result);
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't delegate work item.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delegate work item", ex);
        }
        getPageBase().processResult(target, result, false);
        getPageBase().redirectBack();
    }

    protected Component getCustomForm() {
        return null;
    }

    private void assumePowerOfAttorneyIfRequested(OperationResult result) {
//        if (powerDonor != null) {
//            WebModelServiceUtils.assumePowerOfAttorney(powerDonor, getModelInteractionService(), getTaskManager(), result);
//        }
    }

    private void dropPowerOfAttorneyIfRequested(OperationResult result) {
//        if (powerDonor != null) {
//            WebModelServiceUtils.dropPowerOfAttorney(getModelInteractionService(), getTaskManager(), result);
//        }
    }

    private IModel<String> getApproveButtonTitleModel(){
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

    private IModel<String> getRejectButtonTitleModel(){
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

}

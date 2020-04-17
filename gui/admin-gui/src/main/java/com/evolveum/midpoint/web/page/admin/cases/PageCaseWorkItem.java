/*
 * Copyright (c) 2010-2018 Evolveum et al. and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.workflow.CaseWorkItemSummaryPanel;
import com.evolveum.midpoint.web.page.admin.workflow.PageAttorneySelection;
import com.evolveum.midpoint.web.page.admin.workflow.WorkItemDetailsPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_OBJECT_REF;

/**
 * @author mederly
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/workItem", matchUrlForSecurity = "/admin/workItem")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
                label = PageAdminCaseWorkItems.AUTH_CASE_WORK_ITEMS_ALL_LABEL,
                description = PageAdminCaseWorkItems.AUTH_CASE_WORK_ITEMS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_WORK_ITEM_URL,
                label = "PageCaseWorkItem.auth.caseWorkItem.label",
                description = "PageCaseWorkItem.auth.caseWorkItem.description")})
public class PageCaseWorkItem extends PageAdminCaseWorkItems {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageCaseWorkItem.class.getName() + ".";
    private static final String OPERATION_LOAD_CASE = DOT_CLASS + "loadCase";
    private static final String OPERATION_LOAD_DONOR = DOT_CLASS + "loadPowerDonor";
    private static final String OPERATION_SAVE_CASE_WORK_ITEM = DOT_CLASS + "closeCaseWorkItem";
    private static final String PARAMETER_CASE_ID = "caseId";
    private static final String PARAMETER_CASE_WORK_ITEM_ID = "caseWorkItemId";

    private static final Trace LOGGER = TraceManager.getTrace(PageCaseWorkItem.class);
    private static final String ID_WORK_ITEM_DETAILS = "workItemDetails";
    private static final String ID_SUMMARY_PANEL = "summaryPanel";
    private static final String ID_CASE_WORK_ITEM_ACTIONS_PANEL = "caseWorkItemActionsPanel";
    private static final String ID_DELTA_PANEL = "deltaPanel";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_CASE_WORK_ITEM_FORM = "caseWorkItemForm";
    private static final String ID_CASE_NAME = "caseName";
    private static final String ID_CASE_DESCRIPTION = "caseDescription";
    private static final String ID_CASE_RESOURCE = "caseResource";
    private static final String ID_CASE_TARGET = "caseTarget";
    private static final String ID_CASE_EVENT = "caseEvent";
    private static final String ID_CASE_OUTCOME = "caseOutcome";
    private static final String ID_CASE_OPEN_TIMESTAMP = "caseOpenTimestamp";
    private static final String ID_CASE_CLOSE_TIMESTAMP = "caseCloseTimestamp";
    private static final String ID_CASE_STATE = "caseState";
    private static final String ID_CASE_WORK_ITEM_NAME = "caseWorkItemName";
    private static final String ID_CASE_WORK_ITEM_ASSIGNEES = "caseWorkItemAssignees";
    private static final String ID_CASE_WORK_ITEM_ORIGINAL_ASSIGNEE = "caseWorkItemOriginalAssignee";
    private static final String ID_CASE_WORK_ITEM_CLOSE_TIMESTAMP = "caseWorkItemCloseTimestamp";
    private static final String ID_CASE_WORK_ITEM_DEADLINE = "caseWorkItemDeadline";
    private static final String ID_CASE_WORK_ITEM_OUTCOME = "caseWorkItemOutcome";
    private static final String ID_CASE_WORK_ITEM_COMMENT = "caseWorkItemComment";
    private static final String ID_CASE_WORK_ITEM_FORM_COMMENT = "caseWorkItemFormComment";
    private static final String ID_CASE_WORK_ITEM_FORM_EVIDENCE = "caseWorkItemFormEvidence";
    private static final String ID_CASE_WORK_ITEM_EVIDENCE = "caseWorkItemEvidence";
    private static final String ID_BACK_BUTTON = "backButton";
    private static final String ID_CLOSE_CASE_BUTTON = "closeCaseButton";

    private LoadableModel<CaseType> caseModel;
    private LoadableModel<CaseWorkItemType> caseWorkItemModel;
    private WorkItemId workItemId;
    private PageParameters pageParameters;

    public PageCaseWorkItem() {
        this(null);
    }

    public PageCaseWorkItem(CaseWorkItemType workItem) {
        this(workItem, null);
    }

    public PageCaseWorkItem(CaseWorkItemType workItem, PageParameters parameters) {
        this.pageParameters = parameters;

        caseWorkItemModel = new LoadableModel<CaseWorkItemType>(false) {
            @Override
            protected CaseWorkItemType load() {
                if (workItem != null) {
                    return workItem;
                } else {
                    getSession().error("Workitem model cannot be null");
                    throw redirectBackViaRestartResponseException();
                }
            }
        };

//        String caseId = parameters.get(OnePageParameterEncoder.PARAMETER).toString();
//        if (StringUtils.isEmpty(caseId)) {
//            throw new IllegalStateException("Case ID not specified.");
//        }
//        workItemId = WorkItemId.create(caseId);
//        if (workItemId == null || StringUtils.isEmpty(workItemId.getCaseOid())) {
//            throw new IllegalStateException("Case oid not specified.");
//        }
//
//        caseModel = new LoadableModel<CaseType>(false) {
//            @Override
//            protected CaseType load() {
//                return loadCaseIfNecessary();
//            }
//        };
//
//        caseWorkItemModel = new LoadableModel<CaseWorkItemType>(false) {
//            @Override
//            protected CaseWorkItemType load() {
//                return loadCaseWorkItemIfNecessary();
//            }
//        };

//        initLayout();
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    @Override
    protected void createBreadcrumb() {
        createInstanceBreadcrumb();            // to preserve page state (e.g. approver's comment)
    }

    private CaseType loadCaseIfNecessary() {
        if (caseModel.isLoaded()) {
            return caseModel.getObject();
        }
        Task task = createSimpleTask(OPERATION_LOAD_CASE);
        OperationResult result = task.getResult();
        CaseType caseInstance = null;
        try {
            //GetOperationOptionsBuilder optionsBuilder = getOperationOptionsBuilder().item(F_OBJECT_REF).resolve();
            PrismObject<CaseType> caseObject = WebModelServiceUtils.loadObject(CaseType.class, workItemId.getCaseOid(), null,//optionsBuilder.build(),
                    PageCaseWorkItem.this, task, result);
            caseInstance = caseObject.asObjectable();
            result.recordSuccessIfUnknown();
        } catch (RestartResponseException e) {
            throw e;    // already processed
        } catch (NullPointerException ex) {
            result.recordFatalError(getString("PageCaseWorkItem.couldNotGetCase"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get case because it does not exist. (It might have been already completed or deleted.)", ex);
        } catch (RuntimeException ex) {
            result.recordFatalError(getString("PageCaseWorkItem.couldNotGetCase.runtime"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get case.", ex);
        }
        showResult(result, false);
        if (!result.isSuccess()) {
            throw redirectBackViaRestartResponseException();
        }
        return caseInstance;
    }

    private CaseWorkItemType loadCaseWorkItemIfNecessary() {
        if (caseWorkItemModel.isLoaded()) {
            return caseWorkItemModel.getObject();
        }
        try {
            List<CaseWorkItemType> caseWorkItemList = caseModel.getObject().getWorkItem();
            if (caseWorkItemList == null) {
                throw new ObjectNotFoundException("No case work item found for case " + workItemId.getCaseOid() + " with id " + workItemId.getId());
            }
            for (CaseWorkItemType caseWorkItemType : caseWorkItemList){
                if (caseWorkItemType.getId().equals(workItemId.getId())){
                    return caseWorkItemType;
                }
            }
        } catch (ObjectNotFoundException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get case work item because it does not exist. (It might have been already completed or deleted.)", ex);
        } catch (NumberFormatException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse case work item id.", ex);
        }
        return null;
    }

    private void initLayout(){
        CaseWorkItemSummaryPanel summaryPanel = new CaseWorkItemSummaryPanel(ID_SUMMARY_PANEL, caseWorkItemModel);
        summaryPanel.setOutputMarkupId(true);
        add(summaryPanel);

        WorkItemDetailsPanel workItemDetailsPanel = new WorkItemDetailsPanel(ID_WORK_ITEM_DETAILS, caseWorkItemModel) {
            @Override
            protected PrismObject<? extends FocusType> getPowerDonor() {
                return PageCaseWorkItem.this.getPowerDonor();
            }
        };
        workItemDetailsPanel.setOutputMarkupId(true);
        add(workItemDetailsPanel);

        AjaxButton back = new AjaxButton(ID_BACK_BUTTON, createStringResource("pageCase.button.back")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed();
            }
        };
        back.setOutputMarkupId(true);
        add(back);

        CaseWorkItemActionsPanel actionsPanel = new CaseWorkItemActionsPanel(ID_CASE_WORK_ITEM_ACTIONS_PANEL, caseWorkItemModel){
            private static final long serialVersionUID = 1L;

            @Override
            protected AbstractWorkItemOutputType getWorkItemOutput(boolean approved){
                return super.getWorkItemOutput(approved)
                        .comment(workItemDetailsPanel.getApproverComment())
                        .evidence(workItemDetailsPanel.getWorkItemEvidence());
            }

            @Override
            protected WorkItemDelegationRequestType getDelegationRequest(UserType delegate) {
                return super.getDelegationRequest(delegate)
                        .comment(workItemDetailsPanel.getApproverComment());
            }

            @Override
            protected Component getCustomForm(){
                return workItemDetailsPanel.getCustomForm();
            }

            @Override
            public PrismObject<UserType> getPowerDonor() {
                return PageCaseWorkItem.this.getPowerDonor();
            }

        };
        actionsPanel.setOutputMarkupId(true);
        actionsPanel.add(new VisibleBehaviour(() -> caseWorkItemModel.getObject().getCloseTimestamp() == null));
        add(actionsPanel);

    }

    private void cancelPerformed() {
        redirectBack();
    }

    protected PrismObject<UserType> getPowerDonor(){
        if (pageParameters != null && pageParameters.get(PageAttorneySelection.PARAMETER_DONOR_OID) != null &&
                StringUtils.isNotEmpty(pageParameters.get(PageAttorneySelection.PARAMETER_DONOR_OID).toString())){
            String powerDonorOid = pageParameters.get(PageAttorneySelection.PARAMETER_DONOR_OID).toString();
            if (StringUtils.isEmpty(powerDonorOid)){
                return null;
            }
            Task task = createSimpleTask(OPERATION_LOAD_DONOR);
            OperationResult result = task.getResult();

            PrismObject<UserType> donor = WebModelServiceUtils.loadObject(UserType.class, powerDonorOid,
                    new ArrayList<>(), PageCaseWorkItem.this, task, result);

            return donor;

        }
        return null;
    }
}

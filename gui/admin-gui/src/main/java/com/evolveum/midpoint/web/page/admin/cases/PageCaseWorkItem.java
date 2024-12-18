/*
 * Copyright (C) 2010-2020 Evolveum et al. and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.workflow.CaseWorkItemSummaryPanel;
import com.evolveum.midpoint.web.page.admin.workflow.PageAttorneySelection;
import com.evolveum.midpoint.web.page.admin.workflow.WorkItemDetailsPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
                        description = "PageCaseWorkItem.auth.caseWorkItem.description") })
public class PageCaseWorkItem extends PageAdminCaseWorkItems {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageCaseWorkItem.class.getName() + ".";
    private static final String OPERATION_LOAD_CASE = DOT_CLASS + "loadCase";
    private static final String OPERATION_LOAD_DONOR = DOT_CLASS + "loadPowerDonor";

    private static final Trace LOGGER = TraceManager.getTrace(PageCaseWorkItem.class);
    private static final String ID_WORK_ITEM_DETAILS = "workItemDetails";
    private static final String ID_SUMMARY_PANEL = "summaryPanel";
    private static final String ID_CASE_WORK_ITEM_ACTIONS_PANEL = "caseWorkItemActionsPanel";
    private static final String ID_BACK_BUTTON = "backButton";

    private final LoadableModel<CaseWorkItemType> caseWorkItemModel;

    private LoadableModel<CaseType> caseModel;
    private WorkItemId workItemId;

    public PageCaseWorkItem(PageParameters parameters) {
        super(parameters);

        String caseWorkItemId = parameters.get(OnePageParameterEncoder.PARAMETER).toString();
        if (StringUtils.isEmpty(caseWorkItemId)) {
            throw new IllegalStateException("Work item ID not specified.");
        }
        try {
            workItemId = WorkItemId.create(caseWorkItemId);
        } catch (IllegalStateException e) {
            getSession().error(getString("PageCaseWorkItem.couldNotGetCase.runtime"));
            throw redirectBackViaRestartResponseException();
        }

        caseModel = new LoadableModel<>(false) {
            @Override
            protected CaseType load() {
                return loadCaseIfNecessary();
            }
        };

        caseWorkItemModel = new LoadableModel<>(false) {
            @Override
            protected CaseWorkItemType load() {
                return loadCaseWorkItemIfNecessary();
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private CaseType loadCaseIfNecessary() {
        if (caseModel.isLoaded()) {
            return caseModel.getObject();
        }
        Task task = createSimpleTask(OPERATION_LOAD_CASE);
        OperationResult result = task.getResult();
        CaseType caseInstance = null;
        try {
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
                throw new ObjectNotFoundException(
                        "No case work item found for case " + workItemId.getCaseOid() + " with id " + workItemId.getId(),
                        CaseWorkItemType.class,
                        workItemId.asString());
            }
            for (CaseWorkItemType caseWorkItemType : caseWorkItemList) {
                if (caseWorkItemType.getId().equals(workItemId.getId())) {
                    return caseWorkItemType;
                }
            }
        } catch (ObjectNotFoundException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get case work item because it does not exist. (It might have been already completed or deleted.)", ex);
        } catch (NumberFormatException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse case work item id.", ex);
        }
        getSession().error(getString("PageCaseWorkItem.couldNotGetCaseWorkItem"));
        throw redirectBackViaRestartResponseException();
    }

    private void initLayout() {
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

        CaseWorkItemActionsPanel actionsPanel = new CaseWorkItemActionsPanel(ID_CASE_WORK_ITEM_ACTIONS_PANEL, caseWorkItemModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected WorkItemDelegationRequestType getDelegationRequest(UserType delegate) {
                return super.getDelegationRequest(delegate)
                        .comment(WorkItemTypeUtil.getComment(getModelObject()));
            }

            @Override
            protected Component getCustomForm() {
                return workItemDetailsPanel.getCustomForm();
            }

            @Override
            protected void addAriaDescribedByForButton(AjaxButton workItemApproveButton) {
                workItemApproveButton.add(AttributeAppender.append(
                        "aria-describedby",
                        workItemDetailsPanel.getDeltasPanel().getMarkupId() + " " +
                                workItemDetailsPanel.getReasonPanel().getMarkupId() + " " +
                                workItemDetailsPanel.getCommentPanel().getMarkupId()));
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

    protected PrismObject<UserType> getPowerDonor() {
        String powerDonorOid = getPowerDonorOid();
        if (StringUtils.isEmpty(powerDonorOid)) {
            return null;
        }
        Task task = createSimpleTask(OPERATION_LOAD_DONOR);
        OperationResult result = task.getResult();

        return WebModelServiceUtils.loadObject(UserType.class, powerDonorOid, new ArrayList<>(), this, task, result);
    }

    //TODO WTF?
//    public PageParameters getPageParameters() {
//        PageParameters pageParameters = getPageParameters();
//        if (pageParameters == null || StringUtils.isEmpty(pageParameters.get(OnePageParameterEncoder.PARAMETER).toString())) {
//            pageParameters = new PageParameters();
//            CaseWorkItemType workItem = caseWorkItemModel.getObject();
//            CaseType parentCase = CaseTypeUtil.getCase(workItem);
//            pageParameters.add(OnePageParameterEncoder.PARAMETER, WorkItemId.createWorkItemId(parentCase.getOid(), workItem.getId()));
//        }
//        return pageParameters;
//    }


}
